/* file: linear_regression_train_dense_qr_impl.i */
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
//  Implementation of auxiliary functions for linear regression qrDense method.
//--
*/

#ifndef __LINEAR_REGRESSION_TRAIN_DENSE_QR_IMPL_I__
#define __LINEAR_REGRESSION_TRAIN_DENSE_QR_IMPL_I__

#include "threading.h"
#include "service_lapack.h"
#include "service_memory.h"
#include "service_numeric_table.h"
#include "service_error_handling.h"
#include "linear_regression_train_kernel.h"
#include "algorithms/linear_regression/linear_regression_qr_model.h"

using namespace daal::internal;
using namespace daal::services::internal;

#define DEFINE_TABLE_BLOCK_EX(BlockType, targetVariable, ...)    \
    BlockType<algorithmFPType, cpu> targetVariable(__VA_ARGS__); \
    DAAL_CHECK_BLOCK_STATUS(targetVariable);

#define DEFINE_TABLE_BLOCK(BlockType, targetVariable, table) \
    DEFINE_TABLE_BLOCK_EX(BlockType, targetVariable, table, 0, table->getNumberOfRows())

namespace daal
{
namespace algorithms
{
namespace linear_regression
{
namespace training
{
namespace internal
{

/**
 *  \brief Calculate size of LAPACK WORK buffer needed to perform qrDense decomposition
 *
 *  \param p[in]        Number of columns in input matrix
 *  \param n[in]        Number of rows in input matrix
 *  \param x[in]        Input matrix of size (n x p), n > p
 *  \param tau[in]      LAPACK GERQF TAU parameter. Array of size p
 *  \param work[in]     LAPACK GERQF WORK parameter
 *  \param lwork[out]   Calculated size of WORK array
 *
 */

template <typename algorithmFPType, CpuType cpu>
Status computeQRWorkSize(DAAL_INT *p, DAAL_INT *n, algorithmFPType *x, algorithmFPType *tau, DAAL_INT *lwork)
{
    DAAL_INT info = 0;
    algorithmFPType work_local;

    *lwork = -1;
    Lapack<algorithmFPType, cpu>::xxgerqf(p, n, x, p, tau, &work_local, lwork, &info);
    DAAL_CHECK(info == 0, services::ErrorLinearRegressionInternal);

    *lwork = (DAAL_INT)work_local;
    return Status();
}

/**
 *  \brief Function that allocates memory for storing intermediate data
 *         for qrDense decomposition
 *
 *  \param p[in]        Number of columns in input matrix X
 *  \param n[in]        Number of rows in input matrix X
 *  \param x[in]        Input matrix X of size (n x p), n > p
 *  \param ny[in]       Number of columns in input matrix Y
 *  \param y[in]        Input matrix Y of size (n x ny)
 *  \param tau[in]      LAPACK GERQF/ORMRQ TAU parameter. Array of size p
 *  \param work[out]    LAPACK GERQF/ORMRQ WORK parameter
 *  \param lwork[out]   Calculated size of WORK array
 *
 */
template <typename algorithmFPType, CpuType cpu>
Status mallocQRWorkBuffer(DAAL_INT *p, DAAL_INT *n, algorithmFPType *x, DAAL_INT *ny, algorithmFPType *y,
                          algorithmFPType *tau, TArrayScalable<algorithmFPType, cpu> &work, DAAL_INT *lwork)
{
    DAAL_INT info = 0;
    DAAL_INT lwork1;

    Status status = computeQRWorkSize<algorithmFPType, cpu>(p, n, x, tau, &lwork1);
    DAAL_CHECK_STATUS_VAR(status);

    char side = 'R';
    char trans = 'T';
    DAAL_INT lwork2 = -1;
    algorithmFPType work_local;

    Lapack<algorithmFPType, cpu>::xxormrq(&side, &trans, ny, n, p, x, p, tau, y, ny, &work_local, &lwork2, &info);
    DAAL_CHECK(info == 0, services::ErrorLinearRegressionInternal);

    lwork2 = (DAAL_INT)work_local;
    *lwork = ((lwork1 > lwork2) ? lwork1 : lwork2);

    work.reset(*lwork);
    DAAL_CHECK_MALLOC(work.get());

    return Status();
}

/**
 *  \brief Function that copies input matrices X and Y into intermediate
 *         buffers.
 *
 *  \param dim[in]       Number of columns in input matrix X
 *  \param betaDim[in]   Number of regression coefficients
 *  \param n[in]         Number of rows in input matrix X
 *  \param x[in]         Input matrix X of size (n x p), n > p
 *  \param ny[in]        Number of columns in input matrix Y
 *  \param y[in]         Input matrix Y of size (n x ny)
 *  \param qrBuffer[out] if (dim     == betaDim) copy of matrix X,
 *                       if (dim + 1 == betaDim) qrBuffer = (X|e),
 *                          where e is a column vector of all 1's.
 *  \param qtyBuffer[out] copy of matrix Y
 *
 */
template <typename algorithmFPType, CpuType cpu>
static void copyDataToBuffer(DAAL_INT *dim, DAAL_INT *betaDim, DAAL_INT *n, algorithmFPType *x, DAAL_INT *ny, algorithmFPType *y,
                             algorithmFPType *qrBuffer, algorithmFPType *qtyBuffer)
{
    DAAL_INT iOne = 1;             // integer one
    DAAL_INT dimVal = *dim;
    DAAL_INT betaDimVal = *betaDim;
    DAAL_INT nVal = *n;
    DAAL_INT ySize = (*ny) * nVal;

    /* Copy matrix X to temporary buffer in order not to damage it */
    if (dimVal == betaDimVal)
    {
        DAAL_INT xSize = dimVal * nVal;
        daal::services::daal_memcpy_s(qrBuffer, xSize * sizeof(algorithmFPType), x, xSize * sizeof(algorithmFPType));
    }
    else
    {
        for (size_t i = 0; i < nVal; i++)
        {
            daal::services::daal_memcpy_s(qrBuffer + i * betaDimVal, dimVal * sizeof(algorithmFPType), x + i * dimVal, dimVal * sizeof(algorithmFPType));
            qrBuffer[i * betaDimVal + betaDimVal - 1] = 1.0;
        }
    }

    /* Copy matrix Y to temporary buffer in order not to damage it */
    daal::services::daal_memcpy_s(qtyBuffer, ySize * sizeof(algorithmFPType), y, ySize * sizeof(algorithmFPType));
}

/**
 *  \brief Function that calculates R and Y*Q' from input matrix
 *         of independent variables X and matrix of responses Y.
 *
 *  \param p[in]     Number of columns in input matrix X
 *  \param n[in]     Number of rows in input matrix X
 *  \param x[in,out] Input matrix X of size (n x p), n > p;
 *                   Overwritten by LAPACK on output
 *  \param ny[in]    Number of columns in input matrix Y
 *  \param y[in,out] Input matrix Y of size (n x ny);
 *                   Overwritten by LAPACK on output
 *  \param r[out]    Matrix R of size (p x p)
 *  \param qty[out]  Matrix Y*Q' of size (ny x p)
 *  \param tau[in]   LAPACK GERQF/ORMRQ TAU parameter. Array of size p
 *  \param work[in]  LAPACK GERQF/ORMRQ WORK parameter
 *  \param lwork[in] Calculated size of WORK array
 *
 */
template <typename algorithmFPType, CpuType cpu>
Status computeQRForBlock(DAAL_INT *p, DAAL_INT *n, algorithmFPType *x, DAAL_INT *ny,
                         algorithmFPType *y, algorithmFPType *r, algorithmFPType *qty,
                         algorithmFPType *tau, algorithmFPType *work, DAAL_INT *lwork)
{
    DAAL_INT iOne = 1;             // integer one
    DAAL_INT info = 0;
    DAAL_INT pVal = *p;
    DAAL_INT n_val = *n;
    DAAL_INT ny_val = *ny;
    DAAL_INT qtySize = ny_val * pVal;
    DAAL_INT rOffset = (n_val - pVal) * pVal;
    DAAL_INT yqtOffset = (n_val - pVal) * ny_val;

    /* Calculate RQ decomposition of X */
    Lapack<algorithmFPType, cpu>::xxgerqf(p, n, x, p, tau, work, lwork, &info);
    DAAL_CHECK(info == 0, services::ErrorLinearRegressionInternal);

    /* Copy result into matrix R */
    algorithmFPType *xPtr = x + rOffset;
    for (size_t i = 0; i < pVal; i++)
    {
      PRAGMA_IVDEP
      PRAGMA_VECTOR_ALWAYS
        for (size_t j = 0; j <= i; j++)
        {
            r[i * pVal + j] = xPtr[i * pVal + j];
        }
    }

    /* Calculate Y*Q' */
    char side = 'R';
    char trans = 'T';
    Lapack<algorithmFPType, cpu>::xxormrq(&side, &trans, ny, n, p, x, p, tau, y, ny, work, lwork, &info);
    DAAL_CHECK(info == 0, services::ErrorLinearRegressionInternal);

    /* Copy result into matrix QTY */
    daal::services::daal_memcpy_s(qty, qtySize * sizeof(algorithmFPType), y + yqtOffset, qtySize * sizeof(algorithmFPType));

    return Status();
}


/**
 *  \brief Function that merges qrDense partial sums (R1, QTY1), (R2, QTY2)
 *         into partial sum (R, QTY).
 *
 *  \param p[in]     Number of rows and columns in R, R1, R2 and
 *                   number of rows in QTY, QTY1, QTY2.
 *  \param ny[in]    Number of columns in QTY, QTY1, QTY2.
 *  \param r1[in]    Matrix of size (p x p)
 *  \param qty1[in]  Matrix of size (p x ny)
 *  \param r2[in]    Matrix of size (p x p)
 *  \param qty2[in]  Matrix of size (p x ny)
 *  \param r12[in]   Matrix of size (2p x p)
 *  \param qty12[in] Matrix of size (2p x ny)
 *  \param r[out]    Output matrix of size (p x p)
 *  \param qty[out]  Output matrix of size (p x ny)
 *  \param tau[in]   LAPACK GERQF TAU parameter. Array of size p
 *  \param work[in]  LAPACK GERQF WORK parameter
 *  \param lwork[in] Size of WORK array
 *
 */
template <typename algorithmFPType, CpuType cpu>
Status mergeQR(DAAL_INT *p, DAAL_INT *ny,
               algorithmFPType *r1,   algorithmFPType *qty1, algorithmFPType *r2,
               algorithmFPType *qty2, algorithmFPType *r12,  algorithmFPType *qty12,
               algorithmFPType *r,    algorithmFPType *qty,  algorithmFPType *tau,
               algorithmFPType *work, DAAL_INT *lwork)
{
    DAAL_INT iOne    = 1;
    DAAL_INT p_val   = *p;
    DAAL_INT n_val   = 2 * p_val;
    DAAL_INT ny_val  = *ny;
    DAAL_INT rSize   = p_val * p_val;
    DAAL_INT qtySize = p_val * ny_val;

    /* Copy R1 and R2 into R12. R12 = (R1, R2) */
    daal::services::daal_memcpy_s(r12        , rSize * 2 * sizeof(algorithmFPType), r1, rSize * sizeof(algorithmFPType));
    daal::services::daal_memcpy_s(r12 + rSize, rSize *     sizeof(algorithmFPType), r2, rSize * sizeof(algorithmFPType));
    /* Copy QTY1 and QTY2 into QTY12. QTY12 = (QTY1, QTY2) */
    daal::services::daal_memcpy_s(qty12          , qtySize * 2 * sizeof(algorithmFPType), qty1, qtySize * sizeof(algorithmFPType));
    daal::services::daal_memcpy_s(qty12 + qtySize, qtySize *     sizeof(algorithmFPType),  qty2, qtySize * sizeof(algorithmFPType));

    return computeQRForBlock<algorithmFPType, cpu>(p, &n_val, r12, ny, qty12, r, qty, tau, work, lwork);
}


/**
 *  \brief Function that computes linear regression coefficients
 *         from partial sums (R, QTY).
 *
 *  \param p[in]     Number of regression coefficients
 *  \param r[in]     Matrix of size (p x p)
 *  \param ny[in]    Number of dependent variables
 *  \param qty[in]   Matrix of size (p x ny)
 *  \param beta[out] Matrix of regression coefficients of size (ny x p)
 *
 */
template <typename algorithmFPType, CpuType cpu>
Status finalizeQR(DAAL_INT *p, algorithmFPType *r, DAAL_INT *ny, algorithmFPType *qty, algorithmFPType *beta)
{
    DAAL_INT iOne = 1;             // integer one
    DAAL_INT info = 0;
    DAAL_INT betaSize = (*ny) * (*p);
    DAAL_INT pVal = *p;
    DAAL_INT ny_val = *ny;

    for (size_t i = 0; i < ny_val; i++)
    {
      PRAGMA_IVDEP
      PRAGMA_VECTOR_ALWAYS
        for (size_t j = 0; j < pVal; j++)
        {
            beta[i * pVal + j] = qty[j * ny_val + i];
        }
    }

    /* Solve triangular linear system R'*beta = Y*Q' */
    char uplo = 'U';
    char trans = 'T';
    char diag = 'N';
    Lapack<algorithmFPType, cpu>::xtrtrs(&uplo, &trans, &diag, p, ny, r, p, beta, p, &info);
    DAAL_CHECK(info == 0, services::ErrorLinearRegressionInternal);

    return Status();
}

template <typename algorithmFPType, CpuType cpu>
Status updatePartialModelQR(NumericTable *x, NumericTable *y, linear_regression::Model *r,
                            const daal::algorithms::Parameter *par, bool isOnline)
{
    const linear_regression::Parameter *parameter = static_cast<const linear_regression::Parameter *>(par);
    ModelQR *rr              = static_cast<ModelQR *>(r);
    DAAL_INT nFeatures       = (DAAL_INT)x->getNumberOfColumns();
    DAAL_INT nResponses      = (DAAL_INT)y->getNumberOfColumns();
    DAAL_INT nRows           = (DAAL_INT)x->getNumberOfRows();
    DAAL_INT nBetas          = (DAAL_INT)rr->getNumberOfBetas();
    DAAL_INT nBetasIntercept = nBetas;
    if (parameter && !parameter->interceptFlag) { nBetasIntercept--; }

    /* Retrieve data associated with input tables */
    DEFINE_TABLE_BLOCK_EX( WriteRows, rBlock,   rr->getRTable().get(),   0, nBetasIntercept );
    DEFINE_TABLE_BLOCK_EX( WriteRows, qtyBLock, rr->getQTYTable().get(), 0, nResponses      );
    DEFINE_TABLE_BLOCK   ( ReadRows,  xBlock, x );
    DEFINE_TABLE_BLOCK   ( ReadRows,  yBlock, y );

    algorithmFPType *dy    = const_cast<algorithmFPType *>(yBlock.get());
    algorithmFPType *dx    = const_cast<algorithmFPType *>(xBlock.get());
    algorithmFPType *qrR   = rBlock.get();
    algorithmFPType *qrQTY = qtyBLock.get();

    TArrayScalable<algorithmFPType, cpu> qrROld, qrQTYOld, qrRMerge, qrQTYMerge;
    if (isOnline)
    {
        qrROld.reset(nBetasIntercept * nBetasIntercept);
        DAAL_CHECK_MALLOC(qrROld.get());

        qrQTYOld.reset(nBetasIntercept * nResponses);
        DAAL_CHECK_MALLOC(qrQTYOld.get());

        qrRMerge.reset(2 * nBetasIntercept * nBetasIntercept);
        DAAL_CHECK_MALLOC(qrRMerge.get());

        qrQTYMerge.reset(2 * nBetasIntercept * nResponses);
        DAAL_CHECK_MALLOC(qrQTYMerge.get());

        daal::services::daal_memcpy_s(qrROld.get(), nBetasIntercept * nBetasIntercept * sizeof(algorithmFPType),
                                      qrR,          nBetasIntercept * nBetasIntercept * sizeof(algorithmFPType));
        daal::services::daal_memcpy_s(qrQTYOld.get(), nBetasIntercept * nResponses * sizeof(algorithmFPType),
                                      qrQTY,          nBetasIntercept * nResponses * sizeof(algorithmFPType));
    }

    DAAL_INT lwork = -1;
    TArrayScalable<algorithmFPType, cpu> tau(nBetasIntercept);
    DAAL_CHECK_MALLOC(tau.get());

    TArrayScalable<algorithmFPType, cpu> qrBuffer(nBetasIntercept * nRows);
    DAAL_CHECK_MALLOC(qrBuffer.get());

    TArrayScalable<algorithmFPType, cpu> qtyBuffer(nResponses * nRows);
    DAAL_CHECK_MALLOC(qtyBuffer.get());

    TArrayScalable<algorithmFPType, cpu> work;

    Status status = mallocQRWorkBuffer<algorithmFPType, cpu>(&nBetasIntercept,
        &nRows, dx, &nResponses, dy, tau, work, &lwork);
    DAAL_CHECK_STATUS_VAR(status);

    copyDataToBuffer<algorithmFPType, cpu>(&nFeatures, &nBetasIntercept,
        &nRows, dx, &nResponses, dy, qrBuffer, qtyBuffer);

    status |= computeQRForBlock<algorithmFPType, cpu>(&nBetasIntercept, &nRows,
        qrBuffer, &nResponses, qtyBuffer, qrR, qrQTY, tau, work, &lwork);
    DAAL_CHECK_STATUS_VAR(status);

    if (isOnline)
    {
        status |= mergeQR<algorithmFPType, cpu>(&nBetasIntercept, &nResponses, qrR, qrQTY, qrROld.get(),
            qrQTYOld.get(), qrRMerge.get(), qrQTYMerge.get(), qrR, qrQTY, tau, work, &lwork);
        DAAL_CHECK_STATUS_VAR(status);
    }

    return status;
}


template<typename algorithmFPType, CpuType cpu>
struct tls_task_t
{
    DAAL_NEW_DELETE();

    tls_task_t(size_t nBetasIntercept, size_t nRowsInLastBlock, size_t nResponses) :
        work(0), lwork(-1), memAllocError(false),
        tau         (nBetasIntercept),
        qrBuffer    (nBetasIntercept * nRowsInLastBlock),
        qtyBuffer   (nResponses * nRowsInLastBlock),
        qrR         (nBetasIntercept * nBetasIntercept, true),
        qrQTY       (nBetasIntercept * nResponses,      true),
        qrRNew      (nBetasIntercept * nBetasIntercept, true),
        qrQTYNew    (nBetasIntercept * nResponses,      true),
        qrRMerge    (2 * nBetasIntercept * nBetasIntercept),
        qrQTYMerge  (2 * nBetasIntercept * nResponses)
    {
        memAllocError =  !tau.get()      || !qrBuffer.get() || !qtyBuffer.get() ||
                         !qrR.get()      || !qrQTY.get()    || !qrRNew.get()    ||
                         !qrQTYNew.get() || !qrRMerge.get() || !qrQTYMerge.get();
    }

    TArrayScalable<algorithmFPType, cpu> work;
    TArrayScalable<algorithmFPType, cpu> tau;
    TArrayScalable<algorithmFPType, cpu> qrBuffer;
    TArrayScalable<algorithmFPType, cpu> qtyBuffer;

    TArrayScalable<algorithmFPType, cpu> qrR;
    TArrayScalable<algorithmFPType, cpu> qrQTY;

    TArrayScalable<algorithmFPType, cpu> qrRNew;
    TArrayScalable<algorithmFPType, cpu> qrQTYNew;
    TArrayScalable<algorithmFPType, cpu> qrRMerge;
    TArrayScalable<algorithmFPType, cpu> qrQTYMerge;

    DAAL_INT lwork;
    bool memAllocError;
};


template <typename algorithmFPType, CpuType cpu>
Status updatePartialModelQR_threaded(NumericTable *x,
                                     NumericTable *y,
                                     linear_regression::Model *r,
                                     const daal::algorithms::Parameter *par,
                                     bool isOnline)
{
    const linear_regression::Parameter *parameter = static_cast<const linear_regression::Parameter *>(par);
    ModelQR *rr = static_cast<ModelQR *>(r);

    DAAL_INT nFeatures       = (DAAL_INT)x->getNumberOfColumns();
    DAAL_INT nResponses      = (DAAL_INT)y->getNumberOfColumns();
    DAAL_INT nRows           = (DAAL_INT)x->getNumberOfRows();
    DAAL_INT nBetas          = (DAAL_INT)rr->getNumberOfBetas();
    DAAL_INT nBetasIntercept = nBetas;
    if (parameter && !parameter->interceptFlag) { nBetasIntercept--; }

    DEFINE_TABLE_BLOCK( ReadRows, xBlock, x );
    DEFINE_TABLE_BLOCK( ReadRows, yBlock, y );

    algorithmFPType *dy = const_cast<algorithmFPType *>(yBlock.get());
    algorithmFPType *dx = const_cast<algorithmFPType *>(xBlock.get());

    /* Split rows by blocks */
#if ( __CPUID__(DAAL_CPU) == __avx512_mic__ )
    size_t nDefaultBlockSize = (nRows<=10000)?1024:((nRows>=1000000)?512:2048);
#else
    size_t nDefaultBlockSize = 128;
#endif

    // Block size cannot be bigger than nRows
    size_t nRowsInBlock = (nRows>nDefaultBlockSize)?nDefaultBlockSize:nRows;
    // Block size cannot be smaller than nFeatures+1
    nRowsInBlock = (nRowsInBlock<(nFeatures+1))?(nFeatures+1):nRowsInBlock;

    size_t nBlocks = nRows / nRowsInBlock;
    size_t nRowsInLastBlock = nRowsInBlock + (nRows - nBlocks * nRowsInBlock);

    DEFINE_TABLE_BLOCK_EX( ReadRows, rBlock,   rr->getRTable().get(),   0, nBetasIntercept );
    DEFINE_TABLE_BLOCK_EX( ReadRows, qtyBLock, rr->getQTYTable().get(), 0, nResponses      );

    algorithmFPType *qrR   = const_cast<algorithmFPType *>(rBlock.get());
    algorithmFPType *qrQTY = const_cast<algorithmFPType *>(qtyBLock.get());

    if(!isOnline)
    {
        service_memset<algorithmFPType, cpu>(qrR,   (algorithmFPType)0, nBetasIntercept * nBetasIntercept);
        service_memset<algorithmFPType, cpu>(qrQTY, (algorithmFPType)0, nBetasIntercept * nResponses);
    }

    SafeStatus safeStat;
    daal::tls<tls_task_t<algorithmFPType, cpu>*> tls_task( [ =, &safeStat ] ()-> tls_task_t<algorithmFPType, cpu>*
    {
        tls_task_t<algorithmFPType, cpu> *tt_local = new tls_task_t<algorithmFPType, cpu>(
            nBetasIntercept, nRowsInLastBlock, nResponses);
        if (!tt_local) { safeStat.add(services::ErrorMemoryAllocationFailed); return nullptr; }

        if (tt_local->memAllocError)
        {
            delete tt_local;
            safeStat.add(services::ErrorMemoryAllocationFailed);
            return nullptr;
        }

        algorithmFPType *dx_local = dx;
        algorithmFPType *dy_local = dy;

        DAAL_INT nBetasIntercept_local  = nBetasIntercept;
        DAAL_INT nResponses_local       = nResponses;
        DAAL_INT nRowsInLastBlock_local = nRowsInLastBlock;

        // Function that allocates memory for storing intermediate data for qrDense decomposition
        Status localStatus = mallocQRWorkBuffer<algorithmFPType, cpu>(
            &nBetasIntercept_local,  // in   Number of columns in input matrix X
            &nRowsInLastBlock_local, // in   Number of rows in input matrix X
            dx_local,                // in   Input matrix X of size (n x p), n > p
            &nResponses_local,       // in   Number of columns in input matrix Y
            dy_local,                // in   Input matrix Y of size (n x ny)
            tt_local->tau.get(),     // in   LAPACK GERQF/ORMRQ TAU parameter. Array of size p
            tt_local->work,          // out  LAPACK GERQF/ORMRQ WORK parameter
            & (tt_local->lwork));    // out  Calculated size of WORK array
        safeStat.add(localStatus);

        return tt_local;
    } ); /* Allocate memory for all arrays inside TLS: end */

    daal::threader_for( nBlocks, nBlocks, [=, &tls_task, &safeStat](int iBlock)
    {
        tls_task_t<algorithmFPType, cpu> *tt_local = tls_task.local();
        if (!tt_local) { return; }

        algorithmFPType *work_local = tt_local->work.get();
        DAAL_INT *plwork_local      = &(tt_local->lwork);

        algorithmFPType *tau_local        = tt_local->tau.get();
        algorithmFPType *qrBuffer_local   = tt_local->qrBuffer.get();
        algorithmFPType *qtyBuffer_local  = tt_local->qtyBuffer.get();
        algorithmFPType *qrR_local        = tt_local->qrR.get();
        algorithmFPType *qrQTY_local      = tt_local->qrQTY.get();
        algorithmFPType *qrRNew_local     = tt_local->qrRNew.get();
        algorithmFPType *qrQTYNew_local   = tt_local->qrQTYNew.get();
        algorithmFPType *qrRMerge_local   = tt_local->qrRMerge.get();
        algorithmFPType *qrQTYMerge_local = tt_local->qrQTYMerge.get();

        DAAL_INT nBetasIntercept_local = nBetasIntercept;
        DAAL_INT nResponses_local      = nResponses;
        DAAL_INT nFeatures_local       = nFeatures;

        size_t startRow = iBlock * nRowsInBlock;
        size_t nCurrentRowsInBlock = (iBlock < (nBlocks-1)) ? nRowsInBlock : nRowsInLastBlock;
        size_t endRow = startRow + nCurrentRowsInBlock;
        DAAL_INT nN_local = endRow - startRow;

        algorithmFPType *dx_local = dx + startRow * nFeatures;
        algorithmFPType *dy_local = dy + startRow * nResponses;

        // Function that copies input matrices X and Y into intermediate buffers.
        copyDataToBuffer<algorithmFPType, cpu>(&nFeatures_local,        // in      Number of columns in input matrix X
                                               &nBetasIntercept_local,  // in      Number of regression coefficients
                                               &nN_local,               // in      Number of rows in input matrix X
                                               dx_local,                // in      Input matrix X of size (n x p), n > p
                                               &nResponses_local,       // in      Number of columns in input matrix Y
                                               dy_local,                // in      Input matrix Y of size (n x ny)
                                               qrBuffer_local,          // out     if (dim == betaDim) copy of matrix X, if (dim + 1 == betaDim) qrBuffer = (X|e) where e is a column vector of all 1's.
                                               qtyBuffer_local);        // out     copy of matrix Y

        // Function that calculates R and Y*Q' from input matrix  of independent variables X and matrix of responses Y.
        Status localStatus = computeQRForBlock<algorithmFPType, cpu>(
            &nBetasIntercept_local,  // in      Number of columns in input matrix X
            &nN_local,               // in      Number of rows in input matrix X
            qrBuffer_local,          // in, out Input matrix X of size (n x p), n > p; Overwritten by LAPACK on output
            &nResponses_local,       // in      Number of columns in input matrix Y
            qtyBuffer_local,         // in, out Input matrix Y of size (n x ny); Overwritten by LAPACK on output
            qrRNew_local,            // out     Matrix R of size (p x p)
            qrQTYNew_local,          // out     Matrix Y*Q' of size (ny x p)
            tau_local,               // in      LAPACK GERQF/ORMRQ TAU parameter. Array of size p
            work_local,              // in      LAPACK GERQF/ORMRQ WORK parameter
            plwork_local);           // in      Calculated size of WORK array
        DAAL_CHECK_STATUS_THR(localStatus);

        // Function that merges qrDense partial sums (R1, QTY1), (R2, QTY2) into partial sum (R, QTY)
        localStatus |= mergeQR<algorithmFPType, cpu>(
            &nBetasIntercept_local,  // in      Number of rows and columns in R, R1, R2
            &nResponses_local,       // in      Number of columns in QTY, QTY1, QTY2.
            qrRNew_local,            // in      Matrix of size (p x p)
            qrQTYNew_local,          // in      Matrix of size (p x ny)
            qrR_local,               // in      Matrix of size (p x p)
            qrQTY_local,             // in      Matrix of size (p x ny)
            qrRMerge_local,          // in      Matrix of size (2p x p)
            qrQTYMerge_local,        // in      Matrix of size (2p x ny)
            qrR_local,               // out     Output matrix of size (p x p)
            qrQTY_local,             // out     Output matrix of size (p x ny)
            tau_local,               // in      LAPACK GERQF TAU parameter. Array of size p
            work_local,              // in      LAPACK GERQF WORK parameter
            plwork_local);           // in      Size of WORK array
        DAAL_CHECK_STATUS_THR(localStatus);
    } ); /* for(iBlock = 0; iBlock < nBlocks; iBlock++) */

    tls_task.reduce( [ =, &safeStat ](tls_task_t<algorithmFPType, cpu> *tt_local)
    {
        if (!tt_local) { return; }

        algorithmFPType *work_local = tt_local->work.get();
        DAAL_INT *plwork_local      = &(tt_local->lwork);

        algorithmFPType *tau_local        = tt_local->tau.get();
        algorithmFPType *qrR_local        = tt_local->qrR.get();
        algorithmFPType *qrQTY_local      = tt_local->qrQTY.get();
        algorithmFPType *qrRMerge_local   = tt_local->qrRMerge.get();
        algorithmFPType *qrQTYMerge_local = tt_local->qrQTYMerge.get();

        DAAL_INT nBetasIntercept_local = nBetasIntercept;
        DAAL_INT nResponses_local      = nResponses;
        DAAL_INT nFeatures_local       = nFeatures;

        // Function that merges qrDense partial sums (R1, QTY1), (R2, QTY2) into partial sum (R, QTY)
        Status localStatus = mergeQR<algorithmFPType, cpu>(
            &nBetasIntercept_local,  // in      Number of rows and columns in R, R1, R2
            &nResponses_local,       // in      Number of columns in QTY, QTY1, QTY2.
            qrR_local,               // in      Matrix of size (p x p)
            qrQTY_local,             // in      Matrix of size (p x ny)
            qrR,                     // in      Matrix of size (p x p)
            qrQTY,                   // in      Matrix of size (p x ny)
            qrRMerge_local,          // in      Matrix of size (2p x p)
            qrQTYMerge_local,        // in      Matrix of size (2p x ny)
            qrR,                     // out     Output matrix of size (p x p)
            qrQTY,                   // out     Output matrix of size (p x ny)
            tau_local,               // in      LAPACK GERQF TAU parameter. Array of size p
            work_local,              // in      LAPACK GERQF WORK parameter
            plwork_local);           // in      Size of WORK array
        DAAL_CHECK_STATUS_THR(localStatus);

        delete tt_local;
    } );
    DAAL_CHECK_SAFE_STATUS();

    return Status();
}


template <typename algorithmFPType, CpuType cpu>
Status finalizeModelQR(linear_regression::Model *a, linear_regression::Model *r)
{
    ModelQR *aa = static_cast<ModelQR *>(a);
    ModelQR *rr = static_cast<ModelQR *>(r);

    DAAL_INT nBetas = (DAAL_INT)rr->getNumberOfBetas();
    DAAL_INT nResponses = (DAAL_INT)rr->getNumberOfResponses();
    DAAL_INT nBetasIntercept = nBetas;
    if (!rr->getInterceptFlag())
    {
        nBetasIntercept--;
    }

    TArrayScalable<algorithmFPType, cpu> betaBufferArray(nResponses * nBetas);
    algorithmFPType *betaBuffer = betaBufferArray.get();
    DAAL_CHECK_MALLOC(betaBuffer);

    Status status;
    {
        DEFINE_TABLE_BLOCK_EX( ReadRows, rBlock,   aa->getRTable().get(),   0, nBetasIntercept );
        DEFINE_TABLE_BLOCK_EX( ReadRows, qtyBLock, aa->getQTYTable().get(), 0, nResponses      );

        algorithmFPType *qrR   = const_cast<algorithmFPType *>(rBlock.get());
        algorithmFPType *qrQTY = const_cast<algorithmFPType *>(qtyBLock.get());

        status |= finalizeQR<algorithmFPType, cpu>(&nBetasIntercept, qrR, &nResponses, qrQTY, betaBuffer);
        DAAL_CHECK_STATUS_VAR(status);
    }

    DEFINE_TABLE_BLOCK( WriteOnlyRows, betaBlock, rr->getBeta().get() )
    algorithmFPType *beta = betaBlock.get();

    if (nBetasIntercept == nBetas)
    {
        for(size_t i = 0; i < nResponses; i++)
        {
          PRAGMA_IVDEP
          PRAGMA_VECTOR_ALWAYS
            for(size_t j = 1; j < nBetas; j++)
            {
                beta[i * nBetas + j] = betaBuffer[i * nBetas + j - 1];
            }
            beta[i * nBetas] = betaBuffer[i * nBetas + nBetas - 1];
        }
    }
    else
    {
        for(size_t i = 0; i < nResponses; i++)
        {
          PRAGMA_IVDEP
          PRAGMA_VECTOR_ALWAYS
            for(size_t j = 0; j < nBetas - 1; j++)
            {
                beta[i * nBetas + j + 1] = betaBuffer[i * nBetasIntercept + j];
            }
            beta[i * nBetas] = 0.0;
        }
    }

    return status;
}


template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainBatchKernel<algorithmFPType, training::qrDense, cpu>::compute(
    NumericTable *x, NumericTable *y, linear_regression::Model *r, const daal::algorithms::Parameter *par)
{
    const bool isOnline = false;

    Status status = updatePartialModelQR_threaded<algorithmFPType, cpu>(x, y, r, par, isOnline);
    DAAL_CHECK_STATUS_VAR(status);

    status |= finalizeModelQR<algorithmFPType, cpu>(r, r);
    return status;
}

template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainOnlineKernel<algorithmFPType, training::qrDense, cpu>::compute(
            NumericTable *x, NumericTable *y, linear_regression::Model *r,
            const daal::algorithms::Parameter *par)
{
    const bool isOnline = true;
    return updatePartialModelQR_threaded<algorithmFPType, cpu>(x, y, r, par, isOnline);
}

template <typename algorithmFPType, CpuType cpu>
Status LinearRegressionTrainOnlineKernel<algorithmFPType, training::qrDense, cpu>::finalizeCompute(
            linear_regression::Model *a, linear_regression::Model *r,
            const daal::algorithms::Parameter *par)
{
    return finalizeModelQR<algorithmFPType, cpu>(a, r);
}

} // internal
} // training
} // linear_regression
} // algorithms
} // daal

#endif
