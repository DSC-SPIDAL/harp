cmake_minimum_required(VERSION 3.12)
project(harpc)

SET(CMAKE_C_COMPILER mpicc)
SET(CMAKE_CXX_COMPILER /opt/openmpi-3.0.0/build/bin/mpicxx)

set(CMAKE_CXX_STANDARD 14)

list(APPEND CMAKE_CXX_FLAGS "-pthread -std=c++11 ${CMAKE_CXX_FLAGS}")


if( Log4cxx_DIR )
    find_package( Log4cxx NO_MODULE )
elseif( NOT Log4cxx_FOUND )
    message(STATUS "Searching for log4cxx/logger.h")
    find_path( Log4cxx_INCLUDE_DIR log4cxx/logger.h )

    message(STATUS "Searching for libLog4cxx")
    find_library( Log4cxx_LIBRARY log4cxx )

    include( FindPackageHandleStandardArgs )
    FIND_PACKAGE_HANDLE_STANDARD_ARGS( Log4cxx Log4cxx_INCLUDE_DIR Log4cxx_LIBRARY )
    if( LOG4CXX_FOUND )
        set( Log4cxx_FOUND TRUE )
    endif()
endif()

add_executable(harpc data_structures/Partition.cpp data_structures/Partition.h data_structures/Table.h data_structures/Table.cpp worker/Worker.h worker/Worker.cpp data_structures/inculdes.h communication/Communicator.h communication/Communicator.cpp examples/kmeans/kmeans.cpp examples/generators/DataGenerator.cpp examples/generators/DataGenerator.h data_structures/DataTypes.h data_structures/DataTypes.cpp kernels/kmeans/KMeans.h kernels/kmeans/KMeans.cpp kernels/math/partitions/PartitionMath.h kernels/math/partitions/PartitionMath.cpp kernels/math/table/TableMath.h kernels/math/table/TableMath.cpp kernels/math/HarpMath.h)
#add_executable(harpc main.cpp data_structures/Partition.cpp data_structures/Partition.h data_structures/Table.h data_structures/Table.cpp worker/Worker.h worker/Worker.cpp data_structures/inculdes.h communication/Communicator.h communication/Communicator.cpp examples/generators/DataGenerator.cpp examples/generators/DataGenerator.h harp.h data_structures/DataTypes.cpp)

target_link_libraries(harpc mpi log4cxx)