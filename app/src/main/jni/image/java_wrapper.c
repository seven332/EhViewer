/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// Created by Hippo on 12/27/2015.
//

#include <stdlib.h>
#include <android/bitmap.h>
#include <GLES2/gl2.h>

#include "java_wrapper.h"
#include "input_stream.h"
#include "image.h"
#include "../log.h"

static JavaVM* jvm;

static void* tile_buffer;

JNIEnv *get_env()
{
  JNIEnv *env;

  if ((*jvm)->AttachCurrentThread(jvm, &env, NULL) == JNI_OK) {
    return env;
  } else {
    return NULL;
  }
}

jobject create_image_object(JNIEnv* env, void* ptr, int format, int width, int height)
{
  jclass image_clazz;
  jmethodID constructor;

  image_clazz = (*env)->FindClass(env, "com/hippo/image/Image");
  constructor = (*env)->GetMethodID(env, image_clazz, "<init>", "(JIII)V");
  if (constructor == 0) {
    LOGE(MSG("Can't find Image object constructor"));
    return NULL;
  } else {
    return (*env)->NewObject(env, image_clazz, constructor,
        (jlong) (uintptr_t) ptr, (jint) format, (jint) width, (jint) height);
  }
}

JNIEXPORT jobject JNICALL
Java_com_hippo_image_Image_nativeDecode(JNIEnv* env,
    jclass clazz, jobject is, jboolean partially)
{
  InputStream* input_stream;
  int format;
  void* image;
  jobject image_object;

  input_stream = create_input_stream(env, is);
  if (input_stream == NULL) {
    return NULL;
  }

  image = decode(env, input_stream, partially, &format);
  if (image == NULL) {
    return NULL;
  }

  image_object = create_image_object(env, image, format,
      get_width(image, format), get_height(image, format));
  if (image_object == NULL) {
    recycle(image, format);
    return NULL;
  } else {
    return image_object;
  }
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_image_Image_nativeComplete(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  return (jboolean) complete((void*) (intptr_t) ptr, format);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_image_Image_nativeIsCompleted(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  return (jboolean) is_completed((void*) (intptr_t) ptr, format);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRender(JNIEnv* env,
    jclass clazz, jlong ptr, jint format,
    jint src_x, jint src_y, jobject dst, jint dst_x, jint dst_y,
    jint width, jint height, jboolean fill_blank, jint default_color)
{
  AndroidBitmapInfo info;
  void *pixels = NULL;

  AndroidBitmap_getInfo(env, dst, &info);
  AndroidBitmap_lockPixels(env, dst, &pixels);
  if (pixels == NULL) {
    LOGE(MSG("Can't lock bitmap pixels"));
    return;
  }

  render((void*) (intptr_t) ptr, format, src_x, src_y,
      pixels, info.width, info.height, dst_x, dst_y,
      width, height, fill_blank, default_color);

  AndroidBitmap_unlockPixels(env, dst);

  return;
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeTexImage(JNIEnv* env,
    jclass clazz, jlong ptr, jint format, jboolean init, jint tile_type,
    jint offset_x, jint offset_y)
{
  // Check tile_buffer NULL
  if (NULL == tile_buffer) {
    return;
  }

  // Get border size and tile size
  int border_size;
  int tile_size;
  switch (tile_type) {
    case IMAGE_TILE_TYPE_SMALL:
      border_size = IMAGE_SMALL_TILE_BORDER_SIZE;
      tile_size = IMAGE_SMALL_TILE_SIZE;
      break;
    case IMAGE_TILE_TYPE_LARGE:
      border_size = IMAGE_LARGE_TILE_BORDER_SIZE;
      tile_size = IMAGE_LARGE_TILE_SIZE;
      break;
    default:
      return;
  }

  render((void*) (intptr_t) ptr, format,
      offset_x - border_size, offset_y - border_size,
      tile_buffer, tile_size, tile_size, 0, 0,
      tile_size, tile_size, true, 0);
  if (init) {
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tile_size, tile_size,
        0, GL_RGBA, GL_UNSIGNED_BYTE, tile_buffer);
  } else {
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, tile_size, tile_size,
        GL_RGBA, GL_UNSIGNED_BYTE, tile_buffer);
  }
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeAdvance(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  advance((void*) (intptr_t) ptr, format);
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeGetDelay(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  return (jint) get_delay((void*) (intptr_t) ptr, format);
}

JNIEXPORT jint JNICALL
Java_com_hippo_image_Image_nativeFrameCount(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  return (jint) get_frame_count((void*) (intptr_t) ptr, format);
}

JNIEXPORT jboolean JNICALL
Java_com_hippo_image_Image_nativeIsOpaque(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  return (jboolean) is_opaque((void*) (intptr_t) ptr, format);
}

JNIEXPORT void JNICALL
Java_com_hippo_image_Image_nativeRecycle(JNIEnv* env,
    jclass clazz, jlong ptr, jint format)
{
  recycle((void*) (intptr_t) ptr, format);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
  JNIEnv* env;
  if ((*vm)->GetEnv(vm, (void**) (&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }
  jvm = vm;

  tile_buffer = malloc(IMAGE_LARGE_TILE_SIZE * IMAGE_LARGE_TILE_SIZE * 4);

  return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
  free(tile_buffer);
  tile_buffer = NULL;
}
