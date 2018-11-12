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

#import "TableMath.h"
#include "iostream"

namespace harp {
    namespace math {
        namespace table {
            harp::ds::Partition *mean(harp::ds::Table *table, int partitionSize) {//todo remove partition size
                auto *mean = new float[partitionSize];
                for (int i = 0; i < partitionSize; i++) {
                    mean[i] = 0;
                }
                long partitionCount = table->getPartitionCount();
                std::cout << "pcount " << partitionCount << std::endl;
                for (auto p: table->getPartitions()) {
                    auto *data = static_cast<float *>(p.second->getData());
                    for (int i = 0; i < p.second->getSize(); i++) {
                        mean[i] += (data[i] / partitionCount);
                        std::cout << "point " << data[i] << " mean : " << mean[i] << " pc " << partitionCount
                                  << std::endl;
                    }
                }
                return new harp::ds::Partition(-1, mean, partitionSize, HP_FLOAT);
            }
        }
    }
}
