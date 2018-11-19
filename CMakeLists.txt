cmake_minimum_required(VERSION 3.12)
project(harpc)

SET(CMAKE_C_COMPILER mpicc)
SET(CMAKE_CXX_COMPILER /opt/openmpi-3.0.0/build/bin/mpicxx)

set(CMAKE_CXX_STANDARD 14)

list(APPEND CMAKE_CXX_FLAGS "-pthread -std=c++11 ${CMAKE_CXX_FLAGS}")

#Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost 1.68.0 COMPONENTS thread)

include_directories(${Boost_INCLUDE_DIRS})
add_executable(harpc data_structures/Partition.cpp data_structures/Partition.h data_structures/Table.h data_structures/Table.cpp worker/Worker.h worker/Worker.cpp data_structures/inculdes.h communication/Communicator.h communication/Communicator.cpp examples/kmeans/kmeans.cpp examples/generators/DataGenerator.cpp examples/generators/DataGenerator.h data_structures/DataTypes.h data_structures/DataTypes.cpp kernels/kmeans/KMeans.h kernels/kmeans/KMeans.cpp kernels/math/partitions/PartitionMath.h kernels/math/partitions/PartitionMath.cpp kernels/math/table/TableMath.h kernels/math/table/TableMath.cpp kernels/math/HarpMath.h kernels/HarpKernels.h data_structures/Utils.h data_structures/Utils.cpp communication/Rotator.h communication/Rotator.cpp data_structures/PartitionAccessor.h data_structures/PartitionAccessor.cpp)
#add_executable(harpc main.cpp data_structures/Partition.cpp data_structures/Partition.h data_structures/Table.h data_structures/Table.cpp worker/Worker.h worker/Worker.cpp data_structures/inculdes.h communication/Communicator.h communication/Communicator.cpp examples/generators/DataGenerator.cpp examples/generators/DataGenerator.h harp.h data_structures/DataTypes.cpp)

target_link_libraries(harpc mpi ${Boost_LIBRARIES})