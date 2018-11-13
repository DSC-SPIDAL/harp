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


#include <iostream>
#include "../../data_structures/Table.h"
#include "../math/HarpMath.h"
#include "float.h"

namespace harp {
    namespace kernels {
        template<class TYPE>
        void kmeans(harp::ds::Table<TYPE> *centroids, harp::ds::Table<TYPE> *points, int iterations) {
            long numOfCentroids = centroids->getPartitionCount();
            auto *clusters = new harp::ds::Table<TYPE> *[numOfCentroids];
            for (int i = 0; i < centroids->getPartitionCount(); i++) {
                clusters[i] = new harp::ds::Table<TYPE>(i);
            }

            for (int i = 0; i < iterations; i++) {
                for (auto p :points->getPartitions()) {
                    int minCentroidIndex = 0;
                    auto minDistance = DBL_MAX;
                    int currentCentroidIndex = 0;
                    for (auto c :centroids->getPartitions()) {
                        double distance = harp::math::partition::distance(p.second, c.second);
                        //std::cout << "Distance : " << distance << std::endl;
                        if (distance < minDistance) {
                            minDistance = distance;
                            minCentroidIndex = currentCentroidIndex;
                        }
                        currentCentroidIndex++;
                    }
                    clusters[minCentroidIndex]->addPartition(p.second);
                }
                centroids->clear();
                //new centroids
                for (int c = 0; c < numOfCentroids; c++) {
                    auto *newC = harp::math::table::mean(clusters[c], 3);
                    centroids->addPartition(newC);
                }
            }
        }
    }
}
