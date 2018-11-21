#ifndef __SPDM3_SPMAT_H__
#define __SPDM3_SPMAT_H__

#include <cstdio>
#include <functional>
#include "defs.h"

namespace spdm3 {

template <class IT, class VT>
class DMat;

template <class IT, class VT>
class SpMat {
 public:
  //
  // Constructors & Destructor.
  //
  SpMat(spdm3_format format = SPARSE_CSR, IT idx_base = 0);
  
  // Concatenates existing SpMats.
  SpMat(SpMat<IT, VT> *M, IT blk_rows, IT blk_cols, spdm3_layout layout);
  ~SpMat();
  
  //
  // Allocation.
  //
  void Allocate(IT rows, IT cols, LT buffer_size);
  void Allocate(IT rows, IT cols, IT head_size, LT buffer_size);
  void AllocateHeads(IT rows, IT cols);
  void AllocateBuffers(LT buffer_size);
  void Deallocate();
  void ExpandHeads(IT new_size, bool preserve_contents = true);
  void ExpandBuffers(IT new_size, bool preserve_contents = true);
  void Set(IT rows, IT cols, IT nnz,
           IT head_size, LT *headptrs,
           LT buffer_size, LT *indices, VT *values,
           bool free_buffer = false);
  
  //
  // I/O.
  //
  void Generate(IT rows, IT cols, double nnz_ratio, IT seed = 1000);
  void GenerateWithRMAT(IT rows, IT cols, double nnz_ratio,
                        double a, double b, double c, double d,
                        int seed = 1000);
  void GenerateWithG500(double initiator[4], int log_numverts, double nnz_ratio);
  void Fill(Triplet *triplets, IT len);
  void SetIdentity(IT rows);
  void Load(const char *filename, IT id = -1);
  void LoadMatrixMarket(const char *filename, bool transpose = false);
  void Save(const char *filename, IT id = -1);
  void Save(FILE *fout);
  void SaveMatrixMarket(const char *filename);
  void Print();
  void SaveDense(const char *filename, IT id = -1);
  void SaveDense(FILE *fout);
  void PrintDense();
  void SavePPM(const char *filename);
  void SavePNG(const char *filename);
  void PointersToSpMat(IT rows, IT cols, IT nnz,
                       spdm3_format format, IT idx_base,
                       LT *headptrs, LT *indices, VT *values,
                       bool free_buffers);
  void Submatrix(const SpMat<IT, VT> &X,
                 IT row_offset, IT col_offset, IT rows, IT cols);
  void Diagonal(const SpMat<IT, VT> &X);
  void Add(const VT alpha, const SpMat<IT, VT> &A,
           const VT beta, const SpMat<IT, VT> &B);
  void Convert(const DMat<IT, VT> &D, bool allocate = true);
  void Concatenate(SpMat<IT, VT> *M, IT blk_rows, IT blk_cols, spdm3_layout layout);
  void MakeBlockDiagonal(SpMat<IT, VT> *M, IT n);
  void ThresholdThenConvert(const DMat<IT, VT> &G,
                            const SpMat<IT, VT> &O,
                            const DMat<IT, VT> &Lambda1,
                            const VT tau);
  void ThresholdAndMaskThenConvert(const DMat<IT, VT> &G,
                                   const SpMat<IT, VT> &O,
                                   const DMat<IT, VT> &Lambda1,
                                   const VT tau, const SpMat<IT, VT> &M);
  SpMat<IT, VT> *PartitionToSubmatrices(IT blk_rows, IT *row_counts, IT *row_displs,
                                        IT blk_cols, IT *col_counts, IT *col_displs);
  
  //
  // Operations.
  //
  bool IsEquivalent(const SpMat<IT, VT> &X);  // Mathematically equivalent.
  bool IsEqual(const SpMat<IT, VT> &X);       // Same structures (format_, idx_base_).
  bool IsSymmetric();
  VT FrobeniusNorm();
  VT FrobeniusNormSquare();
  void ElmtWiseOp(std::function<VT(VT)> fn);
  void ElmtWiseOp(std::function<VT(VT, VT)> fn, const VT &op);
  SpMat<IT, VT> GetShallowCopy(bool transfer_buffer_ownership = false);
  SpMat<IT, VT> GetDeepCopy();
  SpMat<IT, VT> GetStructure();
  void ShallowCopy(const SpMat<IT, VT> &X);
  void TakeOwnership(SpMat<IT, VT> &X);  // Only to be called after ShallowCopy
  void CommonCopy(const DMat<IT, VT> &D);
  void CommonCopy(const SpMat<IT, VT> &X);
  void StructuralCopy(const SpMat<IT, VT> &X);
  void DeepCopy(const SpMat<IT, VT> &X);
  void Swap(SpMat<IT, VT> &X);
  void MultiplyAdd(const DMat<IT, VT> &B, DMat<IT, VT> &C);
  void Multiply(const SpMat<IT, VT> &B, SpMat<IT, VT> &C);
  void DotMultiplyI(const DMat<IT, VT> &X);
  void DotMultiplyTransposeI(const DMat<IT, VT> &X);
  VT Reduce(spdm3_op op);
  void ChangeBase(IT base);
  void Create1BasedArrays(IT* &heads, IT* &indices) const;
  void RemoveZeroes();
  
  //
  // Setters.
  //
  void SetName(const char *name);
  void SetMathLib(spdm3_math_library lib);
  void SetMinHeadSize(LT min_head_size);
  void SetMinBufferSize(LT min_buffer_size);
  void SetEpsilon(VT epsilon);
  void SetVerbosity(IT verbosity);

  // for conversion between Subgraph::CSRGraph and SpMaT
  void SetNNZ(const LT nnz);
  void SetRows(const IT rows);
  void SetCols(const IT cols);
  void SetHeadptrs(const LT* ptr);
  void SetIndices(const LT* ptr);
  void SetValues(const VT* ptr);
  
  //
  // Getters.
  //
  IT dim1() const;
  IT dim2() const;
  VT GetElmt(IT row_id, IT col_id) const;
  VT nnz_ratio() const;

  // for conversion between Subgraph::CSRGraph and SpMaT
  LT* GetHeadptrs();
  LT* GetIndices();
  VT* GetValues();

  //
  // Variables.
  //
  LT nnz_;
  IT rows_;
  IT cols_;
  IT idx_base_;
  IT head_size_;
  LT buffer_size_;
  IT min_head_size_;
  LT min_buffer_size_;
  
  LT *headptrs_;
  LT *indices_;
  VT *values_;
  bool free_buffers_;
  
  const char *name_;
  spdm3_format format_;
  spdm3_math_library lib_;
  VT epsilon_;
  
  IT verbosity_;
  
};  // class SpMat

}  // namespace spdm3

#include "../src/spmat.impl"

#endif  // __SPDM3_SPMAT_H__
