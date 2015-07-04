/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.widget;

import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.hotspot.HotspotTouchHelper;
import com.hippo.hotspot.Hotspotable;

public final class AccurateClick {

    public static void setOnAccurateClickListener(@NonNull View view, @NonNull OnAccurateClickListener listener) {
        AccurateClickHelper accurateClickHelper = new AccurateClickHelper();
        view.setTag(R.id.view_tag_accurate_click_helper, accurateClickHelper);
        view.setTag(R.id.view_tag_on_accurate_click_listener, listener);

        // compatible with VectorOld
        HotspotTouchHelper helper = HotspotTouchHelper.getHotspotTouchHelper(view);
        if (helper != null) {
            helper.addOwner(accurateClickHelper);
        } else {
            HotspotTouchHelper.setHotspotTouchHelper(view, new HotspotTouchHelper(accurateClickHelper));
        }

        view.setOnClickListener(new DelegateClickListener());
    }

    static class DelegateClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Object accurateClickHelper = v.getTag(R.id.view_tag_accurate_click_helper);
            Object listener = v.getTag(R.id.view_tag_on_accurate_click_listener);
            if (accurateClickHelper instanceof AccurateClickHelper && listener instanceof OnAccurateClickListener) {
                AccurateClickHelper ach = (AccurateClickHelper) accurateClickHelper;
                ((OnAccurateClickListener) listener).onAccurateClick(v, ach.x, ach.y);
            }
        }
    }

    static class AccurateClickHelper implements Hotspotable {

        public float x;
        public float y;

        @Override
        public void setHotspot(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public interface OnAccurateClickListener {
        void onAccurateClick(View v, float x, float y);
    }
}
