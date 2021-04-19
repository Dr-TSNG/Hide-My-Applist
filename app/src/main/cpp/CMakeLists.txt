cmake_minimum_required(VERSION 3.10.2)

project("hidemyapplist")

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    SET_OPTION(DOBBY_DEBUG OFF)
endif ()

add_subdirectory(Dobby)

target_include_directories(dobby PUBLIC Dobby/include)

add_library(native_detections SHARED NativeDetections.cpp)
add_library(native_hooks SHARED NativeHooks.cpp)

target_link_libraries(native_hooks dobby)