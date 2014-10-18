#ifndef PNG_H_
#define PNG_H_

#include <jni.h>

#include "image.h"

typedef struct {
    void* data;
    int width;
    int height;
    int format;
} PNG;

jobject PNG_DecodeStream(JNIEnv* env, jobject is, jint format);
jobject PNG_DecodeFileHandler(JNIEnv* env, FILE* fp, jint format);
void PNG_Render(JNIEnv* env, PNG* png, int format);
void PNG_Free(JNIEnv* env, PNG* png);

#endif /* PNG_H_ */
