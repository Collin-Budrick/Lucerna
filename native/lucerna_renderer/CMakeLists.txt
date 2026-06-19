cmake_minimum_required(VERSION 3.24)

project(lucerna_renderer LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(JNI REQUIRED)

add_library(lucerna_renderer SHARED
    src/lucerna_native.cpp
    src/lucerna_renderer.cpp
    src/lucerna_resource_manager.cpp
)

target_include_directories(lucerna_renderer
    PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_compile_definitions(lucerna_renderer PRIVATE LUCERNA_RENDERER_EXPORTS)

if (MSVC)
    target_compile_options(lucerna_renderer PRIVATE /W4 /permissive-)
else()
    target_compile_options(lucerna_renderer PRIVATE -Wall -Wextra -Wpedantic)
endif()
