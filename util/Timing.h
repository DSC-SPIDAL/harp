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

#ifndef HARPC_TIMING_H
#define HARPC_TIMING_H

#include <unordered_map>
#include <chrono>

namespace harp {
    namespace util {
        namespace timing {
            std::unordered_map<int, std::chrono::high_resolution_clock::time_point> times;

            void record(int tag) {
                if (times.count(tag) > 0) {
                    times[tag] = std::chrono::high_resolution_clock::now();
                } else {
                    times.insert(std::make_pair(tag, std::chrono::high_resolution_clock::now()));
                }
            }

            double diff(int from, int to, bool store = true) {
                return std::chrono::duration_cast<std::chrono::microseconds>(times.at(to) - times.at(from)).count();
            }

            void clear() {
                times.clear();
            }
        }
    }
}
#endif //HARPC_TIMING_H
