cmake_minimum_required(VERSION 3.12)
project(harp_utils)

FILE(GLOB PUBLIC_HEADERS *.h)

INSTALL(FILES ${PUBLIC_HEADERS}
        DESTINATION include/util)
