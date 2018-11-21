#ifndef PERF_COUNT_H
#define PERF_COUNT_H

#ifdef RISCV
  #include "perf_count_riscv.h"
#elif defined __SUNPRO_CC
  #include "perf_count_sparc.h"
#elif defined USE_PCM
  #include "gail/perf_count_pcm.h"
#else
  #include "perf_count_papi.h"
#endif

#endif //PERF_COUNT_H
