#include "image.h"
#include "gif.h"
#include "png.h"
#include "giflib/gif_lib.h"

static int checkFormat(int format) {
    if (format != FORMAT_AUTO && format != FORMAT_GRAY && format != FORMAT_GRAY_ALPHA
            && format != FORMAT_RGB && format != FORMAT_RGBA)
        return FORMAT_AUTO;
    else
        return format;
}

// TODO
static int detectFileFormat() {
    return FILE_FORMAT_GIF;
}

/**
 *
 */
static FILE* detectFileFormatPathName(const char* namePath, int* format) {

    char jpegSig[2] = {0xFF, 0xD8};
    char pngSig[2] = {137, 80};
    char gifSig[2] = {71, 73};
    char buf[2];

    FILE* fp = fopen(namePath, "r");
    if (fp == NULL) {
        *format = FILE_FORMAT_UNKNOWN;
        return NULL;
    }

    if (fread(buf, 1, 2, fp) != 2) {
        fclose(fp);
        *format = FILE_FORMAT_UNKNOWN;
        return NULL;
    }
    rewind(fp);

    if (!memcmp(buf, jpegSig, 2))
        *format = FILE_FORMAT_JPEG;
    else if (!memcmp(buf, pngSig, 2))
        *format = FILE_FORMAT_PNG;
    else if (!memcmp(buf, gifSig, 2))
        *format = FILE_FORMAT_GIF;
    else
        *format = FILE_FORMAT_UNKNOWN;

    return fp;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeStream(JNIEnv* env,
        jclass clazz, jobject is, jint format) {

    format = checkFormat(format);

    switch (detectFileFormat()) {
    case FILE_FORMAT_JPEG:
        return JPEG_DecodeStream(env, is, format);

    case FILE_FORMAT_PNG:
        return PNG_DecodeStream(env, is, format);

    case FILE_FORMAT_BMP:
        return NULL;

    case FILE_FORMAT_GIF:
        return GIF_DecodeStream(env, is, format);

    default:
        return NULL;
    }
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeFile(JNIEnv* env,
        jclass clazz, jstring namePath, jint format) {

    int fileFormat;
    const char *str;
    FILE* fp;
    jobject image;

    format = checkFormat(format);
    str = (*env)->GetStringUTFChars(env, namePath, 0);

    fp = detectFileFormatPathName(str, &fileFormat);
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, namePath, str);
        return NULL;
    }

    switch (fileFormat) {
    case FILE_FORMAT_JPEG:
        image = JPEG_DecodeFileHandler(env, fp, format);
        break;
    case FILE_FORMAT_PNG:
        image = PNG_DecodeFileHandler(env, fp, format);
        break;
    case FILE_FORMAT_BMP:
        image = NULL;
        break;
    case FILE_FORMAT_GIF:
        image = GIF_DecodeFileHandler(env, fp, format);
        break;
    default:
        image = NULL;
        break;
    }

    (*env)->ReleaseStringUTFChars(env, namePath, str);
    fclose(fp);

    return image;
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeFree(JNIEnv* env,
        jclass clazz, jint nativeImage, jint fileFormat) {

    switch (fileFormat) {
    case FILE_FORMAT_JPEG:
        JPEG_Free(env, nativeImage);
        break;
    case FILE_FORMAT_PNG:
        PNG_Free(env, nativeImage);
        break;
    case FILE_FORMAT_BMP:
        break;
    case FILE_FORMAT_GIF:
        GIF_Free(env, nativeImage);
        break;
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeRender(JNIEnv* env,
        jclass clazz, jint format, jint type, jint nativeImage, jint fileFormat) {

    switch (fileFormat) {
    case FILE_FORMAT_JPEG:
        JPEG_Render(env, nativeImage, format);
        break;
    case FILE_FORMAT_PNG:
        PNG_Render(env, nativeImage, format);
        break;
    case FILE_FORMAT_BMP:
        break;
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_GifImage_nativeRender(JNIEnv* env,
        jclass clazz, jint format, jint type, jint nativeImage, jint fileFormat,
        jint index) {

    if (type != DEFAULT_TYPE || fileFormat != FILE_FORMAT_GIF)
        return;

    GIF_Render(env, nativeImage, format, index);
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
