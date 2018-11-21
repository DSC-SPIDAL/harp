#ifndef PBLOCKER_H_
#define PBLOCKER_H_

#include <algorithm>
#include <cinttypes>
#include <cstring>
#include <iostream>
#include <utility>

#include "commons/platform_atomics.h"
#include "pvector.h"


template <typename NodeID_>
class Buffer;

template <typename NodeID_>
class PBlocker {
public:
  typedef EdgePair<NodeID_, NodeID_> Edge;
  typedef pvector<Edge> EdgeList;
  friend class Buffer<NodeID_>;

  PBlocker(int64_t log_width_bytes, NodeID_ num_nodes, const EdgeList &el,
          bool forward, bool reverse) :
      num_bins_(CalcNumBins(log_width_bytes, el.size())), bins_(num_bins_) {
    shamt_ = 0;
    size_t nodes_per_bins = (num_nodes + num_bins_ - 1) / num_bins_;
    while ((1l<<shamt_) < nodes_per_bins)
      shamt_++;
    std::cout << "bins:  " << num_bins_ << std::endl;
    std::cout << "shamt: " << shamt_ << std::endl;
    pvector<size_t> bin_sizes = BinSizeHistogramParallel(el, forward, reverse);
    #pragma omp parallel for
    for (int b=0; b < num_bins_; b++) {
      bins_[b].resize(bin_sizes[b]);
      TouchMem(bins_[b]);
      bins_[b].clear();
    }
  }

  int get_num_bins() const {
    return num_bins_;
  }

  pvector<Edge>& GetBin(int b) {
    return bins_[b];
  }


protected:
  int num_bins_;
  int shamt_;
  const static int kPageSize_ = 4096;
  pvector<pvector<Edge>> bins_;

  static
  int64_t CalcNumBins(int64_t log_width_bytes, int64_t num_edges) {
    int64_t edges_per_bin = (1l << log_width_bytes) / sizeof(Edge);
    return (num_edges + edges_per_bin - 1) / edges_per_bin;
  }

  int CalcBin(NodeID_ dest) {
    return dest >> shamt_;
  }

  pvector<size_t> BinSizeHistogram(const EdgeList &el, bool add_forward_edge,
      bool add_reverse_edge) {
    pvector<size_t> totals(num_bins_, 0);
    for (Edge e : el) {
      if (add_forward_edge)
        totals[CalcBin(e.v)]++;
      if (add_reverse_edge)
        totals[CalcBin(e.u)]++;
    }
    return totals;
  }

  pvector<size_t> BinSizeHistogramParallel(const EdgeList &el,
      bool add_forward_edge, bool add_reverse_edge) {
    pvector<size_t> totals(num_bins_, 0);
    #pragma omp parallel
    {
      pvector<size_t> local_totals(num_bins_, 0);
      #pragma omp for nowait
      for (Edge* it=el.begin(); it < el.end(); it++) {
        const Edge e = *it;
        if (add_forward_edge)
          local_totals[CalcBin(e.v)]++;
        if (add_reverse_edge)
          local_totals[CalcBin(e.u)]++;
      }
      for (int b=0; b < num_bins_; b++) {
        fetch_and_add(totals[b], local_totals[b]);
      }
    }
    return totals;
  }

  void TouchMem(pvector<Edge> &bin) {
    const size_t stride = kPageSize_ / sizeof(Edge);
    for (size_t i=0; i<bin.size(); i+=stride) {
      bin[i] = Edge(-1,-1);
    }
  }
};



template <typename NodeID_>
class Buffer {
  typedef EdgePair<NodeID_, NodeID_> Edge;
public:
  Buffer(PBlocker<NodeID_> &pb) :
      num_bins_(pb.num_bins_), shamt_(pb.shamt_), bins_(pb.bins_),
      tails_(num_bins_) {
    size_t buffer_size_bytes = num_bins_ * kBufferSize_*sizeof(Edge);
    buffer_ = (Edge*) aligned_alloc(64, buffer_size_bytes);
    for (int b=0; b < num_bins_; b++) {
      tails_[b] = b * kBufferSize_;
    }
  }

  ~Buffer() {
    delete buffer_;
  }

  int CalcBin(NodeID_ dest) {
    return dest >> shamt_;
  }

  void SendMsg(const NodeID_ source, NodeID_ dest) {
    const int b = CalcBin(dest);
    buffer_[tails_[b]++] = Edge(source, dest);
    if (tails_[b] == (b+1)*kBufferSize_) {
      const size_t copy_size_bytes = kBufferSize_ * sizeof(Edge);
      Edge* dest = bins_[b].append_atomic(kBufferSize_);
      AVXmemcpy(dest, buffer_ + b*kBufferSize_, copy_size_bytes);
      tails_[b] = b*kBufferSize_;
    }
  }

  void Flush() {
    for (int b=0; b < num_bins_; b++) {
      const size_t elements_left = tails_[b] - b*kBufferSize_;
      Edge* dest = bins_[b].append_atomic(elements_left);
      memcpy(dest, buffer_ + b*kBufferSize_, elements_left * sizeof(Edge));
      tails_[b] = b * kBufferSize_;
    }
  }

  void AVXmemcpy(void* dst, void* src, const uint64_t size) const {
    float* f_dst = (float*) dst;
    float* f_src = (float*) src;
    for (uint64_t i=0; i<size; i+=sizeof(__m256)) {
      _mm256_stream_ps(f_dst, _mm256_load_ps(f_src));
      f_dst += sizeof(__m256) / sizeof(float);
      f_src += sizeof(__m256) / sizeof(float);
    }
  }

private:
  int num_bins_;
  int shamt_;
  const static int kBufferSize_ = 64;
  Edge* buffer_;
  pvector<pvector<Edge>> &bins_;
  pvector<size_t> tails_;
};

#endif  // PBLOCKER_H_
