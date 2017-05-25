/* file: mf_sgd_dense_default_batch.h */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Implementation of mf_sgd algorithm and types methods.
//--
*/
#ifndef __MF_SGD_DEFAULT_BATCH__
#define __MF_SGD_DEFAULT_BATCH__

#include <stdlib.h>     
#include <string>
#include <vector>
#include <fstream>
#include <iostream>
#include <unordered_map>
#include <time.h>

#include "mf_sgd_types.h"
#include "service_rng.h"
#include <tbb/cache_aligned_allocator.h>
// #include <hbwmalloc.h> /* use memkind lib for MCDRAM */

using namespace tbb;

namespace daal
{
namespace algorithms
{
namespace mf_sgd
{
namespace interface1
{

template <typename algorithmFPType>
DAAL_EXPORT void Result::allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method)
{/*{{{*/

    const Input *in = static_cast<const Input *>(input);
    const Parameter *par = static_cast<const Parameter *>(parameter);
    
    size_t Dim_r = par->_Dim_r;
    size_t Dim_w = par->_Dim_w;
    size_t Dim_h = par->_Dim_h;

    /* allocate NumericTable of model W and H */
    // if (hbw_check_available() == 0)
    //     allocateImpl_hbw_mem<algorithmFPType>(Dim_r, Dim_w, Dim_h);
    // else
    //     allocateImpl_cache_aligned<algorithmFPType>(Dim_r, Dim_w, Dim_h);

    allocateImpl_cache_aligned<algorithmFPType>(Dim_r, Dim_w, Dim_h);

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::free_mem(size_t r, size_t w, size_t h)
{/*{{{*/

    // if (hbw_check_available() == 0)
    //     freeImpl_hbw_mem<algorithmFPType>(r, w, h);
    // else
    //     freeImpl_cache_aligned<algorithmFPType>(r, w, h);

    freeImpl_cache_aligned<algorithmFPType>(r, w, h);

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::allocateImpl(size_t r, size_t w, size_t h)
{/*{{{*/

    /* allocate model W */
    if (r == 0 || w == 0)
    {
        Argument::set(resWMat, data_management::SerializationIfacePtr());
    }
    else
    {
        algorithmFPType* w_data = (algorithmFPType*)malloc(sizeof(algorithmFPType)*r*w);
        Argument::set(resWMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(w_data, r, w)));
    }

    /* allocate model H */
    if (r == 0 || h == 0)
    {
        Argument::set(resHMat, data_management::SerializationIfacePtr());
    }
    else
    {
        algorithmFPType* h_data = (algorithmFPType*)malloc(sizeof(algorithmFPType)*r*h);
        Argument::set(resHMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(h_data, r, h)));
    }

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::freeImpl(size_t r, size_t w, size_t h)
{/*{{{*/

        data_management::HomogenNumericTable<algorithmFPType>* wMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resWMat).get();
        free(wMat->getArray());

        data_management::HomogenNumericTable<algorithmFPType>* hMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resHMat).get();
        free(hMat->getArray());

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::allocateImpl_cache_aligned(size_t r, size_t w, size_t h)
{/*{{{*/

    /* allocate model W */
    if (r == 0 || w == 0)
    {
        Argument::set(resWMat, data_management::SerializationIfacePtr());
    }
    else
    {
        algorithmFPType* w_data = cache_aligned_allocator<algorithmFPType>().allocate(r*w);
        // algorithmFPType* w_data = (algorithmFPType*) _mm_malloc(r*w*sizeof(algorithmFPType), 64);
        Argument::set(resWMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(w_data, r, w)));
    }

    /* allocate model H */
    if (r == 0 || h == 0)
    {
        Argument::set(resHMat, data_management::SerializationIfacePtr());
    }
    else
    {
        algorithmFPType* h_data = cache_aligned_allocator<algorithmFPType>().allocate(r*h);
        // algorithmFPType* h_data = (algorithmFPType*) _mm_malloc(r*h*sizeof(algorithmFPType), 64);
        Argument::set(resHMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(h_data, r, h)));
    }

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::freeImpl_cache_aligned(size_t r, size_t w, size_t h)
{/*{{{*/

        data_management::HomogenNumericTable<algorithmFPType>* wMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resWMat).get();
        cache_aligned_allocator<algorithmFPType>().deallocate(wMat->getArray(), r*w);
        // _mm_free(wMat->getArray());

        data_management::HomogenNumericTable<algorithmFPType>* hMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resHMat).get();
        cache_aligned_allocator<algorithmFPType>().deallocate(hMat->getArray(), r*h);
        // _mm_free(hMat->getArray());

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::allocateImpl_hbw_mem(size_t r, size_t w, size_t h)
{/*{{{*/

    algorithmFPType* w_data;
    algorithmFPType* h_data;

    /* allocate model W */
    if (r == 0 || w == 0)
    {
        Argument::set(resWMat, data_management::SerializationIfacePtr());
    }
    else
    {
        // algorithmFPType* w_data = (algorithmFPType*)hbw_malloc(sizeof(algorithmFPType)*r*w);
        // int ret = hbw_posix_memalign((void**)&w_data, 64, sizeof(algorithmFPType)*r*w); 
        Argument::set(resWMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(w_data, r, w)));
    }

    /* allocate model H */
    if (r == 0 || h == 0)
    {
        Argument::set(resHMat, data_management::SerializationIfacePtr());
    }
    else
    {
        // algorithmFPType* h_data = (algorithmFPType*)hbw_malloc(sizeof(algorithmFPType)*r*h);
        // int ret = hbw_posix_memalign((void**)&h_data, 64, sizeof(algorithmFPType)*r*h); 
        Argument::set(resHMat, data_management::SerializationIfacePtr(
                          new data_management::HomogenNumericTable<algorithmFPType>(h_data, r, h)));
    }

}/*}}}*/

template <typename algorithmFPType>
DAAL_EXPORT void Result::freeImpl_hbw_mem(size_t r, size_t w, size_t h)
{/*{{{*/

        data_management::HomogenNumericTable<algorithmFPType>* wMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resWMat).get();
        // hbw_free(wMat->getArray());

        data_management::HomogenNumericTable<algorithmFPType>* hMat = (data_management::HomogenNumericTable<algorithmFPType>*)Argument::get(resHMat).get();
        // hbw_free(hMat->getArray());

}/*}}}*/

template <typename algorithmFPType>
void Input::generate_points(const int64_t num_Train,
						    const int64_t num_Test, 
						    const int64_t row_num_w, 
						    const int64_t col_num_h,
						    mf_sgd::VPoint<algorithmFPType>* points_Train,
						    mf_sgd::VPoint<algorithmFPType>* points_Test)
{/*{{{*/
	
	daal::internal::UniformRng<algorithmFPType, daal::sse2> rng(time(0));
    algorithmFPType value = 0;

	int64_t counts_train = 0;
	int64_t counts_test = 0;

	for(int64_t i=0;i<row_num_w;++i)
	{
		for (int64_t j=0;j<col_num_h;++j) 
		{
			if (i == j)
			{
				/* put diagonal item into train dataset */
				if (counts_train < num_Train)
				{
					points_Train[counts_train].wPos = i;
					points_Train[counts_train].hPos = j;

                    rng.uniform(1, 0.0, 1.0, &value);
					points_Train[counts_train].val = 10.0*value;
					counts_train++;
				}
			}
			else
			{
				if ( ((algorithmFPType)rand())/RAND_MAX > 0.2)
				{
					/* put item into train dataset */
					if (counts_train < num_Train)
					{
						points_Train[counts_train].wPos = i;
						points_Train[counts_train].hPos = j;
                        
                        rng.uniform(1, 0.0, 1.0, &value);
					    points_Train[counts_train].val = 10.0*value;
						counts_train++;
					}
					else if (counts_test < num_Test)
					{
						points_Test[counts_test].wPos = i;
						points_Test[counts_test].hPos = j;

                        rng.uniform(1, 0.0, 1.0, &value);
						points_Test[counts_test].val = 10.0*value;
						counts_test++;
					}
				}
				else
				{
					/* put item into test dataset */
					if (counts_test < num_Test)
					{
						points_Test[counts_test].wPos = i;
						points_Test[counts_test].hPos = j;

                        rng.uniform(1, 0.0, 1.0, &value);
						points_Test[counts_test].val = 10.0*value;
						counts_test++;
					}
					else if (counts_train < num_Train)
					{
						points_Train[counts_train].wPos = i;
						points_Train[counts_train].hPos = j;

                        rng.uniform(1, 0.0, 1.0, &value);
						points_Train[counts_train].val = 10.0*value;
						counts_train++;
					}
				}
			}
		}
	}


}/*}}}*/

template <typename algorithmFPType>
int64_t Input::loadData(const std::string filename, std::vector<int64_t>* lineContainer, 
				    std::unordered_map<long, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map)
{/*{{{*/
    
    int64_t row_id = 0;
    int64_t col_id = 0;
    algorithmFPType val = 0;
    int64_t num_points = 0;
    std::string line;
    std::ifstream infile(filename);

    while (std::getline(infile, line)) 
    {
        infile >> row_id >> col_id >> val;
        /* processing a line */
        mf_sgd::VPoint<algorithmFPType>* item = new mf_sgd::VPoint<algorithmFPType>();
        item->wPos = row_id;
        item->hPos = col_id;
        item->val = val;

        num_points++;
        if (map.find(row_id) == map.end())
        {
            /* not found row id */
            std::vector<mf_sgd::VPoint<algorithmFPType>*>* vec = new std::vector<mf_sgd::VPoint<algorithmFPType>*>();
            vec->push_back(item);
            map[row_id] = vec;

            if (lineContainer != NULL)
                lineContainer->push_back(row_id);
        }
        else
            map[row_id]->push_back(item);

    }

    infile.close();
	return num_points;

}/*}}}*/

template <typename algorithmFPType>
void Input::convert_format(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_train,
						std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_test,
						const int64_t num_Train,
					    const int64_t num_Test,
						mf_sgd::VPoint<algorithmFPType>* points_Train,  
						mf_sgd::VPoint<algorithmFPType>* points_Test, 
						int64_t &row_num_w, 
						int64_t &col_num_h,
                        size_t &absent_num_test)
{/*{{{*/

    std::unordered_map<int64_t, int64_t> vMap_row_w;
    std::unordered_map<int64_t, int64_t> vMap_col_h;

    int64_t row_pos_itr = 0;
    int64_t col_pos_itr = 0;

    int64_t row_pos = 0;
    int64_t col_pos = 0;
    int64_t entry_itr = 0;

    int64_t row_id = 0;
    int64_t col_id = 0;
    algorithmFPType val = 0;

    typename std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*>::iterator it_map;
    typename std::vector<mf_sgd::VPoint<algorithmFPType>*>::iterator it_vec;

    /* iteration over train data map */
    for (it_map = map_train.begin(); it_map != map_train.end(); ++it_map) 
    {
        if (it_map->second->empty() == false)
        {
            for(it_vec = it_map->second->begin(); it_vec < it_map->second->end();++it_vec)
            {
                row_id = (*it_vec)->wPos;
                col_id = (*it_vec)->hPos;
                val = (*it_vec)->val;

                if (vMap_row_w.find(row_id) == vMap_row_w.end())
                {
                    /* not found row id */
                    vMap_row_w[row_id] = row_pos_itr;
                    row_pos = row_pos_itr;
                    row_pos_itr++;
                }
                else
                    row_pos = vMap_row_w[row_id];

                if (vMap_col_h.find(col_id) == vMap_col_h.end())
                {
                    /* not found col id */
                    vMap_col_h[col_id] = col_pos_itr;
                    col_pos = col_pos_itr;
                    col_pos_itr++;
                }
                else
                    col_pos = vMap_col_h[col_id];

                points_Train[entry_itr].wPos = row_pos;
                points_Train[entry_itr].hPos = col_pos;
                points_Train[entry_itr].val = val;
                entry_itr++;
            }
        }
    }

    row_num_w = row_pos_itr;
    col_num_h = col_pos_itr;

    /* iteration over test data map */
    entry_itr = 0;
    int is_absent = 0;
    for (it_map = map_test.begin(); it_map != map_test.end(); ++it_map) 
    {
        if (it_map->second->empty() == false)
        {
            for(it_vec = it_map->second->begin(); it_vec < it_map->second->end();++it_vec)
            {
                row_id = (*it_vec)->wPos;
                col_id = (*it_vec)->hPos;
                val = (*it_vec)->val;

                if (vMap_row_w.find(row_id) == vMap_row_w.end())
                {
                    /* not found row id */
                    row_pos = -1;
                    is_absent++;
                }
                else
                    row_pos = vMap_row_w[row_id];

                if (vMap_col_h.find(col_id) == vMap_col_h.end())
                {
                    /* not found col id */
                    col_pos = -1;
                    is_absent++;
                }
                else
                    col_pos = vMap_col_h[col_id];

                if (is_absent != 0)
                {
                    is_absent = 0;
                    absent_num_test++;
                }

                points_Test[entry_itr].wPos = row_pos;
                points_Test[entry_itr].hPos = col_pos;
                points_Test[entry_itr].val = val;
                entry_itr++;
            }
        }
    }

}/*}}}*/

template <typename algorithmFPType>
void Input::freeData(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map)
{/*{{{*/
        typename std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*>::iterator it_map;
        typename std::vector<mf_sgd::VPoint<algorithmFPType>*>::iterator it_vec;
   
        for (it_map = map.begin(); it_map != map.end(); ++it_map) 
        {
            if (it_map->second->empty() == false)
            {
                for(it_vec = it_map->second->begin(); it_vec < it_map->second->end();++it_vec)
                {
                    delete *it_vec;
                }
                delete it_map->second;
            }
        }

        map.clear();
}/*}}}*/

}// namespace interface1
}// namespace mf_sgd
}// namespace algorithms
}// namespace daal

#endif
