#include <jni.h>

#include "giflib/gif_lib.h"

#ifndef GIF_H_
#define GIF_H_

#define DEFAULT_DELAY 100

typedef struct {
    GifFileType* gifFile;
    int* trans;
    int* disposals;
    int format;
    void* pixels;
    int pxlIndex;
    void* bak;
    int bakIndex;
} GIF;

jobject GIF_DecodeStream(JNIEnv* env, jobject is, jint format);
void GIF_Render(JNIEnv* env, int nativeImage, int format, int index);
void GIF_Free(JNIEnv* env, int nativeImage);

#endif /* GIF_H_ */
