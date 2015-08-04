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

#include <stddef.h>
#include <malloc.h>
#include <string.h>

#include <android/bitmap.h>

#include "gif_decoder.h"

#define USE_PIXELS 0
#define USE_BAK 1
#define SET_BG 2

/**
* Global VM reference, initialized in JNI_OnLoad
*/
static JavaVM *g_jvm;

static int errorCode;

static inline void clearException(JNIEnv* env) {
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionClear(env);
    }
}

static inline StreamContainer *createStreamContainer(JNIEnv* env, jobject is) {
    StreamContainer *container = NULL;
    jobject stream = NULL;
    jclass streamClazz = NULL;
    jbyteArray buffer = NULL;

    while (true) {
        container = malloc(sizeof(StreamContainer));
        if (container == NULL) break;

        stream = (*env)->NewGlobalRef(env, is);
        if (stream == NULL) break;
        container->stream = stream;

        streamClazz = (*env)->NewGlobalRef(env, (*env)->GetObjectClass(env, is));
        if (streamClazz == NULL) break;
        container->streamClazz = streamClazz;

        jmethodID readMID = (*env)->GetMethodID(env, streamClazz, "read", "([BII)I");
        jmethodID resetMID = (*env)->GetMethodID(env, streamClazz, "reset", "()V");
        if (readMID == 0 || resetMID == 0) break;
        container->readMID = readMID;
        container->resetMID = resetMID;

        buffer = (*env)->NewByteArray(env, 256);
        buffer = (*env)->NewGlobalRef(env, buffer);
        if (buffer == NULL) break;
        container->buffer = buffer;

        return container;
    }

    free(container);
    (*env)->DeleteGlobalRef(env, stream);
    (*env)->DeleteGlobalRef(env, streamClazz);
    (*env)->DeleteGlobalRef(env, buffer);
    return NULL;
}

static inline void freeStreamContainer(JNIEnv* env, StreamContainer* container) {
    (*env)->DeleteGlobalRef(env, container->buffer);
    (*env)->DeleteGlobalRef(env, container->streamClazz);
    (*env)->DeleteGlobalRef(env, container->stream);
    free(container);
}

inline inline JNIEnv *getEnv() {
    JNIEnv *env;

    if ((*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL) == JNI_OK) {
        return env;
    } else {
        return NULL;
    }
}

static int streamReadFun(GifFileType *gif, GifByteType *bytes, int size) {
    StreamContainer *sc = gif->UserData;
    JNIEnv *env = getEnv();
    if (env == NULL || (*env)->MonitorEnter(env, sc->stream) != 0) {
        return 0;
    }

    jint len = (*env)->CallIntMethod(env, sc->stream, sc->readMID, sc->buffer, 0, size);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        len = 0;
    }
    else if (len > 0) {
        (*env)->GetByteArrayRegion(env, sc->buffer, 0, len, (jbyte *) bytes);
    }
    if ((*env)->MonitorExit(env, sc->stream) != 0) {
        len = 0;
    }

    return len >= 0 ? len : 0;
}

static inline GIF* analysisGifFileType(GifFileType* gifFile, int* delays) {

    int i;
    int* trans;
    int* disposals;
    GIF* gif;
    GraphicsControlBlock gcb;

    int imageCount = gifFile->ImageCount;

    gif = (GIF*) malloc(sizeof(GIF));
    if (gif == NULL) {
        return NULL;
    }

    gif->trans = (int*) malloc(imageCount * sizeof(int));
    gif->disposals = (int*) malloc(imageCount * sizeof(int));
    gif->pixels = malloc(gifFile->SWidth * gifFile->SHeight * sizeof(RGBA));
    if (gif->trans == NULL || gif->disposals == NULL || gif->pixels == NULL) {
        free(gif->pixels);
        free(gif->disposals);
        free(gif->trans);
        free(gif);
        return NULL;
    }

    gif->gifFile = gifFile;
    gif->pxlIndex = -1;
    gif->bak = NULL;
    gif->bakIndex = -1;

    trans = gif->trans;
    disposals = gif->disposals;
    for (i = 0; i < imageCount; i++) {
        if (DGifSavedExtensionToGCB(gifFile, i, &gcb) != GIF_OK) {
            delays[i] = MIN_DELAY;
            trans[i] = -1;
            disposals[i] = 0;
        } else {
            delays[i] = gcb.DelayTime * 10;
            trans[i] = gcb.TransparentColor;
            disposals[i] = gcb.DisposalMode;
            if (delays[i] < MIN_DELAY) {
                delays[i] = MIN_DELAY;
            }
        }
    }

    return gif;
}

static inline void freeGif(GIF* gif) {
    DGifCloseFile(gif->gifFile, &errorCode);
    free(gif->trans);
    free(gif->disposals);
    free(gif->pixels);
    free(gif->bak);
    free(gif);
}

static inline jobject createObjFromGifFileType(JNIEnv* env, GifFileType* gifFile) {

    int slurp;
    int frameCount;
    int* delays;
    GIF* gif;

    jintArray delayArray;
    jclass gifClazz;
    jmethodID constructor;

    // Slurp
    slurp = DGifSlurp(gifFile);
    if (slurp != GIF_OK)
        return NULL;

    // Check frame count
    frameCount = gifFile->ImageCount;
    if (frameCount < 1) {
        return NULL;
    }

    // delays malloc
    delays = (int*) malloc(frameCount * sizeof(int));
    if (delays == NULL) {
        return NULL;
    }

    // analysis GifFileType
    gif = analysisGifFileType(gifFile, delays);
    if (gif == NULL) {
        free(delays);
        return NULL;
    }

    // Copy value
    delayArray = (*env)->NewIntArray(env, frameCount);
    (*env)->SetIntArrayRegion(env, delayArray, 0, frameCount, delays);
    free(delays);

    gifClazz = (*env)->FindClass(env, "com/hippo/ehviewer/gallery/GifDecoder");
    constructor = (*env)->GetMethodID(env, gifClazz, "<init>",
                                      "(JIII[I)V");
    if (constructor == 0) {
        freeGif(gif);
        return NULL;
    } else {
        return (*env)->NewObject(env, gifClazz, constructor, (jlong) gif,
                                 gifFile->SWidth, gifFile->SHeight, frameCount,
                                 delayArray);
    }
}

/*
 * stack store the index to draw
 * stack capacity should be imageCount * sizeof(int).
 * When read it, use from tail to head
 */
static void getDrawStack(GIF *gif, int targetIndex, int *which, int *stack,
                         int *stackSize) {
    int i;
    int *disposals = gif->disposals;
    int pxlIndex = gif->pxlIndex;
    int bakIndex = gif->bakIndex;

    *which = SET_BG;
    *stackSize = 0;

    for (i = targetIndex; i >= 0;) {
        if (i == pxlIndex) {
            *which = USE_PIXELS;
            break;
        } else if (i == bakIndex) {
            *which = USE_BAK;
            break;
        }

        // Add index to stack
        stack[*stackSize] = i;
        (*stackSize)++;

        // If it is first index, break;
        if (i == 0)
            break;

        switch (disposals[--i]) {
            default:
            case DISPOSAL_UNSPECIFIED:
            case DISPOSE_DO_NOT:
                break;
            case DISPOSE_BACKGROUND:
                // Just out of while(),
                i = -1;
                break;
            case DISPOSE_PREVIOUS:
                i--;
                break;
        }
    }
}

static inline bool getColorFromTable(const ColorMapObject* cmap, int index,
                                     RGBA* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount) {
        return false;
    }
    GifColorType gct = cmap->Colors[index];
    color->alpha = 0xff;
    color->red = gct.Red;
    color->green = gct.Green;
    color->blue = gct.Blue;
    return true;
}

static inline void erase(RGBA* pixels, int num, RGBA color) {
    int i;
    for (i = 0; i < num; i++) {
        *(pixels + i) = color;
    }
}

static void setBg(GIF* gif, void* pixels) {
    GifFileType* gifFile = gif->gifFile;
    int num = gifFile->SWidth * gifFile->SHeight;
    RGBA color;
    if (!getColorFromTable(gifFile->SColorMap, gifFile->SBackGroundColor, &color)) {
        color.alpha = 0x00;
        color.red = 0x00;
        color.green = 0x00;
        color.blue = 0x00;
    }
    erase(pixels, num, color);
}

static inline void copyLine(GifByteType* src, RGBA* dst,
                            const ColorMapObject* cmap, int tran, int width) {
    for (; width > 0; width--, src++, dst++) {
        int index = *src;
        if (tran == -1 || index != tran) {
            getColorFromTable(cmap, index, dst);
        }
    }
}

static void drawFrame(GIF* gif, void* pixels, int index) {
    GifFileType* gifFile = gif->gifFile;
    int ScreenWidth = gifFile->SWidth;
    int ScreenHeight = gifFile->SHeight;
    SavedImage cur = gifFile->SavedImages[index];
    GifImageDesc gid = cur.ImageDesc;
    ColorMapObject *cmap = gid.ColorMap;
    int tran = gif->trans[index];
    GifByteType* src;
    RGBA* dst;

    if (cmap == NULL)
        cmap = gifFile->SColorMap;
    if (cmap != NULL) {
        src = cur.RasterBits;
        int copyWidth = gid.Width;
        int copyHeight = gid.Height;
        if (copyWidth + gid.Left > ScreenWidth) {
            copyWidth = ScreenWidth - gid.Left;
        }
        if (copyHeight + gid.Top > ScreenHeight) {
            copyHeight = ScreenHeight - gid.Top;
        }

        if (copyWidth > 0 && copyHeight > 0) {
            dst = (RGBA*) pixels + gid.Top * ScreenWidth + gid.Left;
            for (; copyHeight > 0; copyHeight--, src += gid.Width, dst += ScreenWidth) {
                copyLine(src, dst, cmap, tran, copyWidth);
            }
        }
    }
}

static void renderPixels(GIF *gif, int index) {

    int width = gif->gifFile->SWidth;
    int height = gif->gifFile->SHeight;
    int imageCount = gif->gifFile->ImageCount;
    int which, stackSize;
    int stack[imageCount];
    void* tempPoint;
    int tempInt;

    getDrawStack(gif, index, &which, stack, &stackSize);

    // prepare pixels
    switch (which) {
        case USE_PIXELS:
            break;
        case USE_BAK:
            // switch pixel and bak
            tempPoint = gif->bak;
            gif->bak = gif->pixels;
            gif->pixels = tempPoint;
            tempInt = gif->bakIndex;
            gif->bakIndex = gif->pxlIndex;
            gif->pxlIndex = tempInt;
            break;
        default:
        case SET_BG:
            // Set bg
            setBg(gif, gif->pixels);
            break;
    }

    // Draw frame
    for (stackSize--; stackSize >= 0; stackSize--) {
        drawFrame(gif, gif->pixels, stack[stackSize]);
    }

    // update pxlIndex
    gif->pxlIndex = index;

    // Set back if necessary
    if (index + 1 < imageCount && gif->disposals[index + 1] == DISPOSE_PREVIOUS &&
        gif->bakIndex != gif->pxlIndex) {
        size_t size = width * height * sizeof(RGBA);
        if (gif->bak == NULL) {
            gif->bak = malloc(size);
        }
        if (gif->bak != NULL) {
            memcpy(gif->bak, gif->pixels, size);
            gif->bakIndex = index;
        }
    }
}

jobject Java_com_hippo_ehviewer_gallery_GifDecoder_nativeDecodeStream
        (JNIEnv* env, jclass clazz, jobject stream) {

    StreamContainer* container = createStreamContainer(env, stream);

    GIF* gif = (GIF*) malloc(sizeof(GIF));
    if (gif == NULL) {
        freeStreamContainer(env, container);
        return NULL;
    }

    GifFileType *gifFile = DGifOpen(container, &streamReadFun, &errorCode);
    if (gifFile == NULL) {
        freeStreamContainer(env, container);
        clearException(env);
        return NULL;
    }

    GIF *gifImage = createObjFromGifFileType(env, gifFile);
    if (gifImage == NULL) {
        DGifCloseFile(gifFile, &errorCode);
    }

    freeStreamContainer(env, container);
    clearException(env);

    return gifImage;
}

static inline void copy(RGBA *src, RGBA *dst, int size) {
    int i = 0;
    while(i++ < size) {
        *(dst++) = *(src++);
    }
}

jboolean Java_com_hippo_ehviewer_gallery_GifDecoder_nativeRenderBitmap
        (JNIEnv* env, jclass clazz, jlong nativeGif, jobject bitmap, jint index) {
    GIF *gif = (GIF *) nativeGif;
    AndroidBitmapInfo info;
    int width = gif->gifFile->SWidth;
    int height = gif->gifFile->SHeight;
    void *pixels = NULL;

    if (index < 0 || index >= gif->gifFile->ImageCount) {
        return false;
    }

    AndroidBitmap_getInfo(env, bitmap, &info);
    if (width != info.width || height != info.height || width * sizeof(RGBA) != info.stride) {
        return false;
    }

    renderPixels(gif, index);

    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (pixels == NULL) {
        return false;
    }
    copy(gif->pixels, pixels, width * height);
    pixels = NULL;
    AndroidBitmap_unlockPixels(env, bitmap);

    return true;
}

void Java_com_hippo_ehviewer_gallery_GifDecoder_nativeRecycle
        (JNIEnv* env, jclass clazz, jlong nativeGif) {
    freeGif((GIF *) nativeGif);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv* env;
    if ((*vm)->GetEnv(vm, (void**) (&env), JNI_VERSION_1_6) != JNI_OK)
        return -1;
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
}
