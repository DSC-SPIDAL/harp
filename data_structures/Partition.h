#ifndef UNTITLED_PARTITION_H
#define UNTITLED_PARTITION_H

#include <string>

namespace harp {
    namespace ds {
        template<class SAMPLE>
        class Partition {
        private:
            int id;
            SAMPLE *data;
            long size;
        public:

            Partition(int id, SAMPLE *data, long size);

            int getId();

            SAMPLE *getData();

            long getSize();
        };

        enum PartitionState {
            ADDED, ADD_FAILED, COMBINED, COMBINE_FAILED
        };
    }
}


#endif //UNTITLED_PARTITION_H
