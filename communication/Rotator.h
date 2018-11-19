//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//

#ifndef HARPC_ROTATOR_H
#define HARPC_ROTATOR_H

#include <queue>
#include <mutex>

#include "../data_structures/Table.h"
#include "Communicator.h"

namespace harp {
    namespace com {
        template<class TYPE>
        class Rotator {
        private:
            harp::com::Communicator<int> *comm;
            harp::ds::Table<TYPE> *tableReference;

            std::queue<harp::ds::Partition<TYPE> *> readQueue;

            std::queue<harp::ds::Partition<TYPE> *> transferQueue;
            std::mutex transferQueueMutex;
        public:
            /**
             * Constructor will copy all values in table to read queue and clear the table. Table shouldn't be used
             * prior to calling finalize in rotator
             * @param table
             */
            Rotator(harp::com::Communicator<int> *comm, harp::ds::Table<TYPE> *table);


            /**
             * Returns the next element in the read queue
             * @return
             */
            harp::ds::Partition<TYPE> *next();

            void rotate(int count = 1);

            /**
             * This function will copy all remaining elements in readQueue back to the tableReference
             */
            void finalize();

        };


    }
}

#endif //HARPC_ROTATOR_H
