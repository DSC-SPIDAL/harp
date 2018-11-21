#ifndef __SPDM3_DEFS_H__
#define __SPDM3_DEFS_H__

#include <stdint.h>

namespace spdm3 {

// Constants.
#define LT          int

// Enums.
enum spdm3_format {
  DENSE_ROWMAJOR,
  DENSE_COLMAJOR,
  SPARSE_CSR,
  SPARSE_CSC,
  SPARSE_TRIPLET,
  FORMAT_UNSET
};

enum spdm3_layout {
  ONED_BLOCK_ROW,
  ONED_BLOCK_COLUMN,
  TWOD_ROWMAJOR,
  TWOD_COLMAJOR,
  LAYOUT_UNSET
};

enum spdm3_dist {
  DIST_UNIFORM,
  DIST_GREEDY
};

enum spdm3_math_library {
  LIB_BLAS,
  LIB_MKL,
  LIB_ESSL,
  LIB_NAIVE,
  LIB_UNSET
};

enum spdm3_comm {
  WORLD = 0,
  TEAM,
  TEAM_ROW,  // Just using id and size.
  TEAM_COL,  // Just using id and size.
  LAYER,
  LAYER_ROW,
  LAYER_COL,
  NUM_COMMS,
  NOT_SHIFTING
};

enum spdm3_find_owner {
  BINARY_SEARCH = 0,
  UNIFORM
};

enum spdm3_op {
  SPDM3_SUM = 0,
  SPDM3_PROD,
  SPDM3_MAX,
  SPDM3_MAX_ABS,
  SPDM3_MIN
};

// Struct.
// TODO(penpornk): Change int to int64_t and solve the problem
// with qsort in SpMat::Fill
// And don't forget to update the corresponding MPI_Datatype.
struct Triplet {
  int row;
  int col;
  double weight;
};

// Defines default math library based on architecture.
#if 	defined(USE_MKL)
#define	LIB_DEFAULT	LIB_MKL

#elif	defined(USE_ESSL)
#define	LIB_DEFAULT	LIB_ESSL

#elif	defined(USE_BLAS)
#define	LIB_DEFAULT	LIB_BLAS

#else
#define	LIB_DEFAULT	LIB_NAIVE
#endif
  
}  // namespace spdm3

#endif  // __SPDM3_DEFS_H__
