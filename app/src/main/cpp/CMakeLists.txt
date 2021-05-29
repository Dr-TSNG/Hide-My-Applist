cmake_minimum_required(VERSION 3.10.2)
project("hidemyapplist")

add_library(natives SHARED detections.cpp sigverify.cpp)
target_link_libraries(natives android log)