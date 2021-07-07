cmake_minimum_required(VERSION 3.10.2)
project("hidemyapplist")

include_directories(external/linux_syscall_support)

add_library(natives SHARED detections.cpp)
target_link_libraries(natives android log)