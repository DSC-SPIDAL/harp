/* file: mf_sgd_types.h */
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
//  Definition of mf_sgd common types.
//--
*/


#ifndef __MF_SGD_TYPES_H__
#define __MF_SGD_TYPES_H__

#include <string>
#include <vector>
#include <unordered_map>

#include "algorithms/algorithm.h"
#include "data_management/data/numeric_table.h"
#include "data_management/data/homogen_numeric_table.h"
#include "services/daal_defines.h"

#include "tbb/concurrent_hash_map.h"

namespace daal
{

const int SERIALIZATION_MF_SGD_RESULT_ID = 106000; 
const int SERIALIZATION_MF_SGD_DISTRI_PARTIAL_RESULT_ID = 106100; 

namespace algorithms
{

/**
* @defgroup Matrix factorization Recommender System by using Standard SGD 
* \copydoc daal::algorithms::mf_sgd
* @ingroup mf_sgd
* @{
*/
/** \brief Contains classes for computing the results of the mf_sgd algorithm */
namespace mf_sgd
{
/**
 * <a name="DAAL-ENUM-ALGORITHMS__mf_sgd__METHOD"></a>
 * Available methods for computing the mf_sgd algorithm
 */
enum Method
{
    defaultSGD    = 0 /*!< Default Standard SGD */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__mf_sgd__INPUTID"></a>
 * Available types of input objects for the mf_sgd decomposition algorithm
 */
enum InputId
{
    dataTrain = 0,		  /*!< Training Dataset */
	dataTest = 1,	      /*!< Test Dataset */
	wPos = 2,	          /*!< array of row position in model W of dataset, used in distributed mode */
	hPos = 3,	          /*!< array of col position in model H of dataset, used in distributed mode */
	val = 4,				  /*!< array of val of dataset, used in distributed mode */
    wPosTest = 5,
    hPosTest = 6,
    valTest = 7
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__mf_sgd__RESULTID"></a>
 * Available types of results of the mf_sgd algorithm
 */
enum ResultId
{
    resWMat = 0,   /*!< Model W */
    resHMat = 1    /*!< Model H */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__mf_sgd__DISTRIBUTED_RESULTID"></a>
 * Available types of partial results of the mf_sgd algorithm
 */
enum DistributedPartialResultId
{
    presWMat = 0,   /*!< Model W, used in distributed mode */
    presHMat = 1,   /*!< Model H, used in distributed mode*/
    presRMSE = 2,   /*!< RMSE computed from test dataset */
    presWData = 3
};

/**
 * @brief A struct to store sparse matrix data from CSV file 
 */
template<typename interm>
struct VPoint 
{
    int64_t wPos;		/*!< row position in model W of training data point */
    int64_t hPos;		/*!< col position in model H of training data point */
    interm val;			/*!< value of training data */
};


/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
/**
 * <a name="DAAL-CLASS-ALGORITHMS__mf_sgd__INPUT"></a>
 * \brief Input objects for the mf_sgd algorithm in the batch and distributed modes 
 * algorithm.
 */
class DAAL_EXPORT Input : public daal::algorithms::Input
{
public:
    /** Default constructor */
    Input();
    /** Default destructor */
    virtual ~Input() {}

    /**
     * Returns input object of the mf_sgd algorithm
     * \param[in] id    Identifier of the input object
     * \return          Input object that corresponds to the given identifier
     */
    data_management::SerializationIfacePtr get(InputId id) const;

    /**
     * Sets input object for the mf_sgd algorithm
     * \param[in] id    Identifier of the input object
     * \param[in] value Pointer to the input object
     */
    void set(InputId id, const data_management::SerializationIfacePtr &value);

	/**
	 * @brief get the column num of NumericTable associated to an inputid
	 *
	 * @param[in] id of input table
	 * @return column num of input table 
	 */
    // size_t getNumberOfColumns(InputId id) const;

	/**
	 * @brief get the column num of NumericTable associated to an inputid
	 *
	 * @param[in]  id of input table
	 *
	 * @return row num of input table 
	 */
    // size_t getNumberOfRows(InputId id) const;

    daal::services::interface1::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

	/**
	 * @brief A generator for examples to create the training and testing datasets
	 * implemented in mf_sgd_default_batch.h
	 *
	 * @tparam algorithmFPType, float or double
	 * @param[in] num_Train
	 * @param[in] num_Test
	 * @param[in] row_num_w
	 * @param[in] col_num_h
	 * @param[in,out] points_Train
	 * @param[in,out] points_Test
	 */
    // template <typename algorithmFPType>
    // void generate_points(const int64_t num_Train,
	// 					 const int64_t num_Test, 
	// 					 const int64_t row_num_w, 
	// 					 const int64_t col_num_h,
	// 					 mf_sgd::VPoint<algorithmFPType>* points_Train,
	// 					 mf_sgd::VPoint<algorithmFPType>* points_Test);
                                                                                
	/**
	 * @brief load data from CSV files 
	 * implemented in mf_sgd_default_batch.h
	 *
	 * @tparam algorithmFPType, float or double
	 * @param[in] filename
	 * @param[out] lineContainer An array to contain all the row ids of data points used in ksnc mode
	 * @param[in,out] map A map to store all the data points 
	 *
	 * @return the number of loaded data points  
	 */
    // template <typename algorithmFPType>
	// int64_t loadData(const std::string filename, std::vector<int64_t>* lineContainer, 
	// 			    std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map);

	/**
	 * @brief Convert loaded data from CSV files into mf_sgd::VPoint format
	 * implemented in mf_sgd_default_batch.h
	 *
	 * @tparam algorithmFPType, float or double
	 * @param[in] map_train loaded train data from function loadData
	 * @param[in] map_test loaded test data from function loadData
	 * @param[in] num_Train num of train points
	 * @param[in] num_Test num of test points
	 * @param[in,out] points_Train allocated array of points that stores VPoints converted from loaded data
	 * @param[in,out] points_Test allocated array of points that stores VPoints converted from loaded data
	 * @param[out] row_num_w row num of W model for train data
	 * @param[out] col_num_h col num of H model for train data 
	 * @param[out] absent_num_test num of test points whose row or column are not included in Training dataset 
	 */
	// template <typename algorithmFPType>
    // void convert_format(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_train,
	// 					std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_test,
	// 					const int64_t num_Train,
	// 				    const int64_t num_Test,
	// 					mf_sgd::VPoint<algorithmFPType>* points_Train,  
	// 					mf_sgd::VPoint<algorithmFPType>* points_Test, 
	// 					int64_t &row_num_w, 
	// 					int64_t &col_num_h,
    //                     size_t &absent_num_test);
     /**
	 * @brief Convert loaded data from CSV files into mf_sgd::VPoint format
	 * implemented in mf_sgd_default_distri.h
	 *
	 * @tparam algorithmFPType, float or double
	 * @param[in] map_train loaded train data from function loadData
	 * @param[in] num_Train num of train points
	 * @param[in,out] points_Train allocated array of points that stores VPoints converted from loaded data
	 * @param[out] row_num_w row num of W model for train data
	 * @param[out] col_num_h col num of H model for train data 
	 */
    template <typename algorithmFPType>
    void convert_format_distri(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map_train,
						const int64_t num_Train,
						mf_sgd::VPoint<algorithmFPType>* points_Train,  
						int64_t &row_num_w, 
						int64_t &col_num_h);

	/**
	 * @brief free the allocated data of mf_sgd::VPoint
	 * implemented in mf_sgd_default_batch.h
	 *
	 * @tparam algorithmFPType
	 * @param map
	 */
	// template <typename algorithmFPType>
	// void freeData(std::unordered_map<int64_t, std::vector<mf_sgd::VPoint<algorithmFPType>*>*> &map);
};



/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__RESULT"></a>
 * \brief Provides methods to access results obtained with the compute() method of the mf_sgd decomposition algorithm
 *        in the batch processing mode 
 */
class DAAL_EXPORT Result : public daal::algorithms::Result
{
public:

    /** Default constructor */
    Result();
    /** Default destructor */
    virtual ~Result() {}

    /**
     * Returns the result of the mf_sgd decomposition algorithm
     * \param[in] id    Identifier of the result
     * \return          Result that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(ResultId id) const;

    /**
     * Allocates memory for storing final results of the mf_sgd decomposition algorithm
     * implemented in mf_sgd_default_batch.h
     * \param[in] input     Pointer to input object
     * \param[in] parameter Pointer to parameter
     * \param[in] method    Algorithm method
     */
    // template <typename algorithmFPType>
    // DAAL_EXPORT void allocate(const daal::algorithms::Input *input, const daal::algorithms::Parameter *parameter, const int method);


    // template <typename algorithmFPType>
    // DAAL_EXPORT void free_mem(size_t r, size_t w, size_t h);

    /**
    * Sets an input object for the mf_sgd decomposition algorithm
    * \param[in] id    Identifier of the result
    * \param[in] value Pointer to the result
    */
    void set(ResultId id, const data_management::NumericTablePtr &value);

    /**
       * Checks final results of the algorithm
      * \param[in] input  Pointer to input objects
      * \param[in] par    Pointer to parameters
      * \param[in] method Computation method
      */
    daal::services::interface1::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

    /**
     * Allocates memory for storing final results of the mf_sgd decomposition algorithm
     * \tparam     algorithmFPType float or double 
     * \param[in]  r  dimension of feature vector, num col of model W and num row of model H 
     * \param[in]  w  Number of rows in the model W 
     * \param[in]  h  Number of cols in the model H 
     */
    // template <typename algorithmFPType>
    // DAAL_EXPORT void allocateImpl(size_t r, size_t w, size_t h);
    //
    // template <typename algorithmFPType>
    // DAAL_EXPORT void freeImpl(size_t r, size_t w, size_t h);
    //
    // template <typename algorithmFPType>
    // DAAL_EXPORT void allocateImpl_cache_aligned(size_t r, size_t w, size_t h);
    //
    // template <typename algorithmFPType>
    // DAAL_EXPORT void freeImpl_cache_aligned(size_t r, size_t w, size_t h);
    //
    // template <typename algorithmFPType>
    // DAAL_EXPORT void allocateImpl_hbw_mem(size_t r, size_t w, size_t h);
    //
    // template <typename algorithmFPType>
    // DAAL_EXPORT void freeImpl_hbw_mem(size_t r, size_t w, size_t h);


protected:
    /** \private */

    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);
        return services::Status();
    }

    /**
    *  Serializes the object
    *  \param[in]  arch  Storage for the serialized object or data structure
    */
    services::Status serializeImpl(data_management::InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(arch);
        return services::Status();
    }

    /**
    *  Deserializes the object
    *  \param[in]  arch  Storage for the deserialized object or data structure
    */
    services::Status deserializeImpl(const data_management::OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(arch);
        return services::Status();
    }

};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__mf_sgd__RESULT"></a>
 * \brief Provides methods to access results obtained with the compute() method of the mf_sgd decomposition algorithm
 *        in the batch processing mode or finalizeCompute() method of algorithm in the online processing mode
 *        or on the second and third steps of the algorithm in the distributed processing mode
 */
class DAAL_EXPORT DistributedPartialResult : public daal::algorithms::PartialResult
{
public:

    // DECLARE_SERIALIZABLE_CAST(DistributedPartialResult);

    /** Default constructor */
    DistributedPartialResult();
    /** Default destructor */
    virtual ~DistributedPartialResult() {}

    /**
     * Returns the result of the mf_sgd decomposition algorithm 
     * \param[in] id    Identifier of the result
     * \return          Result that corresponds to the given identifier
     */
	data_management::NumericTablePtr get(DistributedPartialResultId id) const;

    /**
     * Sets Result object to store the result of the mf_sgd decomposition algorithm
     * \param[in] id    Identifier of the result
     * \param[in] value Pointer to the Result object
     */
    void set(DistributedPartialResultId id, const data_management::NumericTablePtr &value);


	/**
	 * Checks partial results of the algorithm
	 * \param[in] parameter Pointer to parameters
	 * \param[in] method Computation method
	 */
    daal::services::interface1::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    /**
      * Checks final results of the algorithm
      * \param[in] input      Pointer to input objects
      * \param[in] parameter  Pointer to parameters
      * \param[in] method     Computation method
      */
    daal::services::interface1::Status check(const daal::algorithms::Input* input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        daal::algorithms::PartialResult::serialImpl<Archive, onDeserialize>(arch);
        return services::Status();
    }

    /**
    *  Serializes the object
    *  \param[in]  arch  Storage for the serialized object or data structure
    */
    services::Status serializeImpl(data_management::InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(arch);
        return services::Status();
    }

    /**
    *  Deserializes the object
    *  \param[in]  arch  Storage for the deserialized object or data structure
    */
    services::Status deserializeImpl(const data_management::OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(arch);
        return services::Status();
    }
};

/**
 * <a name="DAAL-STRUCT-ALGORITHMS__mf_sgd__PARAMETER"></a>
 * \brief Parameters for the mf_sgd decomposition compute method
 * used in both of batch mode and distributed mode
 */
struct DAAL_EXPORT Parameter : public daal::algorithms::Parameter
{

    typedef tbb::concurrent_hash_map<int, int> ConcurrentModelMap;
    typedef tbb::concurrent_hash_map<int, std::vector<int> > ConcurrentDataMap;

	/* default constructor */
    Parameter() 
    {
        _learningRate = 0.0;
        _lambda = 0.0;
        _Dim_r = 10;
        _Dim_w = 10;
        _Dim_h = 10;
        _iteration = 10;
        _thread_num = 0;
        _tbb_grainsize = 0;
        _Avx_explicit = 0;
        _ratio = 1.0;
        _itr = 0;
        _innerItr = 0;
        _innerNum = 0;
        _isTrain = 1;
        _timeout = 0;
        _absent_test_num = 0;
        _reorder_length = 0;
        _isReorder = 0;
        _wMat_map = NULL;
        _hMat_map = NULL;
        _train_map = NULL;
        _test_map = NULL;

        _train_list = NULL;
        _train_list_ids = NULL;
        _train_sub_len = NULL;
        _train_list_len = 0;
        _train_col_num = 0;

        _test_list = NULL;
        _test_list_ids = NULL;
        _test_sub_len = NULL;
        _test_list_len = 0;
        _test_col_num = 0;

        _sgd2 = 0;
        _testV=0;
        _trainedNumV = 0;
        _wMatFinished = 0;
        _trainMapFinished = 0;
        _testMapFinished = 0;
    
        _compute_task_time = 0;
        _itrTimeStamp = 0;
        _jniDataConvertTime = 0;
    }

    virtual ~Parameter() {}

	/**
	 * @brief set the parameters in both of batch mode and distributed mode 
	 *
	 * @param learningRate
	 * @param lambda
	 * @param Dim_r
	 * @param Dim_w
	 * @param Dim_h
	 * @param iteration
	 * @param thread_num
	 * @param tbb_grainsize
	 * @param Avx_explicit
	 */
    void setParameter(double learningRate, double lambda, size_t Dim_r, size_t Dim_w, size_t Dim_h, size_t iteration, size_t thread_num, size_t tbb_grainsize, size_t Avx_explicit)
    {
        _learningRate = learningRate;
        _lambda = lambda;
        _Dim_r = Dim_r;
        _Dim_w = Dim_w;
        _Dim_h = Dim_h;
        _iteration = iteration;
        _thread_num = thread_num;
        _tbb_grainsize = tbb_grainsize;
        _Avx_explicit = Avx_explicit;
    }

	/**
	 * @brief set the ratio of executed tasks by all the tasks
	 *
	 * @param ratio
	 */
    void setRatio(double ratio)
    {
        _ratio = ratio;
    }

	/**
	 * @brief set the iteration id, 
	 * used in distributed mode
	 *
	 * @param itr
	 */
    void setIteration(size_t itr)
    {
        _itr = itr;
    }

	/**
	 * @brief set the inner iteration id
	 * used in distributed mode (e.g., model rotation)
	 *
	 * @param innerItr
	 */
    void setInnerItr(size_t innerItr)
    {
        _innerItr = innerItr;
    }

	/**
	 * @brief set the total inner iteration number
	 * used in distributed mode (e.g., model rotation)
	 *
	 * @param innerNum
	 */
    void setInnerNum(size_t innerNum)
    {
        _innerNum = innerNum;
    }

    /**
     * @brief set whether Train or Test tasks
     * used in distributed mode
     *
     * @param isTrain
     */
    void setIsTrain(int isTrain)
    {
        _isTrain = isTrain;
    }

    /**
     * @brief set up the timer in distributed mode
     *
     * @param timeout in seconds
     */
    void setTimer(double timeout)
    {
        _timeout = timeout;
    }

    /**
     * @brief set up the absent num of test points
     *
     * @param absent_test_num
     */
    void setAbsentTestNum(size_t absent_test_num)
    {
        _absent_test_num = absent_test_num;
    }

    void setTestV(size_t testV)
    {
        _testV = testV;
    }

    /**
     * @brief set up the length of queue in reorder mode
     *
     * @param reorder_length
     */
    void setReOrderLength(int reorder_length)
    {
        _reorder_length = reorder_length;
    }

    /**
     * @brief set up whether to use reorder data points or not
     * 1: TBB with re-order
     * 2: OpenMP with re-order
     * 3: OpenMP -no-re-order
     * default: TBB -no-re-order
     * @param isReorder
     */
    void setIsReorder(int isReorder)
    {
        _isReorder = isReorder;
    }

    /**
     * @brief free up the user allocated memory
     */
    void freeData()
    {
        if (_wMat_map != NULL)
            delete _wMat_map;

        if (_hMat_map != NULL)
            delete _hMat_map;

        if (_train_map != NULL)
            delete _train_map;

        if (_test_map != NULL)
            delete _test_map;

        if (_train_list != NULL)
        {
            for(int k=0;k<_train_list_len;k++)
                delete[] _train_list[k];

            delete[] _train_list;
        }

        if (_train_list_ids != NULL)
            delete[] _train_list_ids;

        if (_train_sub_len != NULL)
            delete[] _train_sub_len;

        if (_test_list != NULL)
        {
            for(int k=0;k<_test_list_len;k++)
                delete[] _test_list[k];

            delete[] _test_list;
        }

        if (_test_list_ids != NULL)
            delete[] _test_list_ids;

        if (_test_sub_len != NULL)
            delete[] _test_sub_len;

    }

    double		_learningRate;                    /* the rate of learning by SGD  */
    double		_lambda;                          /* the lambda parameter in standard SGD */
    double      _ratio;                           /* control the percentage of tasks to execute */
    size_t      _Dim_r;                           /* the feature dimension of model W and H */
    size_t      _Dim_w;                           /* the row num of model W */
    size_t      _Dim_h;                           /* the column num of model H */
    size_t      _iteration;                       /* the iterations of SGD */
    size_t      _thread_num;                      /* specify the threads used by TBB */
    size_t      _tbb_grainsize;                   /* specify the grainsize for TBB parallel_for */
    size_t      _Avx_explicit;                    /* specify whether use explicit Avx instructions  */
    size_t      _itr;                             /* id of training iteration, used in distributed mode */
    size_t      _innerItr;						  /* id of inner training iteration, used in distributed mode, e.g., model rotation  */
    size_t      _innerNum;						  /* total num of inner training iteration, used in distributed mode, e.g., model rotation */
    int         _isTrain;                         /* used in distributed mode, 1 for training task, 0 for test task */
    double      _timeout;                         /* timer to control the actual execution time for each threads */
    size_t      _absent_test_num;                 /* num of test points whose row and col id not included in training dataset */
    int         _reorder_length;                  /* length of queue in reorder mode */
    int         _isReorder;                       /* 1 (true) if it uses reorder mode */
    ConcurrentModelMap*  _wMat_map;                /* concurrent hashmap for storing index of W Matrix */
    ConcurrentModelMap*  _hMat_map;                /* concurrent hashmap for storing index of H Matrix */
    ConcurrentDataMap*   _train_map;               /* hashmap to hold the training data indexed by col id */
    ConcurrentDataMap*   _test_map;                /* hashmap to hold the training data indexed by col id */
    int         _sgd2;                            /* 0 default sgd method, 1 the second sgd method */
    size_t      _testV;                           /* the actual computed data points from test dataset */
    long        _trainedNumV;                     /* the trained num of training points if using timer */
    int         _wMatFinished;
    int         _trainMapFinished;
    int         _testMapFinished;
    int**       _train_list;
    int*        _train_list_ids;
    int*        _train_sub_len;
    size_t      _train_list_len;
    int         _train_col_num;
    int**       _test_list;
    int*        _test_list_ids;
    int*        _test_sub_len;
    size_t      _test_list_len;
    int         _test_col_num;
    size_t      _compute_task_time;             /* record the time spent in parallel training points update by each iteration, used in timer setup */
    size_t      _itrTimeStamp;                  /* timestamp for each iteration, used in the convergence analysis */
    size_t      _jniDataConvertTime;            /* time spent in each iteration in converting data from java table to native memory space */

};
/** @} */
/** @} */
} // namespace interface1
using interface1::Input;
using interface1::Result;
using interface1::DistributedPartialResult;
using interface1::Parameter;

} // namespace daal::algorithms::mf_sgd
} // namespace daal::algorithms
} // namespace daal

#endif
