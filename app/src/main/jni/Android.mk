LOCAL_PATH := $(call my-dir)

##################################################
###              libjpeg-turbo                 ###
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE    := libjpeg-turbo
LOCAL_SRC_FILES:= \
libjpeg-turbo/jaricom.c \
libjpeg-turbo/jcomapi.c \
libjpeg-turbo/jdapimin.c \
libjpeg-turbo/jdapistd.c \
libjpeg-turbo/jdarith.c \
libjpeg-turbo/jdatadst.c \
libjpeg-turbo/jdatadst-tj.c \
libjpeg-turbo/jdatasrc.c \
libjpeg-turbo/jdatasrc-tj.c \
libjpeg-turbo/jdcoefct.c \
libjpeg-turbo/jdcolor.c \
libjpeg-turbo/jddctmgr.c \
libjpeg-turbo/jdhuff.c \
libjpeg-turbo/jdinput.c \
libjpeg-turbo/jdmainct.c \
libjpeg-turbo/jdmarker.c \
libjpeg-turbo/jdmaster.c \
libjpeg-turbo/jdmerge.c \
libjpeg-turbo/jdphuff.c \
libjpeg-turbo/jdpostct.c \
libjpeg-turbo/jdsample.c \
libjpeg-turbo/jdtrans.c \
libjpeg-turbo/jerror.c \
libjpeg-turbo/jfdctflt.c \
libjpeg-turbo/jfdctfst.c \
libjpeg-turbo/jfdctint.c \
libjpeg-turbo/jidctflt.c \
libjpeg-turbo/jidctfst.c \
libjpeg-turbo/jidctint.c \
libjpeg-turbo/jidctred.c \
libjpeg-turbo/jmemmgr.c \
libjpeg-turbo/jmemnobs.c \
libjpeg-turbo/jquant1.c \
libjpeg-turbo/jquant2.c \
libjpeg-turbo/jutils.c
LOCAL_CFLAGS := -O3 -DANDROID

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi armeabi-v7a))
    # Don't add -mfpu=neon
    # Some arm v7 cpu do not support neon,
    # libjpeg-turbo can check neon in init_simd()
    # through parse_proc_cpuinfo()
    #LOCAL_CFLAGS += -mfpu=neon
    LOCAL_SRC_FILES += \
    libjpeg-turbo/simd/jsimd_arm.c \
    libjpeg-turbo/simd/jsimd_arm_neon.S
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_SRC_FILES += \
    libjpeg-turbo/simd/jsimd_arm64.c \
    libjpeg-turbo/simd/jsimd_arm64_neon.S
endif

ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_SRC_FILES += \
    libjpeg-turbo/simd/jsimd_i386.c \
    libjpeg-turbo/simd/jsimdcpu.asm \
    libjpeg-turbo/simd/jfdctflt-3dn.asm \
    libjpeg-turbo/simd/jidctflt-3dn.asm \
    libjpeg-turbo/simd/jquant-3dn.asm \
    libjpeg-turbo/simd/jccolor-mmx.asm \
    libjpeg-turbo/simd/jcgray-mmx.asm \
    libjpeg-turbo/simd/jcsample-mmx.asm \
    libjpeg-turbo/simd/jdcolor-mmx.asm \
    libjpeg-turbo/simd/jdmerge-mmx.asm \
    libjpeg-turbo/simd/jdsample-mmx.asm \
    libjpeg-turbo/simd/jfdctfst-mmx.asm \
    libjpeg-turbo/simd/jfdctint-mmx.asm \
    libjpeg-turbo/simd/jidctfst-mmx.asm \
    libjpeg-turbo/simd/jidctint-mmx.asm \
    libjpeg-turbo/simd/jidctred-mmx.asm \
    libjpeg-turbo/simd/jquant-mmx.asm \
    libjpeg-turbo/simd/jfdctflt-sse.asm \
    libjpeg-turbo/simd/jidctflt-sse.asm \
    libjpeg-turbo/simd/jquant-sse.asm \
    libjpeg-turbo/simd/jccolor-sse2.asm \
    libjpeg-turbo/simd/jcgray-sse2.asm \
    libjpeg-turbo/simd/jcsample-sse2.asm \
    libjpeg-turbo/simd/jdcolor-sse2.asm \
    libjpeg-turbo/simd/jdmerge-sse2.asm \
    libjpeg-turbo/simd/jdsample-sse2.asm \
    libjpeg-turbo/simd/jfdctfst-sse2.asm \
    libjpeg-turbo/simd/jfdctint-sse2.asm \
    libjpeg-turbo/simd/jidctflt-sse2.asm \
    libjpeg-turbo/simd/jidctfst-sse2.asm \
    libjpeg-turbo/simd/jidctint-sse2.asm \
    libjpeg-turbo/simd/jidctred-sse2.asm \
    libjpeg-turbo/simd/jquantf-sse2.asm \
    libjpeg-turbo/simd/jquanti-sse2.asm
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
    LOCAL_SRC_FILES += \
    libjpeg-turbo/simd/jsimd_x86_64.c \
    libjpeg-turbo/simd/jfdctflt-sse-64.asm \
    libjpeg-turbo/simd/jccolor-sse2-64.asm \
    libjpeg-turbo/simd/jcgray-sse2-64.asm \
    libjpeg-turbo/simd/jcsample-sse2-64.asm \
    libjpeg-turbo/simd/jdcolor-sse2-64.asm \
    libjpeg-turbo/simd/jdmerge-sse2-64.asm \
    libjpeg-turbo/simd/jdsample-sse2-64.asm \
    libjpeg-turbo/simd/jfdctfst-sse2-64.asm \
    libjpeg-turbo/simd/jfdctint-sse2-64.asm \
    libjpeg-turbo/simd/jidctflt-sse2-64.asm \
    libjpeg-turbo/simd/jidctfst-sse2-64.asm \
    libjpeg-turbo/simd/jidctint-sse2-64.asm \
    libjpeg-turbo/simd/jidctred-sse2-64.asm \
    libjpeg-turbo/simd/jquantf-sse2-64.asm \
    libjpeg-turbo/simd/jquanti-sse2-64.asm
endif

ifeq ($(TARGET_ARCH_ABI),mips)
    LOCAL_SRC_FILES += \
    libjpeg-turbo/simd/jsimd_mips.c \
    libjpeg-turbo/simd/jsimd_mips_dspr2.S
endif

ifeq ($(TARGET_ARCH_ABI),mips64)
    LOCAL_SRC_FILES += \
    libjpeg-turbo/jsimd_none.c
endif

include $(BUILD_STATIC_LIBRARY)


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
LOCAL_CFLAGS := -O3 -DANDROID

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
LOCAL_CFLAGS := -O3 -DANDROID

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    # Add -mfpu=neon
    # libpng only check neon when add -mfpu=neon
    LOCAL_CFLAGS += -mfpu=neon
endif

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi armeabi-v7a arm64-v8a))
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
LOCAL_CFLAGS := -O3 -DANDROID

include $(BUILD_STATIC_LIBRARY)


##################################################
###                libimage                    ###
##################################################
include $(CLEAR_VARS)

RELEASE_FLAG := true

LOCAL_MODULE    := libimage
LOCAL_SRC_FILES := \
image.c \
jpeg.c \
png.c \
gif.c \
utils.c
LOCAL_CFLAGS := -O3 -DANDROID
LOCAL_STATIC_LIBRARIES := libjpeg-turbo libpng libgif
LOCAL_LDLIBS := -lGLESv2

ifdef RELEASE_FLAG
    LOCAL_CFLAGS += -DRELEASE
else
    LOCAL_LDLIBS += -llog
endif

include $(BUILD_SHARED_LIBRARY)
