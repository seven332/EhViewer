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

#ifndef EHVIEWER_GIF_DECODER_H
#define EHVIEWER_GIF_DECODER_H

#include <jni.h>

#include "giflib/gif_lib.h"

#define MIN_DELAY 16

typedef struct {
    jobject stream;
    jclass streamClazz;
    jmethodID readMID;
    jmethodID resetMID;
    jbyteArray buffer;
} StreamContainer;

typedef struct {
    GifFileType* gifFile;
    int* trans;
    int* disposals;
    void* pixels;
    int pxlIndex;
    void* bak;
    int bakIndex;
} GIF;

typedef unsigned char byte;

typedef struct {
    byte red;
    byte green;
    byte blue;
    byte alpha;
} RGBA;

#endif //EHVIEWER_GIF_DECODER_H
