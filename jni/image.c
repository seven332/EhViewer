#include "image.h"
#include "gif.h"
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

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeStream(JNIEnv* env,
        jclass clazz, jobject is, jint format) {

    format = checkFormat(format);

    switch (detectFileFormat()) {
    case FORMAT_JPEG:
        return NULL ;

    case FORMAT_PNG:
        return NULL ;

    case FORMAT_BMP:
        return NULL ;

    case FORMAT_GIF:
        return GIF_DecodeStream(env, is, format);

    default:
        return NULL ;
    }
}

JNIEXPORT jobject JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeDecodeFile(JNIEnv* env,
        jclass clazz, jstring namePath, jint format) {

    const char *str;
    jobject image;

    format = checkFormat(format);
    str = (*env)->GetStringUTFChars(env, namePath, 0);

    switch (detectFileFormat()) {
    case FORMAT_JPEG:
        image = NULL;
        break;
    case FORMAT_PNG:
        image = NULL;
        break;
    case FORMAT_BMP:
        image = NULL;
        break;
    case FORMAT_GIF:
        image = GIF_DecodeFile(env, str, format);
        break;
    default:
        image = NULL;
        break;
    }

    (*env)->ReleaseStringUTFChars(env, namePath, str);

    return image;
}

JNIEXPORT void JNICALL
Java_com_hippo_ehviewer_gallery_image_Image_nativeFree(JNIEnv* env,
        jclass clazz, jint nativeImage, jint fileFormat) {

    switch (fileFormat) {
    case FORMAT_JPEG:
        break;
    case FORMAT_PNG:
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
    // TODO
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
