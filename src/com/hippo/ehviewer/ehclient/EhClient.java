/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ehclient;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.ui.DownloadInfo;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;

import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Download;
import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;

// TODO newer versions tip

public class EhClient {
    
    private static final String TAG = "EhClient";
    
    public static final int FAVORITE_SLOT_NUM = 10;
    
    public static final int G = 0x0;
    public static final int EX = 0x1;
    public static final int LOFI_460x = 0x2;
    public static final int LOFI_780x = 0x3;
    public static final int LOFI_980x = 0x4;
    
    public static final String G_API = "http://g.e-hentai.org/api.php";
    public static final String EX_API = "http://exhentai.org/api.php";
    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";
    
    private static final String loginUrl = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    
    public static final String G_HEADER = "http://g.e-hentai.org/";
    public static final String EX_HEADER = "http://exhentai.org/";
    public static final String LOFI_HEADER = "http://lofi.e-hentai.org/";
    
    private boolean mLogin = false;
    private String mUsername;
    private String mDisplayName;
    private String mLogoutUrl;
    
    private AppContext mAppContext;
    private ThreadPool mThreadPool;
    
    public static String getUrlHeader() {
        return getUrlHeader(Config.getMode());
    }
    
    public static String getUrlHeader(int mode) {
        switch (mode) {
        case EX:
            return EX_HEADER;
        case LOFI_460x:
        case LOFI_780x:
        case LOFI_980x:
            return LOFI_HEADER;
        default:
            return G_HEADER;
        }
    }
    
    public static String getDetailUrl(int gid, String token) {
        return getDetailUrl(gid, token, 0, Config.getMode());
    }
    
    
    public static String getDetailUrl(int gid, String token, int pageNum) {
        return getDetailUrl(gid, token, pageNum, Config.getMode());
    }
    
    public static String getDetailUrl(int gid, String token, int pageNum, int mode) {
        switch (mode) {
        case EX:
            return EX_HEADER + "g/" + gid + "/" + token + "/?p=" + pageNum;
        case LOFI_460x:
        case LOFI_780x:
        case LOFI_980x:
            return LOFI_HEADER + "g/" + gid + "/" + token + "/?p=" + pageNum;
        default:
            return G_HEADER + "g/" + gid + "/" + token + "/?p=" + pageNum;
        }
    }
    
    public static String getPageUrl(String gid, String token, int pageNum) {
        return getPageUrl(gid, token, pageNum, Config.getMode());
    }
    
    public static String getPageUrl(String gid, String token, int pageNum, int mode) {
        switch (mode) {
        case EX:
            return EX_HEADER + "s/" + token + "/" + gid + "-" + pageNum;
        case LOFI_460x:
        case LOFI_780x:
        case LOFI_980x:
            return LOFI_HEADER + "s/" + token + "/" + gid + "-" + pageNum;
        default:
            return G_HEADER + "s/" + token + "/" + gid + "-" + pageNum;
        }
    }
    
    public static String getApiUrl() {
        return getApiUrl(Config.getMode());
    }
    
    public static String getApiUrl(int mode) {
        switch (mode) {
        case EX:
            return EX_API;
        default:
            return G_API;
        }
    }
    
    public static String getFavoriteUrl(int index, int page) {
        return getFavoriteUrl(Config.getMode(), index, page);
    }
    
    public static String getFavoriteUrl(int mode, int index, int page) {
        switch (mode) {
        case EX:
            return EX_HEADER + "favorites.php?favcat=" + index + "&page=" + page;
        default:
            return G_HEADER  + "favorites.php?favcat=" + index + "&page=" + page;
        }
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

    public interface OnGetMangaDetailListener {
        public void onSuccess(GalleryDetail md);
        public void onFailure(String eMsg);
    }

    public interface OnGetPageListListener {
        public void onSuccess(Object checkFlag, PreviewList pageList);
        public void onFailure(Object checkFlag, String eMsg);
    }

    public interface OnGetMangaUrlListener {
        public void onSuccess(Object checkFlag, String[] arg);
        public void onFailure(Object checkFlag, String eMsg);
    }

    public interface OnGetImageListener {
        public void onSuccess(Object checkFlag, Object res);
        public void onFailure(int eMsgId);
    }
    
    public interface OnGetGalleryMetadataListener {
        public void onSuccess(Map<String, GalleryInfo> lmds);
        public void onFailure(String eMsg);
    }
    
    public interface OnGetGalleryTokensListener {
        public void onSuccess(Map<String, String> tokens);
        public void onFailure(String eMsg);
    }
    
    public EhClient(AppContext appContext) {
        mAppContext = appContext;
        mThreadPool = mAppContext.getNetworkThreadPool();
    }

    public boolean isLogin() {
        return mLogin;
    }

    public String getUsername() {
        return mUsername;
    }

    private static final int LOGIN = 0x0;
    private static final int CHECK_LOGIN = 0x1;
    private static final int LOGOUT = 0x2;
    private static final int GET_MANGA_LIST = 0x3;
    private static final int GET_MANGA_DETAIL = 0x4;
    private static final int GET_PAGE_LIST = 0x5;
    private static final int GET_MANGA_URL = 0x6;
    private static final int RATE = 0x7;
    
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            
            switch (msg.what) {
            case LOGIN:
                LoginPackage loginPackage = (LoginPackage) msg.obj;
                OnLoginListener listener1 = loginPackage.listener;
                if (loginPackage.ok)
                    listener1.onSuccess();
                else
                    listener1.onFailure(loginPackage.eMsg);
                break;
                
            case CHECK_LOGIN:
                CheckLoginPackage checkLoginPackage = (CheckLoginPackage) msg.obj;
                OnCheckLoginListener listener2 = checkLoginPackage.listener;
                if (checkLoginPackage.ok)
                    listener2.onSuccess();
                else
                    listener2.onFailure(checkLoginPackage.eMsg);
                break;
                
            case LOGOUT:
                LogoutPackage logoutPackage = (LogoutPackage) msg.obj;
                OnLogoutListener listener3 = logoutPackage.listener;
                if (logoutPackage.ok)
                    listener3.onSuccess();
                else
                    listener3.onFailure(logoutPackage.eMsg);
                break;
                
            case GET_MANGA_LIST:
                GetMangaListPackage getMangaListPackage = (GetMangaListPackage) msg.obj;
                OnGetMangaListListener listener4 = getMangaListPackage.listener;
                if (getMangaListPackage.lmdArray != null)
                    listener4.onSuccess(getMangaListPackage.checkFlag,
                            getMangaListPackage.lmdArray,
                            getMangaListPackage.indexPerPage,
                            getMangaListPackage.maxPage);
                else
                    listener4.onFailure(getMangaListPackage.checkFlag,
                            getMangaListPackage.eMsg);
                break;
                
            case GET_MANGA_DETAIL:
                GetMangaDetailPackage getMangaDetailPackage = (GetMangaDetailPackage) msg.obj;
                OnGetMangaDetailListener listener5 = getMangaDetailPackage.listener;
                if (getMangaDetailPackage.ok)
                    listener5.onSuccess(getMangaDetailPackage.mangaDetail);
                else
                    listener5.onFailure(getMangaDetailPackage.eMsg);
                break;
                
            case GET_PAGE_LIST:
                GetPageListPackage getPageListPackage = (GetPageListPackage) msg.obj;
                OnGetPageListListener listener6 = getPageListPackage.listener;
                if (getPageListPackage.pageList != null)
                    listener6.onSuccess(getPageListPackage.checkFlag, getPageListPackage.pageList);
                else
                    listener6.onFailure(getPageListPackage.checkFlag, getPageListPackage.eMsg);
                break;
                
            case GET_MANGA_URL:
                GetMangaUrlPackage getMangaUrlPackage = (GetMangaUrlPackage) msg.obj;
                OnGetMangaUrlListener listener7 = getMangaUrlPackage.listener;
                if (getMangaUrlPackage.strs != null)
                    listener7.onSuccess(getMangaUrlPackage.checkFlag, getMangaUrlPackage.strs);
                else
                    listener7.onFailure(getMangaUrlPackage.checkFlag, getMangaUrlPackage.eMsg);
                break;
                
            case RATE:
                
            }
        };
    };
    
    
    // Login
    private class LoginPackage {
        public boolean ok;
        public OnLoginListener listener;
        public String eMsg;
        
        public LoginPackage(boolean ok, OnLoginListener listener, String eMsg) {
            this.ok = ok;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
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
            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.post(loginUrl, args);
            if (pageContent != null) {
                if (pageContent.contains("<p>You are now logged in as: "))
                    ok = true;
                else
                    eMsg = mAppContext.getString(R.string.em_logout_error);
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
                Message msg = new Message();
                msg.what = LOGIN;
                msg.obj = new LoginPackage(task.ok, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
    }

    // Check login get info and logout url
    private class CheckLoginPackage {
        public boolean ok;
        public OnCheckLoginListener listener;
        public String eMsg;
        
        public CheckLoginPackage(boolean ok, OnCheckLoginListener listener, String eMsg) {
            this.ok = ok;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class checkLoginRunnable implements Runnable {
        
        public String eMsg;
        public boolean ok;
        public String name;
        public String logoutUrl;

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.get(loginUrl);
            if (pageContent != null) {
                Pattern p = Pattern
                        .compile("<p class=\"home\"><b>Logged in as:  <a href=\"[^<>\"]+\">([^<>]+)</a></b> \\( <a href=\"([^<>\"]+)\">Log Out</a> \\)</p>");
                Matcher m = p.matcher(pageContent);
                if (m.find() && m.groupCount() == 2) {
                    ok = true;
                    name = m.group(1);
                    logoutUrl = StringEscapeUtils.unescapeHtml4(m.group(2));
                } else
                    eMsg = mAppContext.getString(R.string.em_check_login_error);
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
                    mUsername = task.name;
                    mLogoutUrl = task.logoutUrl;
                }
                Message msg = new Message();
                msg.what = CHECK_LOGIN;
                msg.obj = new CheckLoginPackage(task.ok, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
    }
    
    // Test Ex
    
    public interface OnTestExListener {
        public void onSuccess();
        public void onFailure(String eMsg);
    }
    
    public void testEx(final OnTestExListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContext) {
                        if (pageContext.equals("image/gif"))
                            listener.onFailure("Not login");
                        else
                            listener.onSuccess();
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                hp.contentType(EX_HEADER);
            }
        }).start();
    }
    
    // Logout
    private class LogoutPackage {
        public boolean ok;
        public OnLogoutListener listener;
        public String eMsg;
        
        public LogoutPackage(boolean ok, OnLogoutListener listener, String eMsg) {
            this.ok = ok;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class LogoutRunnable implements Runnable {

        public String eMsg;
        public boolean ok;

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.get(mLogoutUrl);
            if (pageContent != null) {
                if (pageContent.contains("<p>You are now logged out<br />"))
                    ok = true;
                else
                    eMsg = mAppContext.getString(R.string.em_logout_error);
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
                if (task.ok)
                    mLogin = false;
                Message msg = new Message();
                msg.what = LOGOUT;
                msg.obj = new LogoutPackage(task.ok, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
    }
    
    // Get Manga List
    public interface OnGetMangaListListener {
        public void onSuccess(Object checkFlag, ArrayList<GalleryInfo> lmdArray,
                int indexPerPage, int maxPage);
        public void onFailure(Object checkFlag, String eMsg);
    }
    
    private class GetMangaListPackage {
        public Object checkFlag;
        public ArrayList<GalleryInfo> lmdArray;
        public int indexPerPage;
        public int maxPage;
        public OnGetMangaListListener listener;
        public String eMsg;
        
        public GetMangaListPackage(Object checkFlag, ArrayList<GalleryInfo> lmdArray, int indexPerPage,
                int maxPage, OnGetMangaListListener listener, String eMsg) {
            this.checkFlag = checkFlag;
            this.lmdArray = lmdArray;
            this.indexPerPage = indexPerPage;
            this.maxPage = maxPage;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class GetMangaListRunnable implements Runnable {

        private String url;
        
        public ArrayList<GalleryInfo> lmdArray;
        public int indexPerPage;
        public int maxPage;
        public String eMsg;

        public GetMangaListRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            lmdArray = null;
            indexPerPage = 25;
            maxPage = 0;

            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.get(url);

            if (pageContent != null) {
                ListParser parser = new ListParser();
                switch (parser.parser(pageContent)) {
                case ListParser.ALL:
                    maxPage = parser.maxPage;
                    indexPerPage = parser.indexPerPage;
                    lmdArray = parser.giList;
                    break;
                case ListParser.NOT_FOUND:
                    maxPage = 0;
                    indexPerPage = parser.indexPerPage;
                    lmdArray = new ArrayList<GalleryInfo>();
                    break;
                case ListParser.PARSER_ERROR:
                    eMsg = "parser error";
                }
            } else
                eMsg = hp.getEMsg();
        }
    }
    
    public void getMangaList(String url, final Object checkFlag,
            final OnGetMangaListListener listener) {
        
        final GetMangaListRunnable task = new GetMangaListRunnable(url);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                Message msg = new Message();
                msg.what = GET_MANGA_LIST;
                msg.obj = new GetMangaListPackage(checkFlag, task.lmdArray, task.indexPerPage, task.maxPage, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
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
    private class GetMangaDetailPackage {
        public boolean ok;
        public GalleryDetail mangaDetail;
        public OnGetMangaDetailListener listener;
        public String eMsg;
        
        public GetMangaDetailPackage(boolean ok, GalleryDetail mangaDetail,
                OnGetMangaDetailListener listener, String eMsg) {
            this.ok = ok;
            this.mangaDetail = mangaDetail;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class GetMangaDetailRunnable implements Runnable {

        private String url;
        private GalleryDetail md;
        
        public boolean ok;
        public String eMsg;

        public GetMangaDetailRunnable(String url, GalleryDetail md) {
            this.url = url;
            this.md = md;
        }

        @Override
        public void run() {
            ok = false;
            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.get(url);
            if (pageContent != null) {
                
                
                DetailParser parser = new DetailParser();
                int mode = DetailParser.DETAIL | DetailParser.TAG
                        | DetailParser.PREVIEW_INFO | DetailParser.PREVIEW
                        | DetailParser.COMMENT;
                parser.setMode(mode);
                int result = parser.parser(pageContent);
                if (result == DetailParser.OFFENSIVE) {
                    ok = true;
                    md.firstPage = "offensive";
                } else if (result == DetailParser.PINING) {
                    ok = true;
                    md.firstPage = "pining";
                } else if ((result & (DetailParser.DETAIL | DetailParser.PREVIEW_INFO)) != 0) {
                    ok = true;
                    md.thumb = parser.thumb;
                    md.title = parser.title;
                    md.title_jpn = parser.title_jpn;
                    md.category = parser.category;
                    md.uploader = parser.uploader;
                    md.posted = parser.posted;
                    md.pages = parser.pages;
                    md.size = parser.size;
                    md.resized = parser.resized;
                    md.parent = parser.parent;
                    md.visible = parser.visible;
                    md.language = parser.language;
                    md.people = parser.people;
                    md.rating = parser.rating;
                    md.firstPage = parser.firstPage;
                    md.previewPerPage = parser.previewPerPage;
                    md.previewSum = parser.previewSum;
                    
                    md.tags = parser.tags;
                    md.previewLists = new PreviewList[md.previewSum];
                    md.previewLists[0] = parser.previewList;
                    md.comments = parser.comments;
                } else {
                    eMsg = mAppContext.getString(R.string.em_parser_error);
                }
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getMangaDetail(String url, final GalleryDetail md,
            final OnGetMangaDetailListener listener) {
        
        final GetMangaDetailRunnable task = new GetMangaDetailRunnable(url, md);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                Message msg = new Message();
                msg.what = GET_MANGA_DETAIL;
                msg.obj = new GetMangaDetailPackage(task.ok, md, listener, task.eMsg);
                mHandler.sendMessage(msg);
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
            return new String[0][0];
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
    private class GetPageListPackage {
        public Object checkFlag;
        public PreviewList pageList;
        public OnGetPageListListener listener;
        public String eMsg;
        
        public GetPageListPackage(Object checkFlag, PreviewList pageList, OnGetPageListListener listener,
                String eMsg) {
            this.checkFlag = checkFlag;
            this.pageList = pageList;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class GetPageListRunnable implements Runnable {
        private String url;
        
        public PreviewList pageList;
        public String eMsg;

        public GetPageListRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            pageList = null;
            
            HttpHelper hp = new HttpHelper(mAppContext);
            String pageContent = hp.get(url);
            if (pageContent != null) {
                
                pageList = getPageList(pageContent);
                if (pageList == null)
                    eMsg = mAppContext.getString(R.string.em_parser_error);
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
                Message msg = new Message();
                msg.what = GET_PAGE_LIST;
                msg.obj = new GetPageListPackage(checkFlag, task.pageList, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
    }

    private PreviewList getPageList(String pageContent) {
        PreviewList pageList = null;
        Pattern p = Pattern
                .compile("<div[^<>]*class=\"gdtm\"[^<>]*><div[^<>]*width:(\\d+)[^<>]*height:(\\d+)[^<>]*\\((.+?)\\)[^<>]*-(\\d+)px[^<>]*><a[^<>]*href=\"(.+?)\"[^<>]*>");
        Matcher m = p.matcher(pageContent);
        while (m.find()) {
            if (pageList == null)
                pageList = new PreviewList();
            pageList.addItem(m.group(3), m.group(4), "0", m.group(1),
                    m.group(2), m.group(5));
        }
        return pageList;
    }

    // Get Manga url and next page
    private class GetMangaUrlPackage {
        public Object checkFlag;
        public String[] strs;
        public OnGetMangaUrlListener listener;
        public String eMsg;
        
        public GetMangaUrlPackage(Object checkFlag, String[] strs, OnGetMangaUrlListener listener,
                String eMsg) {
            this.checkFlag = checkFlag;
            this.strs = strs;
            this.listener = listener;
            this.eMsg = eMsg;
        }
    }
    
    private class GetMangaUrlRunnable implements Runnable {
        private String url;
        
        public String[] strs;
        public String eMsg;

        public GetMangaUrlRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            strs = null;
            
            HttpHelper hp = new HttpHelper(mAppContext);
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
                    eMsg = mAppContext.getString(R.string.em_parser_error);
            } else
                eMsg = hp.getEMsg();
        }
    }

    public void getMangaUrl(String url, final Object checkFlag, final OnGetMangaUrlListener listener) {
        
        final GetMangaUrlRunnable task = new GetMangaUrlRunnable(url);
        mThreadPool.submit(new Job<Object>() {
            @Override
            public Object run(JobContext jc) {
                task.run();
                return null;
            }
        }, new FutureListener<Object>() {
            @Override
            public void onFutureDone(Future<Object> future) {
                Message msg = new Message();
                msg.what = GET_MANGA_URL;
                msg.obj = new GetMangaUrlPackage(checkFlag, task.strs, listener, task.eMsg);
                mHandler.sendMessage(msg);
            }
        });
    }

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
                ByteArrayOutputStream baos = null;
                try {
                    URL url = new URL(this.url);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5 * 1000);
                    conn.setReadTimeout(5 * 1000);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    if (diskCache != null) {
                        
                        int length = conn.getContentLength();
                        if (length >= 0)
                            baos = new ByteArrayOutputStream(length);
                        else
                            baos = new ByteArrayOutputStream();
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
                    if (baos != null)
                        Util.closeStreamQuietly(baos);
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
                    Downloader imageDownloader = new Downloader(mAppContext);
                    imageDownloader.setOnDownloadListener(new Downloader.OnDownloadListener() {
                        @Override
                        public void onDownloadStartConnect() {
                            
                        }
                        
                        
                        @Override
                        public void onDownloadStartDownload(int totalSize) {
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
                        public void onDownloadOver(int status, String eMsg) {
                            if (status == Downloader.COMPLETED)
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
                        File folder = new File(Config.getDownloadPath() + File.separatorChar + StringEscapeUtils.escapeHtml4(curDownloadInfo.title)); // TODO For  title contain invailed char
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
                                    
                                    Log.d(TAG, folder.getPath());
                                    
                                    curControlor = imageDownloader.resetData(folder.getPath(),
                                            String.format("%05d", curDownloadInfo.lastStartIndex + 1) + "." + Util.getExtension(imageUrlStr),
                                            imageUrlStr);
                                    imageDownloader.run();
                                    
                                    curDownloadInfo.downloadSize = 0;
                                    curDownloadInfo.totalSize = 0;
                                    
                                    int downloadStatus = imageDownloader.getStatus();
                                    if (downloadStatus == Downloader.STOPED) { // If stop by user
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
            HttpHelper hp = new HttpHelper(mAppContext);
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
            HttpHelper hp = new HttpHelper(mAppContext);
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
    
    // Post comment
    public interface OnCommentListener {
        void onSuccess(List<Comment> comments);
        void onFailure(String eMsg);
    }
    
    public void comment(final String detailUrl, final String comment,
            final OnCommentListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContent) {
                        DetailParser parser = new DetailParser();
                        parser.setMode(DetailParser.COMMENT);
                        if (parser.parser(pageContent) == DetailParser.COMMENT) {
                            listener.onSuccess(parser.comments);
                        } else {
                            listener.onFailure("parser error");   // TODO
                        }
                    }

                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);   // TODO
                    }
                });
                hp.post(detailUrl, new String[][]{
                        new String[]{"commenttext", comment},
                        new String[]{"postcomment", "Post New"}});
            }
        }).start();
    }
    
    // TODO
    
    /********** Use E-hentai API ************/
    
    // rate for gallery
    public interface OnRateListener {
        void onSuccess(float ratingAvg, int ratingCnt);
        void onFailure(String eMsg);
    }
    
    public void rate(final int gid, final String token,
            final int rating, final OnRateListener listener) {
        final JSONObject json = new JSONObject();
        try {
            json.put("method", "rategallery");
            json.put("apiuid", APIUID);
            json.put("apikey", APIKEY);
            json.put("gid", gid);
            json.put("token", token);
            json.put("rating", rating);
        } catch (JSONException e) {
            listener.onFailure(e.getMessage());
            return;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContext) {
                        RateParser parser = new RateParser();
                        if (parser.parser(pageContext)) {
                            listener.onSuccess(parser.mRatingAvg, parser.mRatingCnt);
                        } else {
                            listener.onFailure("parser error");   // TODO
                        }
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                hp.postJson(getApiUrl(), json);
            }
        }).start();
    }
    
    // vote for tag
    public interface OnVoteListener {
        void onSuccess(String tagPane);
        void onFailure(String eMsg);
    }
    
    public void vote(final int gid, final String token,
            final String group, final String tag,
            final boolean isUp, final OnVoteListener listener) {
        final JSONObject json = new JSONObject();
        try {
            json.put("method", "taggallery");
            json.put("apiuid", APIUID);
            json.put("apikey", APIKEY);
            json.put("gid", gid);
            json.put("token", token);
            json.put("tags", group + ":" + tag);
            if (isUp)
                json.put("vote", 1);
            else
                json.put("vote", -1);
        } catch (JSONException e) {
            listener.onFailure(e.getMessage());
            return;
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContext) {
                        VoteParser parser = new VoteParser();
                        if (parser.parser(pageContext)) {
                            listener.onSuccess(parser.mTagPane);
                        } else {
                            listener.onFailure("parser error");   // TODO
                        }
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                hp.postJson(getApiUrl(), json);
            }
        }).start();
    }
    
    // Add to favorite
    public interface OnAddToFavoriteListener {
        void onSuccess();
        void onFailure(String eMsg);
    }
    
    public String getAddFavouriteUrl(int gid, String token) {
        return getAddFavouriteUrl(gid, token, Config.getMode());
    }
    
    public String getAddFavouriteUrl(int gid, String token, int mode) {
        switch (mode) {
        case EX:
            return EX_HEADER + "gallerypopups.php?gid="
                    + gid + "&t=" + token + "&act=addfav";
        default:
            return G_HEADER + "gallerypopups.php?gid="
                    + gid + "&t=" + token + "&act=addfav";
        }
    }
    
    /**
     * 
     * @param gid
     * @param token
     * @param cat -1 for delete, number bigger than 9 or smaller than -1 go to 0
     * @param note max 250 characters
     * @param listener
     */
    public void addToFavorite(final int gid, final String token,
            final int cat, final String note,
            final OnAddToFavoriteListener listener) {
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContext) {
                        
                        AddToFavoriteParser parser = new AddToFavoriteParser();
                        if (parser.parser(pageContext)) {
                            listener.onSuccess();
                        } else {
                            listener.onFailure("parser error");   // TODO
                        }
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                
                String catStr;
                if (cat == -1)
                    catStr = "favdel";
                else if (cat >= 0 && cat <= 9)
                    catStr = String.valueOf(cat);
                else
                    catStr = "0";
                
                // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
                hp.post(getAddFavouriteUrl(gid, token), new String[][]{
                        new String[]{"favcat", catStr},
                        new String[]{"favnote", note == null ? "" : note},
                        new String[]{"submit", "Apply Changes"}});
            }
        }).start();
    }
    
    private static int CATEGORY_VALUES[] = {
        ListUrls.MISC,
        ListUrls.DOUJINSHI,
        ListUrls.MANGA,
        ListUrls.ARTIST_CG,
        ListUrls.GAME_CG,
        ListUrls.IMAGE_SET,
        ListUrls.COSPLAY,
        ListUrls.ASIAN_PORN,
        ListUrls.NON_H,
        ListUrls.WESTERN,
        ListUrls.UNKNOWN
    };
    
    private static String CATEGORY_STRINGS[] = {
        "misc",
        "doujinshi",
        "manga",
        "artistcg",
        "gamecg",
        "imageset",
        "cosplay",
        "asianporn",
        "non-h",
        "western",
        "unknown"
    };
    
    public static int getType(String type) {
        int i;
        for (i = 0; i < CATEGORY_STRINGS.length - 1; i++) {
            if (CATEGORY_STRINGS[i].equals(type))
                break;
        }
        return CATEGORY_VALUES[i];
    }
    
    public static String getType(int type) {
        int i;
        for (i = 0; i < CATEGORY_VALUES.length - 1; i++) {
            if (CATEGORY_VALUES[i] == type)
                break;
        }
        return CATEGORY_STRINGS[i];
    }
    
    
    // modifyFavorite
    public interface OnModifyFavoriteListener {
        void onSuccess(ArrayList<GalleryInfo> gis,
                int indexPerPage, int maxPage);
        void onFailure(String eMsg);
    }
    
    public String getModifyFavoriteUrl() {
        return getModifyFavoriteUrl(Config.getMode());
    }
    
    public String getModifyFavoriteUrl(int mode) {
        switch (mode) {
        case EX:
            return EX_HEADER + "favorites.php";
        default:
            return G_HEADER + "favorites.php";
        }
    }
    
    /**
     * 
     * @param gids
     * @param dstCat -1 for delete
     * @param srcCat
     * @param listener
     */
    public void modifyFavorite(final int[] gids, final int dstCat, final int srcCat,
            final OnModifyFavoriteListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i;
                
                HttpHelper hp = new HttpHelper(mAppContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(String pageContext) {
                        ListParser parser = new ListParser();
                        int re = parser.parser(pageContext);
                        if (re == ListParser.ALL) {
                            listener.onSuccess(parser.giList, parser.indexPerPage, parser.maxPage);
                        } else if (re == ListParser.NOT_FOUND) {
                            listener.onSuccess(parser.giList, parser.indexPerPage, parser.maxPage);
                        } else if (re == ListParser.PARSER_ERROR) {
                            listener.onFailure("parser error"); // TODO
                        }
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                
                String catStr;
                if (dstCat == -1)
                    catStr = "delete";
                else if (dstCat >= 0 && dstCat <= 9)
                    catStr = "fav" + String.valueOf(dstCat);
                else
                    catStr = "fav0";
                
                String[][] args = new String[gids.length + 2][];
                args[0] = new String[]{"ddact", catStr};
                for (i = 1; i <= gids.length; i++)
                    args[i] = new String[]{"modifygids[]", String.valueOf(gids[i-1])};
                args[i] = new String[]{"apply", "Apply"};
                
                hp.post(getFavoriteUrl(srcCat, 0), args);
            }
        }).start();
    }
    
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
    private static void getGalleryTokens(String gid, String token, String[] pages,
            OnGetGalleryMetadataListener listener) {
        new Thread(new GetGalleryTokensRunnable(gid, token, pages,
                listener)).start();
    }*/
    
}
