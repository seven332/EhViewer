#include <jni.h>
#include <stdlib.h>
#include <memory.h>
#include <GLES2/gl2.h>
#include <android/log.h>

#include "image.h"
#include "giflib/gif_lib.h"

//#define STRICT_FORMAT_89A

#define DEFAULT_DELAY 100

#define TAG "image"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)

static JavaVM *g_jvm;
static int errorCode;


static void eraseRGB(rgb* pixels, int num, rgb color) {
    int i;
    for (i = 0; i < num; i++)
        *(pixels + i) = color;
}


static bool getColorFromTableRGB(const ColorMapObject* cmap, int index, rgb* color) {
    if (cmap == NULL || index < 0 || index >= cmap->ColorCount)
        return false;
    GifColorType gct = cmap->Colors[index];
    color->red = gct.Red;
    color->green = gct.Green;
    color->blue = gct.Blue;
    return true;
}


/***********************************/

static void* getGifPixelRGB(GifFileType* gifFile, int index) {

    int ScreenWidth = gifFile->SWidth;
    int ScreenHeight = gifFile->SHeight;
    ColorMapObject* globalCmap = gifFile->SColorMap;

    int num = ScreenWidth * ScreenHeight;
    int size = num * sizeof(rgb);
    rgb* pixels = (rgb*) malloc(size);
    rgb tempColor;

    tempColor.red = 255;
    tempColor.green = 0;
    tempColor.blue = 0;

    // Set background
    if (getColorFromTableRGB(globalCmap, gifFile->SBackGroundColor, &tempColor))
        eraseRGB(pixels, num, tempColor);
    else
        memset(pixels, 33, size);

    return pixels;
}

static void *getGifPixelRGBA(GifFileType* gifFile, int index);
static void *getGifPixelLUM(GifFileType* gifFile, int index);
static void *getGifPixelLUMA(GifFileType* gifFile, int index);

/**********************************/

static StreamContainer* getStreamContainer(JNIEnv* env, jobject is) {
    jclass streamCls = (*env)->GetObjectClass(env, is);
    jmethodID readMID = (*env)->GetMethodID(env, streamCls, "read", "([BII)I");
    jmethodID closeMID = (*env)->GetMethodID(env, streamCls, "close", "()V");

    if (readMID == 0 || closeMID == 0)
        return NULL ;

    // Create stream container
    StreamContainer* sc = (StreamContainer*) malloc(sizeof(StreamContainer));
    sc->readMID = readMID;
    sc->closeMID = closeMID;
    sc->jvm = g_jvm;
    sc->stream = (*env)->NewGlobalRef(env, is);
    sc->buffer = NULL;

    return sc;
}

static void closeStreamContainer(JNIEnv* env, StreamContainer* sc) {
    (*env)->CallVoidMethod(env, sc->stream, sc->closeMID);
}

static void freeStreamContainer(JNIEnv* env, StreamContainer* sc) {
    (*env)->DeleteGlobalRef(env, sc->stream);
    if (sc->buffer != NULL)
        (*env)->DeleteGlobalRef(env, sc->buffer);
    free(sc);
}

static void clearException(JNIEnv* env) {
    if ((*env)->ExceptionOccurred(env))
        (*env)->ExceptionClear(env);
}

/*
 * Return NULL if error
 */
static JNIEnv*
getEnv(GifFileType* gif) {
    if (gif == NULL)
        return NULL ;

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

// TODO It only do with gif
/*
 * Class:     com_hippo_ehviewer_gallery_image_Image
 * Method:    nativeDecodeStream
 * Signature: (Ljava/io/InputStream;I)Lcom/hippo/ehviewer/gallery/image/Image;
 */
JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeStream(JNIEnv* env,
        jclass clazz, jobject is, jint format) {

    if (format == GL_RGBA)
        format = GL_RGBA;


    StreamContainer* sc = getStreamContainer(env, is);
    if (sc == NULL)
        return NULL ;

    // Open file
    GifFileType* GifFile = DGifOpen(sc, &streamReadFun, &errorCode);
    if (GifFile == NULL) {
        freeStreamContainer(env, sc);
        return NULL ;
    }

    // Slurp
    int slurp = DGifSlurp(GifFile);
    // Free container
    closeStreamContainer(env, sc);
    clearException(env);
    freeStreamContainer(env, sc);

#if defined(STRICT_FORMAT_89A)
    if (slurp != GIF_OK)
    return NULL;
#endif

    // Get control info
    int imageCount = GifFile->ImageCount;
    if (imageCount < 1)
        return NULL ;
    int delays[imageCount];
    int tis[imageCount];
    GraphicsControlBlock* gcb = (GraphicsControlBlock*) malloc(
            sizeof(GraphicsControlBlock));
    int i;
    for (i = 0; i < imageCount; i++) {
        if (DGifSavedExtensionToGCB(GifFile, i, gcb) != GIF_OK) {
            delays[i] = DEFAULT_DELAY;
            tis[i] = -1;
        } else {
            delays[i] = gcb->DelayTime * 10;
            tis[i] = gcb->TransparentColor;
        }
    }
    free(gcb);

    // Copy value
    jintArray delayArray = (*env)->NewIntArray(env, imageCount);
    jintArray tiArray = (*env)->NewIntArray(env, imageCount);
    (*env)->SetIntArrayRegion(env, delayArray, 0, imageCount, delays);
    (*env)->SetIntArrayRegion(env, tiArray, 0, imageCount, tis);

    jclass gifClazz = (*env)->FindClass(env,
            "com/hippo/ehviewer/gallery/image/GifImage");
    jmethodID constructor = (*env)->GetMethodID(env, gifClazz, "<init>",
            "(IIIIII[I[I)V");
    if (constructor == 0)
        return NULL ;
    else
        return (*env)->NewObject(env, gifClazz, constructor, (jint) GifFile,
        FORMAT_GIF, GifFile->SWidth, GifFile->SHeight, format,
        GL_UNSIGNED_BYTE, delayArray, tiArray);
}

// TODO It only do with gif
/*
 * Class:     com_hippo_ehviewer_gallery_image_Image
 * Method:    nativeFree
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeFree(JNIEnv* env,
        jclass clazz, jint nativeImage, jint fileFormat) {

    GifFileType* GifFile;

    switch (fileFormat) {
    case FORMAT_JPEG:
        break;
    case FORMAT_PNG:
        break;
    case FORMAT_BMP:
        break;
    case FORMAT_GIF:
        GifFile = (GifFileType*) nativeImage;
        DGifCloseFile(GifFile, &errorCode);
        break;
    }
}

/*
 * Class:     com_hippo_ehviewer_gallery_image_Image
 * Method:    nativeRender
 * Signature: (IIIIIIII)V
 */
JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeRender(JNIEnv* env,
        jclass clazz, jint target, jint level, jint xoffset, jint yoffset,
        jint format, jint type, jint nativeImage, jint fileFormat) {




}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_GifImage_nativeRender(JNIEnv* env,
        jclass clazz, jint target, jint level, jint xoffset, jint yoffset,
        jint format, jint type, jint nativeImage, jint fileFormat, jint index,
        jint tran) {

    GifFileType* gifFile = (GifFileType*) nativeImage;
    void* pixels = NULL;

    switch (format) {
    case GL_RGB:
    case GL_RGBA:
        format = GL_RGB;
        pixels = getGifPixelRGB(gifFile, index);
        LOGE("GL_RGB");
        break;
    case GL_LUMINANCE:
        break;
    case GL_LUMINANCE_ALPHA:
        break;
    default:
        return;
    }

    if (pixels != NULL) {
        glTexSubImage2D(target, level, xoffset, yoffset, gifFile->SWidth, gifFile->SHeight, GL_RGB, GL_UNSIGNED_BYTE, pixels);
        free(pixels);
    }
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
