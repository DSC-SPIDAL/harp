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

#include <map>
#include <chrono>

namespace harp {
    namespace util {
        namespace timing {
            std::map<int, std::chrono::high_resolution_clock::time_point> times1;
            std::map<int, std::chrono::high_resolution_clock::time_point> times2;

            void record(int tag, bool init = false) {
                if (!init && times1.count(tag) > 0) {
                    times2.insert(std::make_pair(tag, std::chrono::high_resolution_clock::now()));
                } else {
                    times1.insert(std::make_pair(tag, std::chrono::high_resolution_clock::now()));
                }
            }

            double diff(int tag) {
                return std::chrono::duration_cast<std::chrono::microseconds>(times2.at(tag) - times1.at(tag)).count();
            }

            void clear() {
                times1.clear();
                times2.clear();
            }

            void clear(int tag) {
                if (times1.count(tag) > 0)
                    times1.erase(tag);

                if (times2.count(tag) > 0)
                    times2.erase(tag);
            }
        }
    }
}
#endif //HARPC_TIMING_H
