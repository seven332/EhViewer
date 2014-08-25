#ifndef UTILS_H_
#define UTILS_H_

#include "image.h"

typedef struct {
    JavaVM* jvm;
    jobject stream;
    jmethodID readMID;
    jmethodID closeMID;
    jbyteArray buffer;
} StreamContainer;

void eraseRGB(rgb* pixels, int num, rgb color);
void eraseRGBA(rgba* pixels, int num, rgba color);
void eraseLUM(lum* pixels, int num, lum color);
void eraseLUMA(luma* pixels, int num, luma color);

byte getVFrowRGB(byte r, byte g, byte b);

void clearException(JNIEnv* env);

StreamContainer* getSC(JNIEnv* env, jobject is);
void closeSC(JNIEnv* env, StreamContainer* sc);
void freeSC(JNIEnv* env, StreamContainer* sc);

#endif /* UTILS_H_ */
