#include "Table.h"

namespace harp {
    namespace ds {
        template<class SIMPLE>
        Table<SIMPLE>::Table(int id) {
            this->id = id;
        }

        template<class SIMPLE>
        int Table<SIMPLE>::getId() {
            return this->id;
        }

        template<class SIMPLE>
        long Table<SIMPLE>::getPartitionCount() {
            return this->partitionMap.size();
        }

        template<class SIMPLE>
        std::map<int, Partition<SIMPLE> *> Table<SIMPLE>::getPartitions() {
            return this->partitionMap;
        }

        template<class SIMPLE>
        PartitionState Table<SIMPLE>::addPartition(Partition<SIMPLE> *partition) {
            this->partitionMap.insert(std::make_pair(partition->getId(), partition));
            return COMBINED;
        }

        template<class SIMPLE>
        Partition<SIMPLE> *Table<SIMPLE>::getPartition(int tid) {
            return this->partitionMap.at(tid);
        }

        template<class SIMPLE>
        Partition<SIMPLE> *Table<SIMPLE>::removePartition(int tid) {
            return this->partitionMap.erase(tid);
        }

    }
}