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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.GalleryPopular;
import com.hippo.ehviewer.data.ListUrls;
import com.hippo.ehviewer.data.LofiGalleryDetail;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Future;
import com.hippo.ehviewer.util.FutureListener;
import com.hippo.ehviewer.util.ThreadPool;
import com.hippo.ehviewer.util.ThreadPool.Job;
import com.hippo.ehviewer.util.ThreadPool.JobContext;
import com.hippo.ehviewer.util.Utils;

public class EhClient {

    private static final String TAG = "EhClient";

    public static final int FAVORITE_SLOT_NUM = 10;

    public static final int MODE_G = 0x0;
    public static final int MODE_EX = 0x1;
    public static final int MODE_LOFI = 0x2;

    public static final String API_G = "http://g.e-hentai.org/api.php";
    public static final String API_EX = "http://exhentai.org/api.php";
    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";

    private static final String LOGIN_URL = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    private static final String FORUMS_URL = "http://forums.e-hentai.org/index.php";

    public static final String HEADER_G = "http://g.e-hentai.org/";
    public static final String HEADER_EX = "http://exhentai.org/";
    public static final String HEADER_LOFI = "http://lofi.e-hentai.org/";

    private static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    /* Avatar get code */
    public static final int GET_AVATAR_OK = 0x0;
    public static final int GET_AVATAR_ERROR = 0x1;
    public static final int NO_AVATAR = 0x2;

    private final AppContext mAppContext;
    private final ThreadPool mThreadPool;
    private final Handler mHandler;
    private final EhInfo mInfo;

    private static EhClient sInstance;

    public static void createInstance(AppContext appContext) {
        sInstance = new EhClient(appContext);
    }

    public static EhClient getInstance() {
        return sInstance;
    }

    private EhClient(AppContext appContext) {
        mAppContext = appContext;
        mThreadPool = mAppContext.getNetworkThreadPool();
        mHandler = AppHandler.getInstance();
        mInfo = EhInfo.getInstance(appContext);
    }

    public static String getUrlHeader() {
        return getUrlHeader(Config.getMode());
    }

    public static String getUrlHeader(int mode) {
        switch (mode) {
        case MODE_EX:
            return HEADER_EX;
        case MODE_LOFI:
            return HEADER_LOFI;
        case MODE_G:
        default:
            return HEADER_G;
        }
    }

    /**
     * Get gellary detail url in default mode
     * @param gid
     * @param token
     * @return
     */
    public String getDetailUrl(int gid, String token) {
        return getDetailUrl(gid, token, 0, Config.getMode());
    }

    /**
     * Get gellary detail url in default mode
     * @param gid
     * @param token
     * @return
     */
    public String getDetailUrl(int gid, String token, int pageNum) {
        return getDetailUrl(gid, token, pageNum, Config.getMode());
    }

    /**
     * Get gellary detail url in target mode
     * @param gid
     * @param token
     * @return
     */
    public static String getDetailUrl(int gid, String token, int pageNum, int mode) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
        case MODE_EX:
            sb.append(HEADER_EX);
            break;
        case MODE_LOFI:
            sb.append(HEADER_LOFI);
            break;
        case MODE_G:
        default:
            sb.append(HEADER_G);
            break;
        }
        return sb.append("g/").append(gid).append("/").append(token)
                .append("/?p=").append(pageNum).toString();
    }

    public String getPageUrl(int gid, String token, int pageNum) {
        return getPageUrl(gid, token, pageNum, Config.getMode());
    }

    public static String getPageUrl(int gid, String token, int pageNum, int mode) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
        case MODE_EX:
            sb.append(HEADER_EX);
            break;
        case MODE_LOFI:
            sb.append(HEADER_LOFI);
            break;
        case MODE_G:
        default:
            sb.append(HEADER_G);
            break;
        }
        return sb.append("s/").append(token).append("/").append(gid)
                .append("-").append(pageNum).toString();
    }

    public String getApiUrl() {
        return getApiUrl(Config.getApiMode());
    }

    public static String getApiUrl(int mode) {
        switch (mode) {
        case MODE_EX:
            return API_EX;
        case MODE_G:
        default:
            return API_G;
        }
    }

    public String getFavoriteUrl(int index, int page) {
        return getFavoriteUrl(Config.getApiMode(), index, page);
    }

    public static String getFavoriteUrl(int mode, int index, int page) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
        case MODE_EX:
            sb.append(HEADER_EX);
            break;
        case MODE_G:
        default:
            sb.append(HEADER_G);
            break;
        }
        return sb.append("favorites.php?favcat=").append(index).append("&page=")
                .append(page).toString();
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

    /**
     * True if Login
     * @return
     */
    public boolean isLogin() {
        return mInfo.isLogin();
    }

    /**
     * Just remove cookie
     */
    public void logout() {
        mInfo.logout();
    }

    /**
     * Return username, if not return default string
     */
    public String getUsername() {
        return mInfo.getUsername();
    }

    /**
     * Return displayname, if not return default string
     */
    public String getDisplayname() {
        return mInfo.getDisplayname();
    }

    /**
     * Return avatar, if not return default avatar
     */
    public Bitmap getAvatar() {
        return mInfo.getAvatar();
    }

    private static final int GET_MANGA_URL = 0x6;

    private static Handler mHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

            case GET_MANGA_URL:
                GetMangaUrlPackage getMangaUrlPackage = (GetMangaUrlPackage) msg.obj;
                OnGetMangaUrlListener listener7 = getMangaUrlPackage.listener;
                if (getMangaUrlPackage.strs != null)
                    listener7.onSuccess(getMangaUrlPackage.checkFlag, getMangaUrlPackage.strs);
                else
                    listener7.onFailure(getMangaUrlPackage.checkFlag, getMangaUrlPackage.eMsg);
                break;
            }
        };
    };

    public interface OnLoginListener {
        public void onSuccess();
        public void onGetAvatar(int code);
        public void onFailure(String eMesg);
    }

    private class LoginResponder implements Runnable {
        private final boolean isOk;
        private final OnLoginListener listener;
        private final String eMesg;

        public LoginResponder(OnLoginListener listener) {
            this.isOk = true;
            this.listener = listener;
            this.eMesg = null;
        }

        public LoginResponder(OnLoginListener listener, String eMesg) {
            this.isOk = false;
            this.listener = listener;
            this.eMesg = eMesg;
        }

        @Override
        public void run() {
            if (isOk) {
                listener.onSuccess();
            } else {
                listener.onFailure(eMesg);
            }
        }
    }

    public void login(final String username, final String password,
            final OnLoginListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final HttpHelper hh = new HttpHelper(mAppContext);
                String body = hh.postForm(LOGIN_URL, new String[][] {
                        new String[] { "UserName", username },
                        new String[] { "PassWord", password },
                        new String[] { "submit", "Log me in" },
                        new String[] { "CookieDate", "1" },
                        new String[] { "temporary_https", "on" }});
                LoginResponder responder;
                if (body == null) {
                    responder = new LoginResponder(listener, hh.getEMsg());
                } else if (!body.contains("<")) {
                    responder = new LoginResponder(listener, body);
                } else {
                    LoginParser parser = new LoginParser();
                    if (parser.parser(body)) {
                        mInfo.login(username, parser.displayname);
                        getAvatar(listener);
                        responder = new LoginResponder(listener);
                    } else {
                        responder = new LoginResponder(listener, parser.eMesg);
                    }
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    private void getAvatar(final OnLoginListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String body;
                Pattern p;
                Matcher m;
                // Get user profile url
                HttpHelper hp = new HttpHelper(mAppContext);
                body = hp.get(FORUMS_URL);
                if (body == null) {
                    listener.onGetAvatar(GET_AVATAR_ERROR);
                    return;
                }
                p = Pattern.compile("<p class=\"home\">.+?<a href=\"(.+?)\">");
                m = p.matcher(body);
                if (!m.find()) {
                    listener.onGetAvatar(GET_AVATAR_ERROR);
                    return;
                }
                String profileUrl = Utils.unescapeXml(m.group(1));
                // Get avatar url
                body = hp.get(profileUrl);
                if (body == null) {
                    listener.onGetAvatar(GET_AVATAR_ERROR);
                    return;
                }
                p = Pattern.compile("<div><img src='(.+?)'[^<>]*/></div>");
                m = p.matcher(body);
                if (!m.find()) {
                    listener.onGetAvatar(NO_AVATAR);
                    return;
                }
                String avatarUrl = m.group(1);
                if (!avatarUrl.startsWith("http"))
                    avatarUrl = "http://forums.e-hentai.org/" + avatarUrl;
                // Get avatar
                Bitmap avatar = hp.getImage(avatarUrl);
                // Set avatar if not null
                if (avatar != null)
                    mInfo.setAvatar(avatar);
                // notification
                listener.onGetAvatar(GET_AVATAR_OK);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    // Get Gallery List
    public interface OnGetGListListener {
        public void onSuccess(Object checkFlag, List<GalleryInfo> giList,
                int maxPage);
        public void onFailure(Object checkFlag, String eMsg);
    }

    private class GetGListResponder implements Runnable {
        private final boolean isOk;
        private final OnGetGListListener listener;
        private final Object checkFlag;
        private final List<GalleryInfo> giList;
        private final int maxPage;
        private final String eMesg;

        public GetGListResponder(OnGetGListListener listener, Object checkFlag,
                List<GalleryInfo> giList, int maxPage) {
            this.isOk = true;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.giList = giList;
            this.maxPage = maxPage;
            this.eMesg = null;
        }

        public GetGListResponder(OnGetGListListener listener, Object checkFlag, String eMesg) {
            this.isOk = false;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.giList = null;
            this.maxPage = 0;
            this.eMesg = eMesg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(checkFlag, giList, maxPage);
            else
                listener.onFailure(checkFlag, eMesg);
        }
    }

    public void getGList(String url, Object checkFlag,
            OnGetGListListener listener) {
        getGList(url, Config.getMode(), checkFlag, listener);
    }

    public void getGList(final String url, final int mode, final Object checkFlag,
            final OnGetGListListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mAppContext);
                String body = hp.get(url);
                GetGListResponder responder;
                if (body == null) {
                    responder = new GetGListResponder(listener, checkFlag, hp.getEMsg());
                } else if (!body.contains("<")) {
                    responder = new GetGListResponder(listener, checkFlag, body);
                } else {
                    final ListParser parser = new ListParser();
                    switch (parser.parser(body, mode)) {
                    case ListParser.ALL:
                        responder = new GetGListResponder(listener, checkFlag,
                                parser.giList, parser.pageNum);
                        break;
                    case ListParser.NOT_FOUND:
                        responder = new GetGListResponder(listener, checkFlag,
                                parser.giList, 0);
                        break;
                    case ListParser.INDEX_ERROR:
                        responder = new GetGListResponder(listener, checkFlag,
                                "index error"); // TODO
                        break;
                    case ListParser.PARSER_ERROR:
                    default:
                        responder = new GetGListResponder(listener, checkFlag, "parser error"); // TODO
                        break;
                    }
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    // Get Gallery List from file search

    public static final int IMAGE_SEARCH_USE_SIMILARITY_SCAN = 0x1;
    public static final int IMAGE_SEARCH_ONLY_SEARCH_COVERS = 0x2;
    public static final int IMAGE_SEARCH_SHOW_EXPUNGED = 0x4;

    public interface OnGetGListFromImageSearchListener {
        public void onSuccess(Object checkFlag, List<GalleryInfo> giList,
                int maxPage, String newUrl);
        public void onFailure(Object checkFlag, String eMsg);
    }

    private class GetGListFromImageSearchResponder implements Runnable {

        private final boolean isOk;
        private final OnGetGListFromImageSearchListener listener;
        private final Object checkFlag;
        private final List<GalleryInfo> giList;
        private final int maxPage;
        private final String newUrl;
        private final String eMesg;

        public GetGListFromImageSearchResponder(OnGetGListFromImageSearchListener listener,
                Object checkFlag, List<GalleryInfo> giList, int maxPage, String newUrl) {
            this.isOk = true;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.giList = giList;
            this.maxPage = maxPage;
            this.newUrl = newUrl;
            this.eMesg = null;
        }

        public GetGListFromImageSearchResponder(OnGetGListFromImageSearchListener listener,
                Object checkFlag, String eMesg) {
            this.isOk = false;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.giList = null;
            this.maxPage = 0;
            this.newUrl = null;
            this.eMesg = eMesg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(checkFlag, giList, maxPage, newUrl);
            else
                listener.onFailure(checkFlag, eMesg);
        }
    }

    public static String getFileSearchUrl(int apiMode) {
        switch (apiMode) {
        case MODE_EX:
            return "http://ul.exhentai.org/image_lookup.php";
        case MODE_G:
        default:
            return "http://ul.e-hentai.org/image_lookup.php";
        }
    }

    public void getGListFromImageSearch(File file, int searchMode,
            Object checkFlag, OnGetGListFromImageSearchListener listener) {
        getGListFromImageSearch(file, null, searchMode,
                Config.getApiMode(), checkFlag, listener);
    }

    public void getGListFromImageSearch(Bitmap bmp, int searchMode,
            Object checkFlag, OnGetGListFromImageSearchListener listener) {
        getGListFromImageSearch(null, bmp, searchMode,
                Config.getApiMode(), checkFlag, listener);
    }

    private void getGListFromImageSearch(final File file, final Bitmap bitmap,
            final int searchMode, final int apiMode, final Object checkFlag,
            final OnGetGListFromImageSearchListener listener) {
        if (file == null && bitmap == null)
            listener.onFailure(checkFlag, "All null"); // TODO

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hh = new HttpHelper(mAppContext);

                List<HttpHelper.FormData> dataList = new LinkedList<HttpHelper.FormData>();
                HttpHelper.FormData data;
                // Add file or bitmap
                if (file != null) {
                    data = new HttpHelper.FileData(file);
                    data.setProperty("Content-Disposition", "form-data; name=\"sfile\"; filename=\"" + file.getName() + "\"");
                    dataList.add(data);
                } else {
                    data = new HttpHelper.BitmapData(bitmap);
                    data.setProperty("Content-Disposition", "form-data; name=\"sfile\"; filename=\"hehe.jpg\"");
                    dataList.add(data);
                }
                // Add
                data = new HttpHelper.StringData("File Search");
                data.setProperty("Content-Disposition", "form-data; name=\"f_sfile\"");
                dataList.add(data);
                // Add FILE_SEARCH_USE_SIMILARITY_SCAN
                if ((searchMode & IMAGE_SEARCH_USE_SIMILARITY_SCAN) != 0) {
                    data = new HttpHelper.StringData("on");
                    data.setProperty("Content-Disposition", "form-data; name=\"fs_similar\"");
                    dataList.add(data);
                }
                // Add FILE_SEARCH_ONLY_SEARCH_COVERS
                if ((searchMode & IMAGE_SEARCH_ONLY_SEARCH_COVERS) != 0) {
                    data = new HttpHelper.StringData("on");
                    data.setProperty("Content-Disposition", "form-data; name=\"fs_covers\"");
                    dataList.add(data);
                }
                // Add FILE_SEARCH_SHOW_EXPUNGED
                if ((searchMode & IMAGE_SEARCH_SHOW_EXPUNGED) != 0) {
                    data = new HttpHelper.StringData("on");
                    data.setProperty("Content-Disposition", "form-data; name=\"fs_exp\"");
                    dataList.add(data);
                }

                String body = hh.postFormData(getFileSearchUrl(apiMode), dataList);
                GetGListFromImageSearchResponder responder;
                if (body == null) {
                    responder = new GetGListFromImageSearchResponder(listener, checkFlag, hh.getEMsg());
                } else if (!body.contains("<")) {
                    responder = new GetGListFromImageSearchResponder(listener, checkFlag, body);
                } else {
                    String newUrl = hh.getLastUrl();
                    if (newUrl == null) {
                        responder = new GetGListFromImageSearchResponder(listener, checkFlag, "Location is null");
                    } else {
                        final ListParser parser = new ListParser();
                        switch (parser.parser(body, apiMode)) {
                        case ListParser.ALL:
                            responder = new GetGListFromImageSearchResponder(listener, checkFlag,
                                    parser.giList, parser.pageNum, newUrl);
                            break;
                        case ListParser.NOT_FOUND:
                            responder = new GetGListFromImageSearchResponder(listener, checkFlag,
                                    parser.giList, 0, newUrl);
                            break;
                        case ListParser.INDEX_ERROR:
                            responder = new GetGListFromImageSearchResponder(listener, checkFlag,
                                    "index error"); // TODO
                            break;
                        case ListParser.PARSER_ERROR:
                        default:
                            responder = new GetGListFromImageSearchResponder(listener, checkFlag, "parser error"); // TODO
                            break;
                        }
                    }
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    // Get gallery Detail
    public interface OnGetGDetailListener {
        public void onSuccess(GalleryDetail md);
        public void onFailure(String eMsg);
    }

    private class GetGDetaiResponder implements Runnable {
        private final boolean isOk;
        private final OnGetGDetailListener listener;
        private final GalleryDetail gd;
        private final String eMesg;

        public GetGDetaiResponder(OnGetGDetailListener listener, GalleryDetail gd) {
            this.isOk = true;
            this.listener = listener;
            this.gd = gd;
            this.eMesg = null;
        }

        public GetGDetaiResponder(OnGetGDetailListener listener, String eMsg) {
            this.isOk = false;
            this.listener = listener;
            this.gd = null;
            this.eMesg = eMsg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(gd);
            else
                listener.onFailure(eMesg);
        }
    }

    public void getGDetail(final String url, final GalleryDetail md,
            final OnGetGDetailListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hh = new HttpHelper(mAppContext);
                String body = hh.get(url);
                GetGDetaiResponder responder;
                if (body == null) {
                    responder = new GetGDetaiResponder(listener, hh.getEMsg());
                } else if (!body.contains("<")) {
                    responder = new GetGDetaiResponder(listener, body);
                } else {
                    DetailParser parser = new DetailParser();
                    int result = parser.parser(body, DetailParser.DETAIL | DetailParser.TAG
                            | DetailParser.PREVIEW_INFO | DetailParser.PREVIEW
                            | DetailParser.COMMENT);
                    if (result == DetailParser.OFFENSIVE) {
                        md.firstPage = "offensive";
                        responder = new GetGDetaiResponder(listener, md);
                    } else if (result == DetailParser.PINING) {
                        md.firstPage = "pining";
                        responder = new GetGDetaiResponder(listener, md);
                    } else if ((result & (DetailParser.DETAIL | DetailParser.PREVIEW_INFO |
                            DetailParser.PREVIEW)) != 0) {
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
                        responder = new GetGDetaiResponder(listener, md);
                    } else if (result == DetailParser.ERROR) {
                        responder = new GetGDetaiResponder(listener, parser.eMesg);
                    } else {
                        responder = new GetGDetaiResponder(listener,
                                mAppContext.getString(R.string.em_parser_error));
                    }
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    // Get lofi gallery Detail
    public interface OnGetLGDetailListener {
        public void onSuccess(LofiGalleryDetail md, boolean isLastPage);
        public void onFailure(String eMsg);
    }

    private class GetLGDetaiResponder implements Runnable {
        private final boolean isOk;
        private final OnGetLGDetailListener listener;
        private final LofiGalleryDetail gd;
        private final boolean isLastPage;
        private final String eMesg;

        public GetLGDetaiResponder(OnGetLGDetailListener listener, LofiGalleryDetail gd, boolean isLastPage) {
            this.isOk = true;
            this.listener = listener;
            this.gd = gd;
            this.isLastPage = isLastPage;
            this.eMesg = null;
        }

        public GetLGDetaiResponder(OnGetLGDetailListener listener, String eMsg) {
            this.isOk = false;
            this.listener = listener;
            this.gd = null;
            this.isLastPage = false;
            this.eMesg = eMsg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(gd, isLastPage);
            else
                listener.onFailure(eMesg);
        }
    }

    public void getLGDetail(final String url, final LofiGalleryDetail lgd,
            final OnGetLGDetailListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hh = new HttpHelper(mAppContext);
                String body = hh.get(url);
                GetLGDetaiResponder responder;
                if (body == null) {
                    responder = new GetLGDetaiResponder(listener, hh.getEMsg());
                } else if (!body.contains("<")) {
                    responder = new GetLGDetaiResponder(listener, body);
                } else {
                    LofiDetailParser parser = new LofiDetailParser();
                    if (parser.parser(body)) {
                        lgd.setPreview(0, parser.preview);
                        lgd.previewPerPage = parser.preview.size();
                        responder = new GetLGDetaiResponder(listener, lgd, parser.isLastPage);
                    } else {
                        responder = new GetLGDetaiResponder(listener,
                                mAppContext.getString(R.string.em_parser_error));
                    }
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }


    // Get preview list
    public interface OnGetPreviewListListener {
        public void onSuccess(Object checkFlag, PreviewList pageList, boolean isLastPage);
        public void onFailure(Object checkFlag, String eMsg);
    }

    private class GetPreviewListResponder implements Runnable {
        private final boolean isOk;
        private final OnGetPreviewListListener listener;
        private final Object checkFlag;
        private final PreviewList pageList;
        private final boolean isLastPage;
        private final String eMesg;

        public GetPreviewListResponder(OnGetPreviewListListener listener,
                Object checkFlag, PreviewList pageList, boolean isLastPage) {
            this.isOk = true;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.pageList = pageList;
            this.isLastPage = isLastPage;
            this.eMesg = null;
        }

        public GetPreviewListResponder(OnGetPreviewListListener listener,
                Object checkFlag, String eMsg) {
            this.isOk = false;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.pageList = null;
            this.isLastPage = false;
            this.eMesg = eMsg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(checkFlag, pageList, isLastPage);
            else
                listener.onFailure(checkFlag, eMesg);
        }
    }

    public void getPreviewList(final String url, final int mode, final Object checkFlag,
            final OnGetPreviewListListener listener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hh = new HttpHelper(mAppContext);
                String body = hh.get(url);
                GetPreviewListResponder responder;
                if (body != null) {
                    if (mode != EhClient.MODE_LOFI) {
                        DetailParser parser = new DetailParser();
                        if (parser.parser(body, DetailParser.PREVIEW) ==
                                DetailParser.PREVIEW)
                            responder = new GetPreviewListResponder(listener, checkFlag, parser.previewList, false);
                        else
                            responder = new GetPreviewListResponder(listener, checkFlag, "Parser error");
                    } else {
                        LofiDetailParser parser = new LofiDetailParser();
                        if (parser.parser(body))
                            responder = new GetPreviewListResponder(listener, checkFlag, parser.preview, parser.isLastPage);
                        else
                            responder = new GetPreviewListResponder(listener, checkFlag, "Parser error");
                    }
                } else {
                    responder = new GetPreviewListResponder(listener, checkFlag, hh.getEMsg());
                }
                mHandler.post(responder);
            }
        });
        thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
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
        private final String url;

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
                    strs[2] = Utils.unescapeXml(m.group(3));
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
                mHandler1.sendMessage(msg);
            }
        });
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        DetailParser parser = new DetailParser();
                        if (parser.parser(body, DetailParser.COMMENT) ==
                                DetailParser.COMMENT) {
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
                hp.postForm(detailUrl, new String[][]{
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        RateParser parser = new RateParser();
                        if (parser.parser(body)) {
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        VoteParser parser = new VoteParser();
                        if (parser.parser(body)) {
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
        return getAddFavouriteUrl(gid, token, Config.getApiMode());
    }

    public static String getAddFavouriteUrl(int gid, String token, int mode) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
        case MODE_EX:
            sb.append(HEADER_EX);
            break;
        case MODE_G:
        default:
            sb.append(HEADER_G);
            break;
        }
        return sb.append("gallerypopups.php?gid=").append(gid).append("&t=")
                .append(token).append("&act=addfav").toString();
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        AddToFavoriteParser parser = new AddToFavoriteParser();
                        if (parser.parser(body)) {
                            listener.onSuccess();
                            // Analytics
                            Analytics.addToFavoriteGallery(mAppContext, gid, token);
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
                hp.postForm(getAddFavouriteUrl(gid, token), new String[][]{
                        new String[]{"favcat", catStr},
                        new String[]{"favnote", note == null ? "" : note},
                        new String[]{"submit", "Apply Changes"}});
            }
        }).start();
    }

    private static final int[] CATEGORY_VALUES = {
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

    // TODO How about "Private"
    private static final String[][] CATEGORY_STRINGS = {
        new String[]{"misc"},
        new String[]{"doujinshi"},
        new String[]{"manga"},
        new String[]{"artistcg", "Artist CG Sets"},
        new String[]{"gamecg", "Game CG Sets"},
        new String[]{"imageset", "Image Sets"},
        new String[]{"cosplay"},
        new String[]{"asianporn", "Asian Porn"},
        new String[]{"non-h"},
        new String[]{"western"},
        new String[]{"unknown"}
    };

    public static int getType(String type) {
        int i;
        for (i = 0; i < CATEGORY_STRINGS.length - 1; i++) {
            for (String str : CATEGORY_STRINGS[i])
                if (str.equalsIgnoreCase(type))
                    return CATEGORY_VALUES[i];
        }

        return CATEGORY_VALUES[i];
    }

    public static String getType(int type) {
        int i;
        for (i = 0; i < CATEGORY_VALUES.length - 1; i++) {
            if (CATEGORY_VALUES[i] == type)
                break;
        }
        return CATEGORY_STRINGS[i][0];
    }


    // modifyFavorite
    public interface OnModifyFavoriteListener {
        void onSuccess(List<GalleryInfo> gis, int pageNum);
        void onFailure(String eMsg);
    }

    public String getModifyFavoriteUrl() {
        return getModifyFavoriteUrl(Config.getMode());
    }

    public static String getModifyFavoriteUrl(int mode) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
        case MODE_EX:
            sb.append(HEADER_EX);
            break;
        case MODE_G:
        default:
            sb.append(HEADER_G);
            break;
        }
        return sb.append("favorites.php").toString();
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        ListParser parser = new ListParser();
                        int re = parser.parser(body, MODE_G); // Only MODE_G and MODE_EX
                        if (re == ListParser.ALL) {
                            listener.onSuccess(parser.giList, parser.pageNum);
                        } else if (re == ListParser.NOT_FOUND) {
                            listener.onSuccess(parser.giList, parser.pageNum);
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

                hp.postForm(getFavoriteUrl(srcCat, 0), args);
            }
        }).start();
    }

    public interface OnGetPopularListener {
        void onSuccess(List<GalleryInfo> gis, long timeStamp);
        void onFailure(String eMsg);
    }

    public void getPopular(final OnGetPopularListener listener) {
        final JSONObject json = new JSONObject();
        try {
            json.put("method", "popular");
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
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        try {
                            List<GalleryInfo> gis = new ArrayList<GalleryInfo>();
                            JSONObject js = new JSONObject(body);
                            js = js.getJSONObject("popular");

                            if (!js.has("galleries")) {
                                listener.onFailure(js.getString("error"));
                                return;
                            }

                            JSONArray ja = js.getJSONArray("galleries");
                            for (int i = 0; i < ja.length(); i++) {
                                JSONObject j = ja.getJSONObject(i);
                                GalleryPopular gi = new GalleryPopular();
                                if (j.has("count"))
                                    gi.count = j.getLong("count");
                                else
                                    gi.count = -1;
                                gi.gid = j.getInt("gid");
                                gi.token = j.getString("token");
                                gi.title = Utils.unescapeXml(j.getString("title"));
                                gi.posted = AppContext.sFormatter.format(Long.parseLong(j.getString("posted")) * 1000);
                                gi.thumb = j.getString("thumb");
                                gi.category = getType(j.getString("category"));
                                gi.uploader = j.getString("uploader");
                                gi.rating = Float.parseFloat(j.getString("rating"));
                                gi.generateSLang();
                                gis.add(gi);
                            }

                            long timeStamp;
                            if (js.has("time"))
                                timeStamp = js.getLong("time");
                            else
                                timeStamp = -1;

                            listener.onSuccess(gis, timeStamp);
                        } catch (JSONException e) {
                            listener.onFailure(e.getMessage());
                        }
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                hp.postJson(API_EHVIEWER, json);
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
