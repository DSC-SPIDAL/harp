cmake_minimum_required(VERSION 3.12)

#SET(CMAKE_C_COMPILER /home/chathura/intel/compilers_and_libraries_2019.1.144/linux/bin/intel64/icc)
#SET(CMAKE_CXX_COMPILER /home/chathura/intel/compilers_and_libraries_2019.1.144/linux/bin/intel64/icpc)

project(harpc)

set(CMAKE_CXX_STANDARD 14)

list(APPEND CMAKE_CXX_FLAGS "-pthread -std=c++11 ${CMAKE_CXX_FLAGS}")

#Boost
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost 1.68.0 COMPONENTS thread REQUIRED)

#MPI
SET(MPI_C_COMPILER /opt/openmpi-4.0.0/build/bin/mpicc)
SET(MPI_CXX_COMPILER /opt/openmpi-4.0.0/build/bin/mpicxx)
find_package(MPI REQUIRED)

#DAAL
SET(DAAL_INCLUDE_PATH /home/chathura/Code/daal/include)
SET(DAAL_LIBRARIES /home/chathura/Code/daal/__release_lnx_gnu/daal/lib/intel64_lin)

include_directories(
        ${Boost_INCLUDE_DIRS}
        ${DAAL_INCLUDE_PATH}
        ${MPI_INCLUDE_PATH}
)

add_subdirectory(data_structures)
add_subdirectory(util)
add_subdirectory(worker)

add_executable(harpc kernels/daal/DaalKMeans.cpp communication/Communicator.h communication/Communicator.cpp examples/generators/DataGenerator.cpp examples/generators/DataGenerator.h kernels/kmeans/KMeans.h kernels/kmeans/KMeans.cpp kernels/math/partitions/PartitionMath.h kernels/math/partitions/PartitionMath.cpp kernels/math/table/TableMath.h kernels/math/table/TableMath.cpp kernels/math/HarpMath.h kernels/HarpKernels.h data_structures/Utils.h data_structures/Utils.cpp communication/Rotator.h communication/Rotator.cpp data_structures/PartitionAccessor.h data_structures/PartitionAccessor.cpp)
#kernels/daal/DaalKMeans.cpp
#examples/kmeans/kmeans.cpp

find_library(DAAL_CORE_LIB NAMES daal_core PATHS ${DAAL_LIBRARIES})
find_library(DAAL_THREAD_LIB NAMES daal_thread PATHS ${DAAL_LIBRARIES})
find_library(DAAL_SEQ_LIB NAMES daal_sequential PATHS ${DAAL_LIBRARIES})
#find_library(TBB_LIB NAMES tbb tbbmalloc PATHS ${TBB_LIBRARIES})

message("Daal Core Lib : " ${DAAL_CORE_LIB})
message("Daal Thread Lib :" ${DAAL_THREAD_LIB})
message("Daal Sequence Lib : " ${DAAL_SEQ_LIB})

target_link_libraries(
        harpc
        harp_data_structures
        harp_utils
        harp_worker
        ${Boost_LIBRARIES}
        ${DAAL_CORE_LIB}
        ${DAAL_THREAD_LIB}
        ${DAAL_SEQ_LIB}
)