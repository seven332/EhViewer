package com.hippo.ehviewer.activity;

import java.util.ArrayList;

import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.MangaDetail;
import com.hippo.ehviewer.PageList;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.dialog.DialogBuilder;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Favourite;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.OlImageView;
import com.hippo.ehviewer.widget.AutoWrapLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// TODO hover to show larger preview
// TODO add detail cache

public class MangaDetailActivity extends Activity {

    private static final String TAG = "MangaDetailActivity";
    
    private String url;
    //private String gid;
    //private String token;

    private ScrollView scrollView;
    private LinearLayout mainView;
    private ProgressBar waitPb;
    private Button refeshButton;
    private ViewGroup mangaDetailNormal;
    private ViewGroup mangaDetailOffensive;
    private ViewGroup mangaDetailPining;
    private ViewGroup pageListMain;
    private AutoWrapLayout pageListLayout;
    private ProgressBar waitPageList;
    private TextView previewNumText;
    private TextView previewNumText2;
    private Button previewRefreshButton;
    private View bottomPanel;
    
    private AlertDialog goToDialog;
    
    private MangaDetail mangaDetail;
    private int curPage;
    private int screenHeight;
    private int columnCount;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    private class PageImageGetListener implements EhClient.OnGetImageListener {
        @Override
        public void onSuccess(Object checkFlag, Object res) {
            if (res instanceof Bitmap && checkFlag instanceof int[]) {
                int[] indexs = (int[])checkFlag;
                addPageImage(indexs[0], indexs[1], (Bitmap) res);
            } else {
                Log.e(TAG, "WTF? PageImageGetListener is error !");
                waitPageList.setVisibility(View.GONE);
                previewRefreshButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onFailure(int errorMessageId) {
            waitPageList.setVisibility(View.GONE);
            previewRefreshButton.setVisibility(View.VISIBLE);
        }
    }
    
    class TextViewWithUrl extends TextView {
        private String url;

        public TextViewWithUrl(Context context, String url) {
            super(context);
            this.url = url;
        }
    }
    
    private void addPageItem(int page) {
        if (page != curPage)
            return;
        
        waitPageList.setVisibility(View.GONE);
        previewRefreshButton.setVisibility(View.GONE);
        
        PageList pageList = mangaDetail.previewLists[page];
        if (pageList == null) {
            Log.d(TAG, "WTF, I may check mangaDetail.pageLists[page] is not null");
            Toast.makeText(this, "WTF, I may check mangaDetail.pageLists[page] is not null", Toast.LENGTH_SHORT).show();
            return;
        }
        int index = page * mangaDetail.previewPerPage + 1;
        int rowIndex = 0;
        for (PageList.Row row : pageList.rowArray) {
            for (PageList.Row.Item item : row.itemArray) {
                TextViewWithUrl tvu = new TextViewWithUrl(MangaDetailActivity.this,
                        item.url);
                tvu.setGravity(Gravity.CENTER);
                tvu.setText(String.valueOf(index));
                final int index1 = index;
                tvu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MangaDetailActivity.this,
                                MangaActivity.class);
                        intent.putExtra("url", ((TextViewWithUrl) v).url);
                        intent.putExtra("gid", mangaDetail.gid);
                        intent.putExtra("firstPage", index1 - 1);
                        intent.putExtra("pageSum", mangaDetail.pages);
                        startActivity(intent);
                    }
                });
                // Set white drawable for temp
                ColorDrawable white = new ColorDrawable(getResources().getColor(android.R.color.white));
                white.setBounds(0, 0, item.width, item.height);
                tvu.setCompoundDrawables(null, white, null, null);
                
                AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
                lp.leftMargin = Ui.dp2pix(8);
                lp.topMargin = Ui.dp2pix(8);
                lp.rightMargin = Ui.dp2pix(8);
                lp.bottomMargin = Ui.dp2pix(8);
                
                pageListLayout.addView(tvu, lp);
                index++;
            }
            // Download pic
            EhClient.getImage(row.imageUrl, mangaDetail.gid + "-preview-"
                    + curPage + "-" + rowIndex, Util.getResourcesType(row.imageUrl), Cache.memoryCache,
                    Cache.cpCache, new int[]{page, rowIndex}, new PageImageGetListener());
            rowIndex ++;
            
            Log.d(TAG, "add page row");
        }
    }
    
    private void addPageImage(int page, int rowIndex, Bitmap bitmap) {
        if (page != curPage)
            return;
        PageList pageList = mangaDetail.previewLists[page];
        if (pageList == null) {
            Log.d(TAG, "WTF, I may check mangaDetail.pageLists[page] is not null");
            return;
        }
        
        int maxWidth = bitmap.getWidth();
        int maxHeight = bitmap.getHeight();
        
        PageList.Row row = pageList.rowArray.get(rowIndex);
        ArrayList<PageList.Row.Item> items = row.itemArray;
        
        int startIndex = row.startIndex;
        int endIndex = startIndex + items.size();
        for(int i = startIndex ; i < endIndex; i++) {
            PageList.Row.Item item = items.get(i - startIndex);
            
            // 
            if (item.xOffset + item.width > maxWidth)
                item.width = maxWidth - item.xOffset;
            if (item.yOffset + item.height > maxHeight)
                item.height = maxHeight - item.yOffset;
            
            BitmapDrawable bitmapDrawable = new BitmapDrawable(
                    getResources(), Bitmap.createBitmap(bitmap,
                    item.xOffset, item.yOffset, item.width, item.height));
            bitmapDrawable.setBounds(0, 0, item.width, item.height);
            
            TextViewWithUrl tvu = (TextViewWithUrl)pageListLayout.getChildAt(i);
            tvu.setCompoundDrawables(null, bitmapDrawable, null, null);
        }
        
    }

    private class MangaDetailGetListener implements
            EhClient.OnGetManagaDetailListener {
        @Override
        public void onSuccess(MangaDetail md) {
            Cache.mdCache.put(mangaDetail.gid, md);
            layout(md);
        }

        @Override
        public void onFailure(int errorMessageId) {
            // Delete progress bar
            waitPb.setVisibility(View.GONE);
            // Show refresh button
            Toast.makeText(MangaDetailActivity.this,
                    getString(errorMessageId), Toast.LENGTH_SHORT).show();

            refeshButton.setVisibility(View.VISIBLE);
        }
    }

    // *** Button onclick ***//
    // Refresh
    public void buttonRefresh(View v) {
        MangaDetailGetListener listener = new MangaDetailGetListener();
        EhClient.getManagaDetail(url, mangaDetail, listener);
        // Delete refresh button
        refeshButton.setVisibility(View.GONE);
        // Add progressBar
        waitPb.setVisibility(View.VISIBLE);
    }

    // finish this activity
    public void buttonFinsh(View v) {
        MangaDetailActivity.this.finish();
    }

    // Offensive once
    public void buttonOnce(View v) {
        // Handle url
        if (url.endsWith("nw=session")) {

        } else if (url.endsWith("nw=always"))
            url.replace("nw=always", "nw=session");
        else {
            if (!url.endsWith("/"))
                url += "/";
            url += "?nw=session";
        }

        MangaDetailGetListener listener = new MangaDetailGetListener();
        EhClient.getManagaDetail(url, mangaDetail, listener);
        // Delete offensiveView
        mangaDetailOffensive.setVisibility(View.GONE);
        // Add progressBar
        waitPb.setVisibility(View.VISIBLE);
    }

    // Offensive once
    public void buttonEvery(View v) {
        // Handle url
        if (url.endsWith("nw=always")) {

        } else if (url.endsWith("nw=session"))
            url.replace("nw=session", "nw=always");
        else {
            if (!url.endsWith("/"))
                url += "/";
            url += "?nw=always";
        }

        MangaDetailGetListener listener = new MangaDetailGetListener();
        EhClient.getManagaDetail(url, mangaDetail, listener);
        // Delete offensiveView
        mangaDetailOffensive.setVisibility(View.GONE);
        // Add progressBar
        waitPb.setVisibility(View.VISIBLE);
    }

    // Read
    public void buttonRead(View v) {
        Intent intent = new Intent(MangaDetailActivity.this,
                MangaActivity.class);
        intent.putExtra("url", mangaDetail.firstPage);
        intent.putExtra("gid", mangaDetail.gid);
        intent.putExtra("firstPage", 0);
        intent.putExtra("pageSum", mangaDetail.pages);
        startActivity(intent);
    }

    //
    public void ButtonBack(View v) {
        if (curPage <= 0)
            return;
        curPage--;
        refreshPageList();
    }

    //
    public void ButtonFront(View v) {
        if (curPage >= mangaDetail.previewSum - 1)
            return;
        curPage++;
        refreshPageList();
    }

    public void ButtonPreviewRefresh(View v) {
        refreshPageList();
    }

    // *** Button onclick end ***//
    
    private AlertDialog createGoToDialog() {
        return new DialogBuilder(this).setTitle(R.string.jump)
                .setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return mangaDetail.previewSum;
                    }
                    @Override
                    public Object getItem(int position) {
                        return position;
                    }
                    @Override
                    public long getItemId(int position) {
                        return position;
                    }
                    @Override
                    public View getView(int position, View convertView,
                            ViewGroup parent) {
                        LayoutInflater inflater = MangaDetailActivity.this.getLayoutInflater();
                        View view = (View)inflater.inflate(R.layout.list_item_text, null);
                        TextView tv = (TextView)view.findViewById(android.R.id.text1);
                        tv.setText(String.format(getString(R.string.some_page), position + 1));
                        return tv;
                    }
                }, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        goToDialog.dismiss();
                        if (position != curPage) {
                            curPage = position;
                            refreshPageList();
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View paramView) {
                        goToDialog.dismiss();
                    }
                }).create();
    }
    
    public void goTo(View v) {
        goToDialog.show();
    }
    
    private void refreshPageList() {
        pageListLayout.removeAllViews();
        waitPageList.setVisibility(View.VISIBLE);
        previewRefreshButton.setVisibility(View.GONE);
        
        previewNumText.setText(String.format("%d / %d",
                curPage+1, mangaDetail.previewSum));
        previewNumText2.setText(String.format("%d / %d",
                curPage+1, mangaDetail.previewSum));

        PageList pageList = mangaDetail.previewLists[curPage];

        if (pageList == null) {
            String tempUrl = url;
            if (!url.endsWith("/"))
                tempUrl += "/";
            tempUrl += "?p=" + curPage;

            EhClient.getPageList(tempUrl, curPage,
                    new EhClient.OnGetPageListListener() {
                        @Override
                        public void onSuccess(Object checkFlag, PageList pageList) {
                            int page = (Integer)checkFlag;
                            mangaDetail.previewLists[page] = pageList;
                            addPageItem(page);
                        }

                        @Override
                        public void onFailure(Object checkFlag, int errorMessageId) {
                            int page = (Integer)checkFlag;
                            if (page == curPage) {
                                Toast.makeText(MangaDetailActivity.this,
                                        getString(errorMessageId), Toast.LENGTH_SHORT).show();
                                waitPageList.setVisibility(View.GONE);
                                previewRefreshButton.setVisibility(View.VISIBLE);    
                            }
                        }
                    });
        } else {
            addPageItem(curPage);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.fixColour(this);
        }
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // Download service
        Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        // Get information
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        String gid = intent.getStringExtra("gid");
        boolean getFromCache = true;
        mangaDetail = Cache.mdCache.get(gid);
        if (mangaDetail == null) {
            getFromCache = false;
            mangaDetail = new MangaDetail();
            mangaDetail.gid = gid;
            mangaDetail.token = intent.getStringExtra("token");
            mangaDetail.archiver_key = intent.getStringExtra("archiver_key");
            mangaDetail.title = intent.getStringExtra("title");
            mangaDetail.title_jpn = intent.getStringExtra("title_jpn");
            mangaDetail.category = intent.getIntExtra("category", ListUrls.UNKNOWN);
            mangaDetail.thumb = intent.getStringExtra("thumb");
            mangaDetail.uploader = intent.getStringExtra("uploader");
            mangaDetail.posted = intent.getStringExtra("posted");
            mangaDetail.filecount = intent.getStringExtra("filecount");
            mangaDetail.filesize = intent.getLongExtra("filesize", 0);
            mangaDetail.expunged = intent.getBooleanExtra("expunged", false);
            mangaDetail.rating = intent.getStringExtra("rating");
            mangaDetail.torrentcount = intent.getStringExtra("torrentcount");
            mangaDetail.tags = null;
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mainView = (LinearLayout) findViewById(R.id.manga_detail);
        scrollView = (ScrollView) findViewById(R.id.manga_detail_scrollview);
        waitPb = (ProgressBar) findViewById(R.id.detail_wait);
        refeshButton = (Button) findViewById(R.id.detail_refresh);
        mangaDetailNormal = (ViewGroup) findViewById(R.id.manga_detail_normal);
        mangaDetailOffensive = (ViewGroup) findViewById(R.id.manga_detail_offensive);
        mangaDetailPining = (ViewGroup) findViewById(R.id.manga_detail_pining);
        pageListMain = (ViewGroup) findViewById(R.id.page_list);
        pageListLayout = (AutoWrapLayout) findViewById(R.id.paper_list_layout);
        waitPageList = (ProgressBar) findViewById(R.id.paper_list_wait);
        previewRefreshButton = (Button) findViewById(R.id.preview_button_refresh);
        previewNumText = (TextView) findViewById(R.id.preview_num);
        bottomPanel = (View)findViewById(R.id.bottom_panel);
        previewNumText2 = (TextView)bottomPanel.findViewById(R.id.preview_num);
        
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        setTitle(mangaDetail.gid);
        
        //calcGridLayout();
        
        OlImageView coverImage = (OlImageView) findViewById(R.id.detail_cover);
        coverImage.setUrl(mangaDetail.thumb);
        coverImage.setKey(mangaDetail.gid);
        coverImage.setCache(Cache.memoryCache, Cache.cpCache);
        coverImage.loadImage(false);
        
        TextView title = (TextView) findViewById(R.id.detail_title);
        title.setText(mangaDetail.title);
        
        // get from cache
        if (getFromCache)
            layout(mangaDetail);
        else
            EhClient.getManagaDetail(url, mangaDetail, new MangaDetailGetListener());
    }
    
    private void layout(MangaDetail md) {
        // Delete progress bar
        waitPb.setVisibility(View.GONE);

        // Check offensive or not
        if (md.firstPage.equals("offensive"))
            mangaDetailOffensive.setVisibility(View.VISIBLE);
        // Check pining or not
        else if (md.firstPage.equals("pining"))
            mangaDetailPining.setVisibility(View.VISIBLE);
        else {
            mangaDetail = md;
            
            ImageView typeIv = (ImageView) findViewById(R.id.detail_type);
            Ui.setType(typeIv, mangaDetail.category);
            
            TextView uploaderTv = (TextView) findViewById(R.id.detail_uploader);
            uploaderTv.setText(String.format(
                    getString(R.string.detail_uploader), mangaDetail.uploader));
            
            TextView postedTv = (TextView) findViewById(R.id.detail_posted);
            postedTv.setText(String.format(
                    getString(R.string.detail_posted), mangaDetail.posted));

            TextView pagesTv = (TextView) findViewById(R.id.detail_pages);
            pagesTv.setText(String.format(getString(R.string.detail_pages),
                    mangaDetail.pages));

            TextView sizeTv = (TextView) findViewById(R.id.detail_size);
            sizeTv.setText(String.format(getString(R.string.detail_size),
                    mangaDetail.size));

            TextView languageTv = (TextView) findViewById(R.id.detail_language);
            languageTv.setText(String.format(
                    getString(R.string.detail_language),
                    mangaDetail.language));

            LinearLayout ratell = (LinearLayout) findViewById(R.id.detail_rate);
            Ui.addStar(ratell, mangaDetail.rating);

            TextView averageTv = (TextView) findViewById(R.id.detail_average);
            averageTv.setText(String
                    .format(getString(R.string.detail_average),
                            mangaDetail.rating));

            TextView peopleTv = (TextView) findViewById(R.id.detail_people);
            peopleTv.setText(String.format(
                    getString(R.string.detail_people), mangaDetail.people));

            if (mangaDetail.tags != null) {
                LinearLayout tagsLayout = (LinearLayout) findViewById(R.id.tags_layout);
                addTags(tagsLayout, mangaDetail.tags);
            }
            
            mangaDetailNormal.setVisibility(View.VISIBLE);
            
            // Init go to dialog or disable it
            if (mangaDetail.previewSum > 1)
                goToDialog = createGoToDialog();
            else {
                previewNumText.setClickable(false);
                previewNumText2.setClickable(false);
            }
            // paper list
            if (mangaDetail.previewLists[0] != null) {
                pageListMain.setVisibility(View.VISIBLE);
                // preview num
                    bottomPanel.setVisibility(View.GONE);
                
                previewNumText.setText(String.format("%d / %d",
                        1, mangaDetail.previewSum));
                previewNumText2.setText(String.format("%d / %d",
                        1, mangaDetail.previewSum));
                    
                curPage = 0;
                addPageItem(0);
            } else
                Toast.makeText(MangaDetailActivity.this,
                        getString(R.string.detail_preview_error), Toast.LENGTH_SHORT)
                        .show();
        }
    }
    
    private void addTags(LinearLayout tagsLayout, String[][] tagGroups) {
        tagsLayout.removeAllViews();
        int darkBlue = getResources().getColor(R.color.blue_dark);
        int x = Ui.dp2pix(8);
        
        
        AutoWrapLayout layout = new AutoWrapLayout(this);
        for (String[] tagGroup : tagGroups) {
            for (int i = 0; i < tagGroup.length; i ++) {
                if (i == 0)
                    continue;
                TextView tagView = new TextView(this);
                tagView.setText(tagGroup[i]);
                tagView.setTextColor(Color.WHITE);
                tagView.setBackgroundColor(darkBlue);
                tagView.setPadding(x, x, x, x);
                AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
                lp.setMargins(x, x, x, x);
                layout.addView(tagView, lp);
            }
        }
        tagsLayout.addView(layout);
        // TODO show group name when it look well
        /*
        for (String[] tagGroup : tagGroups) {
            AutoWrapLayout layout = new AutoWrapLayout(this);
            for (int i = 0; i < tagGroup.length; i ++) {
                TextView tagView = new TextView(this);
                if (i == 0) {
                    tagView.setText(tagGroup[i] + ":");
                } else {
                    tagView.setText(tagGroup[i]);
                    tagView.setTextColor(Color.WHITE);
                    tagView.setBackgroundColor(darkBlue);
                }
                tagView.setPadding(x, x, x, x);
                AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
                lp.setMargins(x, x, x, x);
                layout.addView(tagView, lp);
            }
            tagsLayout.addView(layout);
        }*/
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.action_favourite:
            Favourite.push(mangaDetail);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_favourite),
                    Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_download:
            Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
            startService(it);
            if (mangaDetail.firstPage == null)
                mServiceConn.getService().add(mangaDetail.gid, mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.title);
            else
                mServiceConn.getService().add(mangaDetail.gid, mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.firstPage, Integer.parseInt(mangaDetail.pages),
                        1, mangaDetail.title);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_download),
                    Toast.LENGTH_SHORT).show();
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
