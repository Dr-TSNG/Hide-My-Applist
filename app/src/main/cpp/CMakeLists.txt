cmake_minimum_required(VERSION 3.10.2)
project("hidemyapplist")

macro(SET_OPTION option value)
    set(${option} ${value} CACHE INTERNAL "" FORCE)
endmacro()

SET_OPTION(DOBBY_GENERATE_SHARED OFF)
if (NOT CMAKE_BUILD_TYPE STREQUAL "Debug")
    SET_OPTION(DOBBY_DEBUG OFF)
endif ()

add_subdirectory(external/Dobby)

target_include_directories(dobby PUBLIC external/Dobby/include)
include_directories(external/jsonxx/include/jsonxx)

add_library(hma_native_detections SHARED NativeDetections.cpp)
add_library(hma_native_hooks SHARED NativeHooks.cpp)

target_link_libraries(hma_native_hooks dobby)