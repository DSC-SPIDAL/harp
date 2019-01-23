cmake_minimum_required(VERSION 3.12)
project(harp_communication)

include_directories(${MPI_INCLUDE_PATH})

add_library(
        harp_communication INTERFACE
)

FILE(GLOB PUBLIC_HEADERS *.h)

INSTALL(FILES ${PUBLIC_HEADERS}
        DESTINATION include/communication)

INSTALL(
        TARGETS harp_communication
        LIBRARY DESTINATION lib
)