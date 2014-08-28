#include "image.h"
#include "gif.h"
#include "png.h"
#include "giflib/gif_lib.h"

static int checkFormat(int format) {
    if (format != GL_RGB && format != GL_RGBA && format != GL_LUMINANCE
            && format != GL_LUMINANCE_ALPHA)
        return GL_RGB;
    else
        return format;
}

// TODO
static int detectFileFormat() {
    return FORMAT_GIF;
}

/**
 *
 */
static FILE* detectFileFormatPathName(const char* namePath, int* format) {

    char pngSig[3] = {137, 80, 78};
    char gifSig[3] = {71, 73, 70};
    char buf[3];

    FILE* fp = fopen(namePath, "r");
    if (fp == NULL) {
        *format = FORMAT_UNKNOWN;
        return NULL;
    }

    if (fread(buf, 1, 3, fp) != 3) {
        fclose(fp);
        *format = FORMAT_UNKNOWN;
        return NULL;
    }
    rewind(fp);

    if (!memcmp(buf, pngSig, 3))
        *format = FORMAT_PNG;
    else if (!memcmp(buf, gifSig, 3))
        *format = FORMAT_GIF;
    else
        *format = FORMAT_UNKNOWN;

    return fp;
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeStream(JNIEnv* env,
        jclass clazz, jobject is, jint format) {

    format = checkFormat(format);

    switch (detectFileFormat()) {
    case FORMAT_JPEG:
        return NULL;

    case FORMAT_PNG:
        return PNG_DecodeStream(env, is, format);
        return NULL;

    case FORMAT_BMP:
        return NULL;

    case FORMAT_GIF:
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
    case FORMAT_JPEG:
        image = NULL;
        break;
    case FORMAT_PNG:
        image = PNG_DecodeFileHandler(env, fp, format);
        break;
    case FORMAT_BMP:
        image = NULL;
        break;
    case FORMAT_GIF:
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
    case FORMAT_JPEG:
        break;
    case FORMAT_PNG:
        PNG_Free(env, nativeImage);
        break;
    case FORMAT_BMP:
        break;
    case FORMAT_GIF:
        GIF_Free(env, nativeImage);
        break;
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeRender(JNIEnv* env,
        jclass clazz, jint format, jint type, jint nativeImage, jint fileFormat) {

    switch (fileFormat) {
    case FORMAT_JPEG:
        break;
    case FORMAT_PNG:
        PNG_Render(env, nativeImage, format);
        break;
    case FORMAT_BMP:
        break;
    }
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_GifImage_nativeRender(JNIEnv* env,
        jclass clazz, jint format, jint type, jint nativeImage, jint fileFormat,
        jint index) {

    if (type != DEFAULT_TYPE || fileFormat != FORMAT_GIF)
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
