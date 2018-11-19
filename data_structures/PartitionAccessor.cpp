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

#include "PartitionAccessor.h"

namespace harp {
    namespace ds {
        template<class TYPE>
        PartitionAccessor<TYPE>::PartitionAccessor(std::map<int, harp::ds::Partition<TYPE> *> *partitionMap,
                                                   std::mutex *mutex) {
            this->partitionMap = partitionMap;
            this->mutex = mutex;
        }

        template<class TYPE>
        class Test : public PartitionAccessor<TYPE> {
            void hasNext() {
                
            }
        };
    }
}

