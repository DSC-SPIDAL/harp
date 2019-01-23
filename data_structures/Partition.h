#ifndef UNTITLED_PARTITION_H
#define UNTITLED_PARTITION_H

#include <string>
#include <iostream>
#include "DataTypes.h"

namespace harp {
    namespace ds {
        template<class TYPE>
        class Partition {
        private:
            int id;
            TYPE *data;
            int size = 0;
        public:

            Partition(int id, TYPE *data, int size) {
                this->id = id;
                this->data = data;
                this->size = size;
            }

            Partition(const Partition &p) {
                std::cout << "Copy partition called" << std::endl;
            }

            ~Partition() {
                this->clear();
            }

            int getId() {
                return this->id;
            }

            TYPE *getData() {
                return this->data;
            }

            void setData(TYPE *data, int size) {
                this->clear();
                this->data = data;
                this->size = size;
            }

            int getSize() {
                return this->size;
            }

            void clear() {
                delete[] data;
            }
        };

        enum PartitionState {
            ADDED, ADD_FAILED, COMBINED, COMBINE_FAILED
        };
    }
}


#endif //UNTITLED_PARTITION_H
