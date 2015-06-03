/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.annotation.Nullable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.fresco.EhCacheKeyFactory;
import com.hippo.ehviewer.fresco.KeyImageRequest;
import com.hippo.scene.Scene;

public class GalleryDetailScene extends Scene {

    public static final String KEY_GALLERY_INFO = "gallery_info";

    private SimpleDraweeView mThumb;
    private TextView mTitle;
    private TextView mUploader;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_gallery_detail);

        GalleryInfo gi = getAnnouncer().getParcelableExtra(KEY_GALLERY_INFO);

        mThumb = (SimpleDraweeView) findViewById(R.id.thumb);
        mThumb.bringToFront();
        ViewGroup a;

        ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(gi.thumb));
        KeyImageRequest keyImageRequest = new KeyImageRequest(builder);
        keyImageRequest.setKey(EhCacheKeyFactory.getThumbKey(gi.gid));
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(keyImageRequest)
                .setTapToRetryEnabled(true)
                .setOldController(mThumb.getController()).build();
        mThumb.setController(controller);

        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText(gi.title);

        mUploader = (TextView) findViewById(R.id.uploader);
        mUploader.setText(gi.uploader);



        /*
        int source = Config.getEhSource();
        EhClient.getInstance().getGalleryDetail(source, EhClient.getDetailUrl(source, gi.gid, gi.token, 0), new EhClient.OnGetGalleryDetailListener() {
            @Override
            public void onSuccess(GalleryDetail galleryDetail) {
                Log.d(galleryDetail.torrentUrl);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("", e);
            }
        });
        */
    }
}
