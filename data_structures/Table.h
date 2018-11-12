#ifndef HARPC_TABLE_H
#define HARPC_TABLE_H

#include <map>
#include <set>
#include <vector>
#include "Partition.h"
#include "DataTypes.h"
#include "mpi.h"

namespace harp {
    namespace ds {
        class Table {
        private:
            std::map<int, Partition *> partitionMap;
            std::set<int> partitionIds;
            int id;
            DataType dataType;
        public:
            Table(int id, DataType dataType);//todo add combiner

            ~Table();

            int getId();

            long getPartitionCount();

            std::map<int, Partition *> getPartitions();

            PartitionState addPartition(Partition *partition);

            Partition *getPartition(int pid);

            DataType getDataType();

            long removePartition(int pid, bool clearMemory = true);

            void replaceParition(int pid, Partition *partition);

            void clear();
        };
    }
}

#endif //HARPC_TABLE_H
