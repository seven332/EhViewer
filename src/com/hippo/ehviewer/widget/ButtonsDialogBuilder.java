package com.hippo.ehviewer.widget;

import java.util.LinkedList;
import java.util.List;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.view.AlertButton;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ButtonsDialogBuilder extends AlertDialog.Builder {
    
    private static final float WEIGHT = 1.0f;
    
    private Context mContext;
    private View mView;
    private LinearLayout mRootView;
    private LinearLayout mContainer;
    
    private List<AlertButton> mButtonList;
    
    public ButtonsDialogBuilder(Context context) {
        super(context);
        mContext = context;
        mButtonList = new LinkedList<AlertButton>();
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.buttons_dialog, null);
        mRootView = (LinearLayout)mView.findViewById(R.id.button_root);
        mContainer = new LinearLayout(mContext);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mRootView.addView(mContainer, lp);
        super.setView(mView);
    }
    
    /**
     * Set the title using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public ButtonsDialogBuilder setTitle(int titleId) {
        setTitle(mContext.getText(titleId));
        return this;
    }
    
    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public ButtonsDialogBuilder setTitle(CharSequence title) {
        TextView titleView = (TextView)mView.findViewById(R.id.title);
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);
        return this;
    }
    
    public ButtonsDialogBuilder addButton(AlertButton button) {
        button.setBackgroundResource(R.drawable.white_bn_bg);
        button.setTextColor(Color.BLACK);
        mButtonList.add(button);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT);
        lp.weight = WEIGHT;
        mContainer.addView(button, lp);
        return this;
    }
    
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        for (AlertButton ab : mButtonList)
            ab.dialog = dialog;
        return dialog;
    }
}
