#include "daal.h"
#include "../../data_structures/DataStructures.h"
#include "iostream"

using namespace daal;
using namespace daal::algorithms;
using namespace daal::services;
using namespace daal::data_management;
using namespace harp::ds;
using namespace std;


int main(int argc, char **argv) {

    auto t = new Table<double>(1);
    cout << t->getId() << endl;


    FileDataSource<CSVFeatureManager> dataSource(
            "",
            DataSource::doAllocateNumericTable,
            DataSource::doDictionaryFromContext
    );
    dataSource.loadDataBlock();
    NumericTablePtr numericTablePtr = dataSource.getNumericTable();

    return 0;
}