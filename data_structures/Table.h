#ifndef HARPC_TABLE_H
#define HARPC_TABLE_H

#include <map>
#include "vector"
#include <unordered_set>
#include <queue>
#include "Partition.h"
#include "DataTypes.h"
#include "mpi.h"
#include <condition_variable>
#include "mutex"

namespace harp {
    namespace ds {
        template<class TYPE>
        class Table {
        private:
            std::map<int, Partition<TYPE> *> partitionMap;


            unsigned long iteratingIndex = 0;

            //this holds partitions received from asynchronous communication
            std::queue<Partition<TYPE> *> pendingPartitions;
            std::mutex pendingPartitionsMutex;

            int id{};

            void pushPendingPartitions() {
                //this->pendingPartitionsMutex.lock();
                while (!this->pendingPartitions.empty()) {
                    this->addPartition(this->pendingPartitions.front());
                    this->pendingPartitions.pop();
                }
                //this->pendingPartitionsMutex.unlock();
            }

        public:
            std::vector<int> orderedPartitions;

            Table(int id) {
                this->id = id;
            }//todo add combiner

            Table(const Table &p) {
                std::cout << "Copy table called" << std::endl;
            }

            ~Table() {
                this->clear();
            }

            int getId() {
                return this->id;
            }

            long getPartitionCount() {
                return this->partitionMap.size();
            }

            //const std::unordered_set<int> *getPartitionKeySet(bool blockForAvailability = false);

            std::map<int, Partition<TYPE> *> *getPartitions(bool blockForAvailability = false) {
                this->pushPendingPartitions();
                if (blockForAvailability) {
                    while (this->partitionMap.empty()) {
                        this->pushPendingPartitions();
                    }
                }
                return &this->partitionMap;
            }

            Partition<TYPE> *nextPartition() {
                this->pushPendingPartitions();
                if (this->iteratingIndex < this->orderedPartitions.size()) {
                    int index = this->orderedPartitions[this->iteratingIndex++];
                    return this->partitionMap.at(index);
                }
                return nullptr;
            }

            bool hasNext(bool blockForAvailability = false) {
                this->pushPendingPartitions();
                if (blockForAvailability) {
                    while (this->partitionMap.empty()) {//todo remove busy waiting
                        this->pushPendingPartitions();
                    }
                }
                return this->iteratingIndex < this->orderedPartitions.size();
            }

            void resetIndex() {
                this->iteratingIndex = 0;
            }

            void resetIterator() {
                int markedCount = 0;
                //iterating in reverse
                for (auto i = this->orderedPartitions.crbegin(); i != this->orderedPartitions.crend();) {
                    if (this->partitionMap.count(*i) == 0 || markedCount == this->partitionMap.size()) {
                        i = decltype(i){this->orderedPartitions.erase(std::next(i).base())};
                    } else {
                        i++;
                        markedCount++;
                    }
                }
                this->iteratingIndex = 0;

                this->pushPendingPartitions();
            }

            PartitionState addPartition(Partition<TYPE> *partition) {
                this->partitionMap.insert(std::make_pair(partition->getId(), partition));
                this->orderedPartitions.push_back(partition->getId());
                return COMBINED;
            }

            Partition<TYPE> *getPartition(int pid) {
                return this->partitionMap.at(pid);
            }

            long removePartition(int pid, bool clearMemory = true) {
                if (this->partitionMap.count(pid) > 0) {
                    if (clearMemory) {
                        delete this->getPartition(pid);
                    }
                    return this->partitionMap.erase(pid);//remove from map
                } else {
                    return 0;
                }
            }

            void replaceParition(int pid, Partition<TYPE> *partition) {
                this->removePartition(pid);
                this->addPartition(partition);
            }

            void clear(bool clearPartitions = false) {
                if (clearPartitions) {
                    for (auto p: this->partitionMap) {
                        delete p.second;
                    }
                }
                this->partitionMap.clear();
            }

            void swap(Table<TYPE> *table) {
                this->partitionMap = table->partitionMap;
                this->orderedPartitions = table->orderedPartitions;
                this->iteratingIndex = 0;
            }

            void addToPendingPartitions(Partition<TYPE> *partition) {
                //this->pendingPartitionsMutex.lock();
                this->pendingPartitions.push(partition);
                //this->pendingPartitionsMutex.unlock();
            }
        };
    }
}

#endif //HARPC_TABLE_H
