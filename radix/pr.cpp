#include <iostream>
#include <math.h>
#include <omp.h>
#include <string.h>
#include <utility>
#include <vector>

#include "pvector.h"
#include "commons/benchmark.h"
#include "commons/bitmap.h"
#include "commons/bucket.h"
#include "commons/builder.h"
#include "commons/command_line.h"
#include "commons/graph.h"
#include "partitioner.h"
#include "par_partitioner.h"
#include "guider.h"
#include "par_guider.h"
#include "blocked_graph.h"

#include "pr.h"

/*
GAP Benchmark Suite
Kernel: PageRank (PR)
Author: Scott Beamer

Will return pagerank scores for all vertices once total change < epsilon

This PR implementation uses the traditional iterative approach. This is done
to ease comparisons to other implementations (often use same algorithm), but
it is not necesarily the fastest way to implement it. It does perform the
updates in the pull direction to remove the need for atomics.
*/


using namespace std;

// typedef float ScoreT;
// const float kDamp = 0.85;
// const double kGoalEpsilon = 1e-4;

// single pull iteration (doesn't update scores) to see error
double CheckPageRank(const RGraph &g, const pvector<ScoreT> &scores) {
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++)
    outgoing_contrib[n] = scores[n] / g.out_degree(n);
  double error = 0;
  #pragma omp parallel for
  for (NodeID u=0; u < g.num_nodes(); u++) {
    ScoreT incoming_total = 0;
    for (NodeID v : g.in_neigh(u))
      incoming_total += outgoing_contrib[v];
    error += fabs(base_score + kDamp * incoming_total - scores[u]);
  }
  return error;
}

pvector<ScoreT> PageRankPush(const RGraph &g, int num_iterations) {
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> sums(g.num_nodes(), 0);
  for (int iter=0; iter < num_iterations; iter++) {
    #pragma omp parallel for
    for (NodeID u=0; u < g.num_nodes(); u++) {
      const ScoreT to_send = scores[u] / g.out_degree(u);
      for (NodeID v : g.out_neigh(u)) {
        #pragma omp atomic   // no hardware fp atomics
        sums[v] += to_send;
      }
    }
    #pragma omp parallel for
    for (NodeID n=0; n < g.num_nodes(); n++) {
      scores[n] = base_score + kDamp * sums[n];
      sums[n] = 0;
    }
  }
  return scores;
}

pvector<ScoreT> PageRankPull(const RGraph &g, int max_iters, double epsilon ) {
                             // double epsilon = 0) 
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  for (int iter=0; iter < max_iters; iter++) {
    double error = 0;
    #pragma omp parallel for schedule(dynamic,1024)
    for (NodeID n=0; n < g.num_nodes(); n++)
      outgoing_contrib[n] = scores[n] / g.out_degree(n);
    #pragma omp parallel for reduction(+ : error) schedule(dynamic,64)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      ScoreT incoming_total = 0;
      for (NodeID v : g.in_neigh(u))
        incoming_total += outgoing_contrib[v];
      ScoreT old_score = scores[u];
      scores[u] = base_score + kDamp * incoming_total;
      error += fabs(scores[u] - old_score);
    }
    printf(" %2d    %lf\n", iter, error);
    if (error < epsilon)
      break;
  }
  return scores;
}

// doesn't wait update scores synchronously
// during computation stores scores[n] as scores[n] / out_degree(n)
//   if out_degree=0, score stored normally
pvector<ScoreT> PageRankPullRacy(const RGraph &g, int max_iters, double epsilon) {
                                 // double epsilon = 0) 
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++) {
    if (g.out_degree(n) > 0)
      scores[n] = init_score / g.out_degree(n);
    else if (g.in_degree(n) > 0)
      scores[n] = init_score;
    else
      scores[n] = base_score;
  }
  int iter = 0;
  double error = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    #pragma omp parallel for reduction(+ : error)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      if (g.out_degree(u) > 0) {
        ScoreT incoming_total = 0;
        for (NodeID v : g.in_neigh(u))
          incoming_total += scores[v];
        ScoreT old_score = scores[u] * g.out_degree(u);
        ScoreT new_score = base_score + kDamp * incoming_total;
        error += fabs(new_score - old_score);
        scores[u] = new_score / g.out_degree(u);
      } else if (g.in_degree(u) > 0) {
        ScoreT incoming_total = 0;
        for (NodeID v : g.in_neigh(u))
          incoming_total += scores[v];
        ScoreT old_score = scores[u];
        ScoreT new_score = base_score + kDamp * incoming_total;
        error += fabs(new_score - old_score);
        scores[u] = new_score;
      }
    }
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++)
    if (g.out_degree(n) > 0)
      scores[n] = scores[n] * g.out_degree(n);
  // cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankPullDiff(const RGraph &g, int max_iters, double epsilon) {
                                 // double epsilon = 0) 
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes());
  pvector<ScoreT> outgoing_contrib(g.num_nodes());  // redundant, but like name
  pvector<ScoreT> curr_diff(g.num_nodes());
  pvector<ScoreT> next_diff(g.num_nodes());
  double error = 0;
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++)
    outgoing_contrib[n] = init_score / g.out_degree(n);
  #pragma omp parallel for reduction(+ : error)
  for (NodeID u=0; u < g.num_nodes(); u++) {
    ScoreT incoming_total = 0;
    for (NodeID v : g.in_neigh(u))
      incoming_total += outgoing_contrib[v];
    ScoreT old_score = init_score;
    scores[u] = base_score + kDamp * incoming_total;
    error += fabs(scores[u] - old_score);
    next_diff[u] = scores[u] - old_score;
  }
  cout << " 0    " << error << endl;
  int iter = 0;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    #pragma omp parallel for
    for (NodeID n=0; n < g.num_nodes(); n++)
      curr_diff[n] = next_diff[n] / g.out_degree(n);
    #pragma omp parallel for reduction(+ : error)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      ScoreT incoming_diff_total = 0;
      for (NodeID v : g.in_neigh(u))
        incoming_diff_total += curr_diff[v];
      next_diff[u] = kDamp * incoming_diff_total;
      scores[u] += kDamp * incoming_diff_total;
      error += fabs(kDamp * incoming_diff_total);
    }
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankPullFilter(const RGraph &g, int max_iters, double epsilon) {
                                   // double epsilon = 0) 
  const ScoreT active_frac = 10 * epsilon;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  Bitmap unchanged(g.num_nodes());
  unchanged.reset();
  int iter = 0;
  double error = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    NodeID awake_count = 0;
    #pragma omp parallel for
    for (NodeID n=0; n < g.num_nodes(); n++)
      outgoing_contrib[n] = scores[n] / g.out_degree(n);
    #pragma omp parallel for reduction(+ : error) reduction(+ : awake_count)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      if (unchanged.get_bit(u))
        continue;
      ScoreT incoming_total = 0;
      for (NodeID v : g.in_neigh(u))
        incoming_total += outgoing_contrib[v];
      ScoreT old_score = scores[u];
      scores[u] = base_score + kDamp * incoming_total;
      double diff = fabs(scores[u] - old_score);
      if (diff < scores[u] * active_frac)
        unchanged.set_bit_atomic(u);
      else
        awake_count++;
      error += fabs(scores[u] - old_score);
    }
    cout << " " << iter << "    " << error << " " << awake_count << endl;
    iter++;
  }
  // cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankPullFilterDiff(const RGraph &g, int max_iters, double epsilon) {
                                       // double epsilon = 0) 
  const ScoreT active_frac = epsilon / 10;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  pvector<ScoreT> next_diff(g.num_nodes());
  Bitmap unchanged(g.num_nodes());
  unchanged.reset();
  double error = 0;
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++)
    outgoing_contrib[n] = init_score / g.out_degree(n);
  #pragma omp parallel for reduction(+ : error)
  for (NodeID u=0; u < g.num_nodes(); u++) {
    ScoreT incoming_total = 0;
    for (NodeID v : g.in_neigh(u))
      incoming_total += outgoing_contrib[v];
    ScoreT old_score = init_score;
    scores[u] = base_score + kDamp * incoming_total;
    error += fabs(scores[u] - old_score);
    next_diff[u] = scores[u] - old_score;
  }
  cout << " 0    " << error << endl;
  int iter = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    NodeID awake_count = 0;
    #pragma omp parallel for
    for (NodeID n=0; n < g.num_nodes(); n++)
      outgoing_contrib[n] = next_diff[n] / g.out_degree(n);
    #pragma omp parallel for reduction(+ : error) reduction(+ : awake_count)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      if (unchanged.get_bit(u))
        continue;
      ScoreT incoming_diff_total = 0;
      for (NodeID v : g.in_neigh(u))
        if (unchanged.get_bit(v))
          incoming_diff_total += outgoing_contrib[v];
      ScoreT old_score = scores[u];
      next_diff[u] = kDamp * incoming_diff_total;
      scores[u] += kDamp * incoming_diff_total;
      error += fabs(kDamp * incoming_diff_total);
      double diff = fabs(scores[u] - old_score);
      if (diff < scores[u] * active_frac)
        unchanged.set_bit_atomic(u);
      else
        awake_count++;
      error += fabs(scores[u] - old_score);
    }
    cout << " " << iter << "    " << error << " " << awake_count << endl;
    iter++;
  }
  cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

// always receive, just filter who read
pvector<ScoreT> PageRankPullFilterDiff2(const RGraph &g, int max_iters, double epsilon) {
                                        // double epsilon = 0) 
  const ScoreT active_frac = epsilon * 100;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes());
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  pvector<ScoreT> next_diff(g.num_nodes());
  Bitmap unchanged(g.num_nodes()), next_unchanged(g.num_nodes());
  unchanged.reset();
  next_unchanged.reset();
  double error = 0;
  #pragma omp parallel for
  for (NodeID n=0; n < g.num_nodes(); n++)
    outgoing_contrib[n] = init_score / g.out_degree(n);
  #pragma omp parallel for reduction(+ : error)
  for (NodeID u=0; u < g.num_nodes(); u++) {
    ScoreT incoming_total = 0;
    for (NodeID v : g.in_neigh(u))
      incoming_total += outgoing_contrib[v];
    scores[u] = base_score + kDamp * incoming_total;
    error += fabs(scores[u] - init_score);
    next_diff[u] = scores[u] - init_score;
  }
  cout << " 0    " << error << endl;
  int iter = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    NodeID awake_count = 0;
    #pragma omp parallel for
    for (NodeID n=0; n < g.num_nodes(); n++)
      outgoing_contrib[n] = next_diff[n] / g.out_degree(n);
    #pragma omp parallel for reduction(+ : error) reduction(+ : awake_count)
    for (NodeID u=0; u < g.num_nodes(); u++) {
      ScoreT incoming_diff_total = 0;
      for (NodeID v : g.in_neigh(u))
        if (!unchanged.get_bit(v))
          incoming_diff_total += outgoing_contrib[v];
      ScoreT old_score = scores[u];
      next_diff[u] = kDamp * incoming_diff_total;
      scores[u] += kDamp * incoming_diff_total;
      error += fabs(kDamp * incoming_diff_total);
      double diff = fabs(scores[u] - old_score);
      if (diff < scores[u] * active_frac)
        next_unchanged.set_bit_atomic(u);
      else
        awake_count++;
      error += fabs(scores[u] - old_score);
    }
    unchanged.swap(next_unchanged);
    next_unchanged.reset();
    cout << " " << iter << "    " << error << " " << awake_count << endl;
    iter++;
  }
  // cout << "one more ":" << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankPullBlock(const RGraph &g, int max_iters, double epsilon) {
                                  // double epsilon = 0) 
  const NodeID blocks = 5, block_inc = (g.num_nodes() + blocks - 1) / blocks;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  pvector<ScoreT> incoming_sum(g.num_nodes());
  pvector<RGraph::Neighborhood::iterator> curr_neigh_it(g.num_nodes());
  int iter = 0;
  double error = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    #pragma omp parallel
    {
      #pragma omp for
      for (NodeID n=0; n < g.num_nodes(); n++) {
        curr_neigh_it[n] = g.in_neigh(n).begin();
        incoming_sum[n] = 0;
        outgoing_contrib[n] = scores[n] / g.out_degree(n);
      }
      for (NodeID block=0; block < g.num_nodes(); block += block_inc) {
        NodeID block_end = block + block_inc;
        #pragma omp for nowait schedule(dynamic, 1024)
        for (NodeID u=0; u < g.num_nodes(); u++) {
          for (curr_neigh_it[u];
               curr_neigh_it[u] < g.in_neigh(u).end();
               ++curr_neigh_it[u]) {
            NodeID v = *curr_neigh_it[u];
            if (v >= block_end)
              break;
            incoming_sum[u] += outgoing_contrib[v];
          }
        }
      }
      #pragma omp barrier
      #pragma omp for reduction(+ : error)
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        error += fabs(scores[n] - old_score);
      }
    }
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  // cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankPullSparseBlock(const RGraph &g, int max_iters, double epsilon) {
                                        // double epsilon = 0) 
  const NodeID blocks = 5, block_inc = (g.num_nodes() + blocks - 1) / blocks;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> outgoing_contrib(g.num_nodes());
  pvector<ScoreT> incoming_sum(g.num_nodes());
  pvector<RGraph::Neighborhood::iterator> curr_neigh_it(g.num_nodes());
  Timer t;
  t.Start();
  vector<vector<NodeID>> indices(blocks);
  // vector<Bitmap>> indices(blocks, g.num_nodes());s
  for (NodeID u=0; u < g.num_nodes(); u++) {
    // curr_neigh_it[u] = g.in_neigh(n).begin();
    for (NodeID v : g.in_neigh(u)) {
      NodeID dest_block = v / block_inc;
      if (indices[dest_block].empty() || indices[dest_block].back() != u)
        indices[dest_block].push_back(u);
    }
  }
  for (auto i : indices) {
    cout << i.size() << endl;
  }
  t.Stop();
  PrintTime("Index Generation", t.Seconds());
  int iter = 0;
  double error = 1;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    #pragma omp parallel
    {
      #pragma omp for
      for (NodeID n=0; n < g.num_nodes(); n++) {
        curr_neigh_it[n] = g.in_neigh(n).begin();
        incoming_sum[n] = 0;
        outgoing_contrib[n] = scores[n] / g.out_degree(n);
      }
      for (NodeID block=0; block < blocks; block++) {
        NodeID block_end = (block+1) * block_inc;
        #pragma omp for nowait schedule(dynamic, 1024)
        for (auto it=indices[block].begin(); it < indices[block].end(); ++it) {
          NodeID u = *it;
          for (curr_neigh_it[u];
               curr_neigh_it[u] < g.in_neigh(u).end();
               ++curr_neigh_it[u]) {
            NodeID v = *curr_neigh_it[u];
            if (v >= block_end)
              break;
            incoming_sum[u] += outgoing_contrib[v];
          }
        }
      }
      #pragma omp barrier
      #pragma omp for reduction(+ : error)
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        error += fabs(scores[n] - old_score);
      }
    }
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  cout << "one more " << CheckPageRank(g, scores) << endl;
  return scores;
}

pvector<ScoreT> PageRankRadixClassic(const RGraph &g, int max_iters, double epsilon) {
                                     // double epsilon = 0) 
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const NodeID num_blockers = 1024;
  const NodeID block_width = (g.num_nodes() + num_blockers - 1) / num_blockers;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  vector<Bucket<NodeScorePair>> global_blockers(num_blockers);
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  int iter = 0;
  double error = 1;
  #pragma omp parallel
  {
    vector<vector<NodeScorePair>> local_blockers(num_blockers);
    while (iter < max_iters && error > epsilon) {
      #pragma omp for nowait
      for (NodeID u=0; u < g.num_nodes(); u++) {
        ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
        for (NodeID v : g.out_neigh(u)) {
          local_blockers[v / block_width].push_back(
              make_pair(v, outgoing_contrib));
        }
      }
      for (int b=0; b < num_blockers; b++)
        global_blockers[b].swap_vector_in(local_blockers[b]);
      #pragma omp single
      error = 0;
      #pragma omp for
      for (int b=0; b < num_blockers; b++) {
        for (auto nsp : global_blockers[b]) {
          NodeID n = nsp.first;
          ScoreT incoming_contrib = nsp.second;
          incoming_sum[n] += incoming_contrib;
        }
        global_blockers[b].clear();
      }
      #pragma omp for reduction(+ : error)
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        error += fabs(scores[n] - old_score);
        incoming_sum[n] = 0;
      }
      #pragma omp single
      {
        cout << " " << iter << "    " << error << endl;
        iter++;
      }
    }
  }
  return scores;
}


pvector<ScoreT> PageRankRadix(const RGraph &g, int max_iters, double epsilon,
                              Partitioner<NodeID, ScoreT> &parts) {
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  int iter = 0;
  double error = 1;
  Timer t;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    // Start measuring perf counters
    t.Start();
    for (NodeID u=0; u < g.num_nodes(); u++) {
      ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
      for (NodeID v : g.out_neigh(u)) {
        parts.SendMsg(v, outgoing_contrib);
      }
    }
    parts.Flush();
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Scatter", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (int b=0; b < parts.get_num_bins(); b++) {
      pvector<NodeScorePair> &curr_block = parts.GetBin(b);
      for (size_t i=0; i<curr_block.size(); i++) {
        NodeID n = curr_block[i].first;
        ScoreT incoming_contrib = curr_block[i].second;
        incoming_sum[n] += incoming_contrib;
        __builtin_prefetch(&curr_block[i+stride], 0, 0);
      }
      curr_block.clear();
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Gather", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (NodeID n=0; n < g.num_nodes(); n++) {
      ScoreT old_score = scores[n];
      scores[n] = base_score + kDamp * incoming_sum[n];
      error += fabs(scores[n] - old_score);
      incoming_sum[n] = 0;
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Calculate", t.Seconds());
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  return scores;
}

/**
 * @brief check this function for our paper
 *
 * @param xArray: length of g.num_nodes() 
 * @param yArray: length of g.num_nodes() 
 * @param g: a csr input graph
 * @param max_iters
 * @param epsilon
 * @param par_parts: the dense vector ?
 *
 * @return 
 */
void SpMVRadixPar(float* xArray, float* yArray, const RGraph &g, int max_iters, 
        double epsilon, pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts)
{

  std::memset(yArray, 0, g.num_nodes()*sizeof(float));
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const size_t stride = 256;
  // pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  const int num_threads = omp_get_max_threads(); // use openmp threads
  const int num_bins = par_parts[0]->get_num_bins();
  Timer t;
  #pragma omp parallel
  {
      // int iter = 0;
      const int thread_id = omp_get_thread_num();
      ParPartitioner<NodeID, ScoreT>* my_parts = par_parts[thread_id];
      // each thread is in charge of vertices
      // in parallel
      NodeID u = my_parts->range_start(thread_id, num_threads, g);
      NodeID u_end = my_parts->range_end(thread_id, num_threads, g);

      for (; u < u_end; u++) {
          ScoreT outgoing_contrib = xArray[u];
          for (NodeID v : g.out_neigh(u)) {
              // check this SendMsg func
              my_parts->SendMsg(v, outgoing_contrib);
          }
      }

      // what is this for ??
      my_parts->Flush();
      #pragma omp barrier

#pragma omp for schedule(dynamic,1)
      for (int b=0; b < num_bins; b++) {
          for (int b_tid=0; b_tid < num_threads; b_tid++) {
              pvector<NodeScorePair> &curr_block = par_parts[b_tid]->GetBin(b);
              for (size_t i=0; i<curr_block.size(); i++) {
                  NodeID n = curr_block[i].first;
                  ScoreT incoming_contrib = curr_block[i].second;
                  // incoming_sum[n] += incoming_contrib;
                  yArray[n] += incoming_contrib;
                  // this prefetching is hardware specific
                  __builtin_prefetch(&curr_block[i+stride], 0, 0);
              }
              curr_block.clear();
          }
      }
      
  }
}

// --------------- end of SpMV kernel ---------------

pvector<ScoreT> PageRankRadixPar(const RGraph &g, int max_iters, double epsilon,
                        pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts) 
{
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  // size_t buffer_bytes = 4096;
  // size_t buffer_pairs = buffer_bytes / sizeof(NodeScorePair);
  pvector<ScoreT> scores(g.num_nodes(), init_score); // give each vertex a score
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  double global_error = 1;
  const int num_threads = omp_get_max_threads(); // use openmp threads
  const int num_bins = par_parts[0]->get_num_bins();
  Timer t;
  #pragma omp parallel
  {
    int iter = 0;
    const int thread_id = omp_get_thread_num();
    // NodeScorePair* stream_buffer = (NodeScorePair*) aligned_alloc(64, buffer_bytes);
    ParPartitioner<NodeID, ScoreT>* my_parts = par_parts[thread_id];

    while (iter < max_iters && global_error > epsilon) {

      #pragma omp single nowait
      {
        // Start measuring perf counters (parallel)
        t.Start();
      }

      // each thread is in charge of vertices
      // in parallel
      NodeID u = my_parts->range_start(thread_id, num_threads, g);
      NodeID u_end = my_parts->range_end(thread_id, num_threads, g);

      for (; u < u_end; u++) {
        ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
        for (NodeID v : g.out_neigh(u)) {
          // check this SendMsg func
          my_parts->SendMsg(v, outgoing_contrib);
        }
      }

      my_parts->Flush();
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Scatter", t.Seconds());
        global_error = 0;
        // Start measuring perf counters (parallel)
        t.Start();
      }

      #pragma omp for schedule(dynamic,1)
      for (int b=0; b < num_bins; b++) {
        for (int b_tid=0; b_tid < num_threads; b_tid++) {
          pvector<NodeScorePair> &curr_block = par_parts[b_tid]->GetBin(b);
          // size_t end_of_streaming = curr_block.size() - (curr_block.size() % buffer_pairs);
          // for (size_t i=0; i<end_of_streaming; i+= buffer_pairs) {
          //   for (size_t j=0; j<buffer_pairs; j+=(16/sizeof(NodeScorePair))) {
          //     _mm_store_si128((__m128i*) &stream_buffer[j],
          //                      _mm_stream_load_si128((__m128i*) &curr_block[i+j]));
          //   }
          //   for (size_t j=0; j<buffer_pairs; j++) {
          //     NodeID n = stream_buffer[j].first;
          //     ScoreT incoming_contrib = stream_buffer[j].second;
          //     incoming_sum[n] += incoming_contrib;
          //   }
          // }
          // for (size_t i=end_of_streaming; i<curr_block.size(); i++) {
          //   NodeID n = curr_block[i].first;
          //   ScoreT incoming_contrib = curr_block[i].second;
          //   incoming_sum[n] += incoming_contrib;
          // }
          for (size_t i=0; i<curr_block.size(); i++) {
            NodeID n = curr_block[i].first;
            ScoreT incoming_contrib = curr_block[i].second;
            incoming_sum[n] += incoming_contrib;
            // this prefetching is hardware specific
            __builtin_prefetch(&curr_block[i+stride], 0, 0);
          }
          curr_block.clear();
        }
      }
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Gather", t.Seconds());
        // Start measuring perf counters (parallel)
        t.Start();
      }
      double local_error = 0;
      #pragma omp for schedule(dynamic, 1024) nowait
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        local_error += fabs(scores[n] - old_score);
        incoming_sum[n] = 0;
      }
      #pragma omp atomic
      global_error += local_error;
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Calculate", t.Seconds());
        cout << " " << iter << "    " << global_error << endl;
      }
      iter++;
    }
    // delete[] stream_buffer;
  }

  //debug printout scores
  for (int i = 0; i < 10; ++i) {
     cout<<"Score "<<i<<" is: "<<scores[i]<<endl; 
  }
  return scores;
}


pvector<ScoreT> PageRankRadixNUMA(const RGraph &g, int max_iters, double epsilon,
                        pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts) {
  Timer t;
  t.Start();
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  pvector<ScoreT> sums0(g.num_nodes()), sums1(g.num_nodes());
  double global_error = 1;
  const int num_threads = omp_get_max_threads();
  const int num_bins = par_parts[0]->get_num_bins();
  t.Stop();
  PrintTime("Init/Alloc", t.Seconds());
  #pragma omp parallel
  {
    int iter = 0;
    const int thread_id = omp_get_thread_num();
    ParPartitioner<NodeID, ScoreT>* my_parts = par_parts[thread_id];
    const int threads_per_socket = 8;
    const int socket_offset = thread_id % threads_per_socket;
    const int socket = thread_id / threads_per_socket;
    const int bins_per_thread = (num_bins + threads_per_socket - 1) /
                                  threads_per_socket;
    const int start_bin = socket_offset * bins_per_thread;
    const int end_bin = min(start_bin + bins_per_thread, num_bins);
    const NodeID verts_per_thread = (g.num_nodes() + threads_per_socket - 1) / threads_per_socket;
    const NodeID vert_start = socket_offset * verts_per_thread;
    const NodeID vert_end = min(vert_start + verts_per_thread, (NodeID) g.num_nodes());
    if (socket == 0) {
      for (NodeID n=vert_start; n < vert_end; n++)
        sums0[n] = 0;
    } else {
      for (NodeID n=vert_start; n < vert_end; n++)
        sums1[n] = 0;
    }
    while (iter < max_iters && global_error > epsilon) {
      #pragma omp single nowait
      {
        // Start measuring perf counters (parallel)
        t.Start();
      }
      NodeID u = my_parts->range_start(thread_id, num_threads, g);
      NodeID u_end = my_parts->range_end(thread_id, num_threads, g);
      for (; u < u_end; u++) {
        ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
        for (NodeID v : g.out_neigh(u)) {
          my_parts->SendMsg(v, outgoing_contrib);
        }
      }
      my_parts->Flush();
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Scatter", t.Seconds());
        global_error = 0;
        // Start measuring perf counters (parallel)
        t.Start();
      }
      for (int b=start_bin; b < end_bin; b++) {
        for (int b_tid=0; b_tid < threads_per_socket; b_tid++) {
          pvector<NodeScorePair> &curr_block = par_parts[b_tid + socket*threads_per_socket]->GetBin(b);
          for (size_t i=0; i<curr_block.size(); i++) {
            NodeID n = curr_block[i].first;
            ScoreT incoming_contrib = curr_block[i].second;
            if (socket == 0)
              sums0[n] += incoming_contrib;
            else
              sums1[n] += incoming_contrib;
            __builtin_prefetch(&curr_block[i+stride], 0, 0);
          }
          curr_block.clear();
        }
      }
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Gather", t.Seconds());
        // Start measuring perf counters (parallel)
        t.Start();
      }
      #pragma omp for schedule(dynamic, 1024)
      for (NodeID n=0; n < g.num_nodes(); n++) {
        incoming_sum[n] = sums0[n] + sums1[n];
        sums0[n] = 0;
        sums1[n] = 0;
      }
      double local_error = 0;
      #pragma omp for schedule(dynamic, 1024) nowait
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        local_error += fabs(scores[n] - old_score);
      }
      #pragma omp atomic
      global_error += local_error;
      #pragma omp barrier
      #pragma omp single
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Calculate", t.Seconds());
        cout << " " << iter << "    " << global_error << endl;
      }
      iter++;
    }
  }
  return scores;
}


pvector<ScoreT> PageRankGuides(const RGraph &g, int max_iters, double epsilon,
                              Guider<NodeID, ScoreT> &guides) {
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  int iter = 0;
  double error = 1;
  Timer t;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    // Start measuring perf counters
    t.Start();
    for (NodeID u=0; u < g.num_nodes(); u++) {
      ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
      for (NodeID v : g.out_neigh(u)) {
        guides.SendMsg(v, outgoing_contrib);
      }
    }
    guides.Flush();
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Scatter", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (int b=0; b < guides.get_num_bins(); b++) {
      pvector<ScoreT> &curr_bin = guides.GetBin(b);
      pvector<NodeID> &curr_index = guides.GetIndex(b);
      for (size_t i=0; i<curr_bin.size(); i++) {
        incoming_sum[curr_index[i]] += curr_bin[i];
        __builtin_prefetch(&curr_bin[i+stride], 0, 0);
        __builtin_prefetch(&curr_index[i+stride], 0, 0);
      }
      curr_bin.clear();
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Gather", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (NodeID n=0; n < g.num_nodes(); n++) {
      ScoreT old_score = scores[n];
      scores[n] = base_score + kDamp * incoming_sum[n];
      error += fabs(scores[n] - old_score);
      incoming_sum[n] = 0;
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Calculate", t.Seconds());
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  return scores;
}

void SpMVGuidesPar(float* xArray, float* yArray, const RGraph &g, int max_iters, double epsilon,
                              pvector<ParGuider<NodeID, ScoreT>*> &par_guides) {
    
  const size_t stride = 256;
  const int num_threads = omp_get_max_threads();
  const int num_bins = par_guides[0]->get_num_bins();
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);

  #pragma omp parallel
  {
    const int thread_id = omp_get_thread_num();
    ParGuider<NodeID, ScoreT>* my_guides = par_guides[thread_id];
    
    for (int i = 0; i < max_iters; ++i) 
    {
        NodeID u = my_guides->range_start(thread_id, num_threads, g);
        NodeID u_end = my_guides->range_end(thread_id, num_threads, g);
        for (; u < u_end; u++) {
            ScoreT outgoing_contrib = xArray[u];
            for (NodeID v : g.out_neigh(u)) {
                my_guides->SendMsg(v, outgoing_contrib);
            }
        }
        my_guides->Flush();
#pragma omp barrier

#pragma omp for schedule(dynamic,1)
        for (int b=0; b < num_bins; b++) {
            for (int b_tid=0; b_tid < num_threads; b_tid++) {
                pvector<ScoreT> &curr_bin = par_guides[b_tid]->GetBin(b);
                pvector<NodeID> &curr_index = par_guides[b_tid]->GetIndex(b);
                for (size_t i=0; i<curr_bin.size(); i++) {
                    // yArray[curr_index[i]] += curr_bin[i];
                    incoming_sum[curr_index[i]] += curr_bin[i];
                    __builtin_prefetch(&curr_bin[i+stride], 0, 0);
                    __builtin_prefetch(&curr_index[i+stride], 0, 0);
                }
                curr_bin.clear();
            }
        }

#pragma omp for schedule(dynamic, 1024) nowait
      for (NodeID n=0; n < g.num_nodes(); n++) {
        // ScoreT old_score = scores[n];
        // scores[n] = base_score + kDamp * incoming_sum[n];
        // local_error += fabs(scores[n] - old_score);
        yArray[n] = incoming_sum[n];
        incoming_sum[n] = 0;
      }
#pragma omp barrier
    }

  }
}

pvector<ScoreT> PageRankGuidesPar(const RGraph &g, int max_iters, double epsilon,
                              pvector<ParGuider<NodeID, ScoreT>*> &par_guides) {
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  double global_error = 1;
  const int num_threads = omp_get_max_threads();
  const int num_bins = par_guides[0]->get_num_bins();
  Timer t;
  #pragma omp parallel
  {
    int iter = 0;
    const int thread_id = omp_get_thread_num();
    ParGuider<NodeID, ScoreT>* my_guides = par_guides[thread_id];
    while (iter < max_iters && global_error > epsilon) {
      #pragma omp single nowait
      {
        // Start measuring perf counters (parallel)
        t.Start();
      }
      NodeID u = my_guides->range_start(thread_id, num_threads, g);
      NodeID u_end = my_guides->range_end(thread_id, num_threads, g);
      for (; u < u_end; u++) {
        ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
        for (NodeID v : g.out_neigh(u)) {
          my_guides->SendMsg(v, outgoing_contrib);
        }
      }
      my_guides->Flush();
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Scatter", t.Seconds());
        global_error = 0;
        // Start measuring perf counters (parallel)
        t.Start();
      }
      #pragma omp for schedule(dynamic,1)
      for (int b=0; b < num_bins; b++) {
        for (int b_tid=0; b_tid < num_threads; b_tid++) {
          pvector<ScoreT> &curr_bin = par_guides[b_tid]->GetBin(b);
          pvector<NodeID> &curr_index = par_guides[b_tid]->GetIndex(b);
          for (size_t i=0; i<curr_bin.size(); i++) {
            incoming_sum[curr_index[i]] += curr_bin[i];
            __builtin_prefetch(&curr_bin[i+stride], 0, 0);
            __builtin_prefetch(&curr_index[i+stride], 0, 0);
          }
          curr_bin.clear();
        }
      }
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Gather", t.Seconds());
        // Start measuring perf counters (parallel)
        t.Start();
      }
      double local_error = 0;
      #pragma omp for schedule(dynamic, 1024) nowait
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        local_error += fabs(scores[n] - old_score);
        incoming_sum[n] = 0;
      }
      #pragma omp atomic
      global_error += local_error;
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Calculate", t.Seconds());
        cout << " " << iter << "    " << global_error << endl;
      }
      iter++;
    }
  }
  return scores;
}

pvector<ScoreT> PageRankGuidesNUMA(const RGraph &g, int max_iters, double epsilon,
                              pvector<ParGuider<NodeID, ScoreT>*> &par_guides) {
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  pvector<ScoreT> sums0(g.num_nodes()), sums1(g.num_nodes());
  double global_error = 1;
  const int num_threads = omp_get_max_threads();
  const int num_bins = par_guides[0]->get_num_bins();
  Timer t;
  #pragma omp parallel
  {
    int iter = 0;
    const int thread_id = omp_get_thread_num();
    ParGuider<NodeID, ScoreT>* my_guides = par_guides[thread_id];
    const int threads_per_socket = 8;
    const int socket_offset = thread_id % threads_per_socket;
    const int socket = thread_id / threads_per_socket;
    const int bins_per_thread = (num_bins + threads_per_socket - 1) /
                                  threads_per_socket;
    const int start_bin = socket_offset * bins_per_thread;
    const int end_bin = min(start_bin + bins_per_thread, num_bins);
    const NodeID verts_per_thread = (g.num_nodes() + threads_per_socket - 1) / threads_per_socket;
    const NodeID vert_start = socket_offset * verts_per_thread;
    const NodeID vert_end = min(vert_start + verts_per_thread, (NodeID) g.num_nodes());
    if (socket == 0) {
      for (NodeID n=vert_start; n < vert_end; n++)
        sums0[n] = 0;
    } else {
      for (NodeID n=vert_start; n < vert_end; n++)
        sums1[n] = 0;
    }
    while (iter < max_iters && global_error > epsilon) {
      #pragma omp single nowait
      {
        // Start measuring perf counters (parallel)
        t.Start();
      }
      NodeID u = my_guides->range_start(thread_id, num_threads, g);
      NodeID u_end = my_guides->range_end(thread_id, num_threads, g);
      for (; u < u_end; u++) {
        ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
        for (NodeID v : g.out_neigh(u)) {
          my_guides->SendMsg(v, outgoing_contrib);
        }
      }
      my_guides->Flush();
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Scatter", t.Seconds());
        global_error = 0;
        // Start measuring perf counters (parallel)
        t.Start();
      }
      for (int b=start_bin; b < end_bin; b++) {
        for (int b_tid=0; b_tid < threads_per_socket; b_tid++) {
          pvector<ScoreT> &curr_bin = par_guides[b_tid + socket*threads_per_socket]->GetBin(b);
          pvector<NodeID> &curr_index = par_guides[b_tid + socket*threads_per_socket]->GetIndex(b);
          for (size_t i=0; i<curr_bin.size(); i++) {
            if (socket == 0)
              sums0[curr_index[i]] += curr_bin[i];
            else
              sums1[curr_index[i]] += curr_bin[i];
            __builtin_prefetch(&curr_bin[i+stride], 0, 0);
            __builtin_prefetch(&curr_index[i+stride], 0, 0);
          }
          curr_bin.clear();
        }
      }
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Gather", t.Seconds());
        // Start measuring perf counters (parallel)
        t.Start();
      }
      #pragma omp for schedule(dynamic, 1024)
      for (NodeID n=0; n < g.num_nodes(); n++) {
        incoming_sum[n] = sums0[n] + sums1[n];
        // sums0[n] = 0;
        // sums1[n] = 0;
      }
      double local_error = 0;
      #pragma omp for schedule(dynamic, 1024) nowait
      for (NodeID n=0; n < g.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * incoming_sum[n];
        local_error += fabs(scores[n] - old_score);
      }
      #pragma omp atomic
      global_error += local_error;
      #pragma omp barrier
      #pragma omp single nowait
      {
        t.Stop();
        // Stop measuring perf counters (parallel)
        PrintTime("Calculate", t.Seconds());
        cout << " " << iter << "    " << global_error << endl;
      }
      iter++;
    }
  }
  return scores;
}

pvector<ScoreT> PageRankCacheBlocked(const RGraph &g, int max_iters,
    double epsilon, BlockedGraph<NodeID> &bg) {
  const ScoreT init_score = 1.0f / bg.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / bg.num_nodes();
  pvector<ScoreT> scores(bg.num_nodes(), init_score);
  pvector<ScoreT> sums(bg.num_nodes(), 0);
  pvector<ScoreT> outgoing_contrib(bg.num_nodes());
  int iter = 0;
  double error = 1;
  while (iter < max_iters && error > epsilon) {
    for (NodeID n=0; n < bg.num_nodes(); n++)
      outgoing_contrib[n] = scores[n] / g.out_degree(n);
    for (int b=0; b < bg.num_blocks(); b++) {
      pvector<pair<NodeID, NodeID>> &curr_block = bg.GetBlock(b);
      for (auto e : curr_block) {
        NodeID u = e.first;
        NodeID v = e.second;
        sums[v] += outgoing_contrib[u];
      }
    }
    error = 0;
    for (NodeID n=0; n < bg.num_nodes(); n++) {
      ScoreT old_score = scores[n];
      scores[n] = base_score + kDamp * sums[n];
      error += fabs(scores[n] - old_score);
      sums[n] = 0;
    }
    iter++;
  }
  return scores;
}

pvector<ScoreT> PageRankCacheBlockedPar(const RGraph &g, int max_iters,
    double epsilon, BlockedGraph<NodeID> &bg) {
  const ScoreT init_score = 1.0f / bg.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / bg.num_nodes();
  pvector<ScoreT> scores(bg.num_nodes(), init_score);
  pvector<ScoreT> sums(bg.num_nodes(), 0);
  pvector<ScoreT> outgoing_contrib(bg.num_nodes());
  int iter = 0;
  double global_error = 1;
  while (iter < max_iters && global_error > epsilon) {
    global_error = 0;
    #pragma omp parallel
    {
      #pragma omp for schedule(dynamic, 1024)
      for (NodeID n=0; n < bg.num_nodes(); n++)
        outgoing_contrib[n] = scores[n] / g.out_degree(n);
      #pragma omp for schedule(dynamic)
      for (int b=0; b < bg.num_blocks(); b++) {
        pvector<pair<NodeID, NodeID>> &curr_block = bg.GetBlock(b);
        for (auto e : curr_block) {
          NodeID u = e.first;
          NodeID v = e.second;
          // #pragma omp atomic
          sums[v] += outgoing_contrib[u];
        }
      }
      double local_error = 0;
      #pragma omp for schedule(dynamic, 1024)
      for (NodeID n=0; n < bg.num_nodes(); n++) {
        ScoreT old_score = scores[n];
        scores[n] = base_score + kDamp * sums[n];
        local_error += fabs(scores[n] - old_score);
        sums[n] = 0;
      }
      #pragma omp atomic
      global_error += local_error;
    }
    iter++;
  }
  return scores;
}

pvector<ScoreT> PageRankRadixEL(const RGraph &g, int max_iters, double epsilon,
                              Partitioner<NodeID, ScoreT> &parts,
                              BlockedGraph<NodeID> &bg) {
  typedef pair<NodeID, ScoreT> NodeScorePair;
  const ScoreT init_score = 1.0f / g.num_nodes();
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  const size_t stride = 256;
  pvector<ScoreT> scores(g.num_nodes(), init_score);
  pvector<ScoreT> incoming_sum(g.num_nodes(), 0);
  pvector<ScoreT> outgoing_contrib(bg.num_nodes());
  int iter = 0;
  double error = 1;
  Timer t;
  while (iter < max_iters && error > epsilon) {
    error = 0;
    // Start measuring perf counters
    t.Start();
    for (NodeID n=0; n < bg.num_nodes(); n++)
      outgoing_contrib[n] = scores[n] / g.out_degree(n);
    for (int b=0; b < bg.num_blocks(); b++) {
      pvector<pair<NodeID, NodeID>> &curr_block = bg.GetBlock(b);
      for (auto e : curr_block) {
        NodeID u = e.first;
        NodeID v = e.second;
        parts.SendMsg(v,outgoing_contrib[u]);
      }
    }
    // for (NodeID u=0; u < g.num_nodes(); u++) {
    //   ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
    //   for (NodeID v : g.out_neigh(u)) {
    //     parts.SendMsg(v, outgoing_contrib);
    //   }
    // }
    parts.Flush();
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Scatter", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (int b=0; b < parts.get_num_bins(); b++) {
      pvector<NodeScorePair> &curr_block = parts.GetBin(b);
      for (size_t i=0; i<curr_block.size(); i++) {
        NodeID n = curr_block[i].first;
        ScoreT incoming_contrib = curr_block[i].second;
        incoming_sum[n] += incoming_contrib;
        __builtin_prefetch(&curr_block[i+stride], 0, 0);
      }
      curr_block.clear();
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Gather", t.Seconds());
    // Start measuring perf counters
    t.Start();
    for (NodeID n=0; n < g.num_nodes(); n++) {
      ScoreT old_score = scores[n];
      scores[n] = base_score + kDamp * incoming_sum[n];
      error += fabs(scores[n] - old_score);
      incoming_sum[n] = 0;
    }
    t.Stop();
    // Stop measuring perf counters
    PrintTime("Calculate", t.Seconds());
    cout << " " << iter << "    " << error << endl;
    iter++;
  }
  return scores;
}


void PrintTopScores(const RGraph &g, const pvector<ScoreT> &scores) {
  vector<pair<NodeID, ScoreT>> score_pairs(g.num_nodes());
  for (NodeID n=0; n < g.num_nodes(); n++) {
    score_pairs[n] = make_pair(n, scores[n]);
  }
  int k = 5;
  vector<pair<ScoreT, NodeID>> top_k = TopK(score_pairs, k);
  k = min(k, static_cast<int>(top_k.size()));
  for (auto kvp : top_k)
    cout << kvp.second << ":" << kvp.first << endl;
}


// Verifies by asserting a single serial iteration in push direction has
//   error < target_error
bool PRVerifier(const RGraph &g, const pvector<ScoreT> &scores,
                        double target_error) {
  const ScoreT base_score = (1.0f - kDamp) / g.num_nodes();
  pvector<ScoreT> incomming_sums(g.num_nodes(), 0);
  double error = 0;
  for (NodeID u=0; u < g.num_nodes(); u++) {
    ScoreT outgoing_contrib = scores[u] / g.out_degree(u);
    for (NodeID v : g.out_neigh(u))
      incomming_sums[v] += outgoing_contrib;
  }
  for (NodeID n=0; n < g.num_nodes(); n++) {
    error += fabs(base_score + kDamp * incomming_sums[n] - scores[n]);
    incomming_sums[n] = 0;
  }
  PrintTime("Total Error", error);
  return error < target_error;
}


void blocker_sweep(const CLIterApp &cli) {
  Builder b(cli);
  RGraph g = b.MakeGraph();
  Timer t;
  auto VerifierBound = [] (const RGraph &g, const pvector<ScoreT> &scores) {
    return PRVerifier(g, scores, kGoalEpsilon);
  };
  pvector<ParPartitioner<NodeID, ScoreT>*> par_parts(omp_get_max_threads());
  for (int log_width=14; log_width<30; log_width++) {
    cout << "log(block width): " << log_width << endl;
    t.Start();
    #pragma omp parallel
    par_parts[omp_get_thread_num()] = new ParPartitioner<NodeID, ScoreT>(log_width,
      omp_get_thread_num(), omp_get_max_threads(), g);
    // Partitioner<NodeID, ScoreT> parts(log_width, g);
    t.Stop();
    PrintTime("Make Blockers", t.Seconds());
    auto PRBound = [&cli, &par_parts] (const RGraph &g) {
      // return PageRankRadix(g, cli.num_iters(), kGoalEpsilon, parts);
      return PageRankRadixPar(g, cli.num_iters(), kGoalEpsilon, par_parts);
    };
    BenchmarkKernel(cli, g, PRBound, PrintTopScores, VerifierBound);
    #pragma omp parallel
    delete par_parts[omp_get_thread_num()];
  }
}

void cb_sweep(const CLIterApp &cli) {
  Builder b(cli);
  RGraph g = b.MakeGraph();
  Timer t;
  auto VerifierBound = [] (const RGraph &g, const pvector<ScoreT> &scores) {
    return PRVerifier(g, scores, kGoalEpsilon);
  };
  for (int log_width=14; log_width<30; log_width++) {
    int64_t desired_block_width = (1l<<log_width) / sizeof(ScoreT);
    int64_t num_blocks = max(1l, (g.num_nodes() + desired_block_width - 1) / desired_block_width);
    cout << "log(block width): " << log_width << endl;
    t.Start();
    BlockedGraph<NodeID> bg(num_blocks, g);
    t.Stop();
    PrintTime("Block Graph", t.Seconds());
    auto PRBound = [&cli, &bg] (const RGraph &g) {
      return PageRankCacheBlockedPar(g, cli.num_iters(), kGoalEpsilon, bg);
    };
    BenchmarkKernel(cli, g, PRBound, PrintTopScores, VerifierBound);
  }
}

void gblocker_sweep(const CLIterApp &cli) {
  Builder b(cli);
  RGraph g = b.MakeGraph();
  Timer t;
  auto VerifierBound = [] (const RGraph &g, const pvector<ScoreT> &scores) {
    return PRVerifier(g, scores, kGoalEpsilon);
  };
  pvector<ParGuider<NodeID, ScoreT>*> par_guides(omp_get_max_threads());
  for (int log_width=14; log_width<30; log_width++) {
    cout << "log(block width): " << log_width << endl;
    t.Start();
    #pragma omp parallel
    par_guides[omp_get_thread_num()] = new ParGuider<NodeID, ScoreT>(log_width,
      omp_get_thread_num(), omp_get_max_threads(), g);
      t.Stop();
    PrintTime("Make Blockers", t.Seconds());
    auto PRBound = [&cli, &par_guides] (const RGraph &g) {
      return PageRankGuidesPar(g, cli.num_iters(), kGoalEpsilon, par_guides);
    };
    BenchmarkKernel(cli, g, PRBound, PrintTopScores, VerifierBound);
    #pragma omp parallel
    delete par_guides[omp_get_thread_num()];
  }
}


int main2(int argc, char* argv[]) {
  CLIterApp cli(argc, argv, "pagerank", 20);
  if (!cli.ParseArgs())
    return -1;
  Builder b(cli);
  RGraph g = b.MakeGraph();
  Timer t;
  // int64_t desired_block_width = (1l<<19) / sizeof(ScoreT);
  // int64_t num_blocks = max(1l, (g.num_nodes() + desired_block_width - 1) / desired_block_width);
  t.Start();
  // Partitioner<NodeID, ScoreT> parts(19, g);
  // pvector<ParPartitioner<NodeID, ScoreT>*> par_parts(omp_get_max_threads());
  // #pragma omp parallel
  // par_parts[omp_get_thread_num()] = new ParPartitioner<NodeID, ScoreT>(19,
  //   omp_get_thread_num(), omp_get_max_threads(), g);
  // Guider<NodeID, ScoreT> guides(21, g);
  pvector<ParGuider<NodeID, ScoreT>*> par_guides(omp_get_max_threads());
  #pragma omp parallel
  par_guides[omp_get_thread_num()] = new ParGuider<NodeID, ScoreT>(19,
    omp_get_thread_num(), omp_get_max_threads(), g);
  // BlockedGraph<NodeID> bg(num_blocks, g, true);
  t.Stop();
  // PrintTime("Make Blockers", t.Seconds());
  PrintTime("Make Guiders", t.Seconds());
  // PrintTime("Block Graph", t.Seconds());
  auto PRBound = [&cli, &par_guides] (const RGraph &g) {
    // return PageRankPull(g, cli.num_iters(), kGoalEpsilon);
    // return PageRankRadix(g, cli.num_iters(), kGoalEpsilon, parts);
    // return PageRankRadixPar(g, cli.num_iters(), kGoalEpsilon, par_parts);
    // return PageRankGuides(g, cli.num_iters(), kGoalEpsilon, guides);
    return PageRankGuidesPar(g, cli.num_iters(), kGoalEpsilon, par_guides);
    // return PageRankCacheBlocked(g, cli.num_iters(), kGoalEpsilon, bg);
    // return PageRankCacheBlockedPar(g, cli.num_iters(), kGoalEpsilon, bg);
    // return PageRankRadixEL(g, cli.num_iters(), kGoalEpsilon, parts, bg);
  };
  auto VerifierBound = [] (const RGraph &g, const pvector<ScoreT> &scores) {
    return PRVerifier(g, scores, kGoalEpsilon);
  };
  BenchmarkKernel(cli, g, PRBound, PrintTopScores, VerifierBound);
  // blocker_sweep(cli);
  // cb_sweep(cli);
  // gblocker_sweep(cli);
  // #pragma omp parallel
  // delete par_parts[omp_get_thread_num()];
  #pragma omp parallel
  delete par_guides[omp_get_thread_num()];
  return 0;
}
