package com.chanapps.four.activity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.*;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.chanapps.four.adapter.AbstractBoardCursorAdapter;
import com.chanapps.four.adapter.BoardGridCursorAdapter;
import com.chanapps.four.component.*;
import com.chanapps.four.data.*;
import com.chanapps.four.data.LastActivity;
import com.chanapps.four.fragment.*;
import com.chanapps.four.loader.BoardCursorLoader;
import com.chanapps.four.loader.ChanImageLoader;
import com.chanapps.four.service.NetworkProfileManager;
import com.chanapps.four.service.profile.NetworkProfile;
import com.chanapps.four.viewer.BoardGridViewer;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/27/12
 * Time: 12:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class ThreadActivity
        extends AbstractBoardSpinnerActivity
        implements ChanIdentifiedActivity
{

    public static final String TAG = ThreadActivity.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static final String BOARD_CODE = "boardCode";
    public static final String THREAD_NO = "threadNo";
    public static final String POST_NO = "postNo";
    protected static final int OFFSCREEN_PAGE_LIMIT = 1;
    protected static final int LOADER_ID = 1;
    protected static final String FIRST_VISIBLE_BOARD_POSITION = "firstVisibleBoardPosition";
    protected static final String FIRST_VISIBLE_BOARD_POSITION_OFFSET = "firstVisibleBoardPositionOffset";

    protected ChanBoard board;
    protected ThreadPagerAdapter mAdapter;
    protected ViewPager mPager;
    protected Handler handler;
    protected String query = "";
    protected MenuItem searchMenuItem;
    protected long postNo; // for direct jumps from latest post / recent images
    protected PullToRefreshAttacher mPullToRefreshAttacher;

    //tablet layout
    protected AbstractBoardCursorAdapter adapterBoardsTablet;
    protected AbsListView boardGrid;
    protected int firstVisibleBoardPosition = -1;
    protected int firstVisibleBoardPositionOffset = -1;
    protected boolean tabletTestDone = false;
    protected int columnWidth = 0;
    protected int columnHeight = 0;

    public static void startActivity(Context from, String boardCode, long threadNo, String query) {
        startActivity(from, boardCode, threadNo, 0, query);
    }

    public static void startActivity(Context from, String boardCode, long threadNo, long postNo, String query) {
        if (threadNo <= 0)
            BoardActivity.startActivity(from, boardCode, query);
        else if (postNo <= 0)
            from.startActivity(createIntent(from, boardCode, threadNo, query));
        else
            from.startActivity(createIntent(from, boardCode, threadNo, postNo, query));
    }

    public static Intent createIntent(Context context, final String boardCode, final long threadNo, String query) {
        return createIntent(context, boardCode, threadNo, 0, query);
    }

    public static Intent createIntent(Context context, final String boardCode,
                                      final long threadNo, final long postNo, String query) {
        Intent intent = new Intent(context, ThreadActivity.class);
        intent.putExtra(BOARD_CODE, boardCode);
        intent.putExtra(THREAD_NO, threadNo);
        intent.putExtra(POST_NO, postNo);
        intent.putExtra(SearchManager.QUERY, query);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        return intent;
    }

    @Override
    public boolean isSelfBoard(String boardAsMenu) {
        return false; // always jump to board
    }

    @Override
    protected int activityLayout() {
        return R.layout.thread_activity_layout;
    }

    @Override
    protected void createViews(Bundle bundle) {
        // first get all the variables
        if (bundle != null)
            onRestoreInstanceState(bundle);
        else
            setFromIntent(getIntent());
        if (DEBUG) Log.i(TAG, "onCreate /" + boardCode + "/" + threadNo + " q=" + query);
        if (boardCode == null || boardCode.isEmpty())
            boardCode = ChanBoard.META_BOARD_CODE;
        if (threadNo <= 0)
            redirectToBoard();

        board = ChanFileStorage.loadBoardData(this, boardCode);
        mPullToRefreshAttacher = new PullToRefreshAttacher(this);

        if (onTablet())
            createAbsListView();
        if (board != null && !board.defData && board.threads.length > 0) {
            if (DEBUG) Log.i(TAG, "createViews() calling initLoader");
            if (onTablet())
                getSupportLoaderManager().initLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
            createPager();
        }
        else {
            if (DEBUG) Log.i(TAG, "Board not ready, postponing pager creation");
        }
    }

    protected void createPager() {
        mAdapter = new ThreadPagerAdapter(getSupportFragmentManager());
        mAdapter.setBoard(board);
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
        mPager.setAdapter(mAdapter);
        setCurrentItemToThread();
    }

    public void showThread(long threadNo) {
        this.threadNo = threadNo;
        setCurrentItemToThread();
    }

    protected void setCurrentItemToThread() {
        int pos = getCurrentThreadPos();
        if (pos != mPager.getCurrentItem() && pos >= 0 && pos < mAdapter.getCount())
            mPager.setCurrentItem(pos, false);
    }

    protected int getCurrentThreadPos() {
        return board.getThreadIndex(boardCode, threadNo);
    }

    /*
    protected String fragmentTag() {
        return fragmentTag(boardCode, threadNo, postNo);
    }

    public static String fragmentTag(String boardCode, long threadNo, long postNo) {
        return "/" + boardCode + "/" + threadNo + (postNo > 0 && postNo != threadNo ? "#p" + postNo : "");
    }
    */
    protected void redirectToBoard() { // backup in case we are missing stuff
        Log.e(TAG, "Empty board code, redirecting to board /" + boardCode + "/");
        Intent intent = BoardActivity.createIntent(this, boardCode, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(ChanBoard.BOARD_CODE, boardCode);
        bundle.putLong(ChanThread.THREAD_NO, threadNo);
        bundle.putString(SearchManager.QUERY, query);
        int boardPos = !onTablet() ? -1 : boardGrid.getFirstVisiblePosition();
        View boardView = !onTablet() ? null : boardGrid.getChildAt(0);
        int boardOffset = boardView == null ? 0 : boardView.getTop();
        bundle.putInt(FIRST_VISIBLE_BOARD_POSITION, boardPos);
        bundle.putInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET, boardOffset);
        if (DEBUG) Log.i(TAG, "onSaveInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        boardCode = bundle.getString(ChanBoard.BOARD_CODE);
        threadNo = bundle.getLong(ChanThread.THREAD_NO, 0);
        query = bundle.getString(SearchManager.QUERY);
        firstVisibleBoardPosition = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION);
        firstVisibleBoardPositionOffset = bundle.getInt(FIRST_VISIBLE_BOARD_POSITION_OFFSET);
        if (DEBUG) Log.i(TAG, "onRestoreInstanceState /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.i(TAG, "onNewIntent begin /" + intent.getStringExtra(ChanBoard.BOARD_CODE) + "/"
                + intent.getLongExtra(ChanThread.THREAD_NO, 0));
        setIntent(intent);
        setFromIntent(intent);
        if (DEBUG) Log.i(TAG, "onNewIntent end /" + boardCode + "/" + threadNo);
    }

    public void setFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data == null) {
            boardCode = intent.getStringExtra(ChanBoard.BOARD_CODE);
            threadNo = intent.getLongExtra(ChanThread.THREAD_NO, 0);
            postNo = intent.getLongExtra(ChanThread.POST_NO, 0);
            query = intent.getStringExtra(SearchManager.QUERY);
        }
        else {
            List<String> params = data.getPathSegments();
            String uriBoardCode = params.get(0);
            String uriThreadNo = params.get(1);
            if (ChanBoard.getBoardByCode(this, uriBoardCode) != null && uriThreadNo != null) {
                boardCode = uriBoardCode;
                threadNo = Long.valueOf(uriThreadNo);
                postNo = 0;
                query = "";
                if (DEBUG) Log.i(TAG, "loaded /" + boardCode + "/" + threadNo + " from url intent");
            }
            else {
                boardCode = ChanBoard.DEFAULT_BOARD_CODE;
                threadNo = 0;
                postNo = 0;
                query = "";
                if (DEBUG) Log.e(TAG, "Received invalid boardCode=" + uriBoardCode + " from url intent, using default board");
            }
        }
        if (DEBUG) Log.i(TAG, "setFromIntent /" + boardCode + "/" + threadNo);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.i(TAG, "onStart /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.i(TAG, "onResume /" + boardCode + "/" + threadNo);
        if (handler == null)
            handler = new Handler();
        if (board == null || !board.link.equals(boardCode) || mPager == null) { // recreate pager
            board = ChanFileStorage.loadBoardData(this, boardCode);
            createPager();
        }
        else {
            setCurrentItemToThread();
        }
        invalidateOptionsMenu(); // for correct spinner display
        ChanActivityId lastActivityId = NetworkProfileManager.instance().getActivityId();
        ChanActivityId activityId = getChanActivityId();
        if (!activityId.equals(lastActivityId))
            NetworkProfileManager.instance().activityChange(this);
        if (onTablet()
                && !getSupportLoaderManager().hasRunningLoaders()
                && (adapterBoardsTablet == null || adapterBoardsTablet.getCount() == 0)) {
            if (DEBUG) Log.i(TAG, "onResume calling restartLoader");
            getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks); // board loader for tablet view
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.i(TAG, "onPause /" + boardCode + "/" + threadNo);
        handler = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.i(TAG, "onStop /" + boardCode + "/" + threadNo);
        handler = null;
        if (onTablet()) {
            if (DEBUG) Log.i(TAG, "onStop calling destroyLoader");
            getSupportLoaderManager().destroyLoader(LOADER_ID);
        }
        setProgress(false);
    }

    private void postReply(long postNos[]) {
        String replyText = "";
        for (long postNo : postNos) {
            replyText += ">>" + postNo + "\n";
        }
        postReply(replyText);
    }

    private void postReply(String replyText) {
        PostReplyActivity.startActivity(this, boardCode, threadNo, 0, ChanPost.planifyText(replyText));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_menu, menu);
        searchMenuItem = menu.findItem(R.id.search_menu);
        SearchActivity.createSearchView(this, searchMenuItem);
        return super.onCreateOptionsMenu(menu);
    }

    public ChanActivityId getChanActivityId() {
        return new ChanActivityId(LastActivity.THREAD_ACTIVITY, boardCode, threadNo, postNo, query);
    }

    @Override
    public void refresh() {
        invalidateOptionsMenu(); // in case spinner needs to be reset
        ThreadFragment fragment = getCurrentFragment();
        if (fragment != null)
            fragment.onRefresh();
        if (handler != null && onTablet())
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) Log.i(TAG, "refreshBoard() restarting loader");
                    getSupportLoaderManager().restartLoader(LOADER_ID, null, loaderCallbacks);
                }
            });
    }

    public void refreshFragment(String boardCode, long threadNo, String message) {
        if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo);
        ChanBoard fragmentBoard = ChanFileStorage.loadBoardData(getApplicationContext(), boardCode);
        if (mPager == null && !fragmentBoard.defData) {
            if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " board loaded, creating pager");
            createPager();
        }
        if (mPager == null) {
            if (DEBUG) Log.i(TAG, "refreshFragment /" + boardCode + "/" + threadNo + " skipping, null pager");
            return;
        }
        if (mAdapter == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " skipping, null adapter");
            return;
        }
        int current = mPager.getCurrentItem();
        int delta = mPager.getOffscreenPageLimit();
        for (int i = current - delta; i < current + delta + 1; i++)
            refreshFragmentAtPosition(boardCode, threadNo, i, i == current ? message : null);
    }

    protected void refreshFragmentAtPosition(String boardCode, long threadNo, int pos, String message) {
        ThreadFragment fragment;
        ChanActivityId data;
        if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos);
        if (pos < 0 || pos >= mAdapter.getCount()) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                + " out of bounds, skipping");
            return;
        }
        if ((fragment = getFragmentAtPosition(pos)) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null fragment at position, skipping");
            return;
        }
        if ((data = fragment.getChanActivityId()) == null) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " null getChanActivityId(), skipping");
            return;
        }
        if (data.boardCode == null || !data.boardCode.equals(boardCode) || data.threadNo != threadNo) {
            if (DEBUG) Log.i(TAG, "refreshFragmentAtPosition /" + boardCode + "/" + threadNo + " pos=" + pos
                    + " unmatching data=/" + data.boardCode + "/" + data.threadNo + ", skipping");
            return;
        }
        if (DEBUG) Log.i(TAG, "refreshing fragment /" + boardCode + "/" + threadNo + " pos=" + pos);
        fragment.refreshThread(message);
    }

    @Override
    public void closeSearch() {
        if (DEBUG) Log.i(TAG, "closeSearch /" + boardCode + "/" + threadNo + " q=" + query);
        if (searchMenuItem != null)
            searchMenuItem.collapseActionView();
    }

    @Override
    public Handler getChanHandler() {
        return handler;
    }

    @Override
    protected void createActionBar() {
        super.createActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    protected Activity getActivity() {
        return this;
    }

    protected Context getActivityContext() {
        return this;
    }

    protected ChanIdentifiedActivity getChanActivity() {
        return this;
    }

    public ThreadFragment getCurrentFragment() {
        if (mPager == null)
            return null;
        int i = mPager.getCurrentItem();
        return getFragmentAtPosition(i);
    }

    protected ThreadFragment getFragmentAtPosition(int pos) {
        if (mAdapter == null)
            return null;
        else
            return mAdapter.getCachedItem(pos);
    }

    public class ThreadPagerAdapter extends FragmentStatePagerAdapter {
        protected String boardCode;
        protected ChanBoard board;
        protected int count;
        protected Map<Integer,WeakReference<ThreadFragment>> fragments
                = new HashMap<Integer, WeakReference<ThreadFragment>>();
        public ThreadPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        public void setBoard(ChanBoard board) {
            if (board == null || board.threads == null)
                throw new UnsupportedOperationException("can't start pager with null board or null threads");
            this.boardCode = board.link;
            this.board = board;
            this.count = board.threads.length;
            notifyDataSetChanged();
        }
        @Override
        public void notifyDataSetChanged() {
            board = ChanFileStorage.loadBoardData(getBaseContext(), boardCode);
            count = board.threads.length;
            super.notifyDataSetChanged();
        }
        @Override
        public int getCount() {
            return count;
        }
        @Override
        public Fragment getItem(int pos) {
            if (pos < count)
                return createFragment(pos);
            else
                return null;
        }
        protected Fragment createFragment(int pos) {
            // get thread
            ChanPost thread = board.threads[pos];
            String boardCode = thread.board;
            long threadNo = thread.no;
            long postNo = 0;
            String query = "";
            // make fragment
            ThreadFragment fragment = new ThreadFragment();
            Bundle bundle = new Bundle();
            bundle.putString(BOARD_CODE, boardCode);
            bundle.putLong(THREAD_NO, threadNo);
            bundle.putLong(POST_NO, postNo);
            bundle.putString(SearchManager.QUERY, query);
            fragment.setArguments(bundle);
            fragment.setHasOptionsMenu(true);
            return fragment;
        }
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object object = super.instantiateItem(container, position);
            fragments.put(position, new WeakReference<ThreadFragment>((ThreadFragment)object));
            return object;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            fragments.remove(position);
        }
        public ThreadFragment getCachedItem(int position) {
            WeakReference<ThreadFragment> ref = fragments.get(position);
            if (ref == null)
                return null;
            else
                return ref.get();
        }
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            ThreadFragment fragment = (ThreadFragment)object;
            if (primaryItem != fragment && fragment.getChanActivityId().threadNo > 0) {
                if (DEBUG) Log.i(TAG, "setPrimaryItem pos=" + position + " obj=" + fragment
                        + " rebinding mPullToRefreshAttacher");
                if (primaryItem != null)
                    primaryItem.setPullToRefreshAttacher(null);
                primaryItem = fragment;
                fragment.setPullToRefreshAttacher(mPullToRefreshAttacher);
                if (onTablet()) {
                    if (DEBUG) Log.i(TAG, "smooth scrolling to position=" + position);
                    boardGrid.setSelection(position);
                }
            }
            super.setPrimaryItem(container, position, object);
        }

        protected ThreadFragment primaryItem = null;
    }

    public void setProgressForFragment(String boardCode, long threadNo, boolean on) {
        ThreadFragment fragment = getCurrentFragment();
        if (fragment == null)
            return;
        ChanActivityId data = fragment.getChanActivityId();
        if (data == null)
            return;
        if (data.boardCode == null)
            return;
        if (!data.boardCode.equals(boardCode))
            return;
        if (data.threadNo != threadNo)
            return;
        setProgress(on);
        if (mPullToRefreshAttacher != null && !on) {
            if (DEBUG) Log.i(TAG, "mPullToRefreshAttacher.setRefreshComplete()");
            mPullToRefreshAttacher.setRefreshComplete();
        }
    }


    protected void initTablet() {
        if (!tabletTestDone) {
            boardGrid = (AbsListView)findViewById(R.id.board_grid_view_tablet);
            tabletTestDone = true;
        }
    }

    // tablet
    public boolean onTablet() {
        initTablet();
        return boardGrid != null;
    }

    protected void createAbsListView() {
        initTablet();
        ImageLoader imageLoader = ChanImageLoader.getInstance(getActivityContext());
        columnWidth = ChanGridSizer.getCalculatedWidth(
                getResources().getDimensionPixelSize(R.dimen.BoardGridViewTablet_layout_width),
                1,
                getResources().getDimensionPixelSize(R.dimen.BoardGridView_spacing));
        columnHeight = 2 * columnWidth;
        adapterBoardsTablet = new BoardGridCursorAdapter(getActivityContext(), viewBinder,
                columnWidth, columnHeight);
        boardGrid.setAdapter(adapterBoardsTablet);
        boardGrid.setOnItemClickListener(boardGridListener);
        boardGrid.setOnScrollListener(new PauseOnScrollListener(imageLoader, true, true));
    }

    protected void onBoardsTabletLoadFinished(Cursor data) {
        if (boardGrid == null)
            createAbsListView();
        this.adapterBoardsTablet.swapCursor(data);
        // retry load if maybe data wasn't there yet
        if (data != null && data.getCount() < 1 && handler != null) {
            if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " data count=0");
            NetworkProfile.Health health = NetworkProfileManager.instance().getCurrentProfile().getConnectionHealth();
            if (health == NetworkProfile.Health.NO_CONNECTION || health == NetworkProfile.Health.BAD) {
                String msg = String.format(getString(R.string.mobile_profile_health_status),
                        health.toString().toLowerCase().replaceAll("_", " "));
                Toast.makeText(getActivityContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
        else if (firstVisibleBoardPosition >= 0) {
            if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " firstVisibleBoardPosition=" + firstVisibleBoardPosition);
            //if (boardGrid instanceof ListView)
            //    ((ListView)boardGrid).setSelectionFromTop(firstVisibleBoardPosition, firstVisibleBoardPositionOffset);
            //else
            boardGrid.setSelection(firstVisibleBoardPosition);
            firstVisibleBoardPosition = -1;
            firstVisibleBoardPositionOffset = -1;
        }
        else if (threadNo > 0) {
            Cursor cursor = adapterBoardsTablet.getCursor();
            cursor.moveToPosition(-1);
            boolean found = false;
            int pos = 0;
            while (cursor.moveToNext()) {
                long threadNoAtPos = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (threadNoAtPos == threadNo) {
                    found = true;
                    break;
                }
                pos++;
            }
            if (found) {
                if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " pos=" + pos);
                boardGrid.setSelection(pos);
            }
            else {
                if (DEBUG) Log.i(TAG, "onBoardsTabletLoadFinished threadNo=" + threadNo + " thread not found");
            }
        }
        //setProgress(false);
    }

    protected LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.i(TAG, "onCreateLoader /" + boardCode + "/ id=" + id);
            //setProgress(true);
            return new BoardCursorLoader(getActivityContext(), boardCode, "");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.i(TAG, "onLoadFinished /" + boardCode + "/ id=" + loader.getId()
                    + " count=" + (data == null ? 0 : data.getCount()) + " loader=" + loader);
            onBoardsTabletLoadFinished(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.i(TAG, "onLoaderReset /" + boardCode + "/ id=" + loader.getId());
            adapterBoardsTablet.swapCursor(null);
        }
    };

    protected AbstractBoardCursorAdapter.ViewBinder viewBinder = new AbstractBoardCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            return BoardGridViewer.setViewValue(view, cursor, boardCode, columnWidth, columnHeight, null, null);
        }
    };

    protected AdapterView.OnItemClickListener boardGridListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int flags = cursor.getInt(cursor.getColumnIndex(ChanThread.THREAD_FLAGS));
            final String title = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_SUBJECT));
            final String desc = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_TEXT));
            if ((flags & ChanThread.THREAD_FLAG_AD) > 0) {
                final String clickUrl = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_CLICK_URL));
                ActivityDispatcher.launchUrlInBrowser(getActivityContext(), clickUrl);
            }
            else if ((flags & ChanThread.THREAD_FLAG_TITLE) > 0
                    && title != null && !title.isEmpty()
                    && desc != null && !desc.isEmpty()) {
                (new GenericDialogFragment(title.replaceAll("<[^>]*>", " "), desc))
                        .show(getSupportFragmentManager(), ThreadFragment.TAG);
            }
            else if ((flags & ChanThread.THREAD_FLAG_BOARD) > 0) {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                BoardActivity.startActivity(getActivityContext(), boardLink, "");
            }
            else {
                final String boardLink = cursor.getString(cursor.getColumnIndex(ChanThread.THREAD_BOARD_CODE));
                final long threadNoLink = cursor.getLong(cursor.getColumnIndex(ChanThread.THREAD_NO));
                if (boardCode.equals(boardLink) && threadNo == threadNoLink) { // already on this, do nothing
                } else if (boardCode.equals(boardLink)) { // just redisplay right tab
                    showThread(threadNoLink);
                } else {
                    ThreadActivity.startActivity(getActivityContext(), boardLink, threadNoLink, "");
                }
            }
        }
    };

    public void notifyBoardChanged() {
        if (DEBUG) Log.i(TAG, "notifyBoardChanged() /" + boardCode + "/ recreating pager");
        if (onTablet())
            createAbsListView();
        createPager();
    }

}
