# Copyright 2015 Hippo Seven
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := image
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../libjpeg-turbo $(LOCAL_PATH)/../libpng $(LOCAL_PATH)/../giflib $(LOCAL_PATH)/../stream
LOCAL_SRC_FILES := \
image.c \
image_jpeg.c \
image_png.c \
image_gif.c \
image_utils.c \
java_wrapper.c
LOCAL_LDLIBS := -llog -ljnigraphics -lGLESv2
LOCAL_STATIC_LIBRARIES := jpeg-turbo png gif stream

include $(BUILD_SHARED_LIBRARY)
