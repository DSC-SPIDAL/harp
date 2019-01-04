cmake_minimum_required(VERSION 3.12)
project(harp_utils)

add_library(
        harp_utils SHARED
        Print.h
        ThreadPool.h
        Timing.h
        Timing.cpp
)

target_link_libraries(
        harp_utils
)