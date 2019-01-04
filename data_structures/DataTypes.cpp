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
#include "typeinfo"
#include "iostream"

#define TYPE_HASH(type)\
    const size_t _##type##_type = typeid(type).hash_code();

TYPE_HASH(int);
TYPE_HASH(long);
TYPE_HASH(float);
TYPE_HASH(double);

template<class TYPE>
MPI_Datatype getMPIDataType() {
    size_t hash = typeid(TYPE).hash_code();
    if (hash == _int_type) {
        return MPI_INT;
    } else if (hash == _long_type) {
        return MPI_LONG;
    } else if (hash == _float_type) {
        return MPI_FLOAT;
    } else if (hash == _double_type) {
        return MPI_DOUBLE;
    } else {
        return MPI_INT;
    }
}
