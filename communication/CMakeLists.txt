cmake_minimum_required(VERSION 3.12)
project(harp_communication)

include_directories(${MPI_INCLUDE_PATH})

add_library(
        harp_communication SHARED
        Communicator.h
        Communicator.cpp
        tcp/MessageAcceptor.cpp
        tcp/Server.cpp
)

target_link_libraries(
        harp_communication
        ${MPI_C_LIBRARIES}
)

FILE(GLOB PUBLIC_HEADERS *.h)

INSTALL(FILES ${PUBLIC_HEADERS}
        DESTINATION include/communication)

INSTALL(
        TARGETS harp_communication
        LIBRARY DESTINATION lib
)