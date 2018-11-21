#ifndef __SPDM3_DMAT_H__
#define __SPDM3_DMAT_H__

#include <cstdio>
#include <cstdint>
#include <functional>

#include "defs.h"

namespace spdm3 {

// Prototype for SpMat.
template <class IT, class VT> class SpMat;

template <class IT, class VT>
class DMat {
 public:
  //
  // Constructors / Destructor.
  //
  DMat(spdm3_format format = DENSE_ROWMAJOR);
  DMat(IT rows, IT cols,
       spdm3_format format = DENSE_ROWMAJOR);
  DMat(IT rows, IT cols, IT lda,
       spdm3_format format = DENSE_ROWMAJOR);
  
  // Existing array.
  DMat(IT rows, IT cols, spdm3_format format, VT *values);
  DMat(IT rows, IT cols, IT lda, spdm3_format format,
       VT *values);
  
  // Converts from SpMat.
  DMat(const SpMat<IT, VT> &X);
  
  // Concatenates existing DMats.
  DMat(DMat<IT, VT> *M, IT blk_rows, IT blk_cols, spdm3_layout layout);
  ~DMat();
  void InitDefault();
  
  //
  // Allocation
  //
  void Allocate();
  void Allocate(IT rows, IT cols);
  void Allocate(LT buffer_size);
  void Allocate(IT rows, IT cols, LT buffer_size);
  void Deallocate();
  void ExpandBuffer(LT new_size, bool preserve_contents);
  
  //
  // I/O.
  //
  void Generate(IT seed = 1000);
  void GenerateNormal(VT mean, VT sigma, IT seed = 1000);
  void Fill(VT val);
  void FillSeries(VT start);
  void Transpose(const DMat<IT, VT> &X, bool allocate = true);
  void TransposeBySwitchingMajor();
  void Load(const char *filename, int id = -1);
  void LoadNumPy(const char *filename,
                 IT cols_to_skip = 0,
                 IT rows = -1,
                 IT cols = -1);
  void LoadNumPyOffset(const char *filename,
                 IT row_offset, IT col_offset,
                 IT rows, IT cols);
  void Save(const char *filename, int id = -1);
  void Save(FILE *file, bool skip_header = false);
  void SaveNumPy(const char *filename);
  void Print(bool skip_header = false);
  void PointerToDMat(IT rows, IT cols, IT lda,
                     spdm3_format format, VT *values, bool free_buffer);
  void StoreElmtWise(std::function<VT(VT, VT)> fn,
                     const DMat<IT, VT> &a, const SpMat<IT, VT> &b);
  void Concatenate(DMat<IT, VT> *M, IT blk_rows, IT blk_cols,
                   spdm3_layout layout);
  void SubDMat(const DMat<IT, VT> &X,
               IT row_offset, IT col_offset, IT rows, IT cols);
  void ShallowSubDMat(const DMat<IT, VT> &X,
                      IT row_offset, IT col_offset, IT rows, IT cols);
  
  //
  // Operators.
  //
  bool IsEquivalent(const DMat<IT, VT> &X);  // Mathematically equivalent.
  bool IsEqual(const DMat<IT, VT> &X);       // Same structures (format_, lda_).
  bool IsAllocated();
  bool IsSymmetric();
  void MakeSymmetric();
  void ElmtWiseOp(std::function<VT(VT, VT)> fn, VT op);
  void ElmtWiseOp(std::function<VT(VT, VT)> fn, const DMat<IT, VT> &Op);
  void ElmtWiseOp(std::function<VT(VT, VT)> fn, const SpMat<IT, VT> &Op);
  void ElmtWiseOpNzOnly(std::function<VT(VT, VT)> fn, const SpMat<IT, VT> &Op);
  VT ReduceElmtWiseOpNzOnly(std::function<VT(VT, VT, VT)> fn,
                              const SpMat<IT, VT> &Op, VT initial_value);
  void SetSubDMat(const DMat<IT, VT> &X, IT row_offset, IT col_offset);
  void MultiplyAdd(const DMat<IT, VT> &B, DMat<IT, VT> &C);
  void ShallowCopy(const DMat<IT, VT> &X);
  void TakeOwnership(DMat<IT, VT> &X);
  void DeepCopy(const DMat<IT, VT> &X);
  void StructuralCopy(const DMat<IT, VT> &X);
  VT Reduce(spdm3_op op);
  void AddI(const SpMat<IT, VT> &X);
  void Standardize();
  
  //
  // Setters.
  //
  void SetName(const char *name);
  void SetEpsilon(VT epsilon);
  void SetFreeBuffer(bool free_buffer);
  
  //
  // Getters.
  //
  IT dim1() const;  // Leading dimension.
  IT dim2() const;  // Second dimension.
  IT max_dim1() const;
  IT max_dim2() const;
  LT nnz() const;
  
  //
  // Variables.
  //
  IT rows_;
  IT cols_;
  IT lda_;
  
  VT *values_;        // Owned by default, unless free_buffer_ is false.
  LT buffer_size_;    // Computed in Allocate() but can be set separately.
  bool free_buffer_;  // Set by Allocate() but can be set manually.
  
  spdm3_format format_;
  spdm3_math_library lib_;
  const char *name_;
  VT epsilon_;
  
};  // class DMat

}  // namespace spdm3

// Must include implementation because of the templating.
#include "../src/dmat.impl"

#endif  // __SPDM3_DMAT_H__
