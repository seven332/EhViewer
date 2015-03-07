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

package com.hippo.ehviewer.windowsanimate;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class AnimateCanvas extends View {

    private final List<Sprite> mSpriteList = new LinkedList<Sprite>();

    public AnimateCanvas(Context context) {
        super(context);
    }

    void addSprite(Sprite sprite) {
        mSpriteList.add(sprite);
    }

    void removeSprite(Sprite sprite) {
        mSpriteList.remove(sprite);
    }

    @Override
    public void onDraw(Canvas canvas) {
        for (Sprite sprite : mSpriteList) {
            sprite.draw(canvas);
        }
    }
}
