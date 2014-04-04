package com.hippo.ehviewer.widget;

import com.hippo.ehviewer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PullListView extends LinearLayout implements AbsListView.OnScrollListener{
    @SuppressWarnings("unused")
    private static final String TAG = "PullListView";
    
    private final static int HEADER_PULL_TO_REFRESH = 0;
    private final static int HEADER_RELEASE_TO_REFRESH = 1;
    private final static int HEADER_REFRESHING = 2;
    private final static int HEADER_DONE = 3;
    private final static int HEADER_CANCEL = 4;
    
    private final static int FOOTER_REFRESHING = 0;
    private final static int FOOTER_SUCCESS = 1;
    private final static int FOOTER_FAIL = 2;
    
    // arrow animation
    private RotateAnimation mAnimation;
    private RotateAnimation mReverseAnimation;
    
    // Header animation
    private Animation mRefreshAnimation;
    private Animation mOverAnimation;
    
    private ListView mListView;
    private LinearLayout mHeader;
    private LinearLayout mFooter;
    private TextView mTipTextView;
    private ImageView mArrowImageView;
    private ProgressBar mProgressBar;
    
    private TextView mFooterTipTextView;
    private ProgressBar mFooterProgressBar;
    
    // Make sure only one pull event
    private boolean isHeaderRecored = false;
    private boolean isFooterRecored = false;
    
    // Header String to show
    private String mHeaderPullStr;
    private String mHeaderReleaseStr;
    private String mHeaderRefreshStr;
    private String mHeaderDoneStr;
    private String mHeaderCancelStr;
    
    // Footer String to show
    private String mFooterRefreshStr;
    private String mFooterSuccessStr;
    private String mFooterFailStr;
    
    // Header
    private int headerHeight;  
    private int headerOriginalLeftPadding;
    private int headerOriginalTopPadding;
    private int headerOriginalRightPadding;
    private int headerOriginalBottomPadding;
    private int releaseTopPadding;
    private int doneTopPadding;
    private int pullThreshold;
    private int pullOffestMax;
    
    // Footer
    private boolean isActionWhenShow = true;
    
    private int startY;
    private int headerState = HEADER_DONE;
    private boolean isBack;
    
    private int footerState = FOOTER_SUCCESS;
    
    private OnHeaderRefreshListener headerRefreshListener;
    private OnFooterRefreshListener footerRefreshListener;
    
    // To Check scroll to top
    // Store y of last point
    private int lastY;
    
    private boolean mHeaderEnabled = true;
    @SuppressWarnings("unused")
    private boolean mFooterEnabled = true;
    
    public interface OnHeaderRefreshListener {  
        public void onHeaderRefresh();
        public void onHeaderRefresh(Object obj);  
    }
    
    public interface OnFooterRefreshListener {
        public void onFooterRefresh();
    }
    
    public PullListView(Context context) {
        super(context);
        init(context);
    }

    public PullListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public PullListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    
    private void init(Context context) {
        // Set arrow animation
        mAnimation = new RotateAnimation(0, -180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setDuration(100);
        mAnimation.setFillAfter(true);
        
        mReverseAnimation = new RotateAnimation(-180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseAnimation.setInterpolator(new LinearInterpolator());
        mReverseAnimation.setDuration(100);
        mReverseAnimation.setFillAfter(true);
        
        // Set header animation
        mRefreshAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mHeader.setPadding(headerOriginalLeftPadding,
                        (int)((1 - interpolatedTime) * releaseTopPadding + (interpolatedTime * headerOriginalTopPadding)),
                        headerOriginalRightPadding, headerOriginalBottomPadding);
                mHeader.invalidate();
            }
        };
        mRefreshAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setEnabled(true);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mRefreshAnimation.setDuration(100);
        
        mOverAnimation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                mHeader.setPadding(headerOriginalLeftPadding,
                        (int)((1 - interpolatedTime) * doneTopPadding - (interpolatedTime * headerHeight)),
                        headerOriginalRightPadding, headerOriginalBottomPadding);
                mHeader.invalidate();
            }
        };
        mOverAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setEnabled(true);
                if (headerState == HEADER_CANCEL) {
                    headerState = HEADER_DONE;
                    isHeaderRecored = false;
                    isBack = false;
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        mOverAnimation.setDuration(200);
        
        View view = LayoutInflater.from(context).inflate(R.layout.pull_list_view, this);
        mFooter = (LinearLayout)LayoutInflater.from(context).inflate(R.layout.pull_list_view_footer, null);
        mFooter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                footerRefresh();
            }
        });
        mListView = (ListView)view.findViewById(R.id.list_view);
        mListView.setOnScrollListener(this);
        mListView.addFooterView(mFooter);
        mFooterTipTextView = (TextView)mFooter.findViewById(R.id.footer_tip_text);
        mFooterProgressBar = (ProgressBar)mFooter.findViewById(R.id.footer_progressBar);
        
        mHeader = (LinearLayout)view.findViewById(R.id.header);
        mArrowImageView = (ImageView) mHeader.findViewById(R.id.header_arrow_image);
        mArrowImageView.setMinimumWidth(50);
        mArrowImageView.setMinimumHeight(50);
        mProgressBar = (ProgressBar)mHeader.findViewById(R.id.header_progressBar);
        mTipTextView = (TextView)mHeader.findViewById(R.id.header_tip_text);
        
        headerOriginalLeftPadding = mHeader.getPaddingLeft();
        headerOriginalTopPadding = mHeader.getPaddingTop();
        headerOriginalRightPadding = mHeader.getPaddingRight();
        headerOriginalBottomPadding = mHeader.getPaddingBottom();
        
        measureView(mHeader);  
        headerHeight = mHeader.getMeasuredHeight();
        pullThreshold = headerHeight;
        pullOffestMax = (int)(headerHeight * 2.3f);
        
        mHeader.setPadding(headerOriginalLeftPadding, -headerHeight, headerOriginalRightPadding, headerOriginalBottomPadding);  
        mHeader.invalidate();
    }
    
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {}

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount == totalItemCount
                && isActionWhenShow && totalItemCount > 1)
            footerRefresh();
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!mHeaderEnabled || isHeaderRecored || isFooterRecored)
            return false;
        
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            lastY = (int)event.getY();
            return false;
            
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            return false;
        
        case MotionEvent.ACTION_MOVE:
            int y = (int)event.getY();
            int offsetY = y - lastY;
            lastY = y;
            
            if (offsetY > 0 && isListAtTop())
                return true;
            else
                return false;
            
        default:
            return super.onInterceptTouchEvent(event);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (mHeaderEnabled && !isHeaderRecored && !isFooterRecored) {
                isHeaderRecored = true;
                startY = (int) event.getY();
            }
            break;
            
        case MotionEvent.ACTION_CANCEL:
            headerState = HEADER_DONE;
            isHeaderRecored = false;
            isBack = false;
            changeHeaderViewByState();
            break;
        case MotionEvent.ACTION_UP:
            if (headerState == HEADER_PULL_TO_REFRESH) {
                headerState = HEADER_CANCEL;
                changeHeaderViewByState();                 
            }
            else if (headerState == HEADER_RELEASE_TO_REFRESH) {
                headerState = HEADER_REFRESHING;
                changeHeaderViewByState();
                onHeaderRefresh();               
            } else if (headerState == HEADER_DONE) {
                isHeaderRecored = false;
                isBack = false;
                changeHeaderViewByState();
            }
            break;
            
        case MotionEvent.ACTION_MOVE:
            int tempY = (int)event.getY();
            if (mHeaderEnabled && !isHeaderRecored && !isFooterRecored) {
                isHeaderRecored = true;  
                startY = tempY;
            }
            if (headerState != HEADER_REFRESHING && isHeaderRecored) {
                int offsetY = getShowOffset(tempY - startY, pullOffestMax);
                if (headerState == HEADER_RELEASE_TO_REFRESH) {
                    // 往上推，推到屏幕足够掩盖head的程度，但还没有全部掩盖
                    if (offsetY < pullThreshold && offsetY > 0) {
                        headerState = HEADER_PULL_TO_REFRESH;
                        changeHeaderViewByState();
                    }
                    // 一下子推到顶
                    else if (offsetY <= 0) {
                        headerState = HEADER_DONE;
                        startY = tempY;
                    }
                } else if (headerState == HEADER_PULL_TO_REFRESH) {
                    // 下拉到可以进入RELEASE_TO_REFRESH的状态
                    if (offsetY >= pullThreshold) {
                        headerState = HEADER_RELEASE_TO_REFRESH;
                        isBack = true;
                        changeHeaderViewByState();
                    }
                    // 上推到顶了
                    else if (offsetY <= 0) {
                        headerState = HEADER_DONE;
                        startY = tempY;
                    }
                } else if (headerState == HEADER_DONE) {
                    if (offsetY > 0) {
                        headerState = HEADER_PULL_TO_REFRESH;
                        changeHeaderViewByState();
                    } else {
                        startY = tempY;
                    }
                }
                
                // 更新headView的size   
                if (headerState == HEADER_PULL_TO_REFRESH) {
                    int topPadding = (int)(offsetY - headerHeight);
                    mHeader.setPadding(headerOriginalLeftPadding, topPadding, headerOriginalRightPadding, headerOriginalBottomPadding);
                    mHeader.invalidate();
                }
                
                // 更新headView的paddingTop
                if (headerState == HEADER_RELEASE_TO_REFRESH) {
                    int topPadding = (int)(offsetY - headerHeight);
                    mHeader.setPadding(headerOriginalLeftPadding, topPadding, headerOriginalRightPadding, headerOriginalBottomPadding);
                    mHeader.invalidate();
                }
            }
            break;
        }
        return true;
    }
    
    private int getShowOffset(int eventOffset, int target) {
        if (eventOffset <= 0) // avoid divide zero
            return 0;
        else
            return (target * eventOffset) / (eventOffset + target);
    }
    
    // Refresh header UI
    private void changeHeaderViewByState() {
        switch (headerState) {
        case HEADER_RELEASE_TO_REFRESH:
            mArrowImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);
            mTipTextView.setVisibility(View.VISIBLE);
            mArrowImageView.clearAnimation();
            mArrowImageView.startAnimation(mAnimation);
            mTipTextView.setText(mHeaderReleaseStr);
            break;  
        case HEADER_PULL_TO_REFRESH:
            mProgressBar.setVisibility(View.GONE);
            mTipTextView.setVisibility(View.VISIBLE);
            mArrowImageView.clearAnimation();
            mArrowImageView.setVisibility(View.VISIBLE);
            if (isBack) {
                isBack = false;
                mArrowImageView.clearAnimation();  
                mArrowImageView.startAnimation(mReverseAnimation);  
            }
            mTipTextView.setText(mHeaderPullStr);
            break;
        case HEADER_REFRESHING:
            releaseTopPadding = mHeader.getPaddingTop();
            mHeader.startAnimation(mRefreshAnimation);
            mProgressBar.setVisibility(View.VISIBLE);  
            mArrowImageView.clearAnimation();  
            mArrowImageView.setVisibility(View.GONE);  
            mTipTextView.setText(mHeaderRefreshStr);
            break;
        case HEADER_DONE:
        case HEADER_CANCEL:
            doneTopPadding = mHeader.getPaddingTop();
            mHeader.startAnimation(mOverAnimation);
            mProgressBar.setVisibility(View.GONE);
            mArrowImageView.setVisibility(View.GONE);
            mArrowImageView.clearAnimation();
            // 此处更换图标
            mArrowImageView.setImageResource(R.drawable.ic_pulltorefresh_arrow);
            if (headerState == HEADER_DONE)
                mTipTextView.setText(mHeaderDoneStr);
            else
                mTipTextView.setText(mHeaderCancelStr);
            break;
        }
    }
    
    // Refresh footer UI
    private void changeFooterViewByState() {
        switch (footerState) {
        case FOOTER_REFRESHING:
            mFooterTipTextView.setText(mFooterRefreshStr);
            mFooterProgressBar.setVisibility(View.VISIBLE);
            break;
        case FOOTER_SUCCESS:
            mFooterTipTextView.setText(mFooterSuccessStr);
            mFooterProgressBar.setVisibility(View.GONE);
            break;
        case FOOTER_FAIL:
            mFooterTipTextView.setText(mFooterFailStr);
            mFooterProgressBar.setVisibility(View.GONE);
            break;
        }
    }
    
    public ListView getListView() {
        return mListView;
    }
    
    // Header
    public void setHeaderEnabled(boolean enabled) {
        mHeaderEnabled = enabled;
    }
    
    public void setHeaderString(String pullStr, String releaseStr, String refreshStr, String doneStr, String cancelStr) {
        mHeaderPullStr = pullStr;
        mHeaderReleaseStr = releaseStr;
        mHeaderRefreshStr = refreshStr;
        mHeaderDoneStr = doneStr;
        mHeaderCancelStr = cancelStr;
        
        switch (headerState) {
        case HEADER_PULL_TO_REFRESH:
            mTipTextView.setText(mHeaderPullStr);
            break;
        case HEADER_RELEASE_TO_REFRESH:
            mTipTextView.setText(mHeaderReleaseStr);
            break;
        case HEADER_REFRESHING:
            mTipTextView.setText(mHeaderRefreshStr);
            break;
        case HEADER_DONE:
            mTipTextView.setText(mHeaderDoneStr);
            break;
        case HEADER_CANCEL:
            mTipTextView.setText(mHeaderCancelStr);
            break;
        }
    }
    
    public boolean clickHeaderRefresh(Object obj) {
        if (!mHeaderEnabled || isHeaderRecored || isFooterRecored)
            return false;
        
        isHeaderRecored = true;
        headerState = HEADER_REFRESHING;
        changeHeaderViewByState();
        onHeaderRefresh(obj);
        return true;
    }
    
    public void setOnRefreshListener(OnHeaderRefreshListener refreshListener) {
        this.headerRefreshListener = refreshListener;
    }
    
    private void onHeaderRefresh() {
        if (headerRefreshListener != null) {
            headerRefreshListener.onHeaderRefresh();
        }
    }
    
    private void onHeaderRefresh(Object obj) {
        if (headerRefreshListener != null) {
            headerRefreshListener.onHeaderRefresh(obj);
        }
    }
    
    public boolean isHeaderRefreshing() {
        return headerState == HEADER_REFRESHING;
    }
    
    public void onHeaderRefreshComplete() {
        headerState = HEADER_DONE;
        isHeaderRecored = false;
        isBack = false;
        changeHeaderViewByState();
    }
    
    // Footer
    public void setFooterEnabled(boolean enabled) {
        // TODO Remove footer if false
        mFooterEnabled = enabled;
    }
    
    public void setFooterString(String refreshStr, String successStr, String failStr) {
        mFooterRefreshStr = refreshStr;
        mFooterSuccessStr = successStr;
        mFooterFailStr = failStr;
        
        switch (footerState) {
        case FOOTER_REFRESHING:
            mFooterTipTextView.setText(mFooterRefreshStr);
            break;
        case FOOTER_SUCCESS:
            mFooterTipTextView.setText(mFooterSuccessStr);
            break;
        case FOOTER_FAIL:
            mFooterTipTextView.setText(mFooterFailStr);
            break;
        }
    }
    
    public void footerRefresh() {
        if (!mHeaderEnabled || isHeaderRecored || isFooterRecored)
            return;
        
        isFooterRecored = true;
        footerState = FOOTER_REFRESHING;
        changeFooterViewByState();
        
        if (footerRefreshListener != null) {
            footerRefreshListener.onFooterRefresh();
        }
    }
    
    public void setOnFooterRefreshListener(OnFooterRefreshListener footerRefreshListener) {
        this.footerRefreshListener = footerRefreshListener;
    }
    
    public void onFooterRefreshComplete(boolean isSuccess) {
        if (isSuccess) {
            footerState = FOOTER_SUCCESS;
            isActionWhenShow = true;
        }
        else {
            footerState = FOOTER_FAIL;
            isActionWhenShow = false;
        }
        isFooterRecored = false;
        changeFooterViewByState();
    }
    
    public boolean isFooterRefreshing() {
        return footerState == FOOTER_REFRESHING;
    }
    
    public void onFooterRefreshComplete(boolean isSuccess, boolean actionWhenShow) {
        if (isSuccess)
            footerState = FOOTER_SUCCESS;
        else
            footerState = FOOTER_FAIL;
        isActionWhenShow = actionWhenShow;
        isFooterRecored = false;
        changeFooterViewByState();
    }
    
    public void setActionWhenShow(boolean actionWhenShow) {
        isActionWhenShow = actionWhenShow;
    }
    
    private boolean isListAtTop()   {
        if(mListView.getChildCount() == 0)
            return true;
        return mListView.getFirstVisiblePosition() == 0
                && mListView.getChildAt(0).getTop() == 0;
    }
    
    // Get header width and height
    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
                    MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }
}
