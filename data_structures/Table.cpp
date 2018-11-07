#include "Table.h"

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
        std::map<int, Partition<TYPE> *> Table<TYPE>::getPartitions() {
            return this->partitionMap;
        }

        template<class TYPE>
        PartitionState Table<TYPE>::addPartition(Partition<TYPE> *partition) {
            this->partitionMap.insert(std::make_pair(partition->getId(), partition));
            return COMBINED;
        }

        template<class TYPE>
        Partition<TYPE> *Table<TYPE>::getPartition(int tid) {
            return this->partitionMap.at(tid);
        }

        template<class TYPE>
        long Table<TYPE>::removePartition(int tid, bool clearMemory) {
            return this->partitionMap.erase(tid);
        }

        template<class TYPE>
        void Table<TYPE>::clear() {
            for (auto p: this->partitionMap) {
                delete p.second;
            }
            this->partitionMap.clear();
        }

        template<class TYPE>
        Table<TYPE>::~Table() {
            this->clear();
        }
    }
}