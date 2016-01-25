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

#include "image_utils.h"

#include <stdlib.h>

#define LITTLE_ENDIAN  0x00
#define BIG_ENDIAN 0x01

static unsigned int endian = ~0U;

static unsigned int get_endian() {
  unsigned int x = 1;

  if (endian == ~0U) {
    if (1 == ((char *)&x)[0]) {
      endian = LITTLE_ENDIAN;
    } else {
      endian = BIG_ENDIAN;
    }
  }

  return endian;
}

static int convert_color(int origin) {
  int result;
  unsigned char* orPtr = (unsigned char *) &origin;
  unsigned char* rePtr = (unsigned char *) &result;

  if (get_endian() == BIG_ENDIAN) {
    rePtr[0] = orPtr[1];
    rePtr[1] = orPtr[2];
    rePtr[2] = orPtr[3];
    rePtr[3] = orPtr[0];
  } else {
    rePtr[0] = orPtr[2];
    rePtr[1] = orPtr[1];
    rePtr[2] = orPtr[0];
    rePtr[3] = orPtr[3];
  }

  return result;
}

static void memset_int(int* dst, int val, size_t size) {
  int* maxPtr = dst + size;
  int* ptr = dst;
  while(ptr < maxPtr)
    *ptr++ = val;
}

bool copy_pixels(const void* src, int src_w, int src_h, int src_x, int src_y,
    void* dst, int dst_w, int dst_h, int dst_x, int dst_y,
    int width, int height, bool fill_blank, int default_color)
{
  int line;
  size_t line_stride;
  int src_stride;
  int src_pos;
  int dst_pos;
  size_t dst_blank_length;
  int color = 0;

  // Check valid first
  if (src_w < 0 || src_h < 0 || src_x < 0 || src_y < 0 ||
      dst_w < 0 || dst_h < 0 || dst_x < 0 || dst_y < 0 ||
      width < 0 || height < 0 ||
      src_x + width > src_w || src_y + height > src_h ||
      dst_x + width > dst_w || dst_y + height > dst_h) {
    return false;
  }

  if (width == 0 || height == 0) {
    // No need to copy pixels
    return true;
  }

  // Init
  line_stride = (size_t) (width * 4);
  src_stride = src_w * 4;
  src_pos = src_y * src_stride + src_x * 4;
  dst_pos = 0;
  if (fill_blank) {
    color = convert_color(default_color);
  }

  dst_blank_length = (size_t) (dst_y * dst_w + dst_x) * 4;
  // Fill start blank if necessary
  if (fill_blank) {
    memset_int((int *) (dst + dst_pos), color, dst_blank_length / 4);
  }

  // First line
  dst_pos += dst_blank_length;
  memcpy(dst + dst_pos, src + src_pos, line_stride);
  dst_pos += line_stride;
  src_pos += src_stride;

  // Other lines
  dst_blank_length = (size_t) ((dst_w - width) * 4);
  for (line = 1; line < height; line++) {
    if (fill_blank) {
      memset_int((int *) (dst + dst_pos), color, dst_blank_length / 4);
    }
    dst_pos += dst_blank_length;
    memcpy(dst + dst_pos, src + src_pos, line_stride);
    dst_pos += line_stride;
    src_pos += src_stride;
  }

  // Fill left blank if necessary
  if (fill_blank) {
    memset_int((int *) (dst + dst_pos), color, (size_t) (dst_w * dst_h * 4 - dst_pos) / 4);
  }

  return true;
}
