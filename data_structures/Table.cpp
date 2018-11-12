#include "Table.h"

namespace harp {
    namespace ds {
        Table::Table(int id, DataType dataType) {
            this->id = id;
            this->dataType = dataType;
        }

        int Table::getId() {
            return this->id;
        }

        long Table::getPartitionCount() {
            return this->partitionMap.size();
        }

        std::map<int, Partition *> Table::getPartitions() {
            return this->partitionMap;
        }

        PartitionState Table::addPartition(Partition *partition) {
            this->partitionMap.insert(std::make_pair(partition->getId(), partition));
            return COMBINED;
        }

        Partition *Table::getPartition(int pid) {
            return this->partitionMap.at(pid);
        }

        long Table::removePartition(int pid, bool clearMemory) {
            if (this->partitionMap.count(pid) > 0) {
                if (clearMemory) {
                    delete this->getPartition(pid);
                }
                return this->partitionMap.erase(pid);//remove from map
            } else {
                return 0;
            }
        }

        void Table::clear() {
            for (auto p: this->partitionMap) {
                delete p.second;
            }
            this->partitionMap.clear();
        }

        Table::~Table() {
            this->clear();
        }

        void Table::replaceParition(int pid, Partition *partition) {
            this->removePartition(pid);
            this->addPartition(partition);
        }

        DataType Table::getDataType() {
            return this->dataType;
        }
    }
}