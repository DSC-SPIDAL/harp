#ifndef COMMON_AVX2_H
#define COMMON_AVX2_H

#include <stdint.h>

#include <omp.h>
#include "immintrin.h"

#include "csr5_common.hpp"
#include "csr5_utils.hpp"

#define ANONYMOUSLIB_CSR5_OMEGA   4
// change omega to 8 for single precision float
// #define ANONYMOUSLIB_CSR5_OMEGA   8
#define ANONYMOUSLIB_CSR5_SIGMA   16
// change sigma to 32 for single precision float
// #define ANONYMOUSLIB_CSR5_SIGMA   32
#define ANONYMOUSLIB_X86_CACHELINE   64

#endif // COMMON_AVX2_H
