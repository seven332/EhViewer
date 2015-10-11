 LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := gifdecoder
LOCAL_SRC_FILES := \
./gif_decoder.c \
./giflib/dgif_lib.c \
./giflib/gifalloc.c

LOCAL_CFLAGS := -O3 -DANDROID
LOCAL_LDLIBS = -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
