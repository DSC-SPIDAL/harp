//
// Created by chathura on 11/1/18.
//
#include "Partition.h"

namespace harp {
    namespace ds {
        Partition::Partition(int id, void *data, int size, DataType dataType) {
            this->id = id;
            this->data = data;
            this->size = size;
            this->dataType = dataType;
        }

        int Partition::getId() {
            return this->id;
        }

        void *Partition::getData() {
            return this->data;
        }

        int Partition::getSize() {
            return this->size;
        }

        void Partition::clear() {
            delete[] data;
        }

        Partition::~Partition() {
            this->clear();
        }

        void Partition::setData(void *data) {
            this->clear();
            this->data = data;
        }

        DataType Partition::getDataType() {
            return this->dataType;
        }
    }
}

