LOCAL_PATH := $(call my-dir)

##################################################
###                libz                        ###
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE    := libz
LOCAL_SRC_FILES := \
zlib/adler32.c \
zlib/crc32.c \
zlib/inffast.c \
zlib/inflate.c \
zlib/inftrees.c \
zlib/trees.c \
zlib/zutil.c

include $(BUILD_STATIC_LIBRARY)


##################################################
###              libpng                        ###
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE    := libpng
LOCAL_SRC_FILES := \
libpng/png.c \
libpng/pngerror.c \
libpng/pngget.c \
libpng/pngmem.c \
libpng/pngpread.c \
libpng/pngread.c \
libpng/pngrio.c \
libpng/pngrtran.c \
libpng/pngrutil.c \
libpng/pngset.c \
libpng/pngtrans.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS := -mfpu=neon
    LOCAL_SRC_FILES += \
    libpng/arm/arm_init.c \
    libpng/arm/filter_neon_intrinsics.c \
    libpng/arm/filter_neon.S
endif

LOCAL_STATIC_LIBRARIES := libz

include $(BUILD_STATIC_LIBRARY)


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
png.c \
gif.c \
utils.c

LOCAL_STATIC_LIBRARIES := libgif libpng
LOCAL_LDLIBS := -lGLESv2 -llog

include $(BUILD_SHARED_LIBRARY)
