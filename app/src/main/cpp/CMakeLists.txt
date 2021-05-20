cmake_minimum_required(VERSION 3.10.2)
project("hidemyapplist")

add_library(native_detections SHARED native_detections.c)
target_link_libraries(native_detections android log)