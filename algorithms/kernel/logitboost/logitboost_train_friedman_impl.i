/* file: logitboost_train_friedman_impl.i */
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
//  Common functions for Logit Boost model training
//--
*/
/*
//
//  REFERENCES
//
//  1. J. Friedman, T. Hastie, R. Tibshirani.
//     Additive logistic regression: a statistical view of boosting,
//     The annals of Statistics, 2000, v28 N2, pp. 337-407
//  2. J. Friedman, T. Hastie, R. Tibshirani.
//     The Elements of Statistical Learning:
//     Data Mining, Inference, and Prediction,
//     Springer, 2001.
//
*/

#ifndef __LOGITBOOST_TRAIN_FRIEDMAN_IMPL_I__
#define __LOGITBOOST_TRAIN_FRIEDMAN_IMPL_I__

#include "threading.h"
#include "service_memory.h"
#include "service_numeric_table.h"
#include "service_data_utils.h"
#include "service_threading.h"
#include "logitboost_impl.i"
#include "logitboost_train_friedman_aux.i"

using namespace daal::algorithms::logitboost::internal;
using namespace daal::internal;

namespace daal
{
namespace algorithms
{
namespace logitboost
{
namespace training
{
namespace internal
{

template<typename algorithmFPType, CpuType cpu>
struct LogitBoostLs
{
    typedef typename daal::internal::HomogenNumericTableCPU<algorithmFPType, cpu> HomogenNT;
    typedef typename services::SharedPtr<HomogenNT> HomogenNTPtr;
    typedef typename daal::services::SharedPtr<daal::algorithms::weak_learner::training::Batch> TrainLernerPtr;
    typedef typename daal::services::SharedPtr<daal::algorithms::weak_learner::prediction::Batch> PredictLernerPtr;

    DAAL_NEW_DELETE();
    HomogenNTPtr wArray;
    HomogenNTPtr zArray;

    LogitBoostLs(const size_t n): _nRows(n), _isInit(false) {}

    services::Status allocate(NumericTablePtr& x, TrainLernerPtr& train, PredictLernerPtr& predict)
    {
        services::Status status;
        if (!_isInit)
        {
            _learnerTrain = train->clone();
            _learnerPredict = predict->clone();
            wArray = HomogenNT::create(1, _nRows, &status);
            zArray = HomogenNT::create(1, _nRows, &status);

            _predictionRes.reset(new classifier::prediction::Result());

            classifier::training::Input *input = _learnerTrain->getInput();
            classifier::prediction::Input *predInput = _learnerPredict->getInput();
            if (!input || !predInput) status.add(services::ErrorNullInput);
            else
            {
                input->set(classifier::training::labels,  zArray);
                input->set(classifier::training::weights, wArray);
                input->set(classifier::training::data,    x);
                predInput->set(classifier::prediction::data, x);
            }
        }
        _isInit = true;
        return status;
    }

    services::Status run(const size_t classIdx, data_management::DataCollection& models,
            TArray<algorithmFPType, cpu>& pred)
    {
        _learnerTrain->resetResult();
        services::Status status = _learnerTrain->computeNoThrow();
        DAAL_CHECK_STATUS_VAR(status);

        classifier::training::ResultPtr trainingRes = _learnerTrain->getResult();
        weak_learner::ModelPtr learnerModel =
                services::staticPointerCast<weak_learner::Model, classifier::Model>(trainingRes->get(classifier::training::model));
        models[classIdx] = learnerModel;

        classifier::prediction::Input *predInput = _learnerPredict->getInput();
        DAAL_CHECK(predInput, services::ErrorNullInput);
        predInput->set(classifier::prediction::model, learnerModel);

        HomogenNTPtr predTable(HomogenNT::create(pred.get() + classIdx * _nRows, 1, _nRows, &status));
        DAAL_CHECK_STATUS_VAR(status);

        _predictionRes->set(classifier::prediction::prediction, predTable);
        status = _learnerPredict->setResult(_predictionRes);
        DAAL_CHECK_STATUS_VAR(status);

        status = _learnerPredict->computeNoThrow();
        return status;
    }

private:
    TrainLernerPtr _learnerTrain;
    PredictLernerPtr _learnerPredict;
    classifier::prediction::ResultPtr _predictionRes;

    const size_t _nRows;
    bool _isInit;
};

template<typename algorithmFPType, CpuType cpu>
services::Status LogitBoostTrainKernel<friedman, algorithmFPType, cpu>::compute(const size_t na, NumericTablePtr a[], Model *r, const Parameter *par)
{
    const algorithmFPType zero(0.0);
    const algorithmFPType fp_one(1.0);
    Parameter *parameter = const_cast<Parameter *>(par);
    NumericTablePtr x = a[0];
    NumericTablePtr y = a[1];
    r->setNFeatures(x->getNumberOfColumns());

    algorithmFPType acc = parameter->accuracyThreshold;
    const size_t M = parameter->maxIterations;
    const size_t nc = parameter->nClasses;
    const algorithmFPType thrW = (algorithmFPType)(parameter->weightsDegenerateCasesThreshold);
    const algorithmFPType thrZ = (algorithmFPType)(parameter->responsesDegenerateCasesThreshold);
    const size_t dim = x->getNumberOfColumns();
    const size_t n = x->getNumberOfRows();

    TArray<algorithmFPType, cpu> pred(n * nc);
    TArray<algorithmFPType, cpu> F(n * nc);
    TArray<algorithmFPType, cpu> Fbuf(nc);
    TArray<algorithmFPType, cpu> P(n * nc);

    DAAL_CHECK(pred.get() && F.get() && P.get() && Fbuf.get(), services::ErrorMemoryAllocationFailed);

    services::Status s;
    HomogenNTPtr wTable(HomogenNT::create(1, n, &s));
    DAAL_CHECK_STATUS_VAR(s);
    HomogenNTPtr zTable(HomogenNT::create(1, n, &s));
    DAAL_CHECK_STATUS_VAR(s);

    algorithmFPType *w = wTable->getArray();
    algorithmFPType *z = zTable->getArray();

    const algorithmFPType inv_n = fp_one / (algorithmFPType)n;
    const algorithmFPType inv_nc = fp_one / (algorithmFPType)nc;

    /* Initialize weights, probs and additive function values.
       Step 1) of the Algorithm 6 from [1] */
    for ( size_t i = 0; i < n; i++ ) { w[i] = inv_n; }
    for ( size_t i = 0; i < n * nc; i++ ) { P[i] = inv_nc; }
    algorithmFPType logL = -algorithmFPType(n)*daal::internal::Math<algorithmFPType, cpu>::sLog(inv_nc);
    algorithmFPType accCur = daal::services::internal::MaxVal<algorithmFPType>::get();

    for (size_t i = 0; i < n * nc; i++)
    {
        F[i] = zero;
    }

    ReadColumns<int, cpu> yCols(*y, 0, 0, n);
    DAAL_CHECK_BLOCK_STATUS(yCols);
    const int *y_label = yCols.get();
    DAAL_ASSERT(y_label);

    services::SharedPtr<weak_learner::training::Batch> learnerTrain = parameter->weakLearnerTraining;
    services::SharedPtr<weak_learner::prediction::Batch> learnerPredict = parameter->weakLearnerPrediction;

    /* Clear the collection of weak learners models in the boosting model */
    r->clearWeakLearnerModels();
    data_management::DataCollection models(nc);

    SafeStatus safeStat;
    daal::ls<LogitBoostLs<algorithmFPType, cpu> *> lsData([&]()
    {
        auto ptr = new LogitBoostLs<algorithmFPType, cpu>(n);
        if(!ptr)
        {
            safeStat.add(services::ErrorMemoryAllocationFailed);
        }
        return ptr;
    });

    /* Repeat for m = 0, 1, ..., M-1
       Step 2) of the Algorithm 6 from [1] */
    for ( size_t m = 0; m < M; m++ )
    {
        /* Repeat for j = 0, 1, ..., nk-1
           Step 2.a) of the Algorithm 6 from [1] */
        daal::threader_for(nc, nc, [&] (size_t j)
        {
            struct LogitBoostLs<algorithmFPType, cpu> *lsLocal = lsData.local();
            if(!lsLocal)
                return;

            services::Status localStatus = lsLocal->allocate(x, learnerTrain, learnerPredict);
            DAAL_CHECK_STATUS_THR(localStatus);

            initWZ<algorithmFPType, cpu>(n, nc, j, y_label, P.get(), thrW, lsLocal->wArray->getArray(),
                    thrZ, lsLocal->zArray->getArray());

            localStatus = lsLocal->run(j, models, pred);
            DAAL_CHECK_STATUS_THR(localStatus);
        });

        DAAL_CHECK_SAFE_STATUS();

        for(size_t j =0; j < nc; ++j)
        {
            r->addWeakLearnerModel(services::staticPointerCast<weak_learner::Model, SerializationIface>(models[j]));
        }

        /* Update additive function's values
           Step 2.b) of the Algorithm 6 from [1] */
        /* i-row contains Fi() for all classes in i-th point x */
        UpdateF<algorithmFPType, cpu>(dim, n, nc, pred.get(), F.get());

        /* Update probabilities
           Step 2.c) of the Algorithm 6 from [1] */
        UpdateP<algorithmFPType, cpu>(nc, n, F.get(), P.get(), Fbuf.get());

        /* Calculate model accuracy */
        calculateAccuracy<algorithmFPType, cpu>(n, nc, y_label, P.get(), logL, accCur);

        if (accCur < acc)
        {
            r->setIterations( m + 1 );
            break;
        }
    }

    lsData.reduce([ = ](LogitBoostLs<algorithmFPType, cpu> *logitBoostLs)
    {
        delete logitBoostLs;
    });
    return s;
}

} // namepsace internal
} // namespace prediction
} // namespace logitboost
} // namespace algorithms
} // namespace daal

#endif
