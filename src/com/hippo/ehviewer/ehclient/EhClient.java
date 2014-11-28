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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Process;

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.AppHandler;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ApiGalleryInfo;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.GalleryPopular;
import com.hippo.ehviewer.data.LofiGalleryDetail;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.BgThread;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Utils;

public class EhClient {
    @SuppressWarnings("unused")
    private static final String TAG = EhClient.class.getSimpleName();

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

    public static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    /* Avatar get code */
    public static final int GET_AVATAR_OK = 0x0;
    public static final int GET_AVATAR_ERROR = 0x1;
    public static final int NO_AVATAR = 0x2;

    private final Context mContext;
    private final Handler mHandler;
    private final EhInfo mInfo;

    private static EhClient sInstance;

    public static void createInstance(Context context) {
        sInstance = new EhClient(context);
    }

    public static EhClient getInstance() {
        return sInstance;
    }

    private EhClient(Context context) {
        mContext = context;
        mHandler = AppHandler.getInstance();
        mInfo = EhInfo.getInstance(context);
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

    public String getFavoriteUrl(int page) {
        return getFavoriteUrl(Config.getApiMode(), page);
    }

    public static String getFavoriteUrl(int mode, int page) {
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
        return sb.append("favorites.php?page=").append(page).toString();
    }

    public String getFavoriteUrlWithCat(int index, int page) {
        return getFavoriteUrlWithCat(Config.getApiMode(), index, page);
    }

    public static String getFavoriteUrlWithCat(int mode, int index, int page) {
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
    public Drawable getAvatar() {
        return mInfo.getAvatar();
    }

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
                final HttpHelper hh = new HttpHelper(mContext);
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
                HttpHelper hp = new HttpHelper(mContext);
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
                    mInfo.setAvatar(new BitmapDrawable(EhClient.this.mContext.getResources(), avatar));
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
                HttpHelper hp = new HttpHelper(mContext);
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
                                mContext.getString(R.string.em_index_error));
                        break;
                    case ListParser.PARSER_ERROR:
                    default:
                        responder = new GetGListResponder(listener, checkFlag,
                                mContext.getString(R.string.em_parser_error));
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
            listener.onFailure(checkFlag, mContext.getString(R.string.invalid_input));

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hh = new HttpHelper(mContext);

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
                                    mContext.getString(R.string.em_index_error));
                            break;
                        case ListParser.PARSER_ERROR:
                        default:
                            responder = new GetGListFromImageSearchResponder(listener, checkFlag,
                                    mContext.getString(R.string.em_parser_error));
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
                HttpHelper hh = new HttpHelper(mContext);
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
                            DetailParser.PREVIEW)) == (DetailParser.DETAIL | DetailParser.PREVIEW_INFO |
                            DetailParser.PREVIEW)) {
                        // At least get detail and preview
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
                                mContext.getString(R.string.em_parser_error));
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
                HttpHelper hh = new HttpHelper(mContext);
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
                                mContext.getString(R.string.em_parser_error));
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
                HttpHelper hh = new HttpHelper(mContext);
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

    // Post comment
    public interface OnCommentListener {
        void onSuccess(List<Comment> comments);
        void onFailure(String eMsg);
    }

    public void comment(final String detailUrl, final String comment,
            final OnCommentListener listener) {
        new BgThread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        DetailParser parser = new DetailParser();
                        if (parser.parser(body, DetailParser.COMMENT) ==
                                DetailParser.COMMENT) {
                            listener.onSuccess(parser.comments);
                        } else {
                            listener.onFailure( mContext.getString(R.string.em_parser_error));
                        }
                    }

                    @Override
                    public void onFailure(String eMsg) {
                        listener.onFailure(eMsg);
                    }
                });
                hp.postForm(detailUrl, new String[][]{
                        new String[]{"commenttext", comment},
                        new String[]{"postcomment", "Post New"}});
            }
        }).start();
    }

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
                HttpHelper hp = new HttpHelper(mContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        RateParser parser = new RateParser();
                        if (parser.parser(body)) {
                            listener.onSuccess(parser.mRatingAvg, parser.mRatingCnt);
                        } else {
                            listener.onFailure(mContext.getString(R.string.em_parser_error));
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
                HttpHelper hp = new HttpHelper(mContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        VoteParser parser = new VoteParser();
                        if (parser.parser(body)) {
                            listener.onSuccess(parser.mTagPane);
                        } else {
                            listener.onFailure( mContext.getString(R.string.em_parser_error));
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
                HttpHelper hp = new HttpHelper(mContext);
                hp.setOnRespondListener(new HttpHelper.OnRespondListener() {
                    @Override
                    public void onSuccess(Object obj) {
                        String body = (String)obj;
                        AddToFavoriteParser parser = new AddToFavoriteParser();
                        if (parser.parser(body)) {
                            listener.onSuccess();
                            // Analytics
                            Analytics.addToFavoriteGallery(mContext, gid, token);
                        } else {
                            listener.onFailure( mContext.getString(R.string.em_parser_error));
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
        new BgThread(new Runnable() {
            @Override
            public void run() {
                int i;

                HttpHelper hp = new HttpHelper(mContext);
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
                            listener.onFailure( mContext.getString(R.string.em_parser_error));
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

                hp.postForm(getFavoriteUrlWithCat(srcCat, 0), args);
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
        new BgThread(new Runnable() {
            @Override
            public void run() {
                HttpHelper hp = new HttpHelper(mContext);
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
                                gi.posted = Utils.sDate.format(Long.parseLong(j.getString("posted")) * 1000);
                                gi.thumb = j.getString("thumb");
                                gi.category = EhUtils.getCategory(j.getString("category"));
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

    public interface OnGetApiGalleryInfoListener {
        public void onSuccess(Object checkFlag, ApiGalleryInfo[] agiArray);
        public void onFailure(Object checkFlag, String eMsg);
    }

    private class GetApiGalleryInfoResponder implements Runnable {
        private final boolean isOk;
        private final OnGetApiGalleryInfoListener listener;
        private final Object checkFlag;
        private final ApiGalleryInfo[] agiArray;
        private final String eMesg;

        public GetApiGalleryInfoResponder(OnGetApiGalleryInfoListener listener,
                Object checkFlag, ApiGalleryInfo[] agiArray) {
            this.isOk = true;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.agiArray = agiArray;
            this.eMesg = null;
        }

        public GetApiGalleryInfoResponder(OnGetApiGalleryInfoListener listener,
                Object checkFlag, String eMsg) {
            this.isOk = false;
            this.listener = listener;
            this.checkFlag = checkFlag;
            this.agiArray = null;
            this.eMesg = eMsg;
        }

        @Override
        public void run() {
            if (isOk)
                listener.onSuccess(checkFlag, agiArray);
            else
                listener.onFailure(checkFlag, eMesg);
        }
    }

    private ApiGalleryInfo Json2Agi(JSONObject js) throws JSONException {
        ApiGalleryInfo agi = new ApiGalleryInfo();
        agi.gid = js.getInt("gid");
        agi.token = js.getString("token");
        agi.archiver_key = js.getString("archiver_key");
        agi.title = js.getString("title");
        agi.title_jpn = js.getString("title_jpn");
        agi.category = EhUtils.getCategory(js.getString("category"));
        agi.thumb = js.getString("thumb");
        agi.uploader = js.getString("uploader");
        agi.posted = Utils.sDate.format(Long.parseLong(js.getString("posted")) * 1000);
        agi.filecount = js.getString("filecount");
        agi.filesize = js.getLong("filesize");
        agi.expunged = js.getBoolean("expunged");
        agi.rating = Float.parseFloat(js.getString("rating"));
        agi.torrentcount = js.getString("torrentcount");
        JSONArray tags = js.getJSONArray("tags");
        agi.apiTags = new String[tags.length()];
        for (int j = 0; j < tags.length(); j++)
            agi.apiTags[j] = tags.getString(j);
        return agi;
    }

    public void getApiGalleryInfo(final Object checkFlag, final int[] gids, final String[] tokens, final OnGetApiGalleryInfoListener l) {
        new BgThread() {
            @Override
            public void run() {
                GetApiGalleryInfoResponder responder;
                ApiGalleryInfo[] agiArray;
                int requstLen;
                // Create json
                JSONObject json = new JSONObject();
                try {
                    json.put("method", "gdata");
                    JSONArray jsonA = new JSONArray();
                    requstLen = Math.min(gids.length, tokens.length);
                    agiArray = new ApiGalleryInfo[requstLen];
                    for (int i = 0; i < requstLen; i++) {
                        JSONArray ja = new JSONArray();
                        ja.put(gids[i]);
                        ja.put(tokens[i]);
                        jsonA.put(ja);
                    }
                    json.put("gidlist", jsonA);
                } catch (Throwable e) {
                    responder = new GetApiGalleryInfoResponder(l, null, "Create json error");
                    mHandler.post(responder);
                    return;
                }
                // Post
                HttpHelper hh = new HttpHelper(mContext);
                String body = hh.postJson(getApiUrl(), json);
                if (body != null) {
                    try {
                        json = new JSONObject(body);
                        JSONArray jsonA = (JSONArray) json.get("gmetadata");
                        for (int i = 0; i < jsonA.length(); i++) {
                            ApiGalleryInfo agi = Json2Agi(jsonA.getJSONObject(i));
                            // Add to agiArray
                            for (int j = 0; j < requstLen; j++) {
                                if (agi.gid == gids[j])
                                    agiArray[j] = agi;
                            }
                        }
                        responder = new GetApiGalleryInfoResponder(l, checkFlag, agiArray);
                    } catch (Throwable e) {
                        responder = new GetApiGalleryInfoResponder(l, checkFlag, "Json error");
                    }
                } else {
                    responder = new GetApiGalleryInfoResponder(l, checkFlag, hh.getEMsg());
                }
                mHandler.post(responder);
            }
        }.start();
    }

    public ApiGalleryInfo[] getApiGalleryInfo(int[] gids, String[] tokens) {
        ApiGalleryInfo[] agiArray;
        int requstLen;
        // Create json
        JSONObject json = new JSONObject();
        try {
            json.put("method", "gdata");
            JSONArray jsonA = new JSONArray();
            requstLen = Math.min(gids.length, tokens.length);
            agiArray = new ApiGalleryInfo[requstLen];
            for (int i = 0; i < requstLen; i++) {
                JSONArray ja = new JSONArray();
                ja.put(gids[i]);
                ja.put(tokens[i]);
                jsonA.put(ja);
            }
            json.put("gidlist", jsonA);
        } catch (Throwable e) {
            return null;
        }
        // Post api
        HttpHelper hh = new HttpHelper(mContext);
        String body = hh.postJson(getApiUrl(), json);
        if (body == null) {
            return null;
        } else {
            try {
                json = new JSONObject(body);
                JSONArray jsonA = (JSONArray) json.get("gmetadata");
                for (int i = 0; i < jsonA.length(); i++) {
                    ApiGalleryInfo agi = Json2Agi(jsonA.getJSONObject(i));
                    // Add to agiArray
                    for (int j = 0; j < requstLen; j++) {
                        if (agi.gid == gids[j])
                            agiArray[j] = agi;
                    }
                }
                return agiArray;
            } catch (Throwable e) {
                return null;
            }
        }
    }

}
