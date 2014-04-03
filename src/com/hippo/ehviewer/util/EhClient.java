package com.hippo.ehviewer.util;

import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.DownloadInfo;
import com.hippo.ehviewer.ListMangaDetail;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.MangaDetail;
import com.hippo.ehviewer.PageList;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.network.ShapreCookieStore;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;

import android.content.Context;
import android.util.Log;

// TODO Stringbuffer to StringBuild

public class EhClient {
    
    private static final String TAG = "EhClient";
    
    private static String API_URL = "http://g.e-hentai.org/api.php";
    private static String loginUrl = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    
    private static final String E_HENTAI_LIST_HEADER = "http://g.e-hentai.org/";
    private static final String EXHENTAI_LIST_HEADER = "http://exhentai.org/";
    public static final String E_HENTAI_DETAIL_HEADER = "http://g.e-hentai.org/g/";
    public static final String EXHENTAI_DETAIL_HEADER = "http://exhentai.org/g/";
    
    public static final String UPDATE_URL = "http://ehviewersu.appsp0t.com/";
    
    
    public static final String UPDATE_URI_QINIU = "http://ehviewer.qiniudn.com/";
    
    private static final int RETRY_TIMES = 5;
    
    public static String listHeader;
    public static String detailHeader;
    
    private boolean mLogin = false;
    private String name;
    private String logoutUrl;
    
    private Context mContext;
    private ThreadPool mThreadPool;

    public interface OnCheckNetworkListener {
        public void onSuccess();
        public void onFailure(String eMsg);
    }
    
    public interface OnCheckUpdateListener {
        public void onSuccess(String pageContext);
        public void onFailure(String eMsg);
    }
    
    public interface OnLogoutListener {
        public void onSuccess();
        public void onFailure(String eMsg);
    }

    public interface OnLoginListener {
        public void onSuccess();
        public void onFailure(String eMsg);
    }

    public interface OnCheckLoginListener {
        public void onSuccess();
        public void onFailure(String eMsg);
    }

    public interface OnGetManagaListListener {
        public void onSuccess(Object checkFlag, ArrayList<ListMangaDetail> lmdArray,
                int indexPerPage, int maxPage);
        public void onFailure(Object checkFlag, String eMsg);
    }

    public interface OnGetManagaDetailListener {
        public void onSuccess(MangaDetail md);
        public void onFailure(String eMsg);
    }

    public interface OnGetPageListListener {
        public void onSuccess(Object checkFlag, PageList pageList);
        public void onFailure(Object checkFlag, String eMsg);
    }

    public interface OnGetManagaUrlListener {
        public void onSuccess(Object checkFlag, String[] arg);
        public void onFailure(Object checkFlag, String eMsg);
    }

    public interface OnGetImageListener {
        public void onSuccess(Object checkFlag, Object res);
        public void onFailure(String eMsg);
    }
    
    public interface OnGetGalleryMetadataListener {
        public void onSuccess(Map<String, ListMangaDetail> lmds);
        public void onFailure(String eMsg);
    }
    
    public interface OnGetGalleryTokensListener {
        public void onSuccess(Map<String, String> tokens);
        public void onFailure(String eMsg);
    }
    
    public EhClient(Context context) {
        mContext = context;
        
        CookieManager cookieManager = new CookieManager(new ShapreCookieStore(mContext), CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        
        if (Config.isExhentai()) {
            listHeader = EXHENTAI_LIST_HEADER;
            detailHeader = EXHENTAI_DETAIL_HEADER;
        } else {
            listHeader = E_HENTAI_LIST_HEADER;
            detailHeader = E_HENTAI_DETAIL_HEADER;
        }
        
        mThreadPool = ((AppContext)(context.getApplicationContext())).getNetworkThreadPool();
    }
    
    public static void setHeader(boolean isExhentai) {
        Config.exhentai(isExhentai);
        if (isExhentai) {
            listHeader = EXHENTAI_LIST_HEADER;
            detailHeader = EXHENTAI_DETAIL_HEADER;
        } else {
            listHeader = E_HENTAI_LIST_HEADER;
            detailHeader = E_HENTAI_DETAIL_HEADER;
        }
    }

    public boolean isLogin() {
        return mLogin;
    }

    public String getUsername() {
        return name;
    }

    // Login
    private class LoginRunnable implements Runnable {
        
        private String username;
        private String password;
        
        public String eMsg;
        public boolean ok;
        
        public LoginRunnable(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        @Override
        public void run() {
            ok = false;
            String[][] args = new String[][] {
                    new String[] { "UserName", username },
                    new String[] { "PassWord", password },
                    new String[] { "submit", "Log+me+in" },
                    new String[] { "CookieDate", "1" },
                    new String[] { "temporary_https", "on" }};
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.post(loginUrl, args);
            if (pageContent != null) {
                if (pageContent.contains("<p>You are now logged in as: "))
                    ok = true;
                else
                    eMsg = mContext.getString(R.string.em_logout_error);
            } else
                eMsg = hp.getEMsg();
        }
    }
    
    public void login(String username, String password,
            final OnLoginListener listener) {
        
        final LoginRunnable task = new LoginRunnable(username, password);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.ok) {
                    if (listener != null)
                        listener.onSuccess();
                } else {
                    if (listener != null)
                        listener.onFailure(task.eMsg);
                }
            }
        });
    }

    // Check login get info and logout url
    private class checkLoginRunnable implements Runnable {
        
        public String eMsg;
        public boolean ok;

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(loginUrl);
            if (pageContent != null) {
                Pattern p = Pattern
                        .compile("<p class=\"home\"><b>Logged in as:  <a href=\"[^<>\"]+\">([^<>]+)</a></b> \\( <a href=\"([^<>\"]+)\">Log Out</a> \\)</p>");
                Matcher m = p.matcher(pageContent);
                if (m.find() && m.groupCount() == 2) {
                    ok = true;
                    mLogin = true;
                    name = m.group(1);
                    logoutUrl = StringEscapeUtils.unescapeHtml4(m.group(2));
                } else
                    eMsg = mContext.getString(R.string.em_check_login_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void checkLogin(final OnCheckLoginListener listener) {
        final checkLoginRunnable task = new checkLoginRunnable();
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.ok) {
                    mLogin = true;
                    if (listener != null)
                        listener.onSuccess();
                } else {
                    if (listener != null)
                        listener.onFailure(task.eMsg);
                }
            }
        });
    }
    
    // Logout
    private class LogoutRunnable implements Runnable {

        public String eMsg;
        public boolean ok;

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(logoutUrl);
            if (pageContent != null) {
                if (pageContent.contains("<p>You are now logged out<br />"))
                    ok = true;
                else
                    eMsg = mContext.getString(R.string.em_logout_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void logout(final OnLogoutListener listener) {
        final LogoutRunnable task = new LogoutRunnable();
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.ok) {
                    mLogin = false;
                    if (listener != null)
                        listener.onSuccess();
                } else {
                    if (listener != null)
                        listener.onFailure(task.eMsg);
                }
            }
        });
    }
    
    // Get Manga List
    private class GetManagaListRunnable implements Runnable {

        private String url;
        
        public ArrayList<ListMangaDetail> lmdArray;
        public int indexPerPage;
        public int maxPage;
        public String eMsg;

        public GetManagaListRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            lmdArray = null;
            indexPerPage = 25;
            maxPage = 0;

            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(url);

            if (pageContent != null) {
                boolean getPageCount = false;
                
                // Get indexPerPage and maxPage
                Pattern p = Pattern
                        .compile("</div><p class=\"ip\" style=\"[^<>\"]+\">Showing ([\\d|,]+)-([\\d|,]+) of ([\\d|,]+)</p>");
                Matcher m = p.matcher(pageContent);
                if (m.find() && m.groupCount() == 3) {
                    int startIndex = Integer.parseInt(m.group(1).replace(",",
                            ""));
                    int endIndex = Integer
                            .parseInt(m.group(2).replace(",", ""));
                    int maxIndex = Integer
                            .parseInt(m.group(3).replace(",", ""));
                    if (endIndex != maxIndex)
                        indexPerPage = endIndex - startIndex + 1;
                    maxPage = (maxIndex + indexPerPage - 1) / indexPerPage;
                    // To continue get list
                    getPageCount = true;
                } else if (pageContent.contains("No hits found</p></div>")) {
                    lmdArray = new ArrayList<ListMangaDetail>();
                    maxPage = 0;
                } else if (pageContent.contains("JFIF")) { // sad panda
                    lmdArray = new ArrayList<ListMangaDetail>();
                    maxPage = -1;
                } else
                    eMsg = mContext.getString(R.string.em_parser_error);

                // Get list
                if (getPageCount) {
                    p = Pattern
                            .compile("alt=\"([\\w|\\-]+)\"[^<>]*/></a></td><td[^<>]*>([\\w|\\-|\\s|:]+)</td><td[^<>]*><div[^<>]*><div[^<>]+>(?:<img[^<>]*src=\"([^<>\"]+)\"[^<>]*alt=\"([^<>]+)\" style[^<>]*/>|init~([^<>\"~]+~[^<>\"~]+)~([^<>]+))</div>(?:<div[^<>]*>(?:<div[^<>]*><img[^<>]*/></div>)?(?:<div[^<>]*>(?:<a[^<>]*>)?<img[^<>]*/>(?:</a>)?</div>)?</div>)?<div[^<>]*><a[^<>\"]*href=\"([^<>\"]*)\"[^<>]*>([^<>]+)</a></div><div[^<>]*><div[^<>]*style=\"([^<>\"]+)\">");
                    m = p.matcher(pageContent);

                    while (m.find()) {
                        if (lmdArray == null)
                            lmdArray = new ArrayList<ListMangaDetail>();
                        ListMangaDetail lmd = new ListMangaDetail();

                        lmd.category = getType(m.group(1));
                        lmd.posted = m.group(2);
                        if (m.group(3) == null) {
                            lmd.thumb = "http://"
                                    + m.group(5).replace('~', '/');
                            lmd.title = StringEscapeUtils.unescapeHtml4(m
                                    .group(6));
                        } else {
                            lmd.thumb = m.group(3);
                            lmd.title = StringEscapeUtils.unescapeHtml4(m
                                    .group(4));
                        }
                        // http://g.e-hentai.org/g/671107/b726a0b986/

                        // Get gid and token
                        Pattern pattern = Pattern
                                .compile("/(\\d+)/(\\w+)");
                        Matcher matcher = pattern.matcher(m.group(7));
                        if (matcher.find()) {
                            lmd.gid = matcher.group(1);
                            lmd.token = matcher.group(2);
                        } else
                            continue;
                        
                        String temp = StringEscapeUtils.unescapeHtml4(m
                                .group(8));
                        if (!lmd.title.equals(temp)) {
                            Log.w(TAG,
                                    "Maybe parser error, !lmd.name.equals(temp)");
                            Log.w(TAG, "first is " + lmd.title);
                            Log.w(TAG, "second is " + temp);
                            lmd.title = temp;
                        }
                        lmd.rating = getRate(m.group(9));
                        lmdArray.add(lmd);
                    }
                    if (lmdArray == null)
                        eMsg = mContext.getString(R.string.em_parser_error);
                }
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getManagaList(String url, final Object checkFlag,
            final OnGetManagaListListener listener) {
        
        final GetManagaListRunnable task = new GetManagaListRunnable(url);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.lmdArray != null) {
                    if (listener != null)
                        listener.onSuccess(checkFlag, task.lmdArray, task.indexPerPage, task.maxPage);
                } else {
                    if (listener != null)
                        listener.onFailure(checkFlag, task.eMsg);
                }
            }
        });
    }

    private static int getType(String rawType) {
        int type;
        if (rawType.equalsIgnoreCase("doujinshi"))
            type = ListUrls.DOUJINSHI;
        else if (rawType.equalsIgnoreCase("manga"))
            type = ListUrls.MANGA;
        else if (rawType.equalsIgnoreCase("artistcg"))
            type = ListUrls.ARTIST_CG;
        else if (rawType.equalsIgnoreCase("gamecg"))
            type = ListUrls.GAME_CG;
        else if (rawType.equalsIgnoreCase("western"))
            type = ListUrls.WESTERN;
        else if (rawType.equalsIgnoreCase("non-h"))
            type = ListUrls.NON_H;
        else if (rawType.equalsIgnoreCase("imageset"))
            type = ListUrls.IMAGE_SET;
        else if (rawType.equalsIgnoreCase("cosplay"))
            type = ListUrls.COSPLAY;
        else if (rawType.equalsIgnoreCase("asianporn"))
            type = ListUrls.ASIAN_PORN;
        else if (rawType.equalsIgnoreCase("misc"))
            type = ListUrls.MISC;
        else
            type = ListUrls.UNKNOWN;
        return type;
    }

    private static String getRate(String rawRate) {
        Pattern p = Pattern.compile("\\d+px");
        Matcher m = p.matcher(rawRate);
        int num1;
        int num2;
        int rate = 5;
        String re;
        if (m.find())
            num1 = Integer.parseInt(m.group().replace("px", ""));
        else
            return null;
        if (m.find())
            num2 = Integer.parseInt(m.group().replace("px", ""));
        else
            return null;
        rate = rate - num1 / 16;
        if (num2 == 21) {
            rate--;
            re = Integer.toString(rate);
            re = re + ".5";
        } else
            re = Integer.toString(rate);
        return re;
    }

    // Get Manga Detail
    private class GetManagaDetailRunnable implements Runnable {

        private String url;
        private MangaDetail md;
        
        public boolean ok;
        public String eMsg;

        public GetManagaDetailRunnable(String url, MangaDetail md) {
            this.url = url;
            this.md = md;
        }

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(url);
            if (pageContent != null) {
                Pattern p = Pattern
                        .compile("<div id=\"gdc\"><a href=\"[^<>\"]+\"><img[^<>]*alt=\"([\\w|\\-]+)\"[^<>]*/></a></div><div id=\"gdn\"><a href=\"[^<>\"]+\">([^<>]+)</a>.+Posted:</td><td[^<>]*>([\\w|\\-|\\s|:]+)</td></tr><tr><td[^<>]*>Images:</td><td[^<>]*>([\\d]+) @ ([\\w|\\.|\\s]+)</td></tr><tr><td[^<>]*>Resized:</td><td[^<>]*>([^<>]+)</td></tr><tr><td[^<>]*>Parent:</td><td[^<>]*>(?:<a[^<>]*>)?([^<>]+)(?:</a>)?</td></tr><tr><td[^<>]*>Visible:</td><td[^<>]*>([^<>]+)</td></tr><tr><td[^<>]*>Language:</td><td[^<>]*>([^<>]+)</td></tr>(?:</tbody>)?</table></div><div[^<>]*><table>(?:<tbody>)?<tr><td[^<>]*>Rating:</td><td[^<>]*><div[^<>]*style=\"([^<>]+)\"[^<>]*><img[^<>]*></div></td><td[^<>]*>\\(<span[^<>]*>([\\d]+)</span>\\)</td></tr><tr><td[^<>]*>([^<>]+)</td>.+<p class=\"ip\">Showing ([\\d|,]+) - ([\\d|,]+) of ([\\d|,]+) images</p>.+<div id=\"gdt\"><div[^<>]*>(?:<div[^<>]*>)?<a[^<>]*href=\"([^<>\"]+)\"[^<>]*>");
                Matcher m = p.matcher(pageContent);
                String offensiveKeystring = "<p>(And if you choose to ignore this warning, you lose all rights to complain about it in the future.)</p>";
                String piningKeyString = "<p>This gallery is pining for the fjords.</p>";
                if (m.find()) {
                    ok = true;
                    md.category = getType(m.group(1));
                    md.uploader = m.group(2);
                    md.posted = m.group(3);
                    md.pages = m.group(4);
                    md.size = m.group(5);
                    md.resized = m.group(6);
                    md.parent = m.group(7);
                    md.visible = m.group(8);
                    md.language = m.group(9);
                    md.people = m.group(11);
                    
                    Pattern pattern = Pattern.compile("([\\d|\\.]+)");
                    Matcher matcher = pattern.matcher(m.group(12));
                    if (matcher.find())
                        md.rating = matcher.group(1);
                    else
                        md.rating = mContext.getString(R.string.detail_not_rated);
                    md.firstPage = m.group(16);
                    md.previewPerPage = Integer.parseInt(m.group(14).replace(",",
                            ""))
                            - Integer.parseInt(m.group(13).replace(",", ""))
                            + 1;
                    int total = Integer.parseInt(m.group(15).replace(",", ""));

                    md.previewSum = (total + md.previewPerPage - 1) / md.previewPerPage;

                    // New pageListArray
                    md.previewLists = new PageList[md.previewSum];
                    // Add page 1 pagelist
                    PageList pageList = getPageList(pageContent);
                    md.previewLists[0] = pageList;
                    md.tags = getTags(pageContent);
                } else if (pageContent.contains(offensiveKeystring)) {
                    ok = true;
                    md.firstPage = "offensive";
                } else if (pageContent.contains(piningKeyString)) {
                    ok = true;
                    md.firstPage = "pining";
                } else
                    eMsg = mContext.getString(R.string.em_parser_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getManagaDetail(String url, final MangaDetail md,
            final OnGetManagaDetailListener listener) {
        
        final GetManagaDetailRunnable task = new GetManagaDetailRunnable(url, md);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.ok) {
                    if (listener != null)
                        listener.onSuccess(md);
                } else {
                    if (listener != null)
                        listener.onFailure(task.eMsg);
                }
            }
        });
    }
    
    public String[][] getTags(String pageContent) {
        ArrayList<String[]> list = new ArrayList<String[]>();
        Pattern p = Pattern
                .compile("<tr><td[^<>]*>([^<>]+):</td><td>(?:<div[^<>]*><a[^<>]*>[^<>]*</a>[^<>]*<span[^<>]*>\\d+</span>[^<>]*</div>)+</td></tr>");
        Matcher m = p.matcher(pageContent);
        while (m.find()) {
            String groupName = m.group(1);
            String[] group = getTagGroup(m.group(0));
            if (groupName != null && group != null) {
                group[0] = groupName;
                list.add(group);
            }
        }
        if (list.size() == 0)
            return null;
        String[][] groups = new String[list.size()][];
        int i = 0;
        for (String[] item : list) {
            groups[i] = item;
            i++;
        }
        return groups;
    }
    
    public String[] getTagGroup(String pageContent) {
        ArrayList<String> list = new ArrayList<String>();
        Pattern p = Pattern.compile("<a[^<>]*>([^<>]+)</a>");
        Matcher m = p.matcher(pageContent);
        while (m.find()) {
            String str = m.group(1);
            if (str != null)
                list.add(str);
        }
        if (list.size() == 0)
            return null;
        String[] strs = new String[list.size() + 1];
        int i = 1;
        for (String str : list) {
            strs[i] = str;
            i++;
        }
        return strs;
    }
    
    // Get page list
    private class GetPageListRunnable implements Runnable {
        private String url;
        
        public PageList pageList;
        public String eMsg;

        public GetPageListRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            PageList pageList = null;
            
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(url);
            if (pageContent != null) {
                pageList = getPageList(pageContent);
                if (pageList == null)
                    eMsg = mContext.getString(R.string.em_parser_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getPageList(String url, final Object checkFlag,
            final OnGetPageListListener listener) {
        
        final GetPageListRunnable task = new GetPageListRunnable(url);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.pageList != null) {
                    if (listener != null)
                        listener.onSuccess(checkFlag, task.pageList);
                } else {
                    if (listener != null)
                        listener.onFailure(checkFlag, task.eMsg);
                }
            }
        });
    }

    private PageList getPageList(String pageContent) {
        PageList pageList = null;
        Pattern p = Pattern
                .compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
        Matcher m = p.matcher(pageContent);
        while (m.find()) {
            if (pageList == null)
                pageList = new PageList();
            pageList.addItem(m.group(3), m.group(4), "0", m.group(1),
                    m.group(2), m.group(5));
        }
        return pageList;
    }

    // Get Manga url and next page
    private class GetManagaUrlRunnable implements Runnable {
        private String url;
        
        public String[] strs;
        public String eMsg;

        public GetManagaUrlRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            String[] strs = null;
            
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(url);
            if (pageContent != null) {
                Pattern p = Pattern.compile("<a[^<>]*id=\"prev\"[^<>]*href=\"([^<>\"]+)\"><img[^<>]*/></a>.+<a[^<>]id=\"next\"[^<>]*href=\"([^<>\"]+)\"><img[^<>]*/></a>.+<img[^<>]*src=\"([^<>\"]+?)\"[^<>]*style=\"[^<>\"]*\"[^<>]*/>");
                        //.compile("<a[^<>]*href=\"([^<>\"]+?)\"[^<>]*><img[^<>]*src=\"([^<>\"]+?)\"[^<>]*style=\"[^<>\"]*\"[^<>]*/></a>");
                Matcher m = p.matcher(pageContent);
                if (m.find() && m.groupCount() == 3) {
                    strs = new String[3];
                    if (url.equals(m.group(1)))
                        strs[0] = "first";
                    else
                        strs[0] = m.group(1);
                    if (url.equals(m.group(2)))
                        strs[1] = "last";
                    else
                        strs[1] = m.group(2);
                    strs[2] = StringEscapeUtils.unescapeHtml4(m.group(3));
                } else
                    eMsg = mContext.getString(R.string.em_parser_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getManagaUrl(String url, final Object checkFlag, final OnGetManagaUrlListener listener) {
        
        final GetManagaUrlRunnable task = new GetManagaUrlRunnable(url);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                if (task.strs != null) {
                    if (listener != null)
                        listener.onSuccess(checkFlag, task.strs);
                } else {
                    if (listener != null)
                        listener.onFailure(checkFlag, task.eMsg);
                }
            }
        });
    }
/*
    // Get image
    private static class GetImagePackage {
        public Object res;
        public Object checkFlag;
        public OnGetImageListener listener;

        public GetImagePackage(Object res, Object checkFlag,
                OnGetImageListener listener) {
            this.res = res;
            this.checkFlag = checkFlag;
            this.listener = listener;
        }
    }

    private static Handler getGetImageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            OnGetImageListener listener = ((GetImagePackage) msg.obj).listener;
            Object checkFlag = ((GetImagePackage) msg.obj).checkFlag;
            Object res = ((GetImagePackage) msg.obj).res;
            if (res == null) {
                listener.onFailure(msg.what);
            }
            else
                listener.onSuccess(checkFlag, res);
        };
    };

    private static class GetImageRunnable implements Runnable {
        private String url;
        private String key;
        private int type;
        private LruCache<String, Bitmap> memoryCache;
        private DiskCache diskCache;
        private Object checkFlag;
        private OnGetImageListener listener;

        public GetImageRunnable(String url, String key, int type,
                LruCache<String, Bitmap> memoryCache, DiskCache diskCache,
                Object checkFlag, OnGetImageListener listener) {
            this.url = url;
            this.key = key;
            this.type = type;
            this.memoryCache = memoryCache;
            this.diskCache = diskCache;
            this.checkFlag = checkFlag;
            this.listener = listener;
        }

        @Override
        public void run() {
            int errorMessageId = R.string.em_unknown_error;
            Object res = null;
            synchronized (url) {
                // Check memory cache
                if (type == Util.BITMAP && memoryCache != null
                        && (res = memoryCache.get(key)) != null) {
                    sendMessage(res, errorMessageId);
                    return;
                }
                // If not find in memory cache or do not have memory cache
                // Check disk cache
                if (diskCache != null
                        && (res = diskCache.get(key, type)) != null) {
                    sendMessage(res, errorMessageId);
                    if (memoryCache != null && res instanceof Bitmap)
                        memoryCache.put(key, (Bitmap) res);
                    return;
                }
                Log.d(TAG, "Download image " + url);

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(this.url);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(TIMEOUT);
                    conn.setRequestProperty("User-Agent", userAgent);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    if (diskCache != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Util.copy(is, baos);
                        byte[] bytes = baos.toByteArray();
                        // To disk cache
                        is = new ByteArrayInputStream(bytes);
                        diskCache.put(key, is);
                        // Read
                        is = new ByteArrayInputStream(bytes);
                        is.reset();
                        if (type == Util.BITMAP) {
                            res = BitmapFactory.decodeStream(is, null,
                                    Ui.getBitmapOpt());
                            if (memoryCache != null && res != null)
                                memoryCache.put(key, (Bitmap) res);
                        } else if (type == Util.MOVIE) {
                            is = new BufferedInputStream(is, 16 * 1024);
                            is.mark(16 * 1024);
                            res = Movie.decodeStream(is);
                        }
                        is.close();
                    } else {
                        if (type == Util.BITMAP) {
                            res = BitmapFactory.decodeStream(is, null,
                                    Ui.getBitmapOpt());
                            if (memoryCache != null && res != null)
                                memoryCache.put(key, (Bitmap) res);
                        } else if (type == Util.MOVIE) {
                            is = new BufferedInputStream(is, 16 * 1024);
                            is.mark(16 * 1024);
                            res = Movie.decodeStream(is);
                        }
                    }
                } catch (MalformedURLException e) {
                    errorMessageId = R.string.em_url_error;
                    e.printStackTrace();
                } catch (ConnectTimeoutException e) {
                    errorMessageId = R.string.em_timeout;
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    errorMessageId = R.string.em_no_network_2;
                    e.printStackTrace();
                } catch (IOException e) {
                    errorMessageId = R.string.em_network_error;
                    e.printStackTrace();
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }
                sendMessage(res, errorMessageId);
            }
        }

        private void sendMessage(Object res, int errorMessageId) {
            Message msg = new Message();
            msg.what = errorMessageId;
            msg.obj = new GetImagePackage(res, checkFlag, listener);
            getGetImageHandler.sendMessage(msg);
        }
    }

    public static void getImage(String url, String key, int type,
            LruCache<String, Bitmap> memoryCache, DiskCache diskCache,
            Object checkFlag, OnGetImageListener listener) {
        new Thread(new GetImageRunnable(url, key, type, memoryCache, diskCache,
                checkFlag, listener)).start();
    }
    
    */
    public interface OnDownloadMangaListener {
        public void onDownloadMangaStart(String id);
        public void onDownloadMangaStart(String id, int pageSum, int startIndex);
        public void onDownloadMangaStop(String id);
        public void onDownloadMangaOver(String id, boolean ok);
        
        public void onDownloadPage(String id, int pageSum, int index);
        public void onDownloadPageProgress(String id, int pageSum,
                int index, float totalSize, float downloadSize);
        
        public void onDownloadMangaAllStart();
        public void onDownloadMangaAllOver();
    }
    
    // TODO 下载前检查文件是否已存在
    
    /****** DownloadMangaManager ******/
    public class DownloadMangaManager {
        private DownloadInfo curDownloadInfo = null;
        private Downloader.Controlor curControlor = null;
        
        private ArrayList<DownloadInfo> mDownloadQueue = new ArrayList<DownloadInfo>();
        private Object taskLock = new Object();
        private OnDownloadMangaListener listener;
        private DownloadService mService;
        
        private boolean mStop = false;
        
        public void init() {

        }
        
        public void setOnDownloadMangaListener(OnDownloadMangaListener listener) {
            this.listener = listener;
        }
        
        public void setDownloadService(DownloadService service) {
            this.mService = service;
        }
        
        /**
         * Add a download task to task queue
         * 
         * @param detailUrlStr
         * @param foldName
         * @return
         */
        public void add(DownloadInfo di) {
            synchronized (taskLock) {
                mDownloadQueue.add(di);
                if (curDownloadInfo == null && mDownloadQueue.size() != 0)
                    start();
            }
        }
        
        public void cancel(String id) {
            synchronized (taskLock) {
                DownloadInfo di = Download.get(id);
                if (di != null) {
                    di.status = DownloadInfo.STOP;
                    mDownloadQueue.remove(di);
                    if (!id.equals(getCurDownloadId())) {
                        if (curControlor != null)
                            curControlor.stop();
                        mService.notifyUpdate();
                        Download.notify(id);
                    }
                }
            }
        }
        
        public String getCurDownloadId() {
            synchronized (taskLock) {
                if (curDownloadInfo == null)
                    return null;
                else
                    return curDownloadInfo.gid;
            }
        }
        
        public boolean isWaiting(String str) {
            synchronized (taskLock) {
                for (DownloadInfo di : mDownloadQueue) {
                    if (di.gid.equals(str))
                        return true;
                }
            }
            return false;
        }
        
        private void start(){
            if(curDownloadInfo != null){
                return;
            }
            
            new Thread(new Runnable(){
                @Override
                public void run() {
                    listener.onDownloadMangaAllStart();
                    
                    Parser parser = new Parser();
                    Downloader imageDownloader = new Downloader();
                    imageDownloader.setOnDownloadListener(new Downloader.OnDownloadListener() {
                        @Override
                        public void onDownloadStart(int totalSize) {
                            if (curDownloadInfo != null) {
                                curDownloadInfo.downloadSize = 0;
                                curDownloadInfo.totalSize = totalSize/1024.0f;
                                mService.notifyUpdate(curDownloadInfo.gid, curDownloadInfo.lastStartIndex, ImageSet.STATE_LOADING);
                            }
                        }

                        @Override
                        public void onDownloadStatusUpdate(int downloadSize,
                                int totalSize) {
                            curDownloadInfo.downloadSize = downloadSize/1024.0f;
                            curDownloadInfo.totalSize = totalSize/1024.0f;
                            mService.notifyUpdate();
                        }

                        @Override
                        public void onDownloadOver(boolean ok, int eMesgId) {
                            if (ok)
                                mService.notifyUpdate(curDownloadInfo.gid, curDownloadInfo.lastStartIndex, ImageSet.STATE_LOADED);
                            else
                                mService.notifyUpdate(curDownloadInfo.gid, curDownloadInfo.lastStartIndex, ImageSet.STATE_FAIL);
                        }
                    });
                    while(mDownloadQueue.size() > 0){
                        synchronized (taskLock) {
                            curDownloadInfo = mDownloadQueue.get(0);
                            curDownloadInfo.status = DownloadInfo.DOWNLOADING;
                            mDownloadQueue.remove(0);
                        }
                        Download.notify(String.valueOf(curDownloadInfo.gid));
                        
                        if (curDownloadInfo.type == DownloadInfo.DETAIL_URL) {
                            listener.onDownloadMangaStart(curDownloadInfo.gid);
                            mService.notifyUpdate();
                            if (parser.getFirstPagePageSumForDetail(curDownloadInfo.detailUrlStr)) { // Get page info
                                curDownloadInfo.type = DownloadInfo.PAGE_URL;
                                curDownloadInfo.lastStartIndex = 0;
                                curDownloadInfo.pageSum = parser.getPageSum();
                                curDownloadInfo.pageUrlStr = parser.getFirstPage();
                                Download.notify(curDownloadInfo.gid);
                                
                            } else { // If get info error
                                listener.onDownloadMangaOver(curDownloadInfo.gid, false);
                                curDownloadInfo.status = DownloadInfo.FAILED;
                                mService.notifyUpdate();
                                Download.notify(String.valueOf(curDownloadInfo.gid));
                                continue;
                            }
                        }
                        
                        //Create folder
                        File folder = new File(Config.getDownloadPath() + curDownloadInfo.title); // TODO For  title contain invailed char
                        if (!folder.mkdirs() && !folder.isDirectory()) {
                            listener.onDownloadMangaOver(curDownloadInfo.gid, false);
                            curDownloadInfo.status = DownloadInfo.FAILED;
                            mService.notifyUpdate();
                            Download.notify(String.valueOf(curDownloadInfo.gid));
                            continue;
                        }
                        
                        String nextPage = curDownloadInfo.pageUrlStr;
                        curDownloadInfo.pageUrlStr = null;
                        String imageUrlStr = null;
                        boolean mComplete = true;
                        boolean mStop = false;
                        // Get page
                        while (!nextPage.equals(curDownloadInfo.pageUrlStr)) {
                            curDownloadInfo.pageUrlStr = nextPage;
                            Download.notify(String.valueOf(curDownloadInfo.gid));
                            listener.onDownloadPage(curDownloadInfo.gid, curDownloadInfo.pageSum, curDownloadInfo.lastStartIndex);
                            mService.notifyUpdate();
                            
                            if (parser.getPageInfoSumForPage(curDownloadInfo.pageUrlStr)) {
                                nextPage = parser.getNextPage();
                                imageUrlStr = parser.getImageUrlStr();
                                
                                // Check stop
                                if (curDownloadInfo.status == DownloadInfo.STOP) {
                                    listener.onDownloadMangaStop(curDownloadInfo.gid);
                                    mService.notifyUpdate();
                                    Download.notify(String.valueOf(curDownloadInfo.gid));
                                    mComplete = false;
                                    mStop = true;
                                    break;
                                }
                                
                                try {
                                    // TODO
                                    curControlor = imageDownloader.resetData(folder.toString(),
                                            String.format("%05d", curDownloadInfo.lastStartIndex + 1) + "." + Util.getExtension(imageUrlStr),
                                            imageUrlStr);
                                    imageDownloader.run();
                                    
                                    curDownloadInfo.downloadSize = 0;
                                    curDownloadInfo.totalSize = 0;
                                    
                                    int downloadStatus = imageDownloader.getStatus();
                                    if (downloadStatus == Downloader.STOP) { // If stop by user
                                        listener.onDownloadMangaStop(curDownloadInfo.gid);
                                        mService.notifyUpdate();
                                        Download.notify(String.valueOf(curDownloadInfo.gid));
                                        mComplete = false;
                                        mStop = true;
                                        break;
                                    } else if (downloadStatus != Downloader.COMPLETED) { // If get image error
                                        Log.e(TAG, "Download image error, downloadStatus is " + downloadStatus);
                                        mComplete = false;
                                        break;
                                    }
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                    mComplete = false;
                                    break;
                                }
                            } else {
                                mComplete = false;
                                break;
                            }
                            curDownloadInfo.lastStartIndex++;
                        }
                        if (mComplete) {
                            listener.onDownloadMangaOver(curDownloadInfo.gid, true);
                            curDownloadInfo.status = DownloadInfo.COMPLETED;
                            mService.notifyUpdate();
                            Download.notify(String.valueOf(curDownloadInfo.gid));
                        } else if (!mStop) {
                            listener.onDownloadMangaOver(curDownloadInfo.gid, false);
                            curDownloadInfo.status = DownloadInfo.FAILED;
                            mService.notifyUpdate();
                            Download.notify(String.valueOf(curDownloadInfo.gid));
                        }
                    }
                    synchronized (taskLock) {
                        curDownloadInfo = null;
                        listener.onDownloadMangaAllOver();
                    }
                }
            }).start();
        }
        
        public boolean isEmpty() {
            synchronized (taskLock) {
                return curDownloadInfo == null;
            }
        }
    }
    
    private class Parser {
        
        private int errorMegId;
        
        private int pageSum;
        private String firstPage;
        
        private String prePage;
        private String nextPage;
        private String imageUrlStr;
        
        
        public int getPageSum() {
            return pageSum;
        }
        
        public String getFirstPage() {
            return firstPage;
        }
        
        public String getPrePage() {
            return prePage;
        }
        
        public String getNextPage() {
            return nextPage;
        }
        
        public String getImageUrlStr() {
            return imageUrlStr;
        }
        
        public boolean getFirstPagePageSumForDetail(String detailUrlStr) {
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(detailUrlStr);
            if (pageContent == null)
                return false;
            
            Pattern p = Pattern.compile("<p class=\"ip\">Showing [\\d|,]+ - [\\d|,]+ of ([\\d|,]+) images</p>.+<div id=\"gdt\"><div[^<>]*>(?:<div[^<>]*>)?<a[^<>]*href=\"([^<>\"]+)\"[^<>]*>");
            Matcher m = p.matcher(pageContent);
            if (m.find()) {
                pageSum = Integer.parseInt(m.group(1).replace(",", ""));
                firstPage = m.group(2);
                return true;
            }
            return false;
        }
        
        /**
         * Get page info, previous page, next page, image url
         * 
         * @param pageUrlStr
         * @return True if get
         */
        public boolean getPageInfoSumForPage(String pageUrlStr) {
            HttpHelper hp = new HttpHelper(mContext);
            String pageContent = hp.get(pageUrlStr);
            if (pageContent == null)
                return false;
            
            Pattern p = Pattern.compile("<a[^<>]*id=\"prev\"[^<>]*href=\"([^<>\"]+)\"><img[^<>]*/></a>.+<a[^<>]id=\"next\"[^<>]*href=\"([^<>\"]+)\"><img[^<>]*/></a>.+<img[^<>]*src=\"([^<>\"]+?)\"[^<>]*style=\"[^<>\"]*\"[^<>]*/>");
            Matcher m = p.matcher(pageContent);
            if (m.find()) {
                prePage = m.group(1);
                nextPage = m.group(2);
                imageUrlStr = StringEscapeUtils.unescapeHtml4(m.group(3));
                return true;
            }
            return false;
        }
    }
    
    
    /********** Use E-hentai API ************/
    
    /*
    private static class GetGalleryMetadataPackage {
        public Map<String, ListMangaDetail> lmds;
        public OnGetGalleryMetadataListener listener;

        public GetGalleryMetadataPackage(Map<String, ListMangaDetail> lmds,
                OnGetGalleryMetadataListener listener) {
            this.lmds = lmds;
            this.listener = listener;
        }
    }

    private static Handler getGalleryMetadataHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            OnGetGalleryMetadataListener listener =
                    ((GetGalleryMetadataPackage) msg.obj).listener;
            Map<String, ListMangaDetail> lmds =
                    ((GetGalleryMetadataPackage) msg.obj).lmds;
            if (lmds == null || lmds.size() == 0)
                listener.onFailure(msg.what);
            else
                listener.onSuccess(lmds);
        };
    };
    
    private static class GetGalleryMetadataRunnable implements Runnable {
        private String[][] gidTokens;
        private OnGetGalleryMetadataListener listener;

        public GetGalleryMetadataRunnable(String[][] gidTokens,
                OnGetGalleryMetadataListener listener) {
            this.gidTokens = gidTokens;
            this.listener = listener;
        }
        
        @Override
        public void run() {
            int errorMessageId = R.string.em_unknown_error;
            Map<String, ListMangaDetail> lmds = new HashMap<String, ListMangaDetail>();
            
            // Create post string
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("{\"method\": \"gdata\",\"gidlist\": [");
            for (String[] item: gidTokens)
                strBuf.append("[" + item[0] + ",\"" + item[1] + "\"],");
            strBuf.delete(strBuf.length()-1, strBuf.length());
            strBuf.append("]}");
            StringBuffer sb = new StringBuffer();
            errorMessageId = postJson(API_URL, strBuf.toString(),sb);
            if (sb.length() != 0) {
                String pageContent = sb.toString();
                try {
                    SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd hh:mm");
                    JSONObject jsonObject = new JSONObject(pageContent);
                    JSONArray jsonArray = jsonObject.getJSONArray("gmetadata");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jo = jsonArray.getJSONObject(i);
                        if (jo.getString("error") != null)
                            continue;
                        ListMangaDetail lmd = new ListMangaDetail();
                        lmd.gid = Long.toString(jo.getLong("gid"));
                        lmd.token = jo.getString("token");
                        lmd.archiver_key = jo.getString("archiver_key");
                        lmd.title = jo.getString("title");
                        lmd.title_jpn = jo.getString("title_jpn");
                        lmd.category = getType(jo.getString("category"));
                        lmd.thumb = jo.getString("thumb");
                        lmd.uploader = jo.getString("uploader");
                        lmd.posted = fm.format(Long.parseLong(jo.getString("posted")) * 1000);
                        lmd.filecount = jo.getString("filecount");
                        lmd.filesize = jo.getLong("filesize");
                        lmd.expunged = jo.getBoolean("expunged");
                        lmd.rating = jo.getString("rating");
                        lmd.torrentcount = jo.getString("torrentcount");
                        JSONArray ja = jo.getJSONArray("tags");
                        int length = ja.length();
                        lmd.tags = new String[1][length+1];
                        lmd.tags[0][0] = "all";
                        for (int j = 1; j < length+1; j++)
                            lmd.tags[0][j] = ja.getString(j);
                        
                        lmds.put(lmd.gid, lmd);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorMessageId = R.string.em_parser_error;
                }
            }
            Message msg = new Message();
            msg.what = errorMessageId;
            msg.obj = new GetGalleryMetadataPackage(lmds, listener);
            getGalleryMetadataHandler.sendMessage(msg);
        }
    }
    
    public static void getGalleryMetadata(String[][] gidTokens,
            OnGetGalleryMetadataListener listener) {
        new Thread(new GetGalleryMetadataRunnable(gidTokens,
                listener)).start();
    }
    
    
    
    private static class GetGalleryTokensPackage {
        public Map<String, String> tokens;
        public OnGetGalleryTokensListener listener;

        public GetGalleryTokensPackage(Map<String, String> tokens,
                OnGetGalleryTokensListener listener) {
            this.tokens = tokens;
            this.listener = listener;
        }
    }

    private static Handler getGalleryTokensHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            OnGetGalleryTokensListener listener =
                    ((GetGalleryTokensPackage) msg.obj).listener;
            Map<String, String> tokens =
                    ((GetGalleryTokensPackage) msg.obj).tokens;
            if (tokens == null || tokens.size() == 0)
                listener.onFailure(msg.what);
            else
                listener.onSuccess(tokens);
        };
    };
    
    
    private static class GetGalleryTokensRunnable implements Runnable {
        private String gid;
        private String token;
        private String[] pages;
        private OnGetGalleryMetadataListener listener;

        public GetGalleryTokensRunnable(String gid, String token, String[] pages,
                OnGetGalleryMetadataListener listener) {
            this.gid = gid;
            this.token = token;
            this.pages = pages;
            this.listener = listener;
        }
        
        @Override
        public void run() {
            int errorMessageId = R.string.em_unknown_error;
            Map<String, String> tokens = new HashMap<String, String>();
            
            // Create post string
            StringBuffer strBuf = new StringBuffer();
            strBuf.append("{\"method\": \"gtoken\",\"pagelist\": [");
            for (String item: pages)
                strBuf.append("[" + gid + ",\"" + token + "\"," + item + "],");
            strBuf.delete(strBuf.length()-1, strBuf.length());
            strBuf.append("]}");
            
            Log.i(TAG, strBuf.toString());
            
            StringBuffer sb = new StringBuffer();
            errorMessageId = postJson(API_URL, strBuf.toString(),sb);
            if (sb.length() != 0) {
                String pageContent = sb.toString();
                Log.d(TAG, pageContent);
            }
        }
    }
    
    
    */
    
    /*
    // TODO this api does not work !!!!!!
    private static void getGalleryTokens(String gid, String token, String[] pages,
            OnGetGalleryMetadataListener listener) {
        new Thread(new GetGalleryTokensRunnable(gid, token, pages,
                listener)).start();
    }*/
    
}
