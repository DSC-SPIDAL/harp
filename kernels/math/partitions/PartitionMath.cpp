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
#include "PartitionMath.h"
#include "math.h"
#include "iostream"

namespace harp {
    namespace math {
        namespace partition {
            template<typename TYPE>
            double distance(harp::ds::Partition<TYPE> *p1, harp::ds::Partition<TYPE> *p2) {
                if (p1->getSize() != p2->getSize()) {
                    throw "Can't calculate distance due to partition size mismatch.";
                }
                auto *p1Data = static_cast<TYPE *>(p1->getData());
                auto *p2Data = static_cast<TYPE *>(p2->getData());
                double sum = 0;
                for (int i = 0; i < p1->getSize(); i++) {
                    sum += (pow(p1Data[i] - p2Data[i], 2));
                }
                return sqrt(sum);
            }
        }
    }
}