#ifndef HARPC_COMMUNICATOR_H
#define HARPC_COMMUNICATOR_H

#include "../data_structures/inculdes.h"

namespace harp {
    namespace com {
        class Communicator {
        public:
            template<class SIMPLE>
            void barrier();

            void barrier() {//todo temp hack till find a solution for template classes, functions in header file problem
                this->barrier<int>();
            }

            template<class SIMPLE>
            void allGather(harp::ds::Table<SIMPLE> *table);

            template<class SIMPLE>
            void broadcast(harp::ds::Table<SIMPLE> *table, int bcastWorkerId);
        };
    }
}
#endif //HARPC_COMMUNICATOR_H
