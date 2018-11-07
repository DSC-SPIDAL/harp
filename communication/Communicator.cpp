#include "Communicator.h"
#include "mpi.h"
#include <map>

namespace harp {
    namespace com {

        Communicator::Communicator(int workerId, int worldSize) {
            this->workerId = workerId;
            this->worldSize = worldSize;
        }

//        void Communicator::barrier() {
//            MPI_Barrier(MPI_COMM_WORLD);
//        }

//        template<class TYPE>
//        void Communicator::allGather(harp::ds::Table<TYPE> *table) {
//
//        }

//        template<class TYPE>
//        void Communicator::broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
//            //todo recheck int, double confusion
//
//            //determining number of partitions to bcast
//            int partitionCount;
//            if (bcastWorkerId == this->workerId) {
//                partitionCount = static_cast<int>(table->getPartitionCount());
//            }
//            MPI_Bcast(&partitionCount, 1, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);
//
//            //broadcasting partition ids and sizes
//            long partitionIds[partitionCount * 2];// [id, size]
//            int index = 0;
//            if (bcastWorkerId == this->workerId) {
//                for (const auto p : table->getPartitions()) {
//                    partitionIds[index++] = p.first;
//                    partitionIds[index++] = p.second->getSize();
//                }
//            }
//            MPI_Bcast(&partitionIds, partitionCount * 2, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);
//
//            MPI_Datatype datatype;//todo determine data type.. currently assuming all int
//
//            //now receiving partitions
//            for (long i = 0; i < partitionCount; i += 2) {
//                int partitionId = static_cast<int>(partitionIds[i]);
//                long partitionSize = partitionIds[i + 1];
//                TYPE *data = (TYPE *) malloc(sizeof(TYPE) * partitionSize);
//                if (bcastWorkerId == this->workerId) {
//                    data = table->getPartition(partitionId)->getData();
//                }
//                MPI_Bcast(data, partitionSize, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);
//                if (bcastWorkerId != this->workerId) {
//                    harp::ds::Partition<TYPE> partition(partitionId, data, partitionSize);
//                    table->addPartition(&partition);
//                }
//            }
//
//
//            printf("%d %li %zu\n", partitionCount, partitionIds[partitionCount - 1], sizeof(typeid(int)));
//        }
//
//        template<class TYPE>
//        void Communicator::rotate(ds::Table<TYPE> *table, int bcastWorkerId) {
//
//        }
    }
}