#ifndef BLOCKED_GRAPH_H
#define BLOCKED_GRAPH_H

#include <iostream>

#include "pvector.h"
#include "commons/benchmark.h"


template <typename NodeID_>
class BlockedGraph {
public:
  typedef std::pair<NodeID_, NodeID_> Edge;

  BlockedGraph(int num_blocks, const RGraph &g, bool row_major=true) :
      blocks_(num_blocks), num_blocks_(num_blocks), row_major_(row_major) {
    block_width_ = (g.num_nodes() + num_blocks - 1) / num_blocks;
    pvector<size_t> sizes = BlockSizeHistogram(g);
    std::cout << "blocks: " << num_blocks << std::endl;
    std::cout << "width:  " << block_width_ << std::endl;
    for (int b=0; b < num_blocks; b++) {
      blocks_[b].reserve(sizes[b]);
    }
    PopulateBlocks(g);
    num_nodes_ = g.num_nodes();
  }

  pvector<size_t> BlockSizeHistogram(const RGraph &g) {
    pvector<size_t> totals(num_blocks_, 0);
    if (row_major_) {
      for (NodeID_ n=0; n<g.num_nodes(); n++) {
        totals[n/block_width_] += g.in_degree(n);
      }
    } else {
      for (NodeID_ n=0; n<g.num_nodes(); n++) {
        totals[n/block_width_] += g.out_degree(n);
      }
    }
    return totals;
  }

  void PopulateBlocks(const RGraph &g) {
    if (row_major_) {
      for (NodeID_ u=0; u < g.num_nodes(); u++) {
        for (NodeID v : g.out_neigh(u)) {
          blocks_[v/block_width_].push_back(std::make_pair(u,v));
        }
      }
    } else {
      for (NodeID_ u=0; u < g.num_nodes(); u++) {
        for (NodeID v : g.in_neigh(u)) {
          blocks_[v/block_width_].push_back(std::make_pair(u,v));
        }
      }
    }
  }

  pvector<Edge>& GetBlock(int b) {
    return blocks_[b];
  }

  NodeID num_nodes() const {
    return num_nodes_;
  }

  int num_blocks() const {
    return num_blocks_;
  }

  NodeID block_start(int b) const {
    return b * block_width_;
  }

  NodeID block_end(int b) const {
    return std::min((b+1) * block_width_, num_nodes_);
  }


  pvector<pvector<Edge>> blocks_;
  int num_blocks_;
  NodeID_ block_width_;
  NodeID_ num_nodes_;
  bool row_major_;
};

#endif  // BLOCKED_GRAPH_H
