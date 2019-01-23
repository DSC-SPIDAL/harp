cmake_minimum_required(VERSION 3.12)
project(harp_worker)

add_library(
        harp_worker INTERFACE
)

include_directories(
        harp_worker
        ${MPI_INCLUDE_PATH}
)

FILE(GLOB PUBLIC_HEADERS *.h)

INSTALL(FILES ${PUBLIC_HEADERS}
        DESTINATION include/worker)

INSTALL(
        TARGETS harp_worker
        LIBRARY DESTINATION lib
)