//
// Created by chathura on 11/1/18.
//
#include "Partition.h"

namespace harp {
    namespace ds {
        template<class SAMPLE>
        Partition<SAMPLE>::Partition(int id, SAMPLE* data) {
            this->id = id;
            this->data = data;
        }

        template<class SAMPLE>
        int Partition<SAMPLE>::getId() {
            return this->id;
        }

        template<class SAMPLE>
        SAMPLE* Partition<SAMPLE>::getData() {
            return this->data;
        }
    }
}

