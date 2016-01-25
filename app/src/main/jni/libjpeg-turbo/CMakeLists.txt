cmake_minimum_required(VERSION 3.3)

project(jpeg-turbo)
include_directories(E:\\Android\\android-sdk\\ndk-bundle\\platforms\\android-14\\arch-arm\\usr\\include)
set(SOURCE_FILES jaricom.c
jcomapi.c
jdapimin.c
jdapistd.c
jdarith.c
jdatasrc.c
jdatasrc-tj.c
jdcoefct.c
jdcolor.c
jddctmgr.c
jdhuff.c
jdinput.c
jdmainct.c
jdmarker.c
jdmaster.c
jdmerge.c
jdphuff.c
jdpostct.c
jdsample.c
jdtrans.c
jerror.c
jfdctflt.c
jfdctfst.c
jfdctint.c
jidctflt.c
jidctfst.c
jidctint.c
jidctred.c
jmemmgr.c
jmemnobs.c
jquant1.c
jquant2.c
jutils.c
jsimd_none.c
simd/jsimd_arm.c
simd/jsimd_arm64.c)

add_executable(jpeg-turbo ${SOURCE_FILES})
