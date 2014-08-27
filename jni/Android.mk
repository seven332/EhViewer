LOCAL_PATH := $(call my-dir)


##################################################
###                libgif                      ###
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE    := libgif

LOCAL_SRC_FILES := \
giflib/dgif_lib.c \
giflib/gif_hash.c \
giflib/gifalloc.c

include $(BUILD_STATIC_LIBRARY)


##################################################
###                libimage                    ###
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE    := libimage

LOCAL_SRC_FILES := \
image.c \
gif.c \
utils.c

LOCAL_STATIC_LIBRARIES := libgif
LOCAL_LDLIBS := -lGLESv2 # -llog

include $(BUILD_SHARED_LIBRARY)
