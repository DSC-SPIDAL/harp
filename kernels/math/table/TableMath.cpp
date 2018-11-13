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

#include "TableMath.h"
#include "iostream"

namespace harp {
    namespace math {
        namespace table {

            template<class TYPE>
            harp::ds::Partition<TYPE> *
            sumAndDivide(harp::ds::Table<TYPE> *table, int partitionSize, int divisor = 1) {//todo remove partition size
                auto *mean = new float[partitionSize];
                for (int i = 0; i < partitionSize; i++) {
                    mean[i] = 0;
                }
                for (auto p: table->getPartitions()) {
                    auto *data = static_cast<TYPE *>(p.second->getData());
                    for (int i = 0; i < p.second->getSize(); i++) {
                        mean[i] += (data[i] /
                                    divisor); //todo check whether compiler optimize this division when divisor = 1
                    }
                }
                return new harp::ds::Partition<TYPE>(table->getId(), mean, partitionSize);
            }

            /**
             * Calaculates the sum of all partitions. Output partition id will be the id of the table
             *
             * @tparam TYPE
             * @param table
             * @param partitionSize
             * @return
             */
            template<class TYPE>
            harp::ds::Partition<TYPE> *
            sum(harp::ds::Table<TYPE> *table, int partitionSize) {
                sumAndDivide(table, partitionSize, 1);
            }

            /**
             * Calculates the mean of all partitions. Output partition id will be the id of the table
             *
             * @tparam TYPE
             * @param table
             * @param partitionSize
             * @return
             */
            template<class TYPE>
            harp::ds::Partition<TYPE> *
            mean(harp::ds::Table<TYPE> *table, int partitionSize) {//todo remove partition size
                return sumAndDivide(table, partitionSize, static_cast<int>(table->getPartitionCount()));
            }
        }
    }
}
