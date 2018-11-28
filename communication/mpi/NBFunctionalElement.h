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

#ifndef HARPC_NBFUNCTIONALELEMENT_H
#define HARPC_NBFUNCTIONALELEMENT_H

#include <mpi.h>
#include <vector>
#include <functional>

namespace harp {
    namespace comm {
        class NBFunctionalElement {
        private:
            std::vector<MPI_Request> requets;
            std::function<void()> nextFunction;
        public:
            bool checkAndExecute();
        };
    }
}
#endif //HARPC_NBFUNCTIONALELEMENT_H
