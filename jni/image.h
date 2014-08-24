/*
 * image.h
 *
 *  Created on: 2014年8月24日
 *      Author: Hippo
 */

#include <jni.h>

#ifndef IMAGE_H_
#define IMAGE_H_

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

#define FORMAT_JPEG 0x0
#define FORMAT_PNG 0x1
#define FORMAT_BMP 0x2
#define FORMAT_GIF 0x3

typedef struct {
    JavaVM* jvm;
    jobject stream;
    jmethodID readMID;
    jmethodID closeMID;
    jbyteArray buffer;
} StreamContainer;


#endif /* IMAGE_H_ */
