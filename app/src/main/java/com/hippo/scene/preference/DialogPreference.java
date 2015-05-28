package com.hippo.scene.preference;

import android.support.v7.widget.RecyclerView;

import com.hippo.scene.SimpleDialog;

public class DialogPreference extends Preference {

    public DialogPreference(String key, String title, String summary) {
        super(key, title, summary);
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        if (!super.onClick(viewHolder, x, y)) {
            SimpleDialog.Builder builder = new SimpleDialog.Builder(viewHolder.itemView.getContext());
            builder.setTitle("Title");
            builder.setSingleChoiceItems(new CharSequence[]{"横向", "竖向", "斜向"}, 0, null);
            builder.setStartPoint(x, y);
            builder.show();
        }
        return true;
    }
}
