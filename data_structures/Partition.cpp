#include "Partition.h"

namespace harp {
    namespace ds {
        template<class TYPE>
        Partition<TYPE>::Partition(int id, TYPE *data, int size) {
            this->id = id;
            this->data = data;
            this->size = size;
        }

        template<class TYPE>
        int Partition<TYPE>::getId() {
            return this->id;
        }

        template<class TYPE>
        void *Partition<TYPE>::getData() {
            return this->data;
        }

        template<class TYPE>
        int Partition<TYPE>::getSize() {
            return this->size;
        }

        template<class TYPE>
        void Partition<TYPE>::clear() {
            delete[] data;
        }

        template<class TYPE>
        Partition<TYPE>::~Partition() {
            this->clear();
        }

        template<class TYPE>
        void Partition<TYPE>::setData(void *data) {
            this->clear();
            this->data = data;
        }
    }
}

