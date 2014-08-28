#include "png.h"
#include "libpng/png.h"

jobject PNG_DecodeStream(JNIEnv* env, jobject is, jint format) {
    // TODO
    return NULL;
}

jobject PNG_DecodeFileHandler(JNIEnv* env, FILE* fp, jint format) {
    int pngFormat, width, height;
    png_image image;
    png_bytep buffer;
    PNG* png;
    jclass imageClazz;
    jmethodID constructor;
    int fakeWidth;
    int fakeStride;

    png = (PNG*) malloc(sizeof(PNG));
    if (png == NULL)
        return NULL;

    // Get png format
    switch (format) {
    case GL_RGB:
        pngFormat = PNG_FORMAT_RGB;
        break;
    case GL_RGBA:
        pngFormat = PNG_FORMAT_RGBA;
        break;
    case GL_LUMINANCE:
        pngFormat = PNG_FORMAT_GRAY;
        break;
    case GL_LUMINANCE_ALPHA:
    default:
        format = GL_LUMINANCE_ALPHA;
        pngFormat = PNG_FORMAT_GA;
        break;
    }

    memset(&image, 0, sizeof image);
    image.version = PNG_IMAGE_VERSION;

    if (!png_image_begin_read_from_stdio(&image, fp))
        return NULL;

    image.format = pngFormat;
    fakeWidth = format != GL_RGBA ? nextMulOf4(image.width) : image.width;
    fakeStride = PNG_IMAGE_PIXEL_CHANNELS(image.format) * fakeWidth;
    buffer = malloc(image.height * fakeStride);
    if (buffer == NULL)
        return NULL;

    if (!png_image_finish_read(&image, (png_const_colorp)&defaultBgColorRGB,
            buffer, fakeStride, NULL)) {
        free(buffer);
        png_image_free(&image);
        return NULL;
    }

    // We get data now ~~~~ !
    png->data = buffer;
    png->width = fakeWidth;
    png->height = image.height; // Tell other the image file width
    png->format = format;

    imageClazz = (*env)->FindClass(env,
            "com/hippo/ehviewer/gallery/image/Image");
    constructor = (*env)->GetMethodID(env, imageClazz, "<init>",
            "(IIIIII)V");

    if (constructor == 0) {
        PNG_Free((JNIEnv*)NULL, (int)png);
        return NULL;
    } else {
        return (*env)->NewObject(env, imageClazz, constructor, (jint) png,
                FORMAT_PNG, png->width, png->height, format,
                DEFAULT_TYPE);
    }
}

void PNG_Render(JNIEnv* env, int nativeImage, int format) {
    PNG* png = (PNG*) nativeImage;
    if (format != png->format)
        return;

    glTexSubImage2D(DEFAULT_TARGET, 0, 0, 0, png->width,
            png->height, format, DEFAULT_TYPE, png->data);
}

void PNG_Free(JNIEnv* env, int nativeImage) {

    PNG* png = (PNG*) nativeImage;
    free(png->data);
    free(png);
}
