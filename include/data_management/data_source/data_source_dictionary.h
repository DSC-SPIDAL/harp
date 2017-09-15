/* file: data_source_dictionary.h */
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
//  Implementation of a data source dictionary.
//--
*/

#ifndef __DATA_SOURCE_DICTIONARY_H__
#define __DATA_SOURCE_DICTIONARY_H__

#include <string>
#include <map>
#include "data_management/data/data_dictionary.h"

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
class CategoricalFeatureDictionary : public std::map<std::string, std::pair<int, int> >
{
public:
};

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__DATASOURCEFEATURE"></a>
 *  \brief Data structure that describes the Data Source feature
 */
class DataSourceFeature : public SerializationIface
{
public:
    NumericTableFeature             ntFeature;
    size_t                          name_length;
    char                           *name;

    CategoricalFeatureDictionary   *cat_dict;

public:
    /**
     *  Constructor of a data feature
     */
    DataSourceFeature() : name(NULL), name_length(0), cat_dict(NULL) {}

    /**
     *  Copy operator for a data feature
     */
    DataSourceFeature &operator= (const DataSourceFeature &f)
    {
        ntFeature   = f.ntFeature;

        name = new char[f.name_length];
        daal::services::daal_memcpy_s(name, name_length, f.name, f.name_length);

        name_length = f.name_length;
        cat_dict    = 0;

        return *this;
    }

    /** \private */
    virtual ~DataSourceFeature()
    {
        if(name)
        {
            delete[] name;
        }

        if(cat_dict)
        {
            delete cat_dict;
        }
    }

    /**
     *  Gets a categorical features dictionary
     *  \return Pointer to the categorical features dictionary
     */
    CategoricalFeatureDictionary *getCategoricalDictionary()
    {
        if( !cat_dict )
        {
            cat_dict = new CategoricalFeatureDictionary;
        }

        return cat_dict;
    }

    /**
     *  Specifies the name of a data feature
     *  \param[in]  featureName  Name of the data feature
     */
    void setFeatureName(const std::string &featureName)
    {
        name_length = featureName.length() + 1;
        if (name)
        {
            delete[] name;
        }
        name = new char[name_length];
        daal::services::daal_memcpy_s(name, name_length, featureName.c_str(), name_length);
    }

    /**
     *  Fills the class based on a specified type
     *  \tparam  T  Name of the data feature
     */
    template<typename T>
    void setType()
    {
        ntFeature.setType<T>();
    }

    /** \private */
    services::Status serializeImpl(InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<InputDataArchive, false>( arch );

        return services::Status();
    }

    /** \private */
    services::Status deserializeImpl(const OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const OutputDataArchive, true>( arch );

        return services::Status();
    }

    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl( Archive *arch )
    {
        arch->setObj( &ntFeature );

        arch->set( name_length );

        if( onDeserialize )
        {
            if( name_length > 0 )
            {
                if( name ) { delete[] name; }
                name = NULL;
                name = new char[name_length];
            }
        }

        arch->set( name, name_length );

        int categoricalFeatureDictionaryFlag = (cat_dict != 0);

        arch->set( categoricalFeatureDictionaryFlag );

        if( categoricalFeatureDictionaryFlag )
        {
            if( onDeserialize )
            {
                /* Make sure that dictionary is allocated */
                getCategoricalDictionary();
                /* Make sure that dictionary is empty */
                cat_dict->empty();
            }

            size_t size = cat_dict->size();

            arch->set( size );

            if( onDeserialize )
            {
                size_t buffLen = 10;
                char*  buff    = new char[buffLen];
                for(size_t i=0; i<size; i++)
                {
                    size_t catNameLen = 0;
                    int catV1 = 0;
                    int catV2 = 0;

                    arch->set( catNameLen );
                    if( catNameLen>buffLen )
                    {
                        delete[] buff;
                        buff = new char[catNameLen];
                        buffLen = catNameLen;
                    }
                    arch->set( buff, catNameLen );
                    arch->set( catV1 );
                    arch->set( catV2 );

                    (*cat_dict)[ std::string(buff, catNameLen) ] = std::pair<int,int>(catV1, catV2);
                }
                delete[] buff;
            }
            else
            {
                typedef CategoricalFeatureDictionary::iterator it_type;

                for(it_type it=cat_dict->begin(); it != cat_dict->end(); it++)
                {
                    const std::string & catName = it->first;
                    size_t catNameLen = catName.size();
                    int catV1 = it->second.first;
                    int catV2 = it->second.second;

                    arch->set( catNameLen );
                    arch->set( catName.c_str(), catNameLen );
                    arch->set( catV1 );
                    arch->set( catV2 );
                }
            }
        }
        else
        {
            cat_dict = 0;
        }

        return services::Status();
    }

    virtual int getSerializationTag() const DAAL_C11_OVERRIDE
    {
        return SERIALIZATION_DATAFEATURE_NT_ID;
    }

    data_feature_utils::IndexNumType getIndexType() const
    {
        return ntFeature.indexType;
    }
};

typedef Dictionary<DataSourceFeature, SERIALIZATION_DATADICTIONARY_DS_ID>   DataSourceDictionary;
/** @} */

} // namespace interface1

using interface1::CategoricalFeatureDictionary;
using interface1::DataSourceFeature;
using interface1::DataSourceDictionary;

}
} // namespace daal
#endif
