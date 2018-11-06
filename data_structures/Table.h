#ifndef HARPC_TABLE_H
#define HARPC_TABLE_H

#include <map>
#include <set>
#include <vector>
#include "Partition.h"
#include "mpi.h"

namespace harp {
    namespace ds {
        template<class SIMPLE>
        class Table {
        private:
            std::map<int, Partition<SIMPLE> *> partitionMap;
            std::set<int> partitionIds;
            int id;
        public:
            Table(int id);//todo add combiner

            int getId();

            long getPartitionCount();

            std::map<int, Partition<SIMPLE> *> getPartitions();

            PartitionState addPartition(Partition<SIMPLE> *partition);

            Partition<SIMPLE> *getPartition(int tid);

            Partition<SIMPLE> *removePartition(int tid);

            void clear();
        };
    }
}

#endif //HARPC_TABLE_H
