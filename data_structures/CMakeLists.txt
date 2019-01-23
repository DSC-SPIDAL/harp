cmake_minimum_required(VERSION 3.12)
project(harp_data_structures)

include_directories(${MPI_INCLUDE_PATH})

add_library(
        harp_data_structures SHARED
        DataTypes.h DataTypes.cpp
        DataStructures.h
        Partition.h
        Partition.cpp
        PartitionAccessor.h
        PartitionAccessor.cpp
        Table.h
        Table.cpp
        Utils.h
        Utils.cpp
)

target_link_libraries(
        harp_data_structures
        ${MPI_C_LIBRARIES}
)

FILE(GLOB PUBLIC_HEADERS *.h)

INSTALL(FILES ${PUBLIC_HEADERS}
        DESTINATION include/data_structures)

INSTALL(
        TARGETS harp_data_structures
        LIBRARY DESTINATION lib
)