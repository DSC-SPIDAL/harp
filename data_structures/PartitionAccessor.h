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

#ifndef HARPC_PARTITIONACCESSOR_H
#define HARPC_PARTITIONACCESSOR_H

#include "map"
#include "Partition.h"
#include "mutex"

namespace harp {
    namespace ds {
        template<class TYPE>
        class PartitionAccessor {
        private:
            std::map<int, Partition<TYPE> *> *partitionMap;
            std::mutex *mutex;
        public:
            PartitionAccessor(std::map<int, Partition<TYPE> *> *partitionMap, std::mutex *mutex) {
                this->partitionMap = partitionMap;
                this->mutex = mutex;
            }

            virtual bool hasNext() = 0;

            Partition<TYPE> *next();
        };
    }
}
#endif //HARPC_PARTITIONACCESSOR_H
