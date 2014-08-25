#include "gif.h"
#include "utils.h"
#include "image.h"

//#define STRICT_FORMAT_89A

#define USE_PIXELS 0
#define USE_BAK 1
#define SET_BG 2

static int errorCode;

/*
 * Return NULL if error
 */
static JNIEnv* getEnv(GifFileType* gif) {
    if (gif == NULL)
        return NULL;

    JNIEnv* env = NULL;
    StreamContainer* sc = (StreamContainer*) (gif->UserData);
    if (sc != NULL) {
        JavaVM* jvm = sc->jvm;
        (*jvm)->AttachCurrentThread(jvm, &env, NULL);
    }
    return env;
}

static int streamReadFun(GifFileType* gif, GifByteType* bytes, int size) {
    StreamContainer* sc = (StreamContainer*) gif->UserData;
    JNIEnv* env = getEnv(gif);

    if (env == NULL)
        return 0;

    (*env)->MonitorEnter(env, sc->stream);

    if (sc->buffer == NULL) {
        jbyteArray buffer = (*env)->NewByteArray(env, size < 256 ? 256 : size);
        sc->buffer = (jbyteArray) (*env)->NewGlobalRef(env, buffer);
    } else {
        jsize bufLen = (*env)->GetArrayLength(env, sc->buffer);
        if (bufLen < size) {
            (*env)->DeleteGlobalRef(env, sc->buffer);
            sc->buffer = NULL;

            jbyteArray buffer = (*env)->NewByteArray(env, size);
            sc->buffer = (*env)->NewGlobalRef(env, buffer);
        }
    }

    int len = (*env)->CallIntMethod(env, sc->stream, sc->readMID, sc->buffer, 0,
            size);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionClear(env);
        len = 0;
    } else if (len > 0) {
        (*env)->GetByteArrayRegion(env, sc->buffer, 0, len, bytes);
    }

    (*env)->MonitorExit(env, sc->stream);

    return len >= 0 ? len : 0;
}

/*
 * stack store the index to draw
 * stack capacity should be imageCount * sizeof(int).
 * When read it, use for(; size > 0; size--).
 */
static void getDrawStack(GIF* gif, int targetIndex, int* which, int* stack,
        int* stackSize) {
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

static inline bool getColorFromTableRGB(const ColorMapObject* cmap, int index,
        rgb* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount)
        return false;
    GifColorType gct = cmap->Colors[index];
    color->red = gct.Red;
    color->green = gct.Green;
    color->blue = gct.Blue;
    return true;
}

static inline bool getColorFromTableRGBA(const ColorMapObject* cmap, int index,
        rgba* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount)
        return false;
    GifColorType gct = cmap->Colors[index];
    color->red = gct.Red;
    color->green = gct.Green;
    color->blue = gct.Blue;
    color->alpha = 0xff;
    return true;
}

static inline bool getColorFromTableLUM(const ColorMapObject* cmap, int index,
        lum* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount)
        return false;
    GifColorType gct = cmap->Colors[index];
    color->l = getVFrowRGB(gct.Red, gct.Green, gct.Blue);
    return true;
}

static inline bool getColorFromTableLUMA(const ColorMapObject* cmap, int index,
        luma* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount)
        return false;
    GifColorType gct = cmap->Colors[index];
    color->l = getVFrowRGB(gct.Red, gct.Green, gct.Blue);
    color->a = 0xff;
    return true;
}

static void setBgRGB(GIF* gif, rgb* pixels) {
    GifFileType* gifFile = gif->gifFile;
    int num = gifFile->SWidth * gifFile->SHeight;
    int size = num * sizeof(rgb);
    rgb color;
    if (getColorFromTableRGB(gifFile->SColorMap, gifFile->SBackGroundColor, &color))
        eraseRGB(pixels, num, color);
    else
        memset(pixels, BG_LUM, size);
}

static void setBgRGBA(GIF* gif, rgba* pixels) {
    GifFileType* gifFile = gif->gifFile;
    int num = gifFile->SWidth * gifFile->SHeight;
    int size = num * sizeof(rgba);
    rgba color;
    if (getColorFromTableRGBA(gifFile->SColorMap, gifFile->SBackGroundColor, &color)) {
        eraseRGBA(pixels, num, color);
        color.red = BG_LUM;
        color.green = BG_LUM;
        color.blue = BG_LUM;
        color.red = 0xff;
    }
    eraseRGBA(pixels, num, color);
}

static void setBgLUM(GIF* gif, lum* pixels) {
    GifFileType* gifFile = gif->gifFile;
    int num = gifFile->SWidth * gifFile->SHeight;
    int size = num * sizeof(lum);
    lum color;
    if (getColorFromTableLUM(gifFile->SColorMap, gifFile->SBackGroundColor, &color))
        eraseLUM(pixels, num, color);
    else
        memset(pixels, BG_LUM, size);
}

static void setBgLUMA(GIF* gif, luma* pixels) {
    GifFileType* gifFile = gif->gifFile;
    int num = gifFile->SWidth * gifFile->SHeight;
    int size = num * sizeof(lum);
    luma color;
    if (!getColorFromTableLUMA(gifFile->SColorMap, gifFile->SBackGroundColor, &color)) {
        color.l = BG_LUM;
        color.a = 0xff;
    }
    eraseLUMA(pixels, num, color);
}

static void setBg(GIF* gif, void* pixels) {
    switch (gif->format) {
    case GL_RGB:
        setBgRGB(gif, pixels);
        break;
    case GL_RGBA:
        setBgRGBA(gif, pixels);
        break;
    case GL_LUMINANCE:
        setBgLUM(gif, pixels);
        break;
    case GL_LUMINANCE_ALPHA:
        setBgLUMA(gif, pixels);
        break;
    }
}

static inline void copyLineRGB(GifByteType* src, rgb* dst,
        const ColorMapObject* cmap, int tran, int width) {
    for (; width > 0; width--, src++, dst++) {
        int index = *src;
        if (tran == -1 || *src != tran)
            getColorFromTableRGB(cmap, *src, dst);
    }
}

static inline void copyLineRGBA(GifByteType* src, rgba* dst,
        const ColorMapObject* cmap, int tran, int width) {
    for (; width > 0; width--, src++, dst++) {
        int index = *src;
        if (tran == -1 || *src != tran)
            getColorFromTableRGBA(cmap, *src, dst);
    }
}

static inline void copyLineLUM(GifByteType* src, lum* dst,
        const ColorMapObject* cmap, int tran, int width) {
    for (; width > 0; width--, src++, dst++) {
        int index = *src;
        if (tran == -1 || *src != tran)
            getColorFromTableLUM(cmap, *src, dst);
    }
}

static inline void copyLineLUMA(GifByteType* src, luma* dst,
        const ColorMapObject* cmap, int tran, int width) {
    for (; width > 0; width--, src++, dst++) {
        int index = *src;
        if (tran == -1 || *src != tran)
            getColorFromTableLUMA(cmap, *src, dst);
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
    rgb* dst1;
    rgba* dst2;
    lum* dst3;
    luma* dst4;

    if (cmap == NULL)
        cmap = gifFile->SColorMap;
    if (cmap != NULL) {
        src = cur.RasterBits;
        int copyWidth = gid.Width;
        int copyHeight = gid.Height;
        if (copyWidth + gid.Left > ScreenWidth)
            copyWidth = ScreenWidth - gid.Left;
        if (copyHeight + gid.Top > ScreenHeight)
            copyHeight = ScreenHeight - gid.Top;

        if (copyWidth > 0 && copyHeight > 0) {
            switch (gif->format) {
            case GL_RGB:
                dst1 = (rgb*)pixels + gid.Top * ScreenWidth + gid.Left;
                for (; copyHeight > 0; copyHeight--, src += gid.Width, dst1 += ScreenWidth)
                    copyLineRGB(src, dst1, cmap, tran, copyWidth);
                break;
            case GL_RGBA:
                dst2 = (rgba*)pixels + gid.Top * ScreenWidth + gid.Left;
                for (; copyHeight > 0; copyHeight--, src += gid.Width, dst2 += ScreenWidth)
                    copyLineRGBA(src, dst2, cmap, tran, copyWidth);
                break;
            case GL_LUMINANCE:
                dst3 = (lum*)pixels + gid.Top * ScreenWidth + gid.Left;
                for (; copyHeight > 0; copyHeight--, src += gid.Width, dst3 += ScreenWidth)
                    copyLineLUM(src, dst3, cmap, tran, copyWidth);
                break;
            case GL_LUMINANCE_ALPHA:
                dst4 = (luma*)pixels + gid.Top * ScreenWidth + gid.Left;
                for (; copyHeight > 0; copyHeight--, src += gid.Width, dst4 += ScreenWidth)
                    copyLineLUMA(src, dst4, cmap, tran, copyWidth);
                break;
            }
        }
    }
}

static void render(GIF* gif, int index) {

    int i;
    int width = gif->gifFile->SWidth;
    int height = gif->gifFile->SHeight;
    int imageCount = gif->gifFile->ImageCount;
    int which, stackSize;
    int* stack = (int*) malloc(imageCount * sizeof(int));
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
    case SET_BG:
        // Set bg
        setBg(gif, gif->pixels);
        break;
    }

    // Draw frame
    for (stackSize--; stackSize >= 0; stackSize--)
        drawFrame(gif, gif->pixels, stack[stackSize]);

    // render in screen
    glTexSubImage2D(DEFAULT_TARGET, 0, 0, 0, width,
            height, gif->format, DEFAULT_TYPE, gif->pixels);

    // release
    free(stack);

    // update pxlIndex
    gif->pxlIndex = index;

    // Set back if necessary
    if (index + 1 < imageCount && gif->disposals[index + 1] == DISPOSE_PREVIOUS &&
            gif->bakIndex != gif->pxlIndex) {
        int size;
        switch (gif->format) {
        case GL_RGB:
            size = width * height * sizeof(rgb);
            break;
        case GL_RGBA:
            size = width * height * sizeof(rgba);
            break;
        case GL_LUMINANCE:
            size = width * height * sizeof(lum);
            break;
        case GL_LUMINANCE_ALPHA:
            size = width * height * sizeof(luma);
            break;
        }
        if (gif->bak == NULL)
            gif->bak = malloc(size);
        memcpy(gif->bak, gif->pixels, size);
        gif->bakIndex = index;
    }
}

static GIF* analysisGifFileType(GifFileType* gifFile, int format, int* delays) {

    int i;
    int* trans;
    int* disposals;
    GIF* gif;
    GraphicsControlBlock gcb;

    int imageCount = gifFile->ImageCount;

    gif = (GIF*) malloc(sizeof(GIF));
    gif->gifFile = gifFile;
    gif->trans = (int*) malloc(imageCount * sizeof(int));
    gif->disposals = (int*) malloc(imageCount * sizeof(int));
    gif->format = format;
    switch (format) {
    case GL_RGB:
        gif->pixels = malloc(gifFile->SWidth * gifFile->SHeight * sizeof(rgb));
        break;
    case GL_RGBA:
        gif->pixels = malloc(gifFile->SWidth * gifFile->SHeight * sizeof(rgba));
        break;
    case GL_LUMINANCE:
        gif->pixels = malloc(gifFile->SWidth * gifFile->SHeight * sizeof(lum));
        break;
    case GL_LUMINANCE_ALPHA:
        gif->pixels = malloc(gifFile->SWidth * gifFile->SHeight * sizeof(luma));
        break;
    }
    gif->pxlIndex = -1;
    gif->bak = NULL;
    gif->bakIndex = -1;

    trans = gif->trans;
    disposals = gif->disposals;
    for (i = 0; i < imageCount; i++) {
        if (DGifSavedExtensionToGCB(gifFile, i, &gcb) != GIF_OK) {
            delays[i] = DEFAULT_DELAY;
            trans[i] = -1;
            disposals[i] = 0;
        } else {
            delays[i] = gcb.DelayTime * 10;
            trans[i] = gcb.TransparentColor;
            disposals[i] = gcb.DisposalMode;
            if (delays[i] <= 0)
                delays[i] = DEFAULT_DELAY;
        }
    }

    return gif;
}

jobject GIF_DecodeStream(JNIEnv* env, jobject is, jint format) {

    StreamContainer* sc;
    GifFileType* gifFile;
    GIF* gif;
    int slurp, imageCount;
    int* delays;

    // GIF not
    sc = getStreamContainer(env, is);
    if (sc == NULL)
        return NULL;

    // Open file
    gifFile = DGifOpen(sc, &streamReadFun, &errorCode);
    if (gifFile == NULL) {
        freeStreamContainer(env, sc);
        return NULL;
    }

    // Slurp
    slurp = DGifSlurp(gifFile);
    // Free container
    closeStreamContainer(env, sc);
    clearException(env);
    freeStreamContainer(env, sc);
#if defined(STRICT_FORMAT_89A)
    if (slurp != GIF_OK) {
        DGifCloseFile(GifFile);
        return NULL;
    }
#endif

    // Check image count
    imageCount = gifFile->ImageCount;
    if (imageCount < 1) {
        DGifCloseFile(gif->gifFile, &errorCode);
        return NULL;
    }

    // Get GIF
    delays = (int*) malloc(imageCount * sizeof(int));
    gif = analysisGifFileType(gifFile, format, delays);
    if (gif == NULL) {
        DGifCloseFile(gif->gifFile, &errorCode);
        return NULL;
    }

    // Copy value
    jintArray delayArray = (*env)->NewIntArray(env, imageCount);
    (*env)->SetIntArrayRegion(env, delayArray, 0, imageCount, delays);

    jclass gifClazz = (*env)->FindClass(env,
            "com/hippo/ehviewer/gallery/image/GifImage");
    jmethodID constructor = (*env)->GetMethodID(env, gifClazz, "<init>",
            "(IIIIII[I)V");
    if (constructor == 0)
        return NULL;
    else
        return (*env)->NewObject(env, gifClazz, constructor, (jint) gif,
                FORMAT_GIF, gifFile->SWidth, gifFile->SHeight, format,
                DEFAULT_TYPE, delayArray);
}

void GIF_Render(JNIEnv* env, int nativeImage, int format, int index) {

    GIF* gif = (GIF*) nativeImage;
    if (format != gif->format)
        return;

    render(gif, index);
}

void GIF_Free(JNIEnv* env, int nativeImage) {

    GIF* gif = (GIF*) nativeImage;
    DGifCloseFile(gif->gifFile, &errorCode);
    if (gif->trans != NULL) free(gif->trans);
    if (gif->disposals != NULL) free(gif->disposals);
    if (gif->pixels != NULL) free(gif->pixels);
    if (gif->bak != NULL) free(gif->bak);
    free(gif);
}

