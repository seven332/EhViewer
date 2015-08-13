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

package com.hippo.ehviewer;

import com.hippo.yorozuya.Messenger;

public final class Constants {

    public static final int MESSENGER_ID_READING_DIRECTION;
    public static final int MESSENGER_ID_EH_SOURCE;
    public static final int MESSENGER_ID_SHOW_APP_STATUS;
    public static final int MESSENGER_ID_SIGN_IN_OR_OUT;
    public static final int MESSENGER_ID_USER_AVATAR;
    public static final int MESSENGER_ID_JPN_TITLE;
    public static final int MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION;
    public static final int MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION;
    public static final int MESSENGER_ID_UPDATE_QUICK_SEARCH;

    static {
        Messenger messenger = Messenger.getInstance();
        MESSENGER_ID_READING_DIRECTION = messenger.newId();
        MESSENGER_ID_EH_SOURCE = messenger.newId();
        MESSENGER_ID_SHOW_APP_STATUS = messenger.newId();
        MESSENGER_ID_SIGN_IN_OR_OUT = messenger.newId();
        MESSENGER_ID_USER_AVATAR = messenger.newId();
        MESSENGER_ID_JPN_TITLE = messenger.newId();
        MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION = messenger.newId();
        MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION = messenger.newId();
        MESSENGER_ID_UPDATE_QUICK_SEARCH = messenger.newId();
    }
}
