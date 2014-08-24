LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := image
LOCAL_SRC_FILES := \
image.c \
giflib/dgif_lib.c \
giflib/gif_hash.c \
giflib/gifalloc.c
LOCAL_LDLIBS := -llog -lGLESv2

include $(BUILD_SHARED_LIBRARY)
