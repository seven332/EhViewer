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

package com.hippo.ehviewer.client.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class GalleryBase implements Parcelable {

    public static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    static {
        DEFAULT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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
    public String titleJpn;
    public String thumb;
    public int category;
    public String posted;
    public String uploader;
    public float rating;

    /**
     * language from title
     */
    public String simpleLanguage;

    public final void generateSLang() {
        for (int i = 0; i < S_LANGS.length; i++) {
            if (Pattern.compile(S_LANG_PATTERNS[i], Pattern.CASE_INSENSITIVE).matcher(title).find()) {
                simpleLanguage = S_LANGS[i];
                return;
            }
        }
        simpleLanguage = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.gid);
        dest.writeString(this.token);
        dest.writeString(this.title);
        dest.writeString(this.titleJpn);
        dest.writeString(this.thumb);
        dest.writeInt(this.category);
        dest.writeString(this.posted);
        dest.writeString(this.uploader);
        dest.writeFloat(this.rating);
        dest.writeString(this.simpleLanguage);
    }

    public GalleryBase() {
    }

    protected GalleryBase(Parcel in) {
        this.gid = in.readInt();
        this.token = in.readString();
        this.title = in.readString();
        this.titleJpn = in.readString();
        this.thumb = in.readString();
        this.category = in.readInt();
        this.posted = in.readString();
        this.uploader = in.readString();
        this.rating = in.readFloat();
        this.simpleLanguage = in.readString();
    }

    public static final Parcelable.Creator<GalleryBase> CREATOR = new Parcelable.Creator<GalleryBase>() {

        @Override
        public GalleryBase createFromParcel(Parcel source) {
            return new GalleryBase(source);
        }

        @Override
        public GalleryBase[] newArray(int size) {
            return new GalleryBase[size];
        }
    };
}
