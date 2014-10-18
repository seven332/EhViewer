#include <setjmp.h>

#include "image.h"
#include "jpeg.h"
#include "libjpeg-turbo/jpeglib.h"

struct my_error_mgr {
    struct jpeg_error_mgr pub;
    jmp_buf setjmp_buffer;
};

typedef struct my_error_mgr * my_error_ptr;

char emsg[JMSG_LENGTH_MAX];

static void my_error_exit(j_common_ptr cinfo) {
    my_error_ptr myerr = (my_error_ptr) cinfo->err;
    (*cinfo->err->format_message)(cinfo, emsg);
    longjmp(myerr->setjmp_buffer, 1);
}

jobject JPEG_DecodeStream(JNIEnv* env, jobject is, jint format) {
    // TODO
    return NULL;
}

jobject JPEG_DecodeFileHandler(JNIEnv* env, FILE* fp, jint format) {

    JPEG* jpeg;
    struct jpeg_decompress_struct cinfo;
    struct my_error_mgr jerr;
    JSAMPARRAY buffer;
    void* data;
    int realStride;
    int fakeWidth;
    int fakeStride;
    int readLines;
    jclass imageClazz;
    jmethodID constructor;

    jpeg = (JPEG*) malloc(sizeof(JPEG));
    if (jpeg == NULL)
        return NULL;

    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    if (setjmp(jerr.setjmp_buffer)) {
        LOGE("%s", emsg);
        free(jpeg);
        jpeg_destroy_decompress(&cinfo);
        return NULL;
    }
    jpeg_create_decompress(&cinfo);
    jpeg_stdio_src(&cinfo, fp);
    jpeg_read_header(&cinfo, TRUE);

    // Set png format
    switch (format) {
    case FORMAT_AUTO:
        if (cinfo.jpeg_color_space == JCS_GRAYSCALE) {
            format = FORMAT_GRAY;
            cinfo.out_color_space == JCS_GRAYSCALE;
        } else {
            format = FORMAT_RGB;
            cinfo.out_color_space = JCS_RGB;
        }
        break;
    case FORMAT_RGB:
        cinfo.out_color_space = JCS_RGB;
        break;
    case FORMAT_RGBA:
        cinfo.out_color_space = JCS_EXT_RGBA;
        break;
    case FORMAT_GRAY:
        cinfo.out_color_space = JCS_GRAYSCALE;
        break;
    default:
        format = FORMAT_RGB;
        cinfo.out_color_space = JCS_RGB;
        break;
    }

    jpeg_start_decompress(&cinfo);

    // Set
    realStride = cinfo.output_width * cinfo.output_components;
    fakeWidth = format != FORMAT_RGBA ? nextMulOf4(cinfo.output_width) : cinfo.output_width;
    fakeStride = fakeWidth * cinfo.output_components;
    data = malloc(fakeStride * cinfo.output_height);
    if (data == NULL) {
        free(jpeg);
        jpeg_finish_decompress(&cinfo);
        jpeg_destroy_decompress(&cinfo);
        return NULL;
    }
    buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE,
            realStride, 3);
    jpeg->data = data;
    jpeg->format = format;
    jpeg->width = fakeWidth;
    jpeg->height = cinfo.output_height;

    while (cinfo.output_scanline < cinfo.output_height) {
        readLines = jpeg_read_scanlines(&cinfo, buffer, 3);
        switch(readLines) {
        case 3:
            memcpy(data, buffer[0], realStride);
            data += fakeStride;
        case 2:
            memcpy(data, buffer[readLines - 2], realStride);
            data += fakeStride;
        case 1:
            memcpy(data, buffer[readLines - 1], realStride);
            data += fakeStride;
        }
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);

    imageClazz = (*env)->FindClass(env,
            "com/hippo/ehviewer/gallery/image/Image");
    constructor = (*env)->GetMethodID(env, imageClazz, "<init>",
            "(JIIIII)V");
    if (constructor == 0) {
        JPEG_Free((JNIEnv*)NULL, jpeg);
        return NULL;
    } else {
        return (*env)->NewObject(env, imageClazz, constructor, (jlong) (intptr_t) jpeg,
                FILE_FORMAT_JPEG, cinfo.output_width, cinfo.output_height, format,
                DEFAULT_TYPE);
    }
}

void JPEG_Render(JNIEnv* env, JPEG* jpeg, int format) {
    if (format != jpeg->format)
        return;

    glTexSubImage2D(DEFAULT_TARGET, 0, 0, 0, jpeg->width,
            jpeg->height, format, DEFAULT_TYPE, jpeg->data);
}

void JPEG_Free(JNIEnv* env, JPEG* jpeg) {

    free(jpeg->data);
    free(jpeg);
}
