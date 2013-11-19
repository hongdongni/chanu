package com.chanapps.four.component;

import android.content.Context;
import android.preference.PreferenceManager;
import com.chanapps.four.activity.SettingsActivity;

/**
 * Created with IntelliJ IDEA.
 * User: johnarleyburns
 * Date: 11/15/13
 * Time: 7:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class URLFormatComponent {

    public static final String ANIME_IMAGE_SEARCH_URL_FORMAT = "//iqdb.org/?url=%s";
    public static final String CHAN_AUTH_URL = "//sys.4chan.org/auth";
    public static final String CHAN_CATALOG_API_URL_FORMAT = "//api.4chan.org/%s/catalog.json";
    public static final String CHAN_COUNTRY_IMAGE_URL_FORMAT = "//static.4chan.org/image/country/%s.gif";
    public static final String CHAN_FRONTPAGE_URL = "//www.4chan.org/";
    public static final String CHAN_IMAGE_URL_FORMAT = "//images.4chan.org/%s/src/%d%s";
    public static final String CHAN_PAGE_API_URL_FORMAT = "//api.4chan.org/%s/%d.json";
    public static final String CHAN_PASS_PURCHASE_URL = "//www.4chan.org/pass";
    public static final String CHAN_POL_COUNTRY_IMAGE_URL_FORMAT = "//static.4chan.org/image/country/troll/%s.gif";
    public static final String CHAN_POST_URL_DELETE_FORMAT = "//sys.4chan.org/%s/imgboard.php";
    public static final String CHAN_POST_URL_FORMAT = "//sys.4chan.org/%s/post";
    public static final String CHAN_SPOILER_IMAGE_URL_FORMAT = "//static.4chan.org/image/spoiler-%s.png";
    public static final String CHAN_SPOILER_NUMBERED_IMAGE_URL_FORMAT = "//static.4chan.org/image/spoiler-%s%d.png";
    public static final String CHAN_THREAD_URL_FORMAT = "//api.4chan.org/%s/res/%d.json";
    public static final String CHAN_THUMBS_URL_FORMAT = "//0.thumbs.4chan.org/%s/thumb/%ds.jpg";
    public static final String CHAN_WEB_BOARD_URL_FORMAT = "//boards.4chan.org/%s/";
    public static final String CHAN_WEB_POST_URL_FORMAT = "//boards.4chan.org/%s/res/%d#p%d";
    public static final String CHAN_WEB_THREAD_URL_FORMAT = "//boards.4chan.org/%s/res/%d";
    public static final String GERMAN_TRANSLATOR_URL = "//www.reddit.com/user/le_avx";
    public static final String GITHUB_ABPTR_URL = "//github.com/chrisbanes/ActionBar-PullToRefresh";
    public static final String GITHUB_CHAN_API_URL = "//github.com/4chan/4chan-API";
    public static final String GITHUB_UIL_URL = "//github.com/nostra13/Android-Universal-Image-Loader";
    public static final String GOOGLE_CHANU_RECAPTCHA_URL = "//www.google.com/recaptcha/api/noscript?k=6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
    public static final String GOOGLE_CODE_COLOR_PICKER_URL = "//code.google.com/p/color-picker-view/";
    public static final String GOOGLE_IMAGE_SEARCH_URL = "//www.google.com/imghp";
    public static final String GOOGLE_MAPS_URL_FORMAT = "//maps.google.com/maps?f=q&q=(%f,%f)";
    public static final String GOOGLE_PLUS_CHANU_URL = "//plus.google.com/communities/107363899339170685863";
    public static final String GOOGLE_QUERY_IMAGE_URL_FORMAT = "//www.google.com/search?safe=off&site=imghp&tbm=isch&source=hp&q=%s";
    //public static final String GOOGLE_RECAPTCHA_API_URL_FORMAT = "//api.recaptcha.net/%s";
    public static final String GOOGLE_RECAPTCHA_API_URL_FORMAT = "//www.google.com/recaptcha/api/%s";
    public static final String GOOGLE_TRANSLATE_URL_FORMAT = "//translate.google.com/m?hl=%s&sl=auto&tl=%s&ie=UTF8&prev=_m&q=%s";
    public static final String SKREENED_CHANU_STORE_URL = "//www.skreened.com/chanapps/";
    public static final String TINEYE_IMAGE_SEARCH_URL_FORMAT = "//tineye.com/search?url=%s";

    public static final String MARKET_APP_URL = "market://details?id=com.chanapps.four.activity";
    public static final String MARKET_CORP_URL = "market://search?q=pub:Chanapps Software";

    public static String getUrl(Context context, String url) {
        String protocol =
                (PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .getBoolean(SettingsActivity.PREF_USE_HTTPS, true)) ? "https:" : "http:";
        return protocol + url;
    }

}
