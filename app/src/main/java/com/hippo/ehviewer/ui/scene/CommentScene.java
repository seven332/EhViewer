package com.hippo.ehviewer.ui.scene;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.Comment;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.AppbarScene;
import com.hippo.util.URLImageGetter;
import com.hippo.util.UiUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.LinearDividerItemDecoration;

import java.util.List;

public class CommentScene extends AppbarScene {

    private EasyRecyclerView mRecyclerView;
    private CommentAdapter mAdapter;

    private List<Comment> mCommentList;

    private void handleAnnouncer(Announcer announcer) {
        if (announcer != null) {
            mCommentList = (List<Comment>) announcer.getObject();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        Context context = getStageActivity();
        Resources resources = context.getResources();

        setContentView(R.layout.scene_comment);
        setTitle(R.string.comments);
        setIcon(R.drawable.ic_arrow_left_dark);

        mRecyclerView = (EasyRecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new CommentAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        LinearDividerItemDecoration decoration = new LinearDividerItemDecoration(
                LinearDividerItemDecoration.VERTICAL, resources.getColor(R.color.divider_light),
                UiUtils.dp2pix(getStageActivity(), 1));
        decoration.setShowLastDivider(true);
        decoration.setPadding(resources.getDimensionPixelOffset(R.dimen.keyline_margin));
        mRecyclerView.addItemDecoration(decoration);
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setHasFixedSize(true);
    }

    @Override
    protected void onBind() {
        super.onBind();

        handleAnnouncer(getAnnouncer());
    }

    @Override
    protected void onRestore() {
        super.onRestore();

        // mCommentList is already setted, nothing to do
    }

    @Override
    public void onIconClick() {
        finish();
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        if (mRecyclerView != null) {
            mRecyclerView.setPadding(0, 0, 0, b);
        }
    }

    private static class CommentHolder extends RecyclerView.ViewHolder {

        private TextView user;
        private TextView time;
        private TextView comment;

        public CommentHolder(View itemView) {
            super(itemView);

            user = (TextView) itemView.findViewById(R.id.user);
            time = (TextView) itemView.findViewById(R.id.time);
            comment = (TextView) itemView.findViewById(R.id.comment);
        }
    }


    private class CommentAdapter extends RecyclerView.Adapter<CommentHolder> {

        @Override
        public CommentHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new CommentHolder(getStageActivity().getLayoutInflater().inflate(
                    R.layout.item_comment, parent, false));
        }

        @Override
        public void onBindViewHolder(CommentHolder holder, int position) {
            Comment comment = mCommentList.get(position);
            holder.user.setText(comment.user);
            holder.time.setText(comment.time);
            URLImageGetter p = new URLImageGetter(
                    holder.comment, EhApplication.getConaco(getStageActivity()));
            holder.comment.setText(Html.fromHtml(comment.comment, p, null));
        }

        @Override
        public int getItemCount() {
            if (mCommentList != null) {
                return mCommentList.size();
            } else {
                return 0;
            }
        }
    }
}
