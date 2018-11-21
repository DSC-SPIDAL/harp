#ifndef __SPDM3_MATMUL_H__
#define __SPDM3_MATMUL_H__

#include "dmat.h"

#ifdef	USE_BLAS
extern "C" {
int dgemm_(char *transa, char *transb,
           int *m, int *n, int *k,
           double *alpha, double *a, int *lda,
           double *b, int *ldb, double *beta, double *c, int *ldc);

int sgemm_(char *transa, char *transb,
           int *m, int *n, int *k,
           float *alpha, float *a, int *lda,
           float *b, int *ldb, float *beta, float *c, int *ldc);
}
#endif

namespace spdm3 {

//
// Dense - Dense.
//
template <class IT, class VT>
void matmul_naive(const DMat<IT, VT> &A, const DMat<IT, VT> &B, DMat<IT, VT> &C);

template <class IT>
void matmul_blas(const DMat<IT, double> &A,
                 const DMat<IT, double> &B, DMat<IT, double> &C);
template <class IT>
void matmul_blas(const DMat<IT, float> &A,
                 const DMat<IT, float> &B, DMat<IT, float> &C);

template <class IT>
void matmul_mkl(const DMat<IT, double> &A,
                const DMat<IT, double> &B, DMat<IT, double> &C);
template <class IT>
void matmul_mkl(const DMat<IT, float> &A,
                const DMat<IT, float> &B, DMat<IT, float> &C);
  
//
// Sparse - Dense.
//
template <class IT, class VT>
void matmul_naive(const SpMat<IT, VT> &A,
                  const DMat<IT, VT> &B, DMat<IT, VT> &C);
  
template <class IT>
void matmul_blas(const SpMat<IT, double> &A,
                 const DMat<IT, double> &B, DMat<IT, double> &C);

template <class IT>
void matmul_blas_colmajor(const SpMat<IT, float> &A,
                 const DMat<IT, float> &B, DMat<IT, float> &C);
 
template <class IT>
void matmul_mkl(const SpMat<IT, double> &A,
                const DMat<IT, double> &B, DMat<IT, double> &C);

}  // namespace spdm3

#include "../src/matmul.impl"

#endif
