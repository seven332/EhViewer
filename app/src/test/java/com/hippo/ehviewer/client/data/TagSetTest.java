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

package com.hippo.ehviewer.client.data;

/*
 * Created by Hippo on 5/15/2017.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.os.Parcel;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class TagSetTest {

  public static void assertTagSetEquals(Map<String, Set<String>> tags, TagSet tagSet) {
    try {
      Field field = TagSet.class.getDeclaredField("groups");
      field.setAccessible(true);
      assertEquals(tags, field.get(tagSet));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAddRemoveSize() {
    TagSet set = new TagSet();

    set.add("namespace1", "tag1");
    assertEquals(1, set.size());
    Map<String, Set<String>> map = new HashMap<>();
    map.put("namespace1", Collections.singleton("tag1"));
    assertTagSetEquals(map, set);

    // Add a duplicate tag
    set.add("namespace1", "tag1");
    assertEquals(1, set.size());
    map = new HashMap<>();
    map.put("namespace1", Collections.singleton("tag1"));
    assertTagSetEquals(map, set);

    set.add("namespace1", "tag2");
    assertEquals(2, set.size());
    map = new HashMap<>();
    map.put("namespace1", Sets.newHashSet("tag1", "tag2"));
    assertTagSetEquals(map, set);

    set.add("namespace2", "tag1");
    assertEquals(3, set.size());
    map = new HashMap<>();
    map.put("namespace1", Sets.newHashSet("tag1", "tag2"));
    map.put("namespace2", Sets.newHashSet("tag1"));
    assertTagSetEquals(map, set);

    // Remove a tag to make a namespace empty
    set.remove("namespace2", "tag1");
    assertEquals(2, set.size());
    map = new HashMap<>();
    map.put("namespace1", Sets.newHashSet("tag1", "tag2"));
    assertTagSetEquals(map, set);

    // Remove a unrelated tag
    set.remove("namespace100", "tag1");
    assertEquals(2, set.size());
    map = new HashMap<>();
    map.put("namespace1", Sets.newHashSet("tag1", "tag2"));
    assertTagSetEquals(map, set);

    // Remove a unrelated tag
    set.remove("namespace1", "tag100");
    assertEquals(2, set.size());
    map = new HashMap<>();
    map.put("namespace1", Sets.newHashSet("tag1", "tag2"));
    assertTagSetEquals(map, set);
  }

  @Test
  public void testEquals() {
    TagSet set1 = new TagSet();
    set1.add("namespace1", "tag1");
    set1.add("namespace1", "tag2");
    set1.add("namespace2", "tag1");
    TagSet set2 = new TagSet();
    set2.add("namespace1", "tag1");
    set2.add("namespace1", "tag2");
    assertNotEquals(set1, set2);

    set2.add("namespace2", "tag1");
    assertEquals(set1, set2);

    set2.add("namespace2", "tag2");
    assertNotEquals(set1, set2);
  }

  @Test
  public void testSet() {
    TagSet set1 = new TagSet();
    set1.add("namespace1", "tag1");
    set1.add("namespace1", "tag2");
    set1.add("namespace2", "tag1");

    TagSet set2 = new TagSet();
    assertNotEquals(set1, set2);

    set2.set(set1);
    assertEquals(set1, set2);
  }

  @Test
  public void testParcel() {
    Parcel parcel = Parcel.obtain();

    TagSet set1 = new TagSet();
    set1.add("namespace1", "tag1");
    set1.add("namespace1", "tag2");
    set1.add("namespace2", "tag1");

    set1.writeToParcel(parcel, 0);
    parcel.setDataPosition(0);

    TagSet set2 = TagSet.CREATOR.createFromParcel(parcel);
    assertEquals(set1, set2);

    parcel.recycle();
  }
}
