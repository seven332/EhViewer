cmake_minimum_required(VERSION 3.3)

project(libpng)

include_directories(E:\\Android\\android-sdk\\ndk-bundle\\platforms\\android-14\\arch-arm\\usr\\include)

set(SOURCE_FILES
        png.c
        pngerror.c
        pngget.c
        pngmem.c
        pngpread.c
        pngread.c
        pngrio.c
        pngrtran.c
        pngrutil.c
        pngset.c
        pngtrans.c)

add_executable(libpng ${SOURCE_FILES})
