cmake_minimum_required(VERSION 3.3)

project(libgif)

include_directories(E:\\Android\\android-sdk\\ndk-bundle\\platforms\\android-14\\arch-arm\\usr\\include)

set(SOURCE_FILES
        dgif_lib.c
        egif_lib.c
        gif_hash.c
        gifalloc.c
        openbsd-reallocarray.c)

add_executable(libgif ${SOURCE_FILES})
