#ifndef HARPC_COMMUNICATOR_H
#define HARPC_COMMUNICATOR_H

#include <iostream>
#include "../util/timing.h"
#include "mpi.h"
#include "../data_structures/DataStructures.h"
#include "../util/ThreadPool.h"
#include "future"

//todo doing implementation in header file due to templates problem
namespace harp {
    namespace com {
        class Communicator {
        private:
            int workerId;
            int worldSize;

            int comThreads;

            //Using separate task queues for each thread pool to prevent unnecessary locking
            //----------------------------------//
            ctpl::thread_pool *threadPool;
            std::queue<std::future<void>> asyncTasks;
            std::mutex asyncTasksMutex;
            //----------------------------------//

            int communicationTag = 0;

        public:
            Communicator(int workerId, int worldSize, int comThreads = 1) {
                this->workerId = workerId;
                this->worldSize = worldSize;
                this->threadPool = new ctpl::thread_pool(comThreads);
            }

            void barrier() {
                MPI_Barrier(MPI_COMM_WORLD);
            }

            template<class TYPE>
            void allGatherDeprecated(harp::ds::Table<TYPE> *table) {
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
                                                       (totalPartitionsInWorld *
                                                        2)];//1*worldSize(for sizes) + [{id,size}]

                long totalSize = 0;//total size of data

                auto *thisNodePartitionMetaData = new int[1 +
                                                          (table->getPartitionCount() * 2)]; //totalSize + [{id,size}]
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
            }

            template <class TYPE>
            void allGather(harp::ds::Table<TYPE>* &table)
            {
                // auto *partitionCounts = new int[worldSize]; //no of partitions in each node
                std::unique_ptr<int[]> partitionCounts(new int[worldSize]); //no of partitions in each node
                int partitionCount = static_cast<int>(table->getPartitionCount());
                MPI_Allgather(
                    &partitionCount,
                    1,
                    MPI_INT,
                    (void*)(partitionCounts.get()),
                    1,
                    MPI_INT,
                    MPI_COMM_WORLD);

                //debug
                // std::cout<<"finish allgather step 1"<<std::endl;

                int totalPartitionsInWorld = 0;
                // int *recvCounts = new int[worldSize];
                // int *displacements = new int[worldSize];
                std::unique_ptr<int[]> recvCounts(new int[worldSize]);
                std::unique_ptr<int[]> displacements(new int[worldSize]);
                displacements[0] = 0;
                for (int i = 0; i < worldSize; i++)
                {
                    totalPartitionsInWorld += partitionCounts[i];
                    recvCounts[i] = 1 + (partitionCounts[i] * 2); //size + [{id,size}]
                    if (i != 0)
                    {
                        displacements[i] = displacements[i - 1] + recvCounts[i];
                    }
                }

                int worldPartitionMetaDataSize = worldSize +
                                                 (totalPartitionsInWorld *
                                                  2); //1*worldSize(for sizes) + [{id,size}]

                // long *worldPartitionMetaData = new long[worldPartitionMetaDataSize];
                std::unique_ptr<long[]> worldPartitionMetaData(new long[worldPartitionMetaDataSize]);

                long thisNodeTotalDataSize = 0; //total size of data

                int thisNodeMetaSendSize = static_cast<int>(1 +
                                                            (table->getPartitionCount() * 2)); //totalSize + [{id,size}]
                //debug
                // std::cout<<"finish allgather step 2"<<std::endl;

                // long *thisNodePartitionMetaData = new long[thisNodeMetaSendSize];
                std::unique_ptr<long[]> thisNodePartitionMetaData(new long[thisNodeMetaSendSize]);

                int pIdIndex = 1;

                std::vector<TYPE> thisNodeDataBuffer;
                for (const auto p : *table->getPartitions())
                {
                    std::copy(p.second->getData(), p.second->getData() + p.second->getSize(),
                              std::back_inserter(thisNodeDataBuffer));
                    thisNodeTotalDataSize += p.second->getSize();
                    thisNodePartitionMetaData[pIdIndex++] = p.first;
                    thisNodePartitionMetaData[pIdIndex++] = p.second->getSize();
                }
                thisNodePartitionMetaData[0] = thisNodeTotalDataSize;

                //                std::cout << "Node " << workerId << " total size : " << thisNodeTotalDataSize << std::endl;
                //
                //                harp::util::print::printArray(thisNodePartitionMetaData, thisNodeMetaSendSize);
                //debug
                // std::cout<<"finish allgather step 3"<<std::endl;

                MPI_Allgatherv(
                    thisNodePartitionMetaData.get(),
                    thisNodeMetaSendSize,
                    MPI_LONG,
                    worldPartitionMetaData.get(),
                    recvCounts.get(),
                    displacements.get(),
                    MPI_LONG,
                    MPI_COMM_WORLD);

                // done meta data exchange
                //                if (workerId == 0) {
                //                    harp::util::print::printArray(worldPartitionMetaData, worldPartitionMetaDataSize);
                //                }
                //debug
                // std::cout<<"finish allgather step 4"<<std::endl;

                long totalDataSize = worldPartitionMetaData[0];
                int tempLastIndex = 0;
                recvCounts[0] = static_cast<int>(worldPartitionMetaData[0]); //todo cast?
                displacements[0] = 0;
                for (int i = 1; i < worldSize; i++)
                {
                    int thisIndex = tempLastIndex + (partitionCounts[i - 1] * 2) + 1;
                    displacements[i] = static_cast<int>(totalDataSize);
                    totalDataSize += worldPartitionMetaData[thisIndex];
                    recvCounts[i] = static_cast<int>(worldPartitionMetaData[thisIndex]);
                    tempLastIndex = thisIndex;
                }

                MPI_Datatype dataType = getMPIDataType<TYPE>();

                //debug
                // std::cout<<"finish allgather step 5"<<std::endl;

                // auto *worldDataBuffer = new TYPE[totalDataSize];
                std::unique_ptr<TYPE[]> worldDataBuffer(new TYPE[totalDataSize]);
                MPI_Allgatherv(
                    &thisNodeDataBuffer[0],
                    static_cast<int>(thisNodeTotalDataSize),
                    dataType,
                    worldDataBuffer.get(),
                    recvCounts.get(),
                    displacements.get(),
                    dataType,
                    MPI_COMM_WORLD);
                
                //                if (workerId == 0) {
                //                    harp::util::print::printArray(worldDataBuffer, totalDataSize);
                //                }

                //now we have all data
                auto *recvTab = new harp::ds::Table<TYPE>(table->getId());
                int lastIndex = 0;
                int metaIndex = 0;
                for (int i = 0; i < worldSize; i++)
                {
                    int noOfPartitions = partitionCounts[i];
                    metaIndex++;
                    for (int j = 0; j < noOfPartitions; j++)
                    {
                        int partitionId = static_cast<int>(worldPartitionMetaData[metaIndex++]);
                        int partitionSize = static_cast<int>(worldPartitionMetaData[metaIndex++]);
                        auto *partitionData = new TYPE[partitionSize];
                        auto *partition = new harp::ds::Partition<TYPE>(partitionId, partitionData, partitionSize);
                        std::copy(worldDataBuffer.get() + lastIndex, worldDataBuffer.get() + lastIndex + partitionSize,
                                  partitionData);
                        //                        if (workerId == 0) {
                        //                            std::cout << "pSize:" << partitionSize << std::endl;
                        //                            harp::util::print::printPartition(partition);
                        //                        }
                        lastIndex += partitionSize;
                        recvTab->addPartition(partition);
                    }
                }
                harp::ds::Table<TYPE>* tableSwap = table;
                table = recvTab; 
                recvTab = nullptr;
                //debug
                // std::cout<<"finish allgather step 6"<<std::endl;
                harp::ds::util::deleteTable(tableSwap, true);
            }

            template<class TYPE>
            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation) {
                MPI_Datatype dataType = getMPIDataType<TYPE>();
                MPI_Request requests[table->getPartitionCount()];
                std::vector<TYPE *> dataArrays;
                int index = 0;
                for (auto p : *table->getPartitions()) {//keys are ordered in ascending order
                    auto *data = new TYPE[p.second->getSize()];
                    MPI_Iallreduce(
                            p.second->getData(),
                            data,
                            p.second->getSize(),
                            dataType,
                            operation,
                            MPI_COMM_WORLD,
                            &requests[index++]
                    );
                    dataArrays.push_back(data);
                }

                MPI_Waitall(static_cast<int>(table->getPartitionCount()), requests, MPI_STATUS_IGNORE);

                index = 0;
                for (auto p : *table->getPartitions()) {
                    p.second->setData(dataArrays[index++], p.second->getSize());
                }
            }

            template<class TYPE>
            void allReduce(harp::ds::Table<TYPE> *table, MPI_Op operation, bool inPlace) {

                MPI_Datatype dataType = getMPIDataType<TYPE>();
                MPI_Request requests[table->getPartitionCount()];
                std::vector<TYPE *> dataArrays;
                int index = 0;
                for (auto p : *table->getPartitions()) {//keys are ordered in ascending order
                    auto *data = new TYPE[p.second->getSize()];
                    MPI_Iallreduce(
                            p.second->getData(),
                            data,
                            p.second->getSize(),
                            dataType,
                            operation,
                            MPI_COMM_WORLD,
                            &requests[index++]
                    );
                    dataArrays.push_back(data);
                }

                MPI_Waitall(static_cast<int>(table->getPartitionCount()), requests, MPI_STATUS_IGNORE);

                if (inPlace)
                {
                    // use the memory space of the original table, delete the 
                    // data buf
                    index = 0;
                    for (auto p : *table->getPartitions())
                    {
                        std::copy(dataArrays[index], dataArrays[index]+p.second->getSize(), 
                        p.second->getData());
                        delete[] dataArrays[index];
                        dataArrays[index] = nullptr;
                        index++;
                    }

                }else
                {
                    // set the created new data to the table
                    /* code */
                    index = 0;
                    for (auto p : *table->getPartitions())
                    {
                        p.second->setData(dataArrays[index++], p.second->getSize());
                    }
                }
            }

            template<class TYPE>
            void broadcast(harp::ds::Table<TYPE> *table, int bcastWorkerId) {
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

            /**
             * @brief fix the memory leak issue
             * 
             * @tparam TYPE 
             * @param table 
             * @param sendTag 
             * @param recvTag 
             */
            template<class TYPE>
            void rotate(harp::ds::Table<TYPE> *table, int sendTag = 1, int recvTag = 1) {
                MPI_Datatype dataType = getMPIDataType<TYPE>();

                int sendTo = (this->workerId + 1) % this->worldSize;
                int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

                //exchange NUMBER OF PARTITIONS
                int numOfPartitionsToSend = static_cast<int>(table->getPartitionCount());
                int numOfPartitionsToRecv = 0;

                if (table->getId() == 0)
                    std::cout << "Will send " << numOfPartitionsToSend << " partitions to " << sendTo << std::endl;

                MPI_Sendrecv(
                        &numOfPartitionsToSend, 1, MPI_INT, sendTo, sendTag,
                        &numOfPartitionsToRecv, 1, MPI_INT, receiveFrom, recvTag,
                        MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE
                );

                if (table->getId() == 0)
                    std::cout << "Will recv " << numOfPartitionsToRecv << " from " << receiveFrom << std::endl;

                //exchange PARTITION METADATA
                int sendingMetaSize = 1 + (numOfPartitionsToSend * 2);// totalDataSize(1) + [{id, size}]
                int receivingMetaSize = 1 + (numOfPartitionsToRecv * 2);// totalDataSize(1) + [{id, size}]

                // contains partition id and the size (of bytes)
                std::unique_ptr<int[]> partitionMetaToSend(new int[sendingMetaSize]);
                // int partitionMetaToSend[sendingMetaSize];

                // contains partition id and the size (of bytes)
                std::unique_ptr<int[]> partitionMetaToRecv(new int[receivingMetaSize]);
                // int partitionMetaToRecv[receivingMetaSize];

                // index 0 reserves for total size
                int index = 1;
                int totalDataSize = 0;
                // pack all of the partitions to the buffer
                for (const auto p : *table->getPartitions()) {
                    partitionMetaToSend[index++] = p.first;
                    partitionMetaToSend[index++] = p.second->getSize();
                    totalDataSize += p.second->getSize();
                }
                partitionMetaToSend[0] = totalDataSize;

                if (table->getId() == 0)
                    std::cout << "Total Data size "<< partitionMetaToSend[0]<< std::endl;

                // check this data buffer issue
                // std::vector<TYPE> dataBuffer;//todo possible error: data buffer gets cleared immediately after returning this function
                // copy the data
                std::unique_ptr<TYPE[]> dataBuffer(new TYPE[totalDataSize]);
                int parSizeStart = 0;
                for (const auto p : *table->getPartitions()) {
                    //todo prevent memory copying if possible
                    std::copy(p.second->getData(), p.second->getData() + p.second->getSize(),
                              dataBuffer.get() + parSizeStart);

                    parSizeStart += p.second->getSize();
                } 

                if (table->getId() == 0)
                    std::cout << "Will send " << partitionMetaToSend[0] << " elements to " << sendTo << std::endl;

                MPI_Sendrecv(
                        partitionMetaToSend.get(), sendingMetaSize, MPI_INT, sendTo, sendTag,
                        partitionMetaToRecv.get(), receivingMetaSize, MPI_INT, receiveFrom, recvTag,
                        MPI_COMM_WORLD,
                        MPI_STATUS_IGNORE
                );

                if (table->getId() == 0)
                    std::cout << "Will recv " << partitionMetaToRecv[0] << " from " << receiveFrom << std::endl;

                //sending DATA
                //todo implement support for data arrays larger than INT_MAX
                MPI_Request dataSendRequest;
                MPI_Isend(dataBuffer.get(), totalDataSize, dataType, sendTo, sendTag, MPI_COMM_WORLD,
                          &dataSendRequest);

                // why use two tables ???
                // auto *recvTab = new harp::ds::Table<TYPE>(table->getId());

                std::unique_ptr<TYPE[]> recvBuffer(new TYPE[partitionMetaToRecv[0]]);
                // auto *recvBuffer = new TYPE[partitionMetaToRecv[0]];

                MPI_Recv(recvBuffer.get(), partitionMetaToRecv[0], dataType, receiveFrom, recvTag,
                         MPI_COMM_WORLD,
                         MPI_STATUS_IGNORE);

                // unpack the receiving data
                // clean the table 
                table->clear(false);
                int copiedCount = 0;
                for (long i = 1; i < receivingMetaSize; i += 2) {
                    int partitionId = partitionMetaToRecv[i];
                    int partitionSize = partitionMetaToRecv[i + 1];

                    auto *data = new TYPE[partitionSize];
                    std::copy(recvBuffer.get() + copiedCount, recvBuffer.get() + copiedCount + partitionSize, data);
                    copiedCount += partitionSize;

                    auto *newPartition = new harp::ds::Partition<TYPE>(partitionId, data, partitionSize);
                    // recvTab->addPartition(newPartition);
                    table->addPartition(newPartition);
                }

                // here to prevent the destroy of databuffer
                MPI_Wait(&dataSendRequest, MPI_STATUS_IGNORE);

                //delete table;
                // table->swap(recvTab);
                // harp::ds::util::deleteTable(recvTab, false);
            }

            template<class TYPE>
            void asyncRotate(harp::ds::Table<TYPE> *table, int pid) {
                util::timing::record(11);
                auto partition = table->getPartition(pid);//take partition out
                table->removePartition(pid, false);//this happens in the same thread all the time
                auto *rotatingTable = new harp::ds::Table<TYPE>(table->getId());//create new table for rotation
                rotatingTable->addPartition(partition);

                int myReceiveTag = table->getId();
                int mySendTag = table->getId();

                if (this->threadPool->size() > 1) {// for just one thread, communication happens one after another.
                    int sendTo = (this->workerId + 1) % this->worldSize;
                    int receiveFrom = (this->workerId + this->worldSize - 1) % this->worldSize;

                    myReceiveTag = this->communicationTag++;
                    mySendTag = -1;

                    MPI_Sendrecv(
                            &myReceiveTag, 1, MPI_INT, receiveFrom, table->getId(),
                            &mySendTag, 1, MPI_INT, sendTo, table->getId(),
                            MPI_COMM_WORLD,
                            MPI_STATUS_IGNORE
                    );
                }

                std::future<void> rotateTaskFuture = this->threadPool->push(
                        [rotatingTable, table, mySendTag, myReceiveTag, this](int id) {
                            //std::cout << "Executing rotate in thread : " << id << std::endl;
                            rotate(rotatingTable, mySendTag, myReceiveTag);
                            if (this->threadPool->size() > 1) {
                                //for a single thread, there is no need in locking
                                this->asyncTasksMutex.lock();
                                for (auto p:*rotatingTable->getPartitions()) {
                                    table->addToPendingPartitions(p.second);
                                }
                                this->asyncTasksMutex.unlock();
                            } else {
                                for (auto p:*rotatingTable->getPartitions()) {
                                    table->addToPendingPartitions(p.second);
                                }
                            }
                        });

                //assuming this happens always on the same thread
                this->asyncTasks.push(std::move(rotateTaskFuture));

                util::timing::record(12);
                util::timing::diff(11, 12, true);
            }

            int getWorkerId() {
                return this->workerId;
            }

            int getWorldSize() {
                return this->worldSize;
            }

            void wait() {
                //wait for async task thread pool queue
                while (!this->asyncTasks.empty()) {
                    this->asyncTasks.front().get();
                    this->asyncTasks.pop();
                }
            }
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
