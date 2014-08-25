#include "image.h"
#include "utils.h"

byte getVFrowRGB(byte r, byte g, byte b) {
    byte t =  g > b ? g : b;
    return r > t ? r : t;
}

void eraseRGB(rgb* pixels, int num, rgb color) {
    int i;
    for (i = 0; i < num; i++)
        *(pixels + i) = color;
}

void eraseRGBA(rgba* pixels, int num, rgba color) {
    int i;
    for (i = 0; i < num; i++)
        *(pixels + i) = color;
}

void eraseLUM(lum* pixels, int num, lum color) {
    int i;
    for (i = 0; i < num; i++)
        *(pixels + i) = color;
}

void eraseLUMA(luma* pixels, int num, luma color) {
    int i;
    for (i = 0; i < num; i++)
        *(pixels + i) = color;
}

void clearException(JNIEnv* env) {
    if ((*env)->ExceptionOccurred(env))
        (*env)->ExceptionClear(env);
}

StreamContainer* getStreamContainer(JNIEnv* env, jobject is) {
    jclass streamCls = (*env)->GetObjectClass(env, is);
    jmethodID readMID = (*env)->GetMethodID(env, streamCls, "read", "([BII)I");
    jmethodID closeMID = (*env)->GetMethodID(env, streamCls, "close", "()V");

    if (readMID == 0 || closeMID == 0)
        return NULL ;

    // Create stream container
    StreamContainer* sc = (StreamContainer*) malloc(sizeof(StreamContainer));
    sc->readMID = readMID;
    sc->closeMID = closeMID;
    sc->jvm = g_jvm;
    sc->stream = (*env)->NewGlobalRef(env, is);
    sc->buffer = NULL;

    return sc;
}

void closeStreamContainer(JNIEnv* env, StreamContainer* sc) {
    (*env)->CallVoidMethod(env, sc->stream, sc->closeMID);
}

void freeStreamContainer(JNIEnv* env, StreamContainer* sc) {
    (*env)->DeleteGlobalRef(env, sc->stream);
    if (sc->buffer != NULL)
        (*env)->DeleteGlobalRef(env, sc->buffer);
    free(sc);
}
