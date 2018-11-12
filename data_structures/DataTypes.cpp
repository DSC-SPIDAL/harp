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

#include "DataTypes.h"

void *createArray(DataType dataType, int size) {
    switch (dataType) {
        case HP_INT:
            return new int[size];
        case HP_LONG:
            return new long[size];
        case HP_FLOAT:
            return new float[size];
    }
}

MPI_Datatype getMPIDataType(DataType dataType) {
    switch (dataType) {
        case HP_INT:
            return MPI_INT;
        case HP_LONG:
            return MPI_LONG;
        case HP_FLOAT:
            return MPI_FLOAT;
    }
}

//template<class TYPE>
//TYPE *castToArray(void *data, DataType dataType) {
//    switch (dataType) {
//        case HP_INT:
//            return static_cast<int *>(data);
//        case HP_LONG:
//            return static_cast<long *>(data);
//        case HP_FLOAT:
//            return static_cast<float *>(data);
//    }
//}


