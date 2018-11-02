cmake_minimum_required(VERSION 3.12)
project(harpc)

SET(CMAKE_C_COMPILER mpicc)
SET(CMAKE_CXX_COMPILER mpicxx)

set(CMAKE_CXX_STANDARD 14)

add_executable(harpc main.cpp data_structures/Partition.cpp data_structures/Partition.h data_structures/Table.h data_structures/Table.cpp worker/Worker.h worker/Worker.cpp data_structures/inculdes.h communication/Communicator.h communication/Communicator.cpp)

target_link_libraries(harpc mpi)