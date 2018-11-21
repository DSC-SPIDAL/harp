#include <algorithm>
#include <cinttypes>
#include <iostream>
#include <stdlib.h>

// #include "asmlib.h"

#include "commons/timer.h"

#ifdef USE_PC
  #include "../perf_count.h"
  #define IF_PC_ON(x) x
#else
  #define IF_PC_ON(x)
#endif


using namespace std;


const uint64_t align_to = 32;


void simple_memcpy(void* dst, void* src, uint64_t size) {
  char* s = (char*) src;
  char* d = (char*) dst;
  char* end = d + size;
  while (d != end) {
    *d = *s;
    d++;
    s++;
  }
}

void sse_memcpy(void* dst, void* src, uint64_t size) {
  __m128i* f_dst = (__m128i*) dst;
  __m128i* f_src = (__m128i*) src;
  for (uint64_t i=0; i<size; i+=sizeof(__m128i)) {
    _mm_stream_si128(f_dst, _mm_stream_load_si128(f_src));
    f_dst++;
    f_src++;
  }
}

void avx_memcpy(void* dst, void* src, uint64_t size) {
  float* f_dst = (float*) dst;
  float* f_src = (float*) src;
  for (uint64_t i=0; i<size; i+=sizeof(__m256)) {
    _mm256_store_ps(f_dst, _mm256_load_ps(f_src));
    f_dst += sizeof(__m256) / sizeof(float);
    f_src += sizeof(__m256) / sizeof(float);
  }
}

void avx_memcpy2(void* dst, void* src, uint64_t size) {
  float* f_dst = (float*) dst;
  float* f_src = (float*) src;
  for (uint64_t i=0; i<size; i+=sizeof(__m256)) {
    _mm256_stream_ps(f_dst, _mm256_load_ps(f_src));
    f_dst += sizeof(__m256) / sizeof(float);
    f_src += sizeof(__m256) / sizeof(float);
  }
}

void avx_memcpy3(void* dst, void* src, uint64_t size) {
  // float* f_dst = (float*) dst;
  float* f_dst = (float*) dst + size/2/sizeof(float);
  float* f_src = (float*) src;
  float total = 0;
  bool swapped = false;
  for (uint64_t i=0; i<size; i+=sizeof(__m256)) {
    _mm256_stream_ps(f_dst, _mm256_load_ps(f_src));
    f_dst += sizeof(__m256) / sizeof(float);
    f_src += sizeof(__m256) / sizeof(float);
    // if ((i&(1*sizeof(__m256)-1)) == 0)
    //   total += *f_dst;
    if (i > (0.5f * size) && !swapped) {
      // f_src = (float*) src;
      // f_dst = (float*) dst;
      f_dst = (float*) dst + size/2/sizeof(float);
      swapped = true;
      // break;
    }
  }
  cout << total << endl;
}

void ermsb_memcpy(void* dst, const void* src, size_t size) {
    __asm__ __volatile__("rep movsb" : "+D"(dst), "+S"(src), "+c"(size) : : "memory");
}

uint64_t* gen_array(uint64_t n) {
  uint64_t* a = (uint64_t*) aligned_alloc(align_to, n*sizeof(uint64_t));
  for (uint64_t i=0; i<n; i++)
    a[i] = i;
  return a;
}

bool check_array(uint64_t *a, uint64_t n) {
  for (uint64_t i=0; i<n; i++)
    if (a[i] != i)
      return false;
  return true;
}

void benchmark_copy(uint64_t n) {
  uint64_t* dst = (uint64_t*) aligned_alloc(align_to, n*sizeof(uint64_t));
  std::fill(dst, dst + n, 0);
  uint64_t* src = gen_array(n);
  // uint64_t total = 0;
  // for (uint64_t i=0; i<n; i+=128)
  //   total += dst[i];
  IF_PC_ON(PerfCount pc);
  Timer t;
  IF_PC_ON(pc.Start());
  t.Start();
  // copy(src, src+n, dst);
  // memcpy(dst, src, n*sizeof(uint64_t));
  // SetMemcpyCacheLimit(64);
  // A_memcpy(dst, src, n*sizeof(uint64_t));
  // simple_memcpy(dst, src, n*sizeof(uint64_t));
  avx_memcpy2(dst, src, n*sizeof(uint64_t));
  // avx_memcpy3(dst, src, n*sizeof(uint64_t));
  // sse_memcpy(dst, src, n*sizeof(uint64_t));
  // ermsb_memcpy(dst, src, n*sizeof(uint64_t));
  // for (uint64_t i=0; i<n; i++)
  //   total += src[i];
  // cout << "total " << total << endl;
  t.Stop();
  IF_PC_ON(pc.Stop());
  IF_PC_ON(pc.PrintAll());
  cout << "Took: " << t.Millisecs() << "ms" << endl;
  cout << "Result was " << check_array(dst, n) << endl;
  cout << "Rate was " << n * sizeof(uint64_t) / t.Seconds() / 1e9 << " GB/s" << endl;
  delete[] src;
  delete[] dst;
}

// int main(int argc, char* argv[]) {
//   benchmark_copy(1l<<atoi(argv[1]));
//   return 0;
// }
