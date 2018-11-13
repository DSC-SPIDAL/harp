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

#ifndef HARPC_DATAGENERATOR_H
#define HARPC_DATAGENERATOR_H

#include <string>
#include "../../data_structures/Table.h"

namespace harp {
    namespace util {
        void
        generateKMeansData(std::string folder, int numberOfRecords, int vectorSize, int splits, int centroidsCount);

        void readKMeansDataFromFile(std::string file, int vectorSize, harp::ds::Table<float> *table,
                                    int partitionIdPivot = 0);
    }
}
#endif //HARPC_DATAGENERATOR_H
