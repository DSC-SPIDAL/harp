/* file: file_data_source.h */
/*******************************************************************************
* Copyright 2014-2017 Intel Corporation
* All Rights Reserved.
*
* If this  software was obtained  under the  Intel Simplified  Software License,
* the following terms apply:
*
* The source code,  information  and material  ("Material") contained  herein is
* owned by Intel Corporation or its  suppliers or licensors,  and  title to such
* Material remains with Intel  Corporation or its  suppliers or  licensors.  The
* Material  contains  proprietary  information  of  Intel or  its suppliers  and
* licensors.  The Material is protected by  worldwide copyright  laws and treaty
* provisions.  No part  of  the  Material   may  be  used,  copied,  reproduced,
* modified, published,  uploaded, posted, transmitted,  distributed or disclosed
* in any way without Intel's prior express written permission.  No license under
* any patent,  copyright or other  intellectual property rights  in the Material
* is granted to  or  conferred  upon  you,  either   expressly,  by implication,
* inducement,  estoppel  or  otherwise.  Any  license   under such  intellectual
* property rights must be express and approved by Intel in writing.
*
* Unless otherwise agreed by Intel in writing,  you may not remove or alter this
* notice or  any  other  notice   embedded  in  Materials  by  Intel  or Intel's
* suppliers or licensors in any way.
*
*
* If this  software  was obtained  under the  Apache License,  Version  2.0 (the
* "License"), the following terms apply:
*
* You may  not use this  file except  in compliance  with  the License.  You may
* obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
*
* Unless  required  by   applicable  law  or  agreed  to  in  writing,  software
* distributed under the License  is distributed  on an  "AS IS"  BASIS,  WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the   License  for the   specific  language   governing   permissions  and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Implementation of the file data source class.
//--
*/

#ifndef __FILE_DATA_SOURCE_H__
#define __FILE_DATA_SOURCE_H__

#include <cstdio>

#include "services/daal_memory.h"
#include "data_management/data_source/data_source.h"
#include "data_management/data_source/csv_data_source.h"
#include "data_management/data/data_dictionary.h"
#include "data_management/data/numeric_table.h"
#include "data_management/data/homogen_numeric_table.h"

namespace daal
{
namespace data_management
{

namespace interface1
{
/**
 * @ingroup data_sources
 * @{
 */
/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__FILEDATASOURCE"></a>
 *  \brief Specifies methods to access data stored in files
 *  \tparam _featureManager     FeatureManager to use to get numeric data from file strings
 */
template< typename _featureManager, typename _summaryStatisticsType = DAAL_SUMMARY_STATISTICS_TYPE >
class FileDataSource : public CsvDataSource< _featureManager, _summaryStatisticsType >
{
public:
    using CsvDataSource<_featureManager,_summaryStatisticsType>::checkDictionary;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::checkNumericTable;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::freeNumericTable;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::_dict;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::_initialMaxRows;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::loadDataBlock;

    using CsvDataSource<_featureManager,_summaryStatisticsType>::featureManager;

    using CsvDataSource<_featureManager,_summaryStatisticsType>::createDictionaryFromContext;

    /**
     *  Typedef that stores the parser datatype
     */
    typedef _featureManager FeatureManager;

protected:
    typedef data_management::HomogenNumericTable<DAAL_DATA_TYPE> DefaultNumericTableType;

    using CsvDataSource<_featureManager,_summaryStatisticsType>::_rawLineBuffer;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::_rawLineBufferLen;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::_rawLineLength;
    using CsvDataSource<_featureManager,_summaryStatisticsType>::enlargeBuffer;

public:
    /**
     *  Main constructor for a Data Source
     *  \param[in]  fileName                        Name of the file that stores data
     *  \param[in]  doAllocateNumericTable          Flag that specifies whether a Numeric Table
     *                                              associated with a File Data Source is allocated inside the Data Source
     *  \param[in]  doCreateDictionaryFromContext   Flag that specifies whether a Data %Dictionary
     *                                              is created from the context of the File Data Source
     *  \param[in]  initialMaxRows                  Initial value of maximum number of rows in Numeric Table allocated in loadDataBlock() method
     */
    FileDataSource( const std::string &fileName,
                    DataSourceIface::NumericTableAllocationFlag doAllocateNumericTable    = DataSource::notAllocateNumericTable,
                    DataSourceIface::DictionaryCreationFlag doCreateDictionaryFromContext = DataSource::notDictionaryFromContext,
                    size_t initialMaxRows = 10):
    CsvDataSource<_featureManager,_summaryStatisticsType>(doAllocateNumericTable, doCreateDictionaryFromContext, initialMaxRows), _fileBuffer(NULL)
    {
        _fileName = fileName;

    #if (defined(_MSC_VER)&&(_MSC_VER >= 1400))
        errno_t error;
        error = fopen_s( &_file, fileName.c_str(), "r" );
        if(error != 0 || !_file)
            this->_status.add(services::throwIfPossible(services::Status(services::ErrorOnFileOpen)));
    #else
        _file = fopen( (char*)(fileName.c_str()), "r" );
        if( !_file )
            this->_status.add(services::throwIfPossible(services::Status(services::ErrorOnFileOpen)));
    #endif

        _fileBufferLen = 1048576;
        _fileBufferPos = _fileBufferLen;
        _fileBuffer = (char *)daal::services::daal_malloc(_fileBufferLen);
    }

    ~FileDataSource()
    {
        if (_file)
            fclose(_file);
        daal::services::daal_free( _fileBuffer );
    }

public:
    services::Status createDictionaryFromContext() DAAL_C11_OVERRIDE
    {
        services::Status s = CsvDataSource<_featureManager,_summaryStatisticsType>::createDictionaryFromContext();
        fseek(_file, 0, SEEK_SET);
        _fileBufferPos = _fileBufferLen;
        return s;
    }

    DataSourceIface::DataSourceStatus getStatus() DAAL_C11_OVERRIDE
    {
        return (iseof() ? DataSourceIface::endOfData : DataSourceIface::readyForLoad);
    }

protected:
    bool iseof() const DAAL_C11_OVERRIDE
    {
        return ((_fileBufferPos == _fileBufferLen || _fileBuffer[_fileBufferPos] == '\0') && feof(_file));
    }

    bool readLine(char *buffer, int count, int& pos)
    {
        bool bRes = true;
        pos = 0;
        while (pos + 1 < count)
        {
            if (_fileBufferPos < _fileBufferLen && _fileBuffer[_fileBufferPos] != '\0')
            {
                buffer[pos] = _fileBuffer[_fileBufferPos];
                pos++;
                _fileBufferPos++;
                if (buffer[pos - 1] == '\n')
                    break;
            }
            else
            {
                if (iseof ())
                    break;
                _fileBufferPos = 0;
                const int readLen = (int)fread(_fileBuffer, 1, _fileBufferLen, _file);
                if (readLen < _fileBufferLen)
                {
                    _fileBuffer[readLen] = '\0';
                }
                if (ferror(_file))
                {
                    bRes = false;
                    break;
                }
            }
        }
        buffer[pos] = '\0';
        return bRes;
    }

    services::Status readLine() DAAL_C11_OVERRIDE
    {
        _rawLineLength = 0;
        while(!iseof())
        {
            int readLen = 0;
            if(!readLine(_rawLineBuffer + _rawLineLength, _rawLineBufferLen - _rawLineLength, readLen))
                return services::Status(services::ErrorOnFileRead);

            if (readLen <= 0)
            {
                _rawLineLength = 0;
                break;
            }
            _rawLineLength += readLen;
            if (_rawLineBuffer[_rawLineLength - 1] == '\n' || _rawLineBuffer[_rawLineLength - 1] == '\r')
            {
                while (_rawLineLength > 0 && (_rawLineBuffer[_rawLineLength - 1] == '\n' || _rawLineBuffer[_rawLineLength - 1] == '\r'))
                {
                    _rawLineLength--;
                }
                _rawLineBuffer[_rawLineLength] = '\0';
                break;
            }
            if(!enlargeBuffer())
                return services::Status(services::ErrorMemoryAllocationFailed);
        }
        return services::Status();
    }

protected:
    std::string  _fileName;

    FILE *_file;

    char *_fileBuffer;
    int   _fileBufferLen;
    int   _fileBufferPos;
};
/** @} */
} // namespace interface1
using interface1::FileDataSource;

}
}
#endif
