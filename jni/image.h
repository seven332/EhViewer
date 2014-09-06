#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <GLES2/gl2.h>
#include <android/log.h>

#ifndef IMAGE_H_
#define IMAGE_H_

#ifdef RELEASE

#define LOGV(...)
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)
#define LOGF(...)

#else

#define TAG "libimage"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG ,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)

#endif /* RELEASE */

typedef unsigned char byte;

typedef struct {
    byte l;
} lum;

typedef struct {
    byte l;
    byte a;
} luma;

typedef struct {
    byte red;
    byte green;
    byte blue;
} rgb;

typedef struct {
    byte red;
    byte green;
    byte blue;
    byte alpha;
} rgba;

#define DEFAULT_TARGET GL_TEXTURE_2D
#define DEFAULT_TYPE GL_UNSIGNED_BYTE

#define FORMAT_UNKNOWN -1
#define FORMAT_JPEG 0x0
#define FORMAT_PNG 0x1
#define FORMAT_BMP 0x2
#define FORMAT_GIF 0x3

#define BG_LUM 0x21
static rgb defaultBgColorRGB ={0x21, 0x21, 0x21};

JavaVM *g_jvm;

#endif /* IMAGE_H_ */
