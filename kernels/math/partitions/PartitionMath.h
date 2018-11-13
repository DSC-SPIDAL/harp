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

#ifndef HARPC_PARTITIONMATH_H
#define HARPC_PARTITIONMATH_H

#include "../../../data_structures/Partition.h"

namespace harp {
    namespace math {
        namespace partition {
            template<class TYPE>
            double distance(harp::ds::Partition<TYPE> *p1, harp::ds::Partition<TYPE> *p2);
        }
    }
}
#endif //HARPC_PARTITIONMATH_H
