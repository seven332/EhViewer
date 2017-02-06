/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.widget;

/*
 * Created by Hippo on 2/3/2017.
 */

import static junit.framework.Assert.assertEquals;

import android.support.annotation.NonNull;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ContentDataTest {

  @Test
  public void testGoTo() {
    VoidData data = new VoidData();
    data.minPage = -1;
    data.maxPage = 3;
    data.beginPage = -1;
    data.endPage = 3;
    data.data.addAll(Arrays.asList(new Void[4]));
    data.dataDivider.addAll(Arrays.asList(1, 2, 3, 4));

    data.goTo(100);
    data.response(7, 45, 88);

    assertEquals(data.minPage, 45);
    assertEquals(data.maxPage, 88);
    assertEquals(data.beginPage, 100);
    assertEquals(data.endPage, 101);
    assertEquals(7, data.data.size());
    assertEquals(Collections.singletonList(7), data.dataDivider);
  }

  @Test
  public void testPrevPage() {
    VoidData data = new VoidData();
    data.minPage = -3;
    data.maxPage = 3;
    data.beginPage = 0;
    data.endPage = 2;
    data.data.addAll(Arrays.asList(new Void[6]));
    data.dataDivider.addAll(Arrays.asList(2, 6));

    data.prevPage();
    data.response(7, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -1);
    assertEquals(data.endPage, 2);
    assertEquals(13, data.data.size());
    assertEquals(Arrays.asList(7, 9, 13), data.dataDivider);

    data.prevPage();
    data.response(0, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -2);
    assertEquals(data.endPage, 2);
    assertEquals(13, data.data.size());
    assertEquals(Arrays.asList(0, 7, 9, 13), data.dataDivider);
  }

  @Test
  public void testNextPage() {
    VoidData data = new VoidData();
    data.minPage = -3;
    data.maxPage = 3;
    data.beginPage = -1;
    data.endPage = 1;
    data.data.addAll(Arrays.asList(new Void[6]));
    data.dataDivider.addAll(Arrays.asList(2, 6));

    data.nextPage();
    data.response(7, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -1);
    assertEquals(data.endPage, 2);
    assertEquals(13, data.data.size());
    assertEquals(Arrays.asList(2, 6, 13), data.dataDivider);

    data.nextPage();
    data.response(0, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -1);
    assertEquals(data.endPage, 3);
    assertEquals(13, data.data.size());
    assertEquals(Arrays.asList(2, 6, 13, 13), data.dataDivider);
  }

  @Test
  public void testRefreshPage() {
    VoidData data = new VoidData();
    data.minPage = -3;
    data.maxPage = 3;
    data.beginPage = -1;
    data.endPage = 2;
    data.data.addAll(Arrays.asList(new Void[6]));
    data.dataDivider.addAll(Arrays.asList(2, 4, 6));

    data.refreshPage(0);
    data.response(7, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -1);
    assertEquals(data.endPage, 2);
    assertEquals(11, data.data.size());
    assertEquals(Arrays.asList(2, 9, 11), data.dataDivider);

    data.refreshPage(0);
    data.response(0, -3, 3);

    assertEquals(data.minPage, -3);
    assertEquals(data.maxPage, 3);
    assertEquals(data.beginPage, -1);
    assertEquals(data.endPage, 2);
    assertEquals(4, data.data.size());
    assertEquals(Arrays.asList(2, 2, 4), data.dataDivider);
  }

  private static final class VoidData extends ContentData<Void> {

    private long id;

    @Override
    public void onRequireData(long id, int page) {
      this.id = id;
    }

    public void response(int size, int min, int max) {
      setData(id, Arrays.asList(new Void[size]), min, max);
    }

    @NonNull
    @Override
    public ContentLayout.TipInfo getTipFromThrowable(Throwable e) {
      return new ContentLayout.TipInfo();
    }

    @Override
    public void showMessage(String text) {}
  }
}
