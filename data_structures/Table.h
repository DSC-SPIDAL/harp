#ifndef HARPC_TABLE_H
#define HARPC_TABLE_H

#include <map>
#include <set>
#include <vector>
#include "Partition.h"
#include "mpi.h"

namespace harp {
    namespace ds {
        template<class TYPE>
        class Table {
        private:
            std::map<int, Partition<TYPE> *> partitionMap;
            std::set<int> partitionIds;
            int id;
        public:
            Table(int id);//todo add combiner

            ~Table();

            int getId();

            long getPartitionCount();

            std::map<int, Partition<TYPE> *> getPartitions();

            PartitionState addPartition(Partition<TYPE> *partition);

            Partition<TYPE> *getPartition(int pid);

            long removePartition(int pid, bool clearMemory = true);

            void replaceParition(int pid, Partition<TYPE> *partition);

            void clear();
        };
    }
}

#endif //HARPC_TABLE_H
