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

#include <fstream>
#include <iostream>
#include <vector>
#include <thread>
#include <cstdlib>
#include "DataGenerator.h"

namespace harp {
    namespace util {
        void generateKMeansData(std::string folder, int numberOfRecords, int vectorSize,
                                int splits, int centroidsCount) {
            srand(time(NULL));
            std::vector<std::thread> threads;
            for (int i = 0; i < splits; i++) {
                std::thread t([folder, numberOfRecords, vectorSize, splits, i]() {
                    std::ofstream ostream;
                    ostream.open(folder + "/" + std::to_string(i));
                    for (int r = 0; r < numberOfRecords / splits; r++) {
                        for (int v = 0; v < vectorSize; v++) {
                            ostream << static_cast <float> (rand()) / (static_cast <float> (RAND_MAX / 150));
                            if (v != vectorSize - 1) {
                                ostream << ",";
                            } else {
                                ostream << std::endl;
                            }
                        }
                    }
                    ostream.close();
                });

                threads.push_back(std::move(t));
            }

            std::ofstream ostream;
            ostream.open(folder + "/centroids");
            for (int i = 0; i < centroidsCount; i++) {
                for (int v = 0; v < vectorSize; v++) {
                    ostream << static_cast <float> (rand()) / (static_cast <float> (RAND_MAX / 150));
                    if (v != vectorSize - 1) {
                        ostream << ",";
                    } else {
                        ostream << std::endl;
                    }
                }
            }
            ostream.close();

            for (auto &t:threads) {
                t.join();
            }
            threads.clear();
        }


        void readKMeansDataFromFile(std::string file, int vectorSize,
                                    harp::ds::Table<float> *table, int partitionIdPivot) {
            std::ifstream istream(file);
            int partitionIndex = 0;
            while (istream.good()) {
                auto *vector = new float[vectorSize];
                bool added = false;
                for (int i = 0; i < vectorSize; i++) {
                    std::string str;
                    if (i < vectorSize - 1) {
                        getline(istream, str, ',');
                    } else {
                        getline(istream, str);
                    }
                    if (!str.empty()) {
                        vector[i] = std::stof(str);
                        added = true;
                    }
                }
                if (added) {
                    auto *part = new harp::ds::Partition<float>(partitionIdPivot + partitionIndex++, vector,
                                                                vectorSize);
                    table->addPartition(part);
                } else {
                    delete[] vector;
                }
            }
            istream.close();
        }
    }
}
