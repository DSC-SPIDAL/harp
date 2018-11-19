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

#include "Rotator.h"
#include "iostream"
#include "thread"

namespace harp {
    namespace com {
        template<class TYPE>
        Rotator<TYPE>::Rotator(harp::com::Communicator<int> *comm, ds::Table<TYPE> *table) {
            this->tableReference = table;
            this->comm = comm;

            //clear the table and copy values to read queue
            std::unordered_map<int, harp::ds::Partition<TYPE> *> *map = table->getPartitions();
            for (auto it = map->cbegin(); it != map->cend(); it++) {
                harp::ds::Partition<TYPE> *part = it->second;
                this->readQueue.push(part);
                map->erase(it);
            }
        }

        template<class TYPE>
        ds::Partition<TYPE> *Rotator<TYPE>::next() {
            //add anything in the transfer queue to the readQueue
            transferQueueMutex.lock();
            while (!this->transferQueue.empty()) {
                this->readQueue.push(transferQueue.front());
                transferQueue.pop();
            }
            transferQueueMutex.unlock();
            return this->readQueue.front();
        }

        template<class TYPE>
        void Rotator<TYPE>::finalize() {
            while (!this->readQueue.empty()) {
                this->tableReference->addPartition(readQueue.front());
                readQueue.pop();
            }
        }

        template<class TYPE>
        void Rotator<TYPE>::rotate(int count) {
            auto *toSend = new harp::ds::Table<TYPE>(this->tableReference->getId());
            int doneCount = 0;
            while (doneCount < count) {
                ds::Partition<TYPE> *current = this->readQueue.front();
                toSend->addPartition(current);
                this->readQueue.pop();
                doneCount++;
            }

            harp::com::Communicator<int> *comm = this->comm;
            std::queue<harp::ds::Partition<TYPE> *> *transferQueue = &this->transferQueue;
//
//            std::cout << "Doing rotation : " << toSend->getPartitionCount() << std::endl;
//            this->comm->rotate(toSend);
//            std::cout << "Done rotation" << std::endl;
//            std::map<int, harp::ds::Partition<TYPE> *> *map = toSend->getPartitions();
//            this->transferQueueMutex.lock();
//            for (auto it = map->cbegin(); it != map->cend(); it++) {
//                harp::ds::Partition<TYPE> *part = it->second;
//                this->transferQueue.push(part);
//                map->erase(it);
//            }
//            this->transferQueueMutex.unlock();

            //todo use single thread
            std::thread t([toSend, this](harp::com::Communicator<int> *comm) {
                std::cout << "Doing rotation : " << toSend->getPartitionCount() << std::endl;
                comm->rotate(toSend);
                std::cout << "Done rotation" << toSend->getPartitionCount() << std::endl;
                std::unordered_map<int, harp::ds::Partition<TYPE> *> *map = toSend->getPartitions();
                this->transferQueueMutex.lock();
                for (auto it = map->cbegin(); it != map->cend(); it++) {
                    harp::ds::Partition<TYPE> *part = it->second;
                    this->transferQueue.push(part);
                    map->erase(it);
                }
                this->transferQueueMutex.unlock();
            }, std::ref(this->comm));
            t.join();
        }
    }
}

