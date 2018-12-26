
#ifndef HARPC_PRINT_H
#define HARPC_PRINT_H


#include <iostream>
#include <iomanip>
#include "../data_structures/Table.h"

template<class TYPE>
void printTable(harp::ds::Table<TYPE> *table) {
    for (auto p : *table->getPartitions()) {
        std::cout << p.first << " : ";
        for (int j = 0; j < p.second->getSize(); j++) {
            std::cout << std::setprecision(10) << p.second->getData()[j] << ",";
        }
        std::cout << std::endl;
    }
}

#endif //HARPC_PRINT_H
