package com.hippo.ehviewer.activity;

import java.util.ArrayList;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.view.OlImageView;
import com.hippo.ehviewer.widget.AutoWrapLayout;
import com.hippo.ehviewer.widget.ButtonsDialogBuilder;
import com.hippo.ehviewer.widget.DialogBuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.hippo.ehviewer.util.Log;

import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

// TODO hover to show larger preview
// TODO add detail cache

public class MangaDetailActivity extends Activity {

    private static final String TAG = "MangaDetailActivity";
    
    public static final String KEY_G_INFO = "gallery_info";
    
    private AppContext mAppContext;
    private Data mData;
    private EhClient mEhClient;
    
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
    //private TextView previewNumText;
    private TextView previewNumText2;
    private Button previewRefreshButton;
    private View bottomPanel;
    private Button readButton;
    private Button rateButton;
    
    private AlertDialog goToDialog;
    
    private GalleryDetail mangaDetail;
    private int curPage;
    
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
        
        PreviewList pageList = mangaDetail.previewLists[page];
        if (pageList == null) {
            Log.d(TAG, "WTF, I may check mangaDetail.pageLists[page] is not null");
            Toast.makeText(this, "WTF, I may check mangaDetail.pageLists[page] is not null", Toast.LENGTH_SHORT).show();
            return;
        }
        int index = page * mangaDetail.previewPerPage + 1;
        int rowIndex = 0;
        for (PreviewList.Row row : pageList.rowArray) {
            for (PreviewList.Row.Item item : row.itemArray) {
                TextViewWithUrl tvu = new TextViewWithUrl(MangaDetailActivity.this,
                        item.url);
                tvu.setGravity(Gravity.CENTER);
                tvu.setText(String.valueOf(index));
                final int index1 = index;
                tvu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add to read in Data
                        mData.addRead(mangaDetail);
                        
                        Intent intent = new Intent(MangaDetailActivity.this,
                                MangaActivity.class);
                        intent.putExtra("url", ((TextViewWithUrl) v).url);
                        intent.putExtra("gid", mangaDetail.gid);
                        intent.putExtra("title", mangaDetail.title);
                        intent.putExtra("firstPage", index1 - 1);
                        intent.putExtra("pageSum", mangaDetail.pages);
                        startActivity(intent);
                    }
                });
                // Set white drawable for temp
                ColorDrawable white = new ColorDrawable(Color.TRANSPARENT);
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
        PreviewList pageList = mangaDetail.previewLists[page];
        if (pageList == null) {
            Log.d(TAG, "WTF, I may check mangaDetail.pageLists[page] is not null");
            return;
        }
        
        int maxWidth = bitmap.getWidth();
        int maxHeight = bitmap.getHeight();
        
        PreviewList.Row row = pageList.rowArray.get(rowIndex);
        ArrayList<PreviewList.Row.Item> items = row.itemArray;
        
        int startIndex = row.startIndex;
        int endIndex = startIndex + items.size();
        for(int i = startIndex ; i < endIndex; i++) {
            PreviewList.Row.Item item = items.get(i - startIndex);
            
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
            EhClient.OnGetMangaDetailListener {
        @Override
        public void onSuccess(GalleryDetail md) {
            Cache.mdCache.put(String.valueOf(mangaDetail.gid), md);
            layout(md);
        }

        @Override
        public void onFailure(String eMsg) {
            // Delete progress bar
            waitPb.setVisibility(View.GONE);
            // Show refresh button
            Toast.makeText(MangaDetailActivity.this,
                    eMsg, Toast.LENGTH_SHORT).show();

            refeshButton.setVisibility(View.VISIBLE);
        }
    }

    // *** Button onclick ***//
    // Refresh
    public void buttonRefresh(View v) {
        MangaDetailGetListener listener = new MangaDetailGetListener();
        mEhClient.getMangaDetail(url, mangaDetail, listener);
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
        mEhClient.getMangaDetail(url, mangaDetail, listener);
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
        mEhClient.getMangaDetail(url, mangaDetail, listener);
        // Delete offensiveView
        mangaDetailOffensive.setVisibility(View.GONE);
        // Add progressBar
        waitPb.setVisibility(View.VISIBLE);
    }

    // Read
    public void buttonRead(View v) {
        // Add to read
        mData.addRead(mangaDetail);
        
        Intent intent = new Intent(MangaDetailActivity.this,
                MangaActivity.class);
        intent.putExtra("url", mangaDetail.firstPage);
        intent.putExtra("gid", mangaDetail.gid);
        intent.putExtra("title", mangaDetail.title);
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
    
    public void ButtonRate(View v) {
        rate();
    }
    
    // *** Button onclick end ***//
    
    private int getSendableRating(float ratingFloat) {
        float dr = ratingFloat*2;        
        return (int)(dr + 0.5);
    }
    
    public void rate() {
        final RatingBar rb = new RatingBar(this);
        rb.setMax(10);
        rb.setStepSize(0.5f);
        LinearLayout ll = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.setMargins(0, Ui.dp2pix(24), 0, Ui.dp2pix(24));
        ll.addView(rb, lp);
        new DialogBuilder(this).setTitle(R.string.rate)
                .setView(ll, true)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int rating = getSendableRating(rb.getRating());
                        if (rating <= 0 || rating > 10)
                            Toast.makeText(MangaDetailActivity.this, "非法评分", Toast.LENGTH_SHORT).show(); // TODO
                        else {
                            ((AlertButton)v).dialog.dismiss();
                            mEhClient.rate(mangaDetail.gid, mangaDetail.token,
                                    rating, new EhClient.OnRateListener() {
                                        @Override
                                        public void onSuccess(float ratingAvg,
                                                int ratingCnt) {
                                            mangaDetail.rating = ratingAvg;
                                            mangaDetail.people = ratingCnt;
                                            if (!MangaDetailActivity.this.isFinishing()) {
                                                //Reset start
                                                LinearLayout rate = (LinearLayout) findViewById(R.id.detail_rate);
                                                Ui.addStar(rate, mangaDetail.rating);
                                                
                                                // Animation
                                                TextView averagePeople = (TextView)findViewById(
                                                        R.id.detail_average_people);
                                                final TextView averagePeopleBak = (TextView)findViewById(
                                                        R.id.detail_average_people_bak);
                                                averagePeopleBak.setText(averagePeople.getText());
                                                averagePeople.setText(String.format("%.2f (%d)",
                                                        mangaDetail.rating, mangaDetail.people));
                                                averagePeopleBak.setVisibility(View.VISIBLE);
                                                
                                                int duration = 1000;
                                                int height = averagePeople.getHeight();
                                                
                                                TranslateAnimation animation = new TranslateAnimation(0, 0, height, 0);
                                                animation.setDuration(duration);
                                                averagePeople.setAnimation(animation);
                                                
                                                TranslateAnimation animationBak = new TranslateAnimation(0, 0, 0, -height);
                                                animationBak.setDuration(duration);
                                                averagePeopleBak.setAnimation(animationBak);
                                                animationBak.setAnimationListener(new AnimationListener() {
                                                    @Override
                                                    public void onAnimationStart(
                                                            Animation animation) {}

                                                    @Override
                                                    public void onAnimationEnd(
                                                            Animation animation) {
                                                        averagePeopleBak.setVisibility(View.GONE);
                                                    }
                                                    @Override
                                                    public void onAnimationRepeat(
                                                            Animation animation) {}
                                                });
                                                animation.startNow();
                                                animationBak.startNow();
                                            }
                                            Toast.makeText(MangaDetailActivity.this,
                                                    "评价成功", Toast.LENGTH_SHORT).show(); // TODO
                                        }
                                        @Override
                                        public void onFailure(String eMsg) {
                                            Toast.makeText(MangaDetailActivity.this,
                                                    "评价失败\n" + eMsg, Toast.LENGTH_SHORT).show(); // TODO
                                        }
                            });
                        }
                    }
                }).setNegativeButton(android.R.string.cancel, new OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        ((AlertButton)v).dialog.dismiss();
                    }
                }).create().show();
    }
    
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
        
        //previewNumText.setText(String.format("%d / %d",
                //curPage+1, mangaDetail.previewSum));
        previewNumText2.setText(String.format("%d / %d",
                curPage+1, mangaDetail.previewSum));

        PreviewList pageList = mangaDetail.previewLists[curPage];

        if (pageList == null) {
            String tempUrl = url;
            if (!url.endsWith("/"))
                tempUrl += "/";
            tempUrl += "?p=" + curPage;

            mEhClient.getPageList(tempUrl, curPage,
                    new EhClient.OnGetPageListListener() {
                        @Override
                        public void onSuccess(Object checkFlag, PreviewList pageList) {
                            int page = (Integer)checkFlag;
                            mangaDetail.previewLists[page] = pageList;
                            addPageItem(page);
                        }

                        @Override
                        public void onFailure(Object checkFlag, String eMsg) {
                            int page = (Integer)checkFlag;
                            if (page == curPage) {
                                Toast.makeText(MangaDetailActivity.this,
                                        eMsg, Toast.LENGTH_SHORT).show();
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
        
        mAppContext = (AppContext)getApplication();
        mEhClient = mAppContext.getEhClient();
        mData = mAppContext.getData();
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // Download service
        Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        // Get information
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        GalleryInfo galleryInfo = (GalleryInfo)(intent.getParcelableExtra(KEY_G_INFO));
        
        boolean getFromCache = true;
        mangaDetail = Cache.mdCache.get(String.valueOf(galleryInfo.gid));
        if (mangaDetail == null) {
            getFromCache = false;
            mangaDetail = new GalleryDetail(galleryInfo);
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
        //previewNumText = (TextView) findViewById(R.id.preview_num);
        bottomPanel = (View)findViewById(R.id.bottom_panel);
        previewNumText2 = (TextView)bottomPanel.findViewById(R.id.preview_num);
        readButton = (Button)findViewById(R.id.detail_read);
        rateButton = (Button)findViewById(R.id.detail_do_rate);
        
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.ColourfyScreen(this);
        }
        
        setTitle(String.valueOf(mangaDetail.gid));
        
        OlImageView coverImage = (OlImageView) findViewById(R.id.detail_cover);
        coverImage.setUrl(mangaDetail.thumb);
        coverImage.setKey(String.valueOf(mangaDetail.gid));
        coverImage.setCache(Cache.memoryCache, Cache.cpCache);
        coverImage.loadImage(false);
        
        TextView title = (TextView) findViewById(R.id.detail_title);
        title.setText(mangaDetail.title);
        
        TextView uploader = (TextView) findViewById(R.id.detail_uploader);
        uploader.setText(mangaDetail.uploader);
        
        TextView category = (TextView) findViewById(R.id.detail_category);
        category.setText(Ui.getCategoryText(mangaDetail.category));
        category.setBackgroundColor(Ui.getCategoryColor(mangaDetail.category));
        
        // Make rate and read button same width
        // Disable them for temp
        Ui.measureView(readButton);
        Ui.measureView(rateButton);
        int readbw = readButton.getMeasuredWidth();
        int ratebw = rateButton.getMeasuredWidth();
        if (readbw > ratebw) {
            rateButton.setWidth(readbw);
        } else if (ratebw > readbw) {
            readButton.setWidth(ratebw);
        }
        readButton.setEnabled(false);
        rateButton.setEnabled(false);
        
        // get from cache
        if (getFromCache)
            layout(mangaDetail);
        else
            mEhClient.getMangaDetail(url, mangaDetail, new MangaDetailGetListener());
    }
    
    private void layout(GalleryDetail md) {
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
            
            // Enable button
            readButton.setEnabled(true);
            rateButton.setEnabled(true);
            
            LinearLayout rate = (LinearLayout) findViewById(R.id.detail_rate);
            Ui.addStar(rate, mangaDetail.rating);
            
            TextView averagePeople = (TextView) findViewById(R.id.detail_average_people);
            averagePeople.setText(String.format("%.2f (%d)", mangaDetail.rating, mangaDetail.people));
            
            TextView posted = (TextView) findViewById(R.id.detail_posted);
            posted.setText(mangaDetail.posted);
            
            TextView language = (TextView) findViewById(R.id.detail_language);
            language.setText(String.format(getString(R.string.detail_language), mangaDetail.language));
            
            TextView pagesSize = (TextView) findViewById(R.id.detail_pages_size);
            pagesSize.setText(String.format(getString(R.string.detail_pages_size), mangaDetail.pages, mangaDetail.size));
            
            // TODO
            
            LinearLayout tagsLayout = (LinearLayout) findViewById(R.id.tags_layout);
            addTags(tagsLayout, mangaDetail.tags);
            
            mangaDetailNormal.setVisibility(View.VISIBLE);
            
            // Init go to dialog or disable it
            if (mangaDetail.previewSum > 1)
                goToDialog = createGoToDialog();
            else {
                //previewNumText.setClickable(false);
                previewNumText2.setClickable(false);
            }
            // paper list
            if (mangaDetail.previewLists[0] != null) {
                pageListMain.setVisibility(View.VISIBLE);
                // preview num
                    bottomPanel.setVisibility(View.VISIBLE);
                
                //previewNumText.setText(String.format("%d / %d",
                        //1, mangaDetail.previewSum));
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
        int x = Ui.dp2pix(8);
        int y = Ui.dp2pix(6);
        Resources resources = getResources();
        // Get tag view resources
        int tagTextSize = resources.getDimensionPixelOffset(R.dimen.button_small_size);
        ColorStateList tagTextColor = resources.getColorStateList(R.color.blue_bn_text);
        int tagPaddingX = resources.getDimensionPixelOffset(R.dimen.button_tag_padding_x);
        int tagPaddingY = resources.getDimensionPixelOffset(R.dimen.button_tag_padding_y);
        for (String[] tagGroup : tagGroups) {
            LinearLayout tagGroupLayout = new LinearLayout(this);
            tagGroupLayout.setOrientation(LinearLayout.HORIZONTAL);
            AutoWrapLayout tagLayout = new AutoWrapLayout(this);
            for (int i = 0; i < tagGroup.length; i ++) {
                if (i == 0) {
                    TextView groupNameView = new TextView(new ContextThemeWrapper(this, R.style.TextTag));
                    groupNameView.setText(tagGroup[i]);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                    lp.setMargins(x, y, x, y);
                    tagGroupLayout.addView(groupNameView, lp);
                } else {
                    final String tagText = tagGroup[i];
                    Button tagView = new Button(this);
                    tagView.setTextSize(TypedValue.COMPLEX_UNIT_PX, tagTextSize);
                    tagView.setText(tagText);
                    tagView.setTextColor(tagTextColor);
                    tagView.setBackgroundResource(R.drawable.blue_bn_bg);
                    tagView.setPadding(tagPaddingX, tagPaddingY, tagPaddingX, tagPaddingY);
                    tagView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AlertButton voteUp = new AlertButton(MangaDetailActivity.this);
                            voteUp.setText(getString(R.string.vote_up)); // TODO
                            AlertButton voteDown = new AlertButton(MangaDetailActivity.this);
                            voteDown.setText(getString(R.string.vote_down)); // TODO
                            AlertButton showTagged = new AlertButton(MangaDetailActivity.this);
                            showTagged.setText(getString(R.string.show_tagged)); // TODO
                            new ButtonsDialogBuilder(MangaDetailActivity.this).setTitle(tagText)
                                    .addButton(voteUp).addButton(voteDown).addButton(showTagged)
                                    .create().show();
                        }
                    });
                    AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
                    lp.setMargins(x, y, x, y);
                    tagLayout.addView(tagView, lp);
                }
            }
            tagGroupLayout.addView(tagLayout);
            tagsLayout.addView(tagGroupLayout);
        }
        
        // Add tag
        Button addtagView = new Button(this);
        addtagView.setTextSize(TypedValue.COMPLEX_UNIT_PX, tagTextSize);
        addtagView.setText("+");
        addtagView.setTextColor(tagTextColor);
        addtagView.setBackgroundResource(R.drawable.blue_bn_bg);
        addtagView.setPadding(tagPaddingX, tagPaddingY, tagPaddingX, tagPaddingY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT;
        lp.setMargins(x, y, x, y);
        tagsLayout.addView(addtagView, lp);
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
            mData.addLocalFavourite(mangaDetail);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_favourite),
                    Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_download:
            Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
            startService(it);
            if (mangaDetail.firstPage == null)
                mServiceConn.getService().add(String.valueOf(mangaDetail.gid), mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.title);
            else
                mServiceConn.getService().add(String.valueOf(mangaDetail.gid), mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.firstPage, mangaDetail.pages,
                        1, mangaDetail.title);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_download),
                    Toast.LENGTH_SHORT).show();
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
