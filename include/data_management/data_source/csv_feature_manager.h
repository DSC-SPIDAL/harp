/* file: csv_feature_manager.h */
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
//  Implementation of the CSV feature manager class.
//--
*/

#ifndef __CSV_FEATURE_MANAGER_H__
#define __CSV_FEATURE_MANAGER_H__

#include <sstream>
#include <list>

#include "services/daal_memory.h"
#include "data_management/data_source/data_source.h"
#include "data_management/data_source/data_source_dictionary.h"
#include "data_management/data/numeric_table.h"
#include "data_management/data/homogen_numeric_table.h"

namespace daal
{
namespace data_management
{

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__FEATUREAUXDATA"></a>
 *  \brief Structure for auxiliary data used for feature extraction.
 */
struct FeatureAuxData
{
    FeatureAuxData() : idx(0), wide(1), dsFeat(0), ntFeat(0), nCats(0) {};
    size_t idx;
    size_t wide;
    size_t nCats;
    DataSourceFeature   *dsFeat;
    NumericTableFeature *ntFeat;
};

typedef void (*functionT)(const char* word, FeatureAuxData& aux, DAAL_DATA_TYPE* arr);

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__MODIFIERIFACE"></a>
 *  \brief Abstract interface class that defines the interface for a features modifier
 */
class ModifierIface
{
public:
    virtual void apply(services::Collection<functionT> &funcList, services::Collection<FeatureAuxData> &auxVect) const = 0;

    static void contFunc(const char* word, FeatureAuxData& aux, DAAL_DATA_TYPE* arr)
    {
        DAAL_DATA_TYPE f;
        readNumeric<>( word, f );
        arr[ aux.idx ] = f;
    }

    static void catFunc(const char* word, FeatureAuxData& aux, DAAL_DATA_TYPE* arr)
    {
        std::string sWord(word);

        CategoricalFeatureDictionary *catDict = aux.dsFeat->getCategoricalDictionary();
        CategoricalFeatureDictionary::iterator it = catDict->find( sWord );

        if( it != catDict->end() )
        {
            arr[ aux.idx ] = (DAAL_DATA_TYPE)it->second.first;
            it->second.second++;
        }
        else
        {
            int index = (int)(catDict->size());
            catDict->insert( std::pair<std::string, std::pair<int, int> >( sWord, std::pair<int, int>(index, 1) ) );
            arr[ aux.idx ] = (DAAL_DATA_TYPE)index;
            aux.ntFeat->categoryNumber = index + 1;
        }
    }

protected:
    template<class T>
    static void readNumeric(const char *text, T &f)
    {
        f = daal::services::daal_string_to_float(text, 0);
    }

    static void binFunc(const char* word, FeatureAuxData& aux, DAAL_DATA_TYPE* arr)
    {
        std::string sWord(word);

        CategoricalFeatureDictionary *catDict = aux.dsFeat->getCategoricalDictionary();
        CategoricalFeatureDictionary::iterator it = catDict->find( sWord );

        size_t index = 0;

        if( it != catDict->end() )
        {
            index = it->second.first;
            it->second.second++;
        }
        else
        {
            index = catDict->size();
            catDict->insert( std::pair<std::string, std::pair<int, int> >( sWord, std::pair<int, int>((int)index, 1) ) );
            aux.ntFeat->categoryNumber = index + 1;
        }

        size_t nCats = aux.nCats;

        for(size_t i=0; i<nCats; i++)
        {
            arr[ aux.idx + i ] = (DAAL_DATA_TYPE)(i == index);
        }
    }

    static void nullFunc(const char* word, FeatureAuxData& aux, DAAL_DATA_TYPE* arr) {}

};

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__MAKECATEGORICAL"></a>
 *  \brief Methods of the class to set a feature categorical.
 */
class MakeCategorical : public ModifierIface
{
    size_t idx;
public:
    MakeCategorical(size_t idx) : idx(idx) {}

    virtual void apply(services::Collection<functionT> &funcList, services::Collection<FeatureAuxData> &auxVect) const
    {
        size_t nCols = funcList.size();

        if(idx < nCols)
        {
            funcList[idx] = catFunc;
        }
    }
};

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__MAKECATEBINARY"></a>
 *  \brief Methods of the class to set a feature binary categorical.
 */
class OneHotEncoder : public ModifierIface
{
    size_t idx;
    size_t nCats;
public:
    OneHotEncoder(size_t idx, size_t nCats) : idx(idx), nCats(nCats) {}

    virtual void apply(services::Collection<functionT> &funcList, services::Collection<FeatureAuxData> &auxVect) const
    {
        size_t nCols = funcList.size();

        if(idx < nCols)
        {
            funcList[idx] = binFunc;
            auxVect[idx].nCats = nCats;
            auxVect[idx].wide  = nCats;
        }

        size_t nNTCols = 0;
        for(size_t i=0; i<nCols; i++)
        {
            auxVect[i].idx = nNTCols;
            nNTCols += auxVect[i].wide;
        }
    }
};

/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__COLUMNFILTER"></a>
 *  \brief Methods of the class to filter out data source features from output numeric table.
 */
class ColumnFilter : public ModifierIface
{
    bool oddFlag;
    bool evenFlag;
    bool noneFlag;
    bool listFlag;
    services::Collection<size_t> validList;
public:
    ColumnFilter() : oddFlag(false), evenFlag(false), noneFlag(false), listFlag(false) {}

    ColumnFilter& odd()  { oddFlag=true; return *this;}
    ColumnFilter& even() {evenFlag=true; return *this;}
    ColumnFilter& none() {noneFlag=true; return *this;}
    ColumnFilter& list(services::Collection<size_t> valid) {validList=valid; listFlag=true; return *this;}

    virtual void apply(services::Collection<functionT> &funcList, services::Collection<FeatureAuxData> &auxVect) const
    {
        size_t nCols = funcList.size();

        if( oddFlag )
        {
            for(size_t i=0; i<nCols; i+=2)
            {
                funcList[i] = nullFunc;
                auxVect[i].wide = 0;
            }
        }

        if( evenFlag )
        {
            for(size_t i=1; i<nCols; i+=2)
            {
                funcList[i] = nullFunc;
                auxVect[i].wide = 0;
            }
        }

        if( noneFlag )
        {
            for(size_t i=0; i<nCols; i++)
            {
                funcList[i] = nullFunc;
                auxVect[i].wide = 0;
            }
        }

        if( listFlag )
        {
            services::Collection<bool> flags(nCols);

            for(size_t i=0; i<nCols; i++)
            {
                flags[i] = false;
            }

            for(size_t i=0; i<validList.size(); i++)
            {
                size_t el = validList[i];
                if(el<nCols)
                {
                    flags[el] = true;
                }
            }

            for(size_t i=0; i<nCols; i++)
            {
                if(flags[i]) continue;
                funcList[i] = nullFunc;
                auxVect[i].wide = 0;
            }
        }

        size_t nNTCols = 0;
        for(size_t i=0; i<nCols; i++)
        {
            auxVect[i].idx = nNTCols;
            nNTCols += auxVect[i].wide;
        }
    }
};

namespace interface1
{
/**
 * @defgroup data_sources Data Sources
 * \brief Specifies methods to access data
 * @ingroup data_management
 * @{
 */
/**
 *  <a name="DAAL-CLASS-DATA_MANAGEMENT__CSVFEATUREMANAGER"></a>
 *  \brief Methods of the class to preprocess data represented in the CSV format.
 */
class CSVFeatureManager : public StringRowFeatureManagerIface
{
protected:
    char _delimiter;

    services::Collection<functionT> funcList;
    services::Collection<FeatureAuxData>     auxVect;

public:
    /**
     *  Default constructor
     */
    CSVFeatureManager() : _delimiter(',') {}

    /**
     *  Sets a new character as a delimiter for parsing CSV data (default ',')
     */
    void setDelimiter( char delimiter )
    {
        _delimiter = delimiter;
    }

public:
    size_t getNumericTableNumberOfColumns()
    {
        size_t nDSCols = auxVect.size();
        return auxVect[nDSCols-1].idx + auxVect[nDSCols-1].wide;
    }

    virtual void parseRowAsDictionary( char *rawRowData, size_t rawDataSize,
                                       DataSourceDictionary *dict ) DAAL_C11_OVERRIDE
    {
        char *word = new char[rawDataSize + 1];

        std::list<DataSourceFeature> featureList;

        bool isEmpty = false;
        size_t nCols = 0;
        size_t pos = 0;
        while (true)
        {
            if (rawRowData[pos] == '\0') { break; }
            size_t len = 0;

            while (len < rawDataSize && rawRowData[pos] != _delimiter && rawRowData[pos] != '\0')
            {
                word[len] = rawRowData[pos];
                len++;
                pos++;
            }

            word[len] = '\0';

            if (rawRowData[pos] == _delimiter) pos++;

            DAAL_DATA_TYPE f;
            isEmpty = (word[0] == 0 || word[0] == '\r' || word[0] == '\n');

            if (isEmpty) { break; }

            bool isNumeric = readNumericDetailed<>( word, f );

            DataSourceFeature feat;

            if( isNumeric )
            {
                feat.setType<DAAL_DATA_TYPE>();
            }
            else
            {
                feat.setType<int>();
                feat.ntFeature.featureType = data_feature_utils::DAAL_CATEGORICAL;
            }

            featureList.push_back(feat);

            nCols++;
        }

        delete[] word;

        dict->setNumberOfFeatures(nCols);

        size_t idx = 0;
        for( std::list<DataSourceFeature>::iterator it = featureList.begin() ; it != featureList.end() ; it++ )
        {
            dict->setFeature( *it, idx );
            idx++;
            if( idx == nCols ) { break; }
        }

        initializeFeatureDetails(dict);
    }

    void setFeatureDetailsFromDictionary(DataSourceDictionary* dict)
    {
        initializeFeatureDetails(dict);
    }

    void addModifier( const ModifierIface& modifier )
    {
        modifier.apply( funcList, auxVect );
    }

    /**
     *  Parses a string that represents a feature vector and converts it into a numeric representation
     *  \param[in]  rawRowData   Array of characters with the string that represents the feature vector
     *  \param[in]  rawDataSize  Size of the rawRowData array
     *  \param[in]  dict         Pointer to the dictionary
     *  \param[out] nt           Pointer to a Numeric Table to store the result of parsing
     *  \param[in]  ntRowIndex   Position in the Numeric Table at which to store the result of parsing
     */
    virtual void parseRowIn ( char *rawRowData, size_t rawDataSize, DataSourceDictionary *dict,
                              NumericTable *nt, size_t  ntRowIndex  ) DAAL_C11_OVERRIDE
    {
        size_t dFeatures = auxVect.size();

        char const **words = new char const *[dFeatures];

        if(!words)
        {
            return;
        }

        nt->getBlockOfRows( ntRowIndex, 1, writeOnly, block );
        DAAL_DATA_TYPE *row = block.getBlockPtr();

        size_t pos = 0;
        words[ pos ] = rawRowData;

        for( size_t i=0; i<rawDataSize; i++ )
        {
            if( rawRowData[i] == _delimiter )
            {
                rawRowData[i] = 0;
                pos++;
                if(pos < dFeatures)
                {
                    words[pos] = rawRowData + i + 1;
                }
            }
        }
        rawRowData[rawDataSize] = 0;

        const char* zeroStr = "0";
        for( pos++; pos<dFeatures; pos++ )
        {
            words[pos] = zeroStr;
        }

        for( size_t i = 0; i < dFeatures; i++ )
        {
            funcList[i]( words[i], auxVect[i], row );
        }

        nt->releaseBlockOfRows( block );

        delete[] words;
    }

protected:
    BlockDescriptor<DAAL_DATA_TYPE> block;

    template<class T>
    bool readNumericDetailed(char *text, T &f)
    {
        std::istringstream iss(text);
        iss >> f;
        return !(iss.fail());
    }

    void initializeFeatureDetails(DataSourceDictionary* dict)
    {
        const size_t nCols = dict->getNumberOfFeatures();
        funcList.resize(nCols);
        auxVect.resize(nCols);

        for(size_t i=0; i<nCols; i++)
        {
            if( (*dict)[i].ntFeature.featureType == data_feature_utils::DAAL_CONTINUOUS )
            {
                funcList.push_back( ModifierIface::contFunc );
            }
            else
            {
                funcList.push_back( ModifierIface::catFunc );
            }
            auxVect.push_back( FeatureAuxData() );
            auxVect[i].idx = i;
            auxVect[i].dsFeat = &(*dict)[i];
            auxVect[i].ntFeat = &auxVect[i].dsFeat->ntFeature;
        }
    }
};
/** @} */
} // namespace interface1
using interface1::CSVFeatureManager;

}
}
#endif
