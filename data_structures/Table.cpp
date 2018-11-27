#include "Table.h"
#include "iostream"

namespace harp {
    namespace ds {
        template<class TYPE>
        Table<TYPE>::Table(int id) {
            this->id = id;
        }

        template<class TYPE>
        int Table<TYPE>::getId() {
            return this->id;
        }

        template<class TYPE>
        long Table<TYPE>::getPartitionCount() {
            return this->partitionMap.size();
        }

        template<class TYPE>
        std::map<int, Partition<TYPE> *> *Table<TYPE>::getPartitions(bool blockForAvailability) {
            this->pushPendingPartitions();
            if (blockForAvailability) {
                while (this->partitionMap.empty()) {
                    this->pushPendingPartitions();
                }
            }
            return &this->partitionMap;
        }

        template<class TYPE>
        PartitionState Table<TYPE>::addPartition(Partition<TYPE> *partition) {
            this->partitionMap.insert(std::make_pair(partition->getId(), partition));
            this->orderedPartitions.push_back(partition->getId());
            return COMBINED;
        }

        template<class TYPE>
        Partition<TYPE> *Table<TYPE>::getPartition(int pid) {
            return this->partitionMap.at(pid);
        }

        template<class TYPE>
        long Table<TYPE>::removePartition(int pid, bool clearMemory) {
            if (this->partitionMap.count(pid) > 0) {
                if (clearMemory) {
                    delete this->getPartition(pid);
                }
                return this->partitionMap.erase(pid);//remove from map
            } else {
                return 0;
            }
        }

        template<class TYPE>
        void Table<TYPE>::clear(bool clearPartitions) {
            if (clearPartitions) {
                for (auto p: this->partitionMap) {
                    delete p.second;
                }
            }
            this->partitionMap.clear();
        }

        template<class TYPE>
        Table<TYPE>::~Table() {
            this->clear();
        }

        template<class TYPE>
        void Table<TYPE>::replaceParition(int pid, Partition<TYPE> *partition) {
            this->removePartition(pid);
            this->addPartition(partition);
        }

//        template<class TYPE>
//        const std::unordered_set<int> *Table<TYPE>::getPartitionKeySet(bool blockForAvailability) {
//            while (!this->deletedKeys.empty()) {
//                this->partitionKeys.erase(this->deletedKeys.front());
//                this->deletedKeys.pop();
//            }
//
//            if (blockForAvailability) {
//                std::unique_lock<std::mutex> lock(this->pendingPartitionsMutex);
//                while (this->partitionMap.empty()) {
//                    this->availability.wait(lock);
//                }
//            }
//
//            return &this->partitionKeys;
//        }

        template<class TYPE>
        void Table<TYPE>::swap(Table<TYPE> *table) {
            this->partitionMap = table->partitionMap;
            this->orderedPartitions = table->orderedPartitions;
            this->iteratingIndex = 0;
        }

        template<class TYPE>
        void Table<TYPE>::addToPendingPartitions(Partition<TYPE> *partition) {
            //this->pendingPartitionsMutex.lock();
            this->pendingPartitions.push(partition);
            //this->pendingPartitionsMutex.unlock();
        }

        template<class TYPE>
        Partition<TYPE> *Table<TYPE>::nextPartition() {
            this->pushPendingPartitions();
            if (this->iteratingIndex < this->orderedPartitions.size()) {
                int index = this->orderedPartitions[this->iteratingIndex++];
                return this->partitionMap.at(index);
            }
            return nullptr;
        }

        template<class TYPE>
        bool Table<TYPE>::hasNext(bool blockForAvailability) {
            this->pushPendingPartitions();
            if (blockForAvailability) {
                while (this->partitionMap.empty()) {//todo remove busy waiting
                    this->pushPendingPartitions();
                }
            }
            return this->iteratingIndex < this->orderedPartitions.size();
        }

        template<class TYPE>
        void Table<TYPE>::resetIterator() {
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

        template<class TYPE>
        void Table<TYPE>::pushPendingPartitions() {
            //this->pendingPartitionsMutex.lock();
            while (!this->pendingPartitions.empty()) {
                this->addPartition(this->pendingPartitions.front());
                this->pendingPartitions.pop();
            }
            //this->pendingPartitionsMutex.unlock();
        }

        template<class TYPE>
        Table<TYPE>::Table(const Table &p) {
            std::cout << "Copy table called" << std::endl;
        }
    }
}