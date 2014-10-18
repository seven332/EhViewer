#include "png.h"
#include "libpng/png.h"

jobject PNG_DecodeStream(JNIEnv* env, jobject is, jint format) {
    // TODO
    return NULL;
}

jobject PNG_DecodeFileHandler(JNIEnv* env, FILE* fp, jint format) {
    int width, height;
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

    memset(&image, 0, sizeof image);
    image.version = PNG_IMAGE_VERSION;

    if (!png_image_begin_read_from_stdio(&image, fp))
        return NULL;

    // Get png format
    switch (format) {
    case FORMAT_AUTO:
        if (image.format == PNG_FORMAT_GRAY) {
            format = FORMAT_GRAY;
        }  else if (image.format == PNG_FORMAT_GA) {
            format = FORMAT_GRAY_ALPHA;
        } else if (image.format == PNG_FORMAT_RGB) {
            format = FORMAT_RGB;
        } else if (image.format == PNG_FORMAT_RGBA) {
            format = FORMAT_RGBA;
        } else {
            format = FORMAT_RGB;
            image.format = PNG_FORMAT_RGB;
        }
        break;
    case FORMAT_RGB:
        image.format = PNG_FORMAT_RGB;
        break;
    case FORMAT_RGBA:
        image.format = PNG_FORMAT_RGBA;
        break;
    case FORMAT_GRAY:
        image.format = PNG_FORMAT_GRAY;
        break;
    case FORMAT_GRAY_ALPHA:
        image.format = PNG_FORMAT_GA;
        break;
    default:
        format = FORMAT_RGB;
        image.format = PNG_FORMAT_RGB;
        break;
    }

    fakeWidth = format != FORMAT_RGBA ? nextMulOf4(image.width) : image.width;
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
            "(JIIIII)V");

    if (constructor == 0) {
        PNG_Free((JNIEnv*)NULL, png);
        return NULL;
    } else {
        return (*env)->NewObject(env, imageClazz, constructor, (jlong) (intptr_t) png,
                FILE_FORMAT_PNG, png->width, png->height, format,
                DEFAULT_TYPE);
    }
}

void PNG_Render(JNIEnv* env, PNG* png, int format) {
    if (format != png->format)
        return;

    glTexSubImage2D(DEFAULT_TARGET, 0, 0, 0, png->width,
            png->height, format, DEFAULT_TYPE, png->data);
}

void PNG_Free(JNIEnv* env, PNG* png) {

    free(png->data);
    free(png);
}
