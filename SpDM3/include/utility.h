#ifndef __SPDM3_UTILITY_H__
#define __SPDM3_UTILITY_H__

#include <cassert>
#include <cmath>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <list>
#include <string>
#include <sstream>
#include <typeinfo>

#define EPSILON	1E-08
#define FEQ(a, b) (fabs((a)-(b)) <= EPSILON)
#define TEQ(a, b, threshold) (fabs((a)-(b)) <= threshold)
#define FREE_IF_NOT_NULL(x) if(x != NULL) { delete [] x; x = NULL; }
#define MOD_POSITIVE(x, m) (((x % m) + m) % m)

namespace spdm3 {

int find_option(int argc, char *argv[], const char *option);
double read_timer();
int read_int(int argc, char *argv[], const char *option, int default_value);
double read_double(int argc, char *argv[], const char *option, double default_value);
char *read_string(int argc, char *argv[], const char *option, char *default_value);

int reverse_bit(int num, int size);
int find_count(int n, int p, int rank);
int find_count_frontloaded(int n, int p, int rank);
int find_offset_frontloaded(int n, int p, int rank);
void find_grid_size(int size, int *rows, int *cols);
void parse_npy_header(const char* filename, unsigned int& word_size,
                      unsigned int*& shape, unsigned int& ndims,
                      bool& fortran_order, long long int& fpos_offset);
void parse_npy_header(FILE* fp, unsigned int& word_size,
                      unsigned int*& shape, unsigned int& ndims,
                      bool& fortran_order);
FILE *parse_matrix_market_header(const char *filename,
                                 int64_t &nrows, int64_t &ncols,
                                 int64_t &nonzeros, int64_t &linesread,
                                 int &type, int &symmetric);
char BigEndianTest();
char map_type(const std::type_info &t);

void get_npy_shapes(const char *filename,
                    unsigned int *&shape, unsigned int &ndims);
  
template <typename VT>
int num_comparator(VT a, VT b) {
  if (a == b) return 0;
  else if (a > b) return 1;
  return -1;
}

template <typename VT>
void fill_vec(VT *vec, int n, VT val);

template <typename IT>
bool is_powers_of_two(IT x) {
  if (x == 0) return false;
  return ((x & (x-1)) == 0);
}

template <typename IT>
void gen_displs_from_counts(int n, IT *count, IT *displs) {
  displs[0] = 0;
  for (int i = 0; i < n; ++i)
    displs[i+1] = displs[i] + count[i];
}

template <typename IT>
void gen_counts_displs_balanced(int n, int p, IT *counts, IT *displs) {
  int r = n % p;
  int min_count = (IT)(n / p);
  fill_vec(counts, p, min_count);
  
  if (r > 0) {
    // Counts will still be balanced if concatenated.
    if (is_powers_of_two(p)) {
      for (int i = 0; i < r; ++i)
        ++counts[reverse_bit(i, p-1)];
    } else {
      int increased = 1;
      ++counts[0];
      std::list< std::pair<IT, IT> > queue;
      queue.emplace_back(std::make_pair(0, p));
      while (increased < r && !queue.empty()) {
        auto pair = queue.front();
        queue.pop_front();
        IT min = pair.first;
        IT max = pair.second;
        IT mid = (min + max)/2;
        
        ++counts[mid];
        ++increased;
        
        queue.emplace_back(std::make_pair(min, mid));
        queue.emplace_back(std::make_pair(mid, max));
      }
      queue.clear();
    }
  }
  gen_displs_from_counts(p, counts, displs);
}

template <typename IT>
void gen_counts_displs_frontloaded(int n, int p, IT *counts, IT *displs) {
  int i;
  int r = n % p;
  int min_count = (IT)(n / p);
  for (i = 0; i < r; ++i)
    counts[i] = min_count + 1;
  for (; i < p; ++i)
    counts[i] = min_count;
  gen_displs_from_counts(p, counts, displs);
}

//
// Queries.
//
template <typename IT>
IT find_owner_uniform_frontloaded(IT id, IT size, IT nprocs) {
  IT div = size / nprocs;
  IT rmd = size % nprocs;
  IT larger = (rmd == 0)? div : div + 1;

  IT threshold = larger * rmd;
  if (id > threshold)
    return (id - threshold) / div + rmd;
  return id / larger;
}

template <typename IT>
IT find_owner_uniform(IT id, IT nprocs, IT *displs) {
  IT size = displs[nprocs];
  IT bin = (IT) floor((float) id / (float) size * (float) nprocs);
  if (displs[bin] <= id) {
    assert(id < displs[bin+1]);
    return bin;
  } else {
    assert(displs[bin-1] <= id);
    return bin-1;
  }
}

// Binary search.
template <typename IT>
IT find_owner_search(IT id, IT nprocs, IT *displs) {
  IT min = 0, max = nprocs;
  IT mid = (min + max) >> 1;
  while (min != mid && id != displs[mid]) {
    if (id > displs[mid]) min = mid;
    else                  max = mid;
    mid = (min + max) >> 1;
  }
  if (id < displs[mid])
    --mid;
  return mid;
}

//
// Vectors.
//
template <typename VT>
VT sum(VT *vec, int n) {
  VT total = 0;
  for (int i = 0; i < n; ++i)
    total += vec[i];
  return total;
}

template <typename VT>
void fill_vec(VT *vec, int n, VT val) {
  for (int i = 0; i < n; ++i)
    vec[i] = val;
}

template <typename VT>
void fill_series(VT *vec, int n, VT start) {
  for (int i = 0; i < n; ++i)
    vec[i] = start + i;
}

template <typename VT>
void print_vec(VT *vec, int n) {
  for (int i = 0; i < n; ++i)
    std::cout << std::setw(6) << vec[i] << " ";
  std::cout << std::endl;
}

template <typename VT>
void save_vec(const char *filename, int rank, VT *vec, int n) {
  char name[100];
  sprintf(name, "%s-%05d", filename, rank);
  std::ofstream output;
  output.open(name);
  for (int i = 0; i < n; ++i)
    output << std::setw(6) << vec[i] << " ";
  output << std::endl;
  output.close();
}

template <typename VT>
bool vec_equal(VT *a, VT *b, int n) {
  for (int i = 0; i < n; ++i)
    if (!FEQ(a[i], b[i]))
      return false;
  return true;
}

template <typename T>
std::string create_npy_header(const T* data, const unsigned int* shape,
                              const unsigned int ndims, bool fortran_order) {
  std::stringstream dict;
  dict << "{'descr': '";
  dict << BigEndianTest();
  dict << map_type(typeid(T));
  dict << sizeof(T);
  dict << "', 'fortran_order': ";
  if (fortran_order)
    dict << "True";
  else
    dict << "False";
  dict << ", 'shape': (";
  dict << shape[0];
  for(int i = 1;i < ndims;i++) {
      dict << ", ";
      dict << shape[i];
  }
  if(ndims == 1) dict << ",";
  dict << "), }";
  //pad with spaces so that preamble+dict is modulo 16 bytes. preamble is 10 bytes. dict needs to end with \n
  int remainder = 16 - (10 + dict.str().size() + 1) % 16;
  for (int i = 0; i < remainder; ++i)
    dict << " ";
  dict << std::endl;
  unsigned short dict_size = dict.str().size();
  char *dict_size_ptr = (char *) &dict_size;

  std::stringstream header;
  header << (char) 0x93;
  header << "NUMPY";
  header << (char) 0x01; //major version of numpy format
  header << (char) 0x00; //minor version of numpy format
  header << *(dict_size_ptr) << *(dict_size_ptr+1);
  header << dict.str();

  return header.str();
}

template <typename T>
void write_npy_header(FILE *fp, const T *data,
                      const unsigned int *shape, const unsigned int ndims,
                      bool fortran_order) {
  std::string header = create_npy_header<T>(NULL, shape, ndims, fortran_order);
  fwrite(header.c_str(), sizeof(char), header.size(), fp);
}

template <typename T>
long int write_npy_header(const char *filename, const T *data,
                          const unsigned int* shape, const unsigned int ndims,
                          bool fortran_order, int64_t &fpos_offset) {
  FILE *fp = fopen(filename, "wb");
  write_npy_header(fp, data, shape, ndims, fortran_order);
  fpos_offset = ftell(fp);
  fclose(fp);
  return fpos_offset;
}

template <typename _ForwardIter, typename T>
void my_iota(_ForwardIter first, _ForwardIter last, T val) {
  while (first != last)
    *first++ = val++;
}

}  // namespace spdm3

#endif  // __SPDM3_UTILITY_H__
