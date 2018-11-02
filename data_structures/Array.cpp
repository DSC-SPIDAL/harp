#include "Array.h"

namespace harp {
    namespace ds {
        template<class TYPE>
        Array<TYPE>::Array(TYPE *data, int size, int start) {
            this->data = data;
            this->size = size;
            this->start = start;
        }

        template<class TYPE>
        TYPE *Array<TYPE>::getData() {
            return this->data;
        }

        template<class TYPE>
        int Array<TYPE>::getSize() {
            return size;
        }

        template<class TYPE>
        int Array<TYPE>::getStart() {
            return size;
        }


        template<class TYPE>
        void Array<TYPE>::reset() {
            this->size = -1;
            this->start = -1;
            this->data = nullptr;
        }
    }
}