#include "Communicator.h"
#include "mpi.h"
#include <map>
#include "future"
#include "iomanip"

template<class TYPE>
void printTable(harp::ds::Table<TYPE> *table) {
    for (auto p : *table->getPartitions()) {
        std::cout << p.first << " : ";
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << std::setprecision(10) << p.second->getData()[j] << ",";
        }
        std::cout << std::endl;
    }
}

namespace harp {
    namespace com {

        template<class TYPE>
        Communicator<TYPE>::Communicator(TYPE workerId, TYPE worldSize) {
            this->workerId = workerId;
            this->worldSize = worldSize;
            this->threadPool = new ctpl::thread_pool(
                    12 * 2);//todo create only one thread until MPI concurrency issue is resolved

        }

        template<class TYPE>
        void Communicator<TYPE>::allGather(harp::ds::Table<TYPE> *table) {
            auto *partitionCounts = new int[worldSize];//no of partitions in each node
            int partitionCount = static_cast<int>(table->getPartitionCount());
            MPI_Allgather(
                    &partitionCount,
                    1,
                    MPI_INT,
                    partitionCounts,
                    1,
                    MPI_INT,
                    MPI_COMM_WORLD
            );

            int totalPartitionsInWorld = 0;
            auto recvCounts = new int[worldSize];
            auto displacements = new int[worldSize];
            for (int i = 0; i < worldSize; i++) {
                displacements[i] = totalPartitionsInWorld;
                totalPartitionsInWorld += partitionCounts[i];
                recvCounts[i] = 1 + (partitionCounts[i] * 2);
            }

            auto *worldPartitionMetaData = new int[worldSize +
                                                   (totalPartitionsInWorld * 2)];//1*worldSize(for sizes) + [{id,size}]

            long totalSize = 0;//total size of data

            auto *thisNodePartitionMetaData = new int[1 + (table->getPartitionCount() * 2)]; //totalSize + [{id,size}]
            int pIdIndex = 1;

            std::vector<TYPE> dataBuffer;
            for (const auto p : *table->getPartitions()) {
                std::copy(p.second->getData(), p.second->getData() + p.second->getSize(),
                          std::back_inserter(dataBuffer));
                totalSize += p.second->getSize();
                thisNodePartitionMetaData[pIdIndex++] = p.first;
                thisNodePartitionMetaData[pIdIndex++] = p.second->getSize();
            }

            MPI_Allgatherv(
                    thisNodePartitionMetaData,
                    static_cast<int>(1 + (table->getPartitionCount() * 2)),
                    MPI_INT,
                    worldPartitionMetaData,
                    recvCounts,
                    displacements,
                    MPI_INT,
                    MPI_COMM_WORLD
            );

            if (workerId == 0) {
                for (int i = 0; i < worldSize +
                                    (totalPartitionsInWorld * 2); i++) {
                    std::cout << worldPartitionMetaData[i] << ",";
                }

                std::cout << std::endl;
            }


            /*


            MPI_Datatype dataType = getMPIDataType<TYPE>();


            auto *data = new TYPE[totalSize * worldSize];
            MPI_Allgather(
                    &dataBuffer[0],
                    totalSize,
                    dataType,
                    data,
                    totalSize,
                    dataType,
                    MPI_COMM_WORLD
            );
            table->clear(true);

            for (auto p : *table->getPartitions()) {//keys are ordered in ascending order
                auto *data = new TYPE[p.second->getSize() * worldSize];
                MPI_Allgather(
                        p.second->getData(),
                        p.second->getSize(),
                        dataType,
                        data,
                        p.second->getSize(),
                        dataType,
                        MPI_COMM_WORLD
                );
                p.second->setData(data, p.second->getSize() * worldSize);
            }*/
        }

        template<class TYPE>
        void Communicator<TYPE>::allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation) {
            MPI_Datatype dataType = getMPIDataType<TYPE>();
            for (auto p : *table->getPartitions()) {//keys are ordered in ascending order
                auto *data = new TYPE[p.second->getSize()];
                MPI_Allreduce(
                        p.second->getData(),
                        data,
                        p.second->getSize(),
                        dataType,
                        operation,
                        MPI_COMM_WORLD
                );
                p.second->setData(data, p.second->getSize());
            }
        }

        template<class TYPE>
        void Communicator<TYPE>::broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
            //determining number of partitions to bcast
            int partitionCount;
            if (bcastWorkerId == this->workerId) {
                partitionCount = static_cast<int>(table->getPartitionCount());
            }
            MPI_Bcast(&partitionCount, 1, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);

            //broadcasting partition ids and sizes
            int partitionIds[partitionCount * 2];// [id, size]
            int index = 0;
            if (bcastWorkerId == this->workerId) {
                for (const auto p : table->getPartitions()) {
                    partitionIds[index++] = p.first;
                    partitionIds[index++] = p.second->getSize();
                }
            }
            MPI_Bcast(&partitionIds, partitionCount * 2, MPI_INT, bcastWorkerId, MPI_COMM_WORLD);

            MPI_Datatype dataType = getMPIDataType(table->getDataType());

            //now receiving partitions
            for (long i = 0; i < partitionCount * 2; i += 2) {
                int partitionId = partitionIds[i];
                int partitionSize = partitionIds[i + 1];
                if (partitionSize > 0) {
                    auto *data = createArray(table->getDataType(),
                                             partitionSize);
                    if (bcastWorkerId == this->workerId) {
                        data = table->getPartition(partitionId)->getData();
                    }
                    MPI_Bcast(data, partitionSize, dataType, bcastWorkerId, MPI_COMM_WORLD);
                    if (bcastWorkerId != this->workerId) {
                        auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data,
                                                                           partitionSize,
                                                                           table->getDataType());
                        table->addPartition(newPartition);
                    }
                }
            }
        }

        template<class CLS_TYPE>
        template<class TYPE>
        void Communicator<CLS_TYPE>::rotate(harp::ds::Table<TYPE> *table, int sendTag, int recvTag) {
            MPI_Datatype dataType = getMPIDataType<TYPE>();

            int sendTo = (this->workerId + 1) % this->worldSize;
            int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

            //exchange NUMBER OF PARTITIONS
            int numOfPartitionsToSend = static_cast<int>(table->getPartitionCount());
            int numOfPartitionsToRecv = 0;


            //std::cout << "Will send " << numOfPartitionsToSend << " partitions to " << sendTo << std::endl;

            MPI_Sendrecv(
                    &numOfPartitionsToSend, 1, MPI_INT, sendTo, sendTag,
                    &numOfPartitionsToRecv, 1, MPI_INT, receiveFrom, recvTag,
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );

            //std::cout << "Will recv " << numOfPartitionsToRecv << " from " << receiveFrom << std::endl;

            //exchange PARTITION METADATA
            int sendingMetaSize = 1 + (numOfPartitionsToSend * 2);// totalDataSize(1) + [{id, size}]
            int receivingMetaSize = 1 + (numOfPartitionsToRecv * 2);// totalDataSize(1) + [{id, size}]

            int partitionMetaToSend[sendingMetaSize];
            int partitionMetaToRecv[receivingMetaSize];

            int index = 1;
            int totalDataSize = 0;
            std::vector<TYPE> dataBuffer;//todo possible error: data buffer gets cleared immediately after returning this function

            for (const auto p : *table->getPartitions()) {
                partitionMetaToSend[index++] = p.first;
                partitionMetaToSend[index++] = p.second->getSize();
                totalDataSize += p.second->getSize();
                //todo prevent memory copying if possible
                std::copy(p.second->getData(), p.second->getData() + p.second->getSize(),
                          std::back_inserter(dataBuffer));
            }
            partitionMetaToSend[0] = totalDataSize;

            //std::cout << "Will send " << partitionMetaToSend[0] << " elements to " << sendTo << std::endl;

            MPI_Sendrecv(
                    &partitionMetaToSend, sendingMetaSize, MPI_INT, sendTo, sendTag,
                    &partitionMetaToRecv, receivingMetaSize, MPI_INT, receiveFrom, recvTag,
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );

            //std::cout << "Will recv " << partitionMetaToRecv[0] << " from " << receiveFrom << std::endl;

            //sending DATA
            //todo implement support for data arrays larger than INT_MAX
            MPI_Request dataSendRequest;
            MPI_Isend(&dataBuffer[0], totalDataSize, dataType, sendTo, sendTag, MPI_COMM_WORLD,
                      &dataSendRequest);

            auto *recvTab = new harp::ds::Table<TYPE>(table->getId());
            auto *recvBuffer = new TYPE[partitionMetaToRecv[0]];

            MPI_Recv(recvBuffer, partitionMetaToRecv[0], dataType, receiveFrom, recvTag,
                     MPI_COMM_WORLD,
                     MPI_STATUS_IGNORE);

            int copiedCount = 0;
            for (long i = 1; i < receivingMetaSize; i += 2) {
                int partitionId = partitionMetaToRecv[i];
                int partitionSize = partitionMetaToRecv[i + 1];

                auto *data = new TYPE[partitionSize];
                std::copy(recvBuffer + copiedCount, recvBuffer + copiedCount + partitionSize, data);
                copiedCount += partitionSize;

                auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                recvTab->addPartition(newPartition);
            }

            MPI_Wait(&dataSendRequest, MPI_STATUS_IGNORE);

            //delete table;
            table->swap(recvTab);
            harp::ds::util::deleteTable(recvTab, false);
        }

        template<class TYPE>
        void Communicator<TYPE>::barrier() {
            MPI_Barrier(MPI_COMM_WORLD);
        }

        template<class TYPE>
        template<class TAB_TYPE>
        void Communicator<TYPE>::asyncRotate(ds::Table<TAB_TYPE> *table, int pid) {
            auto partition = table->getPartition(pid);//take partition out
            table->removePartition(pid, false);
            auto *rotatingTable = new harp::ds::Table<TAB_TYPE>(table->getId());//create new table for rotation
            rotatingTable->addPartition(partition);

            //todo move to a singleton threadpool : count time for asyncRotate call before doing that
            int sendTo = (this->workerId + 1) % this->worldSize;
            int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

            int myRecieveTag = this->communicationTag++;
            int mySendTag = -1;

            MPI_Sendrecv(
                    &myRecieveTag, 1, MPI_INT, receiveFrom, table->getId(),
                    &mySendTag, 1, MPI_INT, sendTo, table->getId(),
                    MPI_COMM_WORLD,
                    MPI_STATUS_IGNORE
            );

            std::future<void> rotateTaskFuture;
            rotateTaskFuture = this->threadPool->push(
                    [rotatingTable, table, mySendTag, myRecieveTag, this](int id) {
                        //std::cout << "Executing rotate in thread : " << id << std::endl;
                        rotate(rotatingTable, mySendTag, myRecieveTag);
                        for (auto p:*rotatingTable->getPartitions()) {
                            table->addToPendingPartitions(p.second);//todo add locks
                        }
                    });
            this->asyncTasks.push(std::move(rotateTaskFuture));
        }

        template<class TYPE>
        void Communicator<TYPE>::wait() {
            while (!this->asyncTasks.empty()) {
                this->asyncTasks.front().get();
                this->asyncTasks.pop();
            }
        }
    }
}