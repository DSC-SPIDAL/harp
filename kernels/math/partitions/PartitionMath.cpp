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
                return distance(p1, 0, p1->getSize(), p2, 0, p2->getSize());
            }


            template<class TYPE>
            double distance(harp::ds::Partition<TYPE> *p1, int i1Start, int i1End,
                            harp::ds::Partition<TYPE> *p2, int i2Start, int i2End) {
                if (i1End - i1Start != i2End - i2Start) {
                    throw "Can't calculate distance due to partition size mismatch.";
                }
                auto *p1Data = p1->getData();
                auto *p2Data = p2->getData();
                double sum = 0;
                for (int i = 0; i < i1End - i1Start; i++) {
                    sum += (pow(p1Data[i1Start + i] - p2Data[i2Start + i], 2));
                }
                return sqrt(sum);
            }
        }
    }
}