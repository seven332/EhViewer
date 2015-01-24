/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.regex.Pattern;

public class GalleryInfo implements Parcelable {

    /**
     * ISO 639-1
     */
    @SuppressWarnings("unused")
    public static final String S_LANG_JA = "JA";
    public static final String S_LANG_EN = "EN";
    public static final String S_LANG_ZH = "ZH";
    public static final String S_LANG_NL = "NL";
    public static final String S_LANG_FR = "FR";
    public static final String S_LANG_DE = "DE";
    public static final String S_LANG_HU = "HU";
    public static final String S_LANG_IT = "IT";
    public static final String S_LANG_KO = "KO";
    public static final String S_LANG_PL = "PL";
    public static final String S_LANG_PT = "PT";
    public static final String S_LANG_RU = "RU";
    public static final String S_LANG_ES = "ES";
    public static final String S_LANG_TH = "TH";
    public static final String S_LANG_VI = "VI";

    public static final String[] S_LANGS = {
        S_LANG_EN,
        S_LANG_ZH,
        S_LANG_ES,
        S_LANG_KO,
        S_LANG_RU,
        S_LANG_FR,
        S_LANG_PT,
        S_LANG_TH,
        S_LANG_DE,
        S_LANG_IT,
        S_LANG_VI,
        S_LANG_PL,
        S_LANG_HU,
        S_LANG_NL,
    };

    public static final String[] S_LANG_PATTERNS = {
        "[(\\[]eng(?:lish)?[)\\]]",
        "[(（\\[]chinese[)）\\]]|[汉漢]化|中[国國][语語]|中文|[(\\[]CN[)\\]]",
        "[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]",
        "[(\\[]korean?[)\\]]",
        "[(\\[]rus(?:sian)?[)\\]]",
        "[(\\[]fr(?:ench)?[)\\]]",
        "[(\\[]portuguese",
        "[(\\[]thai(?: ภาษาไทย)?[)\\]]",
        "[(\\[]german[)\\]]",
        "[(\\[]italiano?[)\\]]",
        "[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]",
        "[(\\[]polish[)\\]]",
        "[(\\[]hun(?:garian)?[)\\]]",
        "[(\\[]dutch[)\\]]",
    };

    public int gid;
    public String token;
    public String title;
    public String posted;
    public int category;
    public String thumb;
    public String uploader;
    public float rating;
    /**
     * Will not be recond
     */
    public int thumbWidth;
    /**
     * Will not be recond
     */
    public int thumbHeight;
    /**
     * language get from title
     */
    public String simpleLanguage;

    public static final Creator<GalleryInfo> CREATOR =
            new Creator<GalleryInfo>() {
                @Override
                public GalleryInfo createFromParcel(Parcel source) {
                    GalleryInfo p = new GalleryInfo();
                    p.gid = source.readInt();
                    p.token = source.readString();
                    p.title = source.readString();
                    p.posted = source.readString();
                    p.category = source.readInt();
                    p.thumb = source.readString();
                    p.uploader = source.readString();
                    p.rating = source.readFloat();
                    p.simpleLanguage = source.readString();
                    return p;
                }

                @Override
                public GalleryInfo[] newArray(int size) {
                    return new GalleryInfo[size];
                }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(gid);
        dest.writeString(token);
        dest.writeString(title);
        dest.writeString(posted);
        dest.writeInt(category);
        dest.writeString(thumb);
        dest.writeString(uploader);
        dest.writeFloat(rating);
        dest.writeString(simpleLanguage);
    }

    public GalleryInfo() {
        // Empty
    }

    public GalleryInfo(GalleryInfo gi) {
        gid = gi.gid;
        token = gi.token;
        title = gi.title;
        posted = gi.posted;
        category = gi.category;
        thumb = gi.thumb;
        uploader = gi.uploader;
        rating = gi.rating;
        simpleLanguage = gi.simpleLanguage;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GalleryInfo && ((GalleryInfo) o).gid == gid)
            return true;
        else
            return false;
    }

    public final void generateSLang() {
        for (int i = 0; i < S_LANGS.length; i++) {
            if (Pattern.compile(S_LANG_PATTERNS[i], Pattern.CASE_INSENSITIVE).matcher(title).find()) {
                simpleLanguage = S_LANGS[i];
                return;
            }
        }
        simpleLanguage = null;
    }
}
