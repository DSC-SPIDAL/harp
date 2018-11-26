#include "Partition.h"
#include "iostream"

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
        TYPE *Partition<TYPE>::getData() {
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
        void Partition<TYPE>::setData(TYPE *data, int size) {
            this->clear();
            this->data = data;
            this->size = size;
        }

        template<class TYPE>
        Partition<TYPE>::Partition(const Partition &p) {
            std::cout << "Copy partition called" << std::endl;
        }
    }
}

