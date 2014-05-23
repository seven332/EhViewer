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

package com.hippo.ehviewer.data;

import java.util.ArrayList;

public class PreviewList {
    
    public class Row {
        
        public class Item {
            public int xOffset;
            public int yOffset;
            public int width;
            public int height;
            public String url;

            public Item(int xOffset, int yOffset, int width, int height, String url) {
                this.xOffset = xOffset;
                this.yOffset = yOffset;
                this.width = width;
                this.height = height;
                this.url = url;
            }
        }
        
        public String imageUrl;
        public ArrayList<Item> itemArray = new ArrayList<Item>();
        public int startIndex;
        public Row(String imageUrl) {
            this.imageUrl = imageUrl;
        }
        
        public void addItem(int xOffset, int yOffset, int width, int height,
                String url) {
            itemArray.add(new Item(xOffset, yOffset, width, height, url));
        }
    }
    
    private Row curRow;
    public ArrayList<Row> rowArray = new ArrayList<Row>();

    public void addItem(String imageUrl, String xOffset, String yOffset, String width, String height,
            String url) {
        if (curRow == null) {
            curRow = new Row(imageUrl);
            curRow.startIndex = 0;
            rowArray.add(curRow);
        } else if (!curRow.imageUrl.equals(imageUrl)) {
            Row lastRow = curRow;
            curRow = new Row(imageUrl);
            curRow.startIndex = lastRow.startIndex + lastRow.itemArray.size();
            rowArray.add(curRow);
        }
        
        curRow.addItem(Integer.parseInt(xOffset),
                Integer.parseInt(yOffset), Integer.parseInt(width),
                Integer.parseInt(height), url);
    }
    
    public int getSum() {
        int sum = 0;
        for (Row row : rowArray)
            sum += row.itemArray.size();
        return sum;
    }
}
