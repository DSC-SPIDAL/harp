#ifndef UNTITLED_PARTITION_H
#define UNTITLED_PARTITION_H

#include <string>
#include "DataTypes.h"

namespace harp {
    namespace ds {
        template <class TYPE>
        class Partition {
        private:
            int id;
            TYPE *data;
            int size = 0;
        public:

            Partition(int id, TYPE *data, int size);

            ~Partition();

            int getId();

            void *getData();

            void setData(void *data);

            int getSize();

            void clear();
        };

        enum PartitionState {
            ADDED, ADD_FAILED, COMBINED, COMBINE_FAILED
        };
    }
}


#endif //UNTITLED_PARTITION_H
