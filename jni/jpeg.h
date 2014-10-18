#ifndef JPEG_H_
#define JPEG_H_

#include <jni.h>

#include "image.h"

typedef struct {
    void* data;
    int width;
    int height;
    int format;
} JPEG;

jobject JPEG_DecodeStream(JNIEnv* env, jobject is, jint format);
jobject JPEG_DecodeFileHandler(JNIEnv* env, FILE* fp, jint format);
void JPEG_Render(JNIEnv* env, JPEG* jpeg, int format);
void JPEG_Free(JNIEnv* env, JPEG* jpeg);

#endif /* JPEG_H_ */
