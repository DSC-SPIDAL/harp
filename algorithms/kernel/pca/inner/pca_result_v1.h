/* file: pca_result_v1.h */
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

/*
//++
//  Implementation of PCA algorithm result.
//--
*/

#ifndef __PCA_RESULT_V1_H__
#define __PCA_RESULT_V1_H__

#include "pca/inner/pca_types_v1.h"
#include "data_management/data/data_collection.h"
#include "data_management/data/numeric_table.h"


namespace daal
{
namespace algorithms
{
namespace pca
{
namespace interface1
{

class ResultImpl : public data_management::interface1::DataCollection
{
public:
    DAAL_CAST_OPERATOR(ResultImpl);

    ResultImpl(const size_t n);
    ResultImpl(const ResultImpl& o);
    virtual ~ResultImpl();

    /**
    * Allocates memory for storing partial results of the PCA algorithm
    * \param[in] input Pointer to an object containing input data
    * \return Status of computations
    */
    template <typename algorithmFPType>
    services::Status allocate(const daal::algorithms::Input *input);

    /**
    * Allocates memory for storing partial results of the PCA algorithm
    * \param[in] partialResult Pointer to an object containing partialResult data
    * \return Status of computations
    */
    template <typename algorithmFPType>
    services::Status allocate(const daal::algorithms::PartialResult *partialResult);

    /**
    * Checks the results of the PCA algorithm implementation
    * \param[in] nFeatures      Number of features
    * \param[in] nTables        Number of tables
    *
    * \return Status
    */
    virtual services::Status check(size_t nFeatures, size_t nTables) const;

    /**
    * Sets numeric table to storage
    * \param[in] key     key
    * \param[in] table   table
    */
    virtual void setTable(size_t key, data_management::NumericTablePtr table);

protected:

    /**
    * Allocates memory for storing partial results of the PCA algorithm
    * \param[in] nFeatures Number of features
    * \return Status of computations
    */
    template <typename algorithmFPType>
    services::Status allocate(size_t nFeatures);

};

} // interface1
} // namespace pca
} // namespace algorithms
} // namespace daal

#endif
