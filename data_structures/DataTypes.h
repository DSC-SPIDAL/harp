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

#ifndef HARPC_DATA_TYPES_H
#define HARPC_DATA_TYPES_H

#include "mpi.h"

enum DataType {
    HP_INT, HP_LONG, HP_FLOAT
};

void *createArray(DataType dataType, int size);

template<class TYPE>
MPI_Datatype getMPIDataType();

#endif //HARPC_DATA_TYPES_H
