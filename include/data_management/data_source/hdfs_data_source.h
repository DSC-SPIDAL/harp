/* file: hdfs_data_source.h */
/*******************************************************************************
* Copyright 2014-2018 Intel Corporation
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

#ifndef __DATA_MANAGEMENT_DATA_SOURCE_HDFS_DATA_SOURCE_H__
#define __DATA_MANAGEMENT_DATA_SOURCE_HDFS_DATA_SOURCE_H__

#include "data_management/data_source/data_source.h"
#include "data_management/data_source/csv_data_source.h"
#include "data_management/data_source/csv_feature_manager.h"
#include "data_management/data_source/internal/hdfs_wrappers.h"

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

template<typename FeatureManager, typename SummaryStatisticsType = DAAL_SUMMARY_STATISTICS_TYPE>
class HDFSDataSource;

template<typename SummaryStatisticsType>
class HDFSDataSource<CSVFeatureManager, SummaryStatisticsType> :
    public CsvDataSource<CSVFeatureManager, SummaryStatisticsType>
{
private:
    typedef CsvDataSource<CSVFeatureManager, SummaryStatisticsType> super;

public:
    explicit HDFSDataSource(const std::string &fileName,
                            const CsvDataSourceOptions &options,
                            size_t initialMaxRows = 10) :
        super(options, initialMaxRows)
    {
        super::_status |= initialize(fileName);
    }

    virtual DataSourceIface::DataSourceStatus getStatus() DAAL_C11_OVERRIDE
    {
        return iseof()
            ? DataSourceIface::endOfData
            : DataSourceIface::readyForLoad;
    }

    virtual services::Status createDictionaryFromContext() DAAL_C11_OVERRIDE
    {
        services::Status status;

        DAAL_CHECK_STATUS(status, super::createDictionaryFromContext());
        DAAL_CHECK_STATUS(status, _hdfsReader.reset());

        return status;
    }

protected:
    virtual bool iseof() const DAAL_C11_OVERRIDE
    {
        return _hdfsReader.eof();
    }

    virtual services::Status readLine() DAAL_C11_OVERRIDE
    {
        DAAL_ASSERT( super::_rawLineBuffer );
        DAAL_ASSERT( super::_rawLineBufferLen );

        _readLineStatus.clear();
        super::_rawLineLength = 0;

        while (!_hdfsReader.eof())
        {
            DAAL_ASSERT( super::_rawLineBufferLen >= super::_rawLineLength );

            bool endOfLineReached;
            const uint32_t readLineLength = _hdfsReader.readLine(super::_rawLineBuffer + super::_rawLineLength,
                                                                 super::_rawLineBufferLen - super::_rawLineLength,
                                                                 endOfLineReached, &_readLineStatus);
            DAAL_CHECK_STATUS_VAR(_readLineStatus);

            super::_rawLineLength += readLineLength;

            if (endOfLineReached)
            { break; }
            else
            {
                if (!super::enlargeBuffer())
                {
                    _readLineStatus |= services::throwIfPossible(services::ErrorMemoryAllocationFailed);
                    DAAL_CHECK_STATUS_VAR(_readLineStatus);
                }
            }
        }

        return _readLineStatus;
    }

private:
    /* Disable copy & assigment */
    HDFSDataSource(const HDFSDataSource &);
    HDFSDataSource &operator = (const HDFSDataSource &);

    services::Status initialize(const std::string &fileName)
    {
        services::Status status;

        internal::HDFSConnection connection("default", 0, &status);
        DAAL_CHECK_STATUS_VAR(status);

        internal::HDFSFile file = connection.openFile(fileName, &status);
        DAAL_CHECK_STATUS_VAR(status);

        status |= _hdfsReader.initialize(file);
        DAAL_CHECK_STATUS_VAR(status);

        return status;
    }

    internal::HDSFFileReader _hdfsReader;
    services::Status _readLineStatus;
};
/** @} */

} // namespace interface1

using interface1::HDFSDataSource;

} // namespace data_management
} // namespace daal

#endif
