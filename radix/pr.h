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

typedef float ScoreT;
const float kDamp = 0.85;
const double kGoalEpsilon = 1e-4;

// single pull iteration (doesn't update scores) to see error
double CheckPageRank(const RGraph &g, const pvector<ScoreT> &scores);
pvector<ScoreT> PageRankPush(const RGraph &g, int num_iterations); 
pvector<ScoreT> PageRankPull(const RGraph &g, int max_iters, double epsilon = 0); 
pvector<ScoreT> PageRankPullRacy(const RGraph &g, int max_iters, double epsilon = 0);
pvector<ScoreT> PageRankPullDiff(const RGraph &g, int max_iters, double epsilon = 0); 
pvector<ScoreT> PageRankPullFilter(const RGraph &g, int max_iters, double epsilon = 0); 
pvector<ScoreT> PageRankPullFilterDiff(const RGraph &g, int max_iters, double epsilon = 0); 
// always receive, just filter who read
pvector<ScoreT> PageRankPullFilterDiff2(const RGraph &g, int max_iters,double epsilon = 0); 
pvector<ScoreT> PageRankPullBlock(const RGraph &g, int max_iters, double epsilon = 0); 
pvector<ScoreT> PageRankPullSparseBlock(const RGraph &g, int max_iters, double epsilon = 0); 
pvector<ScoreT> PageRankRadixClassic(const RGraph &g, int max_iters, double epsilon = 0);
pvector<ScoreT> PageRankRadix(const RGraph &g, int max_iters, double epsilon, Partitioner<NodeID, ScoreT> &parts); 

/**
 * @brief check this function for our paper
 * TODO: 
 * 1) The definition of stride
 * 2) The meaning of kDamp
 * 3) The meaning of num_bins
 *
 * 
 *
 * @param g: a csr input graph
 * @param max_iters
 * @param epsilon
 * @param par_parts: the dense vector ?
 *
 * @return 
 */
pvector<ScoreT> PageRankRadixPar(const RGraph &g, int max_iters, double epsilon, pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts); 
void SpMVRadixPar(float* xArray, float* yArray, const RGraph &g, int max_iters, double epsilon, pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts); 
void SpMVGuidesPar(float* xArray, float* yArray, const RGraph &g, int max_iters, double epsilon,
                              pvector<ParGuider<NodeID, ScoreT>*> &par_guides);
pvector<ScoreT> PageRankRadixNUMA(const RGraph &g, int max_iters, double epsilon, pvector<ParPartitioner<NodeID, ScoreT>*> &par_parts); 
pvector<ScoreT> PageRankGuides(const RGraph &g, int max_iters, double epsilon,Guider<NodeID, ScoreT> &guides); 
pvector<ScoreT> PageRankGuidesPar(const RGraph &g, int max_iters, double epsilon, pvector<ParGuider<NodeID, ScoreT>*> &par_guides); 
pvector<ScoreT> PageRankGuidesNUMA(const RGraph &g, int max_iters, double epsilon, pvector<ParGuider<NodeID, ScoreT>*> &par_guides); 
pvector<ScoreT> PageRankCacheBlocked(const RGraph &g, int max_iters, double epsilon, BlockedGraph<NodeID> &bg); 
pvector<ScoreT> PageRankCacheBlockedPar(const RGraph &g, int max_iters, double epsilon, BlockedGraph<NodeID> &bg); 
pvector<ScoreT> PageRankRadixEL(const RGraph &g, int max_iters, double epsilon, Partitioner<NodeID, ScoreT> &parts, BlockedGraph<NodeID> &bg); 
void PrintTopScores(const RGraph &g, const pvector<ScoreT> &scores); 


// Verifies by asserting a single serial iteration in push direction has
//   error < target_error
bool PRVerifier(const RGraph &g, const pvector<ScoreT> &scores, double target_error); 
void blocker_sweep(const CLIterApp &cli); 
void cb_sweep(const CLIterApp &cli); 
void gblocker_sweep(const CLIterApp &cli); 

