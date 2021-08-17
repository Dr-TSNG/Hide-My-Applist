cmake_minimum_required(VERSION 3.18.1)
project("hidemyapplist")

include_directories(external/linux_syscall_support)

add_library(natives SHARED magic.cpp)
target_link_libraries(natives android log)