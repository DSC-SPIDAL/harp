#ifndef UNTITLED_PARTITION_H
#define UNTITLED_PARTITION_H

#include <string>
#include "DataTypes.h"

namespace harp {
    namespace ds {
        class Partition {
        private:
            int id;
            void *data;
            int size = 0;
            DataType  dataType;
        public:

            Partition(int id, void *data, int size, DataType dataType);

            ~Partition();

            int getId();

            void *getData();

            void setData(void *data);

            int getSize();

            void clear();

            DataType  getDataType();
        };

        enum PartitionState {
            ADDED, ADD_FAILED, COMBINED, COMBINE_FAILED
        };
    }
}


#endif //UNTITLED_PARTITION_H
