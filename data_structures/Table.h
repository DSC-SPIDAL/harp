#ifndef HARPC_TABLE_H
#define HARPC_TABLE_H

#include <unordered_map>
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
            std::unordered_map<int, Partition<TYPE> *> partitionMap;
            std::unordered_set<int> partitionKeys;

            std::queue<int> deletedKeys;//holds deleted keys until next iteration

            std::condition_variable availability;
            std::mutex partitionMapMutex;

            int id;
        public:
            Table(int id);//todo add combiner

            ~Table();

            int getId();

            long getPartitionCount();

            const std::unordered_set<int> *getPartitionKeySet(bool blockForAvailability = false);

            //std::unordered_map<int, Partition<TYPE> *> *getPartitions();

            PartitionState addPartition(Partition<TYPE> *partition);

            Partition<TYPE> *getPartition(int pid);

            long removePartition(int pid, bool clearMemory = true);

            void replaceParition(int pid, Partition<TYPE> *partition);

            void clear(bool clearPartitions = false);

            void swap(Table<TYPE>* table);
        };
    }
}

#endif //HARPC_TABLE_H
