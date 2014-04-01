package com.hippo.ehviewer.widget;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.view.AlertButton;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class DialogBuilder extends AlertDialog.Builder{
    private View mView;
    private Context mContext;
    public DialogBuilder(Context context) {
        super(context);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.dialog, null);
        super.setView(mView);
    }
    
    /**
     * Set the title using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public DialogBuilder setTitle(int titleId) {
        setTitle(mContext.getText(titleId));
        return this;
    }
    
    /**
     * Set the title displayed in the {@link Dialog}.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public DialogBuilder setTitle(CharSequence title) {
        TextView titleView = (TextView)mView.findViewById(R.id.title);
        titleView.setVisibility(View.VISIBLE);
        titleView.setText(title);
        return this;
    }
    
    /**
     * Set the message to display using the given resource id.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public DialogBuilder setMessage(int messageId) {
        setMessage(mContext.getText(messageId));
        return this;
    }
    
    /**
     * Set the message to display.
      *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public DialogBuilder setMessage(CharSequence message) {
        ScrollView scrollView = (ScrollView)mView.findViewById(R.id.scroll_view);
        scrollView.setVisibility(View.VISIBLE);
        TextView messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(message);
        return this;
    }
    
    public DialogBuilder setLongMessage(int messageId) {
        setLongMessage(mContext.getText(messageId));
        return this;
    }
    
    public DialogBuilder setLongMessage(CharSequence message) {
        ScrollView scrollView = (ScrollView)mView.findViewById(R.id.scroll_view);
        scrollView.setVisibility(View.VISIBLE);
        TextView messageView = (TextView)mView.findViewById(R.id.message);
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(message);
        LinearLayout.LayoutParams lp =
                (LinearLayout.LayoutParams)messageView.getLayoutParams();
        lp.setMargins(lp.leftMargin, lp.topMargin/3,
                lp.rightMargin, lp.bottomMargin/3);
        return this;
    }
    
    /**
     * Set the view to display in that dialog.
     * @return 
     */
    public DialogBuilder setView(View view, boolean center) {
        ScrollView scrollView = (ScrollView)mView.findViewById(R.id.scroll_view);
        scrollView.setVisibility(View.VISIBLE);
        LinearLayout customLayout = (LinearLayout)mView.findViewById(R.id.custom);
        customLayout.setVisibility(View.VISIBLE);
        if (center) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER;
            customLayout.addView(view, lp);
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            customLayout.addView(view, lp);
        }
        return this;
    }
    
    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     * @param textId The resource id of the text to display in the positive button
     * @param listener The View.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setPositiveButton(int textId, final View.OnClickListener listener) {
        setPositiveButton(mContext.getText(textId), listener);
        return this;
    }
    
    /**
     * Set a listener to be invoked when the positive button of the dialog is pressed.
     * @param text The text to display in the positive button
     * @param listener The View.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
        Button button = (Button)mView.findViewById(R.id.positive_button);
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setOnClickListener(listener);
        return this;
    }
    
    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     * @param textId The resource id of the text to display in the negative button
     * @param listener The DialogInterface.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNegativeButton(int textId, final View.OnClickListener listener) {
        setNegativeButton(mContext.getText(textId), listener);
        return this;
    }
    
    /**
     * Set a listener to be invoked when the negative button of the dialog is pressed.
     * @param text The text to display in the negative button
     * @param listener The DialogInterface.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
        Button button = (Button)mView.findViewById(R.id.negative_button);
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setOnClickListener(listener);
        return this;
    }
    
    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     * @param textId The resource id of the text to display in the neutral button
     * @param listener The DialogInterface.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNeutralButton(int textId, final View.OnClickListener listener) {
        setNeutralButton(mContext.getText(textId), listener);
        return this;
    }
    
    /**
     * Set a listener to be invoked when the neutral button of the dialog is pressed.
     * @param text The text to display in the neutral button
     * @param listener The View.DialogInterface.OnClickListener to use.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setNeutralButton(CharSequence text, final View.OnClickListener listener) {
        Button button = (Button)mView.findViewById(R.id.neutral_button);
        button.setVisibility(View.VISIBLE);
        button.setText(text);
        button.setOnClickListener(listener);
        return this;
    }
    
    /**
     * Sets whether the dialog is cancelable or not.  Default is true.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    @Override
    public DialogBuilder setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
        return this;
    }
    
    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener. This should be an array type i.e. R.array.foo
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setItems(int itemsId, AdapterView.OnItemClickListener listener) {
        setItems(mContext.getResources().getTextArray(itemsId), listener);
        return this;
    }
    
    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setItems(CharSequence[] items, AdapterView.OnItemClickListener listener) {
        setAdapter(new ArrayAdapter<CharSequence>(mContext,
                R.layout.list_item_text, items), listener);
        return this;
    }
    
    /**
     * Set a list of items, which are supplied by the given {@link ListAdapter}, to be
     * displayed in the dialog as the content, you will be notified of the
     * selected item via the supplied listener.
     * 
     * @param adapter The {@link ListAdapter} to supply the list of items
     * @param listener The listener that will be called when an item is clicked.
     *
     * @return This Builder object to allow for chaining of calls to set methods
     */
    public DialogBuilder setAdapter(ListAdapter adapter, AdapterView.OnItemClickListener listener) {
        ListView listView = (ListView)mView.findViewById(R.id.list);
        listView.setVisibility(View.VISIBLE);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(listener);
        return this;
    }
    
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        AlertButton button = (AlertButton)mView.findViewById(R.id.positive_button);
        button.dialog = dialog;
        button = (AlertButton)mView.findViewById(R.id.negative_button);
        button.dialog = dialog;
        button = (AlertButton)mView.findViewById(R.id.neutral_button);
        button.dialog = dialog;
        return dialog;
    }
}
