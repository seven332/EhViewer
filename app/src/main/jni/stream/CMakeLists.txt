cmake_minimum_required(VERSION 3.3)

project(stream)

include_directories(E:\\Android\\android-sdk\\ndk-bundle\\platforms\\android-14\\arch-arm\\usr\\include)

set(SOURCE_FILES
        input_stream.c
        input_stream_pipe.c
        output_stream.c
        output_stream_pipe.c
        patch_head_input_stream.c)

add_executable(stream ${SOURCE_FILES})
