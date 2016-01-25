cmake_minimum_required(VERSION 3.3)

project(image)

include_directories(E:\\Android\\android-sdk\\ndk-bundle\\platforms\\android-14\\arch-arm\\usr\\include)
include_directories(..\\libjpeg-turbo)
include_directories(..\\libpng)
include_directories(..\\giflib)
include_directories(..\\stream)

set(SOURCE_FILES
        image.c
        image_jpeg.c
        image_png.c
        image_gif.c
        image_utils.c
        java_wrapper.c)

add_executable(image ${SOURCE_FILES})
