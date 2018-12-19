#include <omp.h>

#include <stdio.h>
#include <cstdlib>
#include <assert.h>
#include <fstream>
#include <math.h>
#include <sys/time.h>
#include <vector>
#include <iostream>
#include <sys/stat.h>
#include <string>
#include <cstring>
#include <unistd.h>
#include <climits>
#include <stdint.h>
#include <omp.h>

#include "mkl.h"

#include "Graph.hpp"
#include "CSRGraph.hpp"
#include "CSCGraph.hpp"
#include "CountMat.hpp"
#include "Helper.hpp"
#include "EdgeList.hpp"

// for testing pb radix
#include "radix/commons/builder.h"
#include "radix/commons/command_line.h"
#include "radix/pr.h"

// for testing spmd3
#include "SpDM3/include/dmat.h"
#include "SpDM3/include/spmat.h"
#include "SpDM3/include/matmul.h"

// for RCM reordering
#include "SpMP/CSR.hpp"

#ifdef __INTEL_COMPILER
// use avx intrinsics
#include "immintrin.h"
#include "zmmintrin.h"
#endif

using namespace std;

// LLC 33 for Skylake
static const size_t LLC_CAPACITY = 33*1024*1024;

void flushLlc(float* bufToFlushLlc)
{
  double sum = 0;
#pragma omp parallel for reduction(+:sum)
  for (size_t i = 0; i < LLC_CAPACITY/sizeof(bufToFlushLlc[0]); ++i) {
    sum += bufToFlushLlc[i];
  }
  FILE *fp = fopen("/dev/null", "w");
  fprintf(fp, "%f\n", sum);
  fclose(fp);
}

void benchmarkSpMVPBRadix(int argc, char** argv, EdgeList& elist, int numCols)
{
    double startTime = 0.0;
    double timeElapsed = 0.0;
    int binSize = 15;
    if (argc > 9)
        binSize = atoi(argv[9]);

    
    // -------------------- start debug the Radix SpMV ------------------------------
    printf("start radix spmv\n");
    std::fflush(stdout);

    // for radix
    typedef BuilderBase<int32_t, int32_t, int32_t> Builder;
    typedef RCSRGraph<int32_t> RGraph;
    // EdgeList elist(graph_name);
    pvector<EdgePair<int32_t, int32_t> > radixList(elist.getNumEdges()); 
    elist.convertToRadixList(radixList);

    CLBase cli(argc, argv);
    Builder b(cli);
    RGraph radixG = b.MakeGraphFromEL(radixList);

    printf("Finish build radix graph, vert: %d\n", radixG.num_nodes());
    std::fflush(stdout);

    double flopsTotal =  2*radixG.num_edges();
    double bytesTotal = sizeof(float)*(radixG.num_edges() + 2*radixG.num_nodes()) + sizeof(int)*(radixG.num_edges() + radixG.num_nodes()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    pvector<ParGuider<int32_t, float>*> par_guides(omp_get_max_threads());
#pragma omp parallel
    // par_guides[omp_get_thread_num()] = new ParGuider<int32_t, float>(19,omp_get_thread_num(), omp_get_max_threads(), radixG);
    par_guides[omp_get_thread_num()] = new ParGuider<int32_t, float>(binSize,omp_get_thread_num(), omp_get_max_threads(), radixG);

    printf("Finish build par_parts\n");
    std::fflush(stdout);

    float* xMat = (float*) _mm_malloc(radixG.num_nodes()*sizeof(float), 64);
    float* yMat = (float*) _mm_malloc(radixG.num_nodes()*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < radixG.num_nodes(); ++i) {
        xMat[i] = 2.0; 
        yMat[i] = 0.0; 
    }

    startTime = utility::timer();

    // check pagerank scores
    // for (int j = 0; j < 100; ++j) {
    // SpMVRadixPar(xMat, yMat, radixG, 1, kGoalEpsilon, par_parts);
    SpMVGuidesPar(xMat, yMat, radixG, numCols, kGoalEpsilon, par_guides);
    // }
    //
    timeElapsed = (utility::timer() - startTime);
    printf("Radix SpMV using %f secs\n", timeElapsed/numCols);
    printf("Radix SpMV Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("Radix SpMV Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("Radix SpMV Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           
    //
    // check yMat
    // for (int i = 0; i < 10; ++i) {
    //     printf("Elem: %d is: %f\n", i, yMat[i]); 
    //     std::fflush(stdout);
    // }

    // free memory
    _mm_free(xMat);
    _mm_free(yMat);

    // -------------------- end debug the Radix SpMV ------------------------------
}

// test bandwidth utilization and throughput
void benchmarkSpMVNaive(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{

    double startTime = 0.0;
    double timeElapsed = 0.0;
    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, false, true);

    double flopsTotal =  csrnaiveG.getNNZ();
    double bytesTotal = sizeof(float)*2*csrnaiveG.getNumVertices() + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    float* xMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);
    float* yMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < csrnaiveG.getNumVertices(); ++i) {
        xMat[i] = 2.0; 
        yMat[i] = 0.0; 
    }

    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    // test SpMV naive
    // startTime = utility::timer();
    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        for (int k = 0; k < 16; ++k) {
            flushLlc(bufToFlushLlc);
        }

        // startTime = omp_get_wtime();
        startTime = utility::timer();
        csrnaiveG.SpMVNaive(xMat, yMat, comp_thds);
        // timeElapsed += (omp_get_wtime() = startTime);
        timeElapsed += (utility::timer() - startTime);
    }

    printf("Naive SpMV using %f secs\n", timeElapsed/numCols);
    printf("Naive SpMV Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("Naive SpMV Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("Naive SpMV Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    _mm_free(xMat);
    _mm_free(yMat);
    _mm_free(bufToFlushLlc);
}

void benchmarkSpMVNaiveFull(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{

    double startTime = 0.0;
    double timeElapsed = 0.0;
    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, false, true);

    double flopsTotal =  2*csrnaiveG.getNNZ();
    // read n x, nnz val, write n y, Index col idx, row idx
    double bytesTotal = sizeof(float)*(csrnaiveG.getNNZ() + 2*csrnaiveG.getNumVertices()) 
        + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 

    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    int testLen = csrnaiveG.getNumVertices()*numCols;

    float* xMat = (float*)_mm_malloc(testLen*sizeof(float), 64);
    float* yMat = (float*)_mm_malloc(testLen*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < testLen; ++i) {
        xMat[i] = 2.0; 
        yMat[i] = 0.0; 
    }

    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    // test SpMV naive
    // startTime = utility::timer();
    for (int j = 0; j < numCols; ++j) {

        // // flush out LLC
        // for (int k = 0; k < 16; ++k) {
        //     flushLlc(bufToFlushLlc);
        // }
        


        startTime = utility::timer();
        csrnaiveG.SpMVNaiveFull(xMat+j*csrnaiveG.getNumVertices(), 
                yMat+j*csrnaiveG.getNumVertices(), comp_thds);
        
        timeElapsed += (utility::timer() - startTime);
    }

    printf("Naive SpMVFull using %f secs\n", timeElapsed/numCols);
    printf("Naive SpMVFull Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("Naive SpMVFull Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("Naive SpMVFull Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    _mm_free(xMat);
    _mm_free(yMat);
    _mm_free(bufToFlushLlc);
}

void benchmarkSpMVNaiveFullCSC(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{

    double startTime = 0.0;
    double timeElapsed = 0.0;
    CSCGraph<int32_t, float> csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    double flopsTotal =  2*csrnaiveG.getNNZ();
    // read n x, nnz val, write n y, Index col idx, row idx
    double bytesTotal = sizeof(float)*(csrnaiveG.getNNZ() + 2*csrnaiveG.getNumVertices()) 
        + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 

    csrnaiveG.splitCSC(4*comp_thds);

    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    float* xMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);
    float* yMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < csrnaiveG.getNumVertices(); ++i) {
        xMat[i] = 2.0; 
        yMat[i] = 0.0; 
    }

    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    // test SpMV naive
    // startTime = utility::timer();
    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        // for (int k = 0; k < 16; ++k) {
        //     flushLlc(bufToFlushLlc);
        // }
        
        // clear the yMat for each iteration
#pragma omp parallel for
    for (int i = 0; i < csrnaiveG.getNumVertices(); ++i) {
        yMat[i] = 0.0; 
    }
        startTime = utility::timer();
        csrnaiveG.spmvNaiveSplit(xMat, yMat, comp_thds);
        timeElapsed += (utility::timer() - startTime);
    }

    printf("Naive SpMVFull CSC using %f secs\n", timeElapsed/numCols);
    printf("Naive SpMVFull CSC Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("Naive SpMVFull CSC Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("Naive SpMVFull CSC Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);
    std::fflush(stdout);           

    //check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    _mm_free(xMat);
    _mm_free(yMat);
    _mm_free(bufToFlushLlc);
}

void benchmarkCSCSplitMM(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{

    double startTime = 0.0;
    double timeElapsed = 0.0;
    CSCGraph<int32_t, float> csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    double flopsTotal =  csrnaiveG.getNNZ();
    // read n x, nnz val, write n y, Index col idx, row idx
    double bytesTotal = sizeof(float)*(2*csrnaiveG.getNumVertices()) 
        + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 

    csrnaiveG.splitCSC(4*comp_thds);

    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    // right-hand multiple vectors
    int testLen = csrnaiveG.getNumVertices()*numCols;

    float* xMat = (float*)_mm_malloc(testLen*sizeof(float), 64);
    float* yMat = (float*)_mm_malloc(testLen*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < testLen; ++i) {
        xMat[i] = 2.0; 
        yMat[i] = 0.0; 
    }

    // test CSC-Split SpMM
    double computeElapsedTime = 0.0;
    startTime = utility::timer();
    computeElapsedTime = csrnaiveG.spmmSplitExp(xMat, yMat, numCols, comp_thds);
    timeElapsed = (utility::timer() - startTime);

    printf("CSC-Split SpMM Compute using %f secs\n", computeElapsedTime/numCols);
    printf("CSC-Split SpMM Total using %f secs\n", timeElapsed/numCols);
    printf("CSC-Split Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("CSC-Split Bd: %f GBytes/sec\n", bytesTotal*numCols/computeElapsedTime);
    printf("CSC-Split Tht: %f GFLOP/sec\n", flopsTotal*numCols/computeElapsedTime);
    std::fflush(stdout);           

    //check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    _mm_free(xMat);
    _mm_free(yMat);

}

// Inspector-Executor interface in MKL 11.3+
// NOTICE: the way to invoke the mkl 11.3 inspector-executor
void benchmarkSpMVMKL(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
  
    int numCalls = numCols;
    double startTime = 0.0;
    double timeElapsed = 0.0;

    CSRGraph csrGMKL;
    csrGMKL.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), true, false, true);

    double flopsTotal =  2*csrGMKL.getNNZ();
    double bytesTotal = sizeof(float)*(csrGMKL.getNNZ() + 2*csrGMKL.getNumVertices()) + sizeof(int)*(csrGMKL.getNNZ() + csrGMKL.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    sparse_matrix_t mklA;
    sparse_status_t stat = mkl_sparse_s_create_csr(
    &mklA,
    SPARSE_INDEX_BASE_ZERO, csrGMKL.getNumVertices(), csrGMKL.getNumVertices(),
    csrGMKL.getIndexRow(), csrGMKL.getIndexRow() + 1,
    csrGMKL.getIndexCol(), csrGMKL.getNNZVal());

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to create mkl csr\n");
        return;
    }

    matrix_descr descA;
    descA.type = SPARSE_MATRIX_TYPE_GENERAL;
    descA.diag = SPARSE_DIAG_NON_UNIT;

    stat = mkl_sparse_set_mv_hint(mklA, SPARSE_OPERATION_NON_TRANSPOSE, descA, numCalls);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to set mv hint\n");
        return;
    }

    stat = mkl_sparse_optimize(mklA);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to sparse optimize\n");
        return;
    }

    float* xArray = (float*) _mm_malloc(csrGMKL.getNumVertices()*sizeof(float), 64);
    float* yArray = (float*) _mm_malloc(csrGMKL.getNumVertices()*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < csrGMKL.getNumVertices(); ++i) {
        xArray[i] = 2.0; 
        yArray[i] = 0.0; 
    }

    // float* yArray = (float*) malloc(csrGMKL.getNumVertices()*sizeof(float));
    // std::memset(yArray, 0, csrGMKL.getNumVertices()*sizeof(float));
    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);


    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        for (int k = 0; k < 16; ++k) {
            flushLlc(bufToFlushLlc);
        }

        startTime = utility::timer();
        mkl_sparse_s_mv(SPARSE_OPERATION_NON_TRANSPOSE, 1, mklA, descA, xArray, 0, yArray);
        timeElapsed += (utility::timer() - startTime);
    }

    printf("MKL SpMV using %f secs\n", timeElapsed/numCols);
    printf("MKL SpMV Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("MKL SpMV Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("MKL SpMV Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           

    // check yMat
    // for (int i = 0; i < 10; ++i) {
    //     printf("Elem: %d is: %f\n", i, yArray[i]); 
    //     std::fflush(stdout);
    // }

    _mm_free(xArray);
    _mm_free(yArray);
    _mm_free(bufToFlushLlc);
}

void benchmarkMMMKL(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
  
    double startTime;
    const int calls = 100;
    CSRGraph csrGMKL;
    csrGMKL.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), true, false, true);

    double flopsTotal =  2*csrGMKL.getNNZ();
    double bytesTotal = sizeof(float)*(csrGMKL.getNNZ() + 2*csrGMKL.getNumVertices()) + sizeof(int)*(csrGMKL.getNNZ() + csrGMKL.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    sparse_matrix_t mklA;
    sparse_status_t stat = mkl_sparse_s_create_csr(
    &mklA,
    SPARSE_INDEX_BASE_ZERO, csrGMKL.getNumVertices(), csrGMKL.getNumVertices(),
    csrGMKL.getIndexRow(), csrGMKL.getIndexRow() + 1,
    csrGMKL.getIndexCol(), csrGMKL.getNNZVal());

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to create mkl csr\n");
        return;
    }

    matrix_descr descA;
    descA.type = SPARSE_MATRIX_TYPE_GENERAL;
    descA.diag = SPARSE_DIAG_NON_UNIT;

    stat = mkl_sparse_set_mm_hint(mklA, SPARSE_OPERATION_NON_TRANSPOSE, descA, SPARSE_LAYOUT_COLUMN_MAJOR, numCols, calls);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to set mm hint\n");
        return;
    }

    stat = mkl_sparse_optimize(mklA);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to sparse optimize\n");
        return;
    }

    int testLen = numCols*csrGMKL.getNumVertices();

    float* xMat = (float*) _mm_malloc(testLen*sizeof(float), 64);
    float* yMat = (float*) _mm_malloc(testLen*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < testLen; ++i) {
       xMat[i] = 2.0; 
       yMat[i] = 0.0;
    }

    startTime = utility::timer();

    mkl_sparse_s_mm(SPARSE_OPERATION_NON_TRANSPOSE, 1, mklA, descA, SPARSE_LAYOUT_COLUMN_MAJOR, 
            xMat, numCols, csrGMKL.getNumVertices(), 0, yMat, csrGMKL.getNumVertices());

    double timeElapsed = (utility::timer() - startTime);
    printf("MKL MM using %f secs\n", timeElapsed);
    printf("MKL MM using %f secs per SpMV\n", timeElapsed/numCols);
    printf("MKL MM Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("MKL MM Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("MKL MM Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           

    // check yMat
    // for (int i = 0; i < 10; ++i) {
    //     printf("Elem: %d is: %f\n", i, yMat[i]); 
    //     std::fflush(stdout);
    // }

    _mm_free(xMat);
    _mm_free(yMat);
}

void benchmarkSpMMMKL(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
    double startTime = 0.0;
    double timeElapsed = 0.0;
    printf("Start debug CSR SpMM \n");
    std::fflush(stdout);

    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), true, false, true);

    double flopsTotal =  2*csrnaiveG.getNNZ();
    double bytesTotal = sizeof(float)*(csrnaiveG.getNNZ() + 2*csrnaiveG.getNumVertices()) + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    //
    int csrNNZA = csrnaiveG.getNNZ(); 
    int csrRows = csrnaiveG.getNumVertices();
    int* csrRowIdx = csrnaiveG.getIndexRow();
    int* csrColIdx = csrnaiveG.getIndexCol();
    float* csrVals = csrnaiveG.getNNZVal();

    int testLen = numCols*csrRows;

    float* xMat = (float*) _mm_malloc(testLen*sizeof(float), 64);
    float* yMat = (float*) _mm_malloc(testLen*sizeof(float), 64);
    // float* yMat = (float*) malloc(testLen*sizeof(float));
    // std::memset(yMat, 0, testLen*sizeof(float));

#pragma omp parallel for
    for (int i = 0; i < testLen; ++i) {
       xMat[i] = 2.0; 
       yMat[i] = 0.0; 
    }

    // invoke mkl scsrmm
    char transa = 'n';
    MKL_INT m = csrRows;
    MKL_INT n = numCols;
    MKL_INT k = csrRows;

    float alpha = 1.0;
    float beta = 0.0;

    char matdescra[5];
    matdescra[0] = 'g';
    matdescra[3] = 'f'; /*one-based indexing is used*/

    startTime = utility::timer();
    mkl_scsrmm(&transa, &m, &n, &k, &alpha, matdescra, csrVals, csrColIdx, csrRowIdx, &(csrRowIdx[1]), xMat, &k, &beta, yMat, &k);
    timeElapsed += (utility::timer() - startTime);

    printf("MKL Old CSR SpMM using %f secs\n", timeElapsed);
    printf("MKL Old CSR SpMM using %f secs per SpMV\n", timeElapsed/numCols);
    printf("MKL Old CSR SpMM Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("MKL Old CSR SpMM Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("MKL Old CSR SpMM Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    // check yMat
    // for (int i = 0; i < 10; ++i) {
    //    printf("Elem: %d is: %f\n", i, yMat[i]); 
    //    std::fflush(stdout);
    // }

    // free test mem
    _mm_free(xMat);
    _mm_free(yMat);

    printf("Finish debug CSR SpMM\n");
    std::fflush(stdout);

}

void benchmarkSpDM3(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{    

    double startTime = 0.0;
    double timeElapsed = 0.0;

    printf("Start debug Spdm3 SpMM\n");
    std::fflush(stdout);

    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, false, true);

    double flopsTotal =  2*csrnaiveG.getNNZ();
    double bytesTotal = sizeof(float)*(csrnaiveG.getNNZ() + 2*csrnaiveG.getNumVertices()) + sizeof(int)*(csrnaiveG.getNNZ() + csrnaiveG.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    spdm3::SpMat<int, float> smat(spdm3::SPARSE_CSR, 0);
    csrnaiveG.fillSpMat(smat);

    // use smat
    int rowNum = smat.dim1();
    int testLen = rowNum*numCols;
    float* xArray = (float*) _mm_malloc (testLen*sizeof(float), 64);
    float* yArray = (float*) _mm_malloc (testLen*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < testLen; ++i) {
       xArray[i] = 2.0; 
       yArray[i] = 0.0; 
    }   

    // float* yArray = (float*) malloc (testLen*sizeof(float));
    // std::memset(yArray, 0, testLen*sizeof(float));

    // data copy from xArray to xMat
    // TODO replace data copy by pointer assignment
    spdm3::DMat<int, float> xMat(rowNum, numCols, rowNum, spdm3::DENSE_COLMAJOR, xArray);
    spdm3::DMat<int, float> yMat(rowNum, numCols, rowNum, spdm3::DENSE_COLMAJOR, yArray);

    printf("Dmat: row: %d, cols: %d\n", xMat.rows_, xMat.cols_);
    std::fflush(stdout);

    startTime = utility::timer();
    // start the SpMM 
    spdm3::matmul_blas_colmajor<int>(smat, xMat, yMat);

    timeElapsed = (utility::timer() - startTime);
    printf("SpDM3 SpMM using %f secs\n", timeElapsed);
    printf("SpDM3 SpMM using %f secs per SpMV\n", timeElapsed/numCols);
    printf("SpDM3 SpMM Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("SpDM3 SpMM Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("SpDM3 SpMM Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           

    // check yMat
    // for (int i = 0; i < 10; ++i) {
    //    printf("Elem: %d is: %f\n", i, yMat.values_[i]); 
    //    std::fflush(stdout);
    // }

    printf("Finish debug Spdm3 SpMM\n");
    std::fflush(stdout);

    _mm_free(xArray);
    _mm_free(yArray);
}

void arrayWiseFMAAVX(float** blockPtrDst,float** blockPtrA,float** blockPtrB, int* blockSize, 
        int blockSizeBasic, float* dst, float* a, float* b, int num_threads)
{
    blockPtrDst[0] = dst; 
    blockPtrA[0] = a;
    blockPtrB[0] = b;
    //
    for (int i = 1; i < num_threads; ++i) {
        blockPtrDst[i] = blockPtrDst[i-1] + blockSizeBasic; 
        blockPtrA[i] = blockPtrA[i-1] + blockSizeBasic;
        blockPtrB[i] = blockPtrB[i-1] + blockSizeBasic;
    }

#pragma omp parallel for schedule(static) num_threads(num_threads)
    for(int i=0; i<num_threads; i++)
    {
        float* blockPtrDstLocal = blockPtrDst[i]; 
        float* blockPtrALocal = blockPtrA[i]; 
        float* blockPtrBLocal = blockPtrB[i]; 
        int blockSizeLocal = blockSize[i];

#ifdef __AVX512F__ 

    // unrolled by 16 float
    int n16 = blockSizeLocal & ~(16-1); 
    __m512 tmpzero = _mm512_set1_ps (0);
    __mmask16 mask = (1 << (blockSizeLocal - n16)) - 1;

    __m512 vecA;
    __m512 vecB;
    __m512 vecC;
    __m512 vecBuf;

    for (int j = 0; j < n16; j+=16)
    {
        vecA = _mm512_load_ps (&(blockPtrALocal[j]));
        vecB = _mm512_load_ps (&(blockPtrBLocal[j]));
        vecC = _mm512_load_ps (&(blockPtrDstLocal[j]));
        vecBuf = _mm512_fmadd_ps(vecA, vecB, vecC);
        _mm512_store_ps(&(blockPtrDstLocal[j]), vecBuf);
    }

    if (n16 < blockSizeLocal)
    {
        vecA = _mm512_mask_load_ps(tmpzero, mask, &(blockPtrALocal[n16]));
        vecB = _mm512_mask_load_ps(tmpzero, mask, &(blockPtrBLocal[n16]));
        vecC = _mm512_mask_load_ps(tmpzero, mask, &(blockPtrDstLocal[n16]));
        vecBuf = _mm512_fmadd_ps(vecA, vecB, vecC);
        _mm512_mask_store_ps(&(blockPtrDstLocal[n16]), mask, vecBuf);
    }
#else

#ifdef __AVX2__ 
    // use avx256
    // unrolled by 8 float
    int n8 = blockSizeLocal & ~(8-1); 

    int mask_integer[8]={0,0,0,0,0,0,0,0};
    for (int i=0;i<(blockSizeLocal - n8); i++)
        mask_integer[i] = -1;

    __m256i mask = _mm256_setr_epi32(mask_integer[0],mask_integer[1],mask_integer[2],mask_integer[3],mask_integer[4],
                mask_integer[5],mask_integer[6],mask_integer[7]);

    __m256 vecA;
    __m256 vecB;
    __m256 vecC;
    __m256 vecBuf;

    for (int j = 0; j < n8; j+=8)
    {
        vecA = _mm256_load_ps (&(blockPtrALocal[j]));
        vecB = _mm256_load_ps (&(blockPtrBLocal[j]));
        vecC = _mm256_load_ps (&(blockPtrDstLocal[j]));
        vecBuf = _mm256_fmadd_ps(vecA, vecB, vecC);
        _mm256_store_ps(&(blockPtrDstLocal[j]), vecBuf);
    }

    if (n8 < blockSizeLocal)
    {
        vecA = _mm256_maskload_ps(&(blockPtrALocal[n8]), mask);
        vecB = _mm256_maskload_ps(&(blockPtrBLocal[n8]), mask);
        vecC = _mm256_maskload_ps(&(blockPtrDstLocal[n8]), mask);
        vecBuf = _mm256_fmadd_ps(vecA, vecB, vecC);
        _mm256_maskstore_ps(&(blockPtrDstLocal[n8]), mask, vecBuf);
    }   

#else
   // no avx detected 
 #pragma omp simd aligned(dst, a, b: 64)
        for(int j=0; j<blockSizeLocal;j++)
            blockPtrDstLocal[j] = blockPtrDstLocal[j] + blockPtrALocal[j]*blockPtrBLocal[j];          
#endif

#endif

    }

}

// benchmark the EMA codes
// element-wised vector multiplication and addition
// with LLC flush
void benchmarkEMA(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
    printf("Start benchmarking eMA\n");
    std::fflush(stdout);           

    double startTime = 0.0;
    double timeElapsed = 0.0;

    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, false, true);

    // a mul plus a add
    double flopsTotal =  2*csrnaiveG.getNumVertices();
    // z += x*y
    // 3n read/write
    double bytesTotal = sizeof(float)*(3*csrnaiveG.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    float* xMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);
    float* yMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);
    float* zMat = (float*)_mm_malloc(csrnaiveG.getNumVertices()*sizeof(float), 64);

    #pragma omp parallel for
    for (int i = 0; i < csrnaiveG.getNumVertices(); ++i) {
        xMat[i] = 2.0;
        yMat[i] = 2.0;
        zMat[i] = 0.0;
    }

    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    int blockSizeBasic = csrnaiveG.getNumVertices()/comp_thds;
    int* blockSize = (int*) malloc(comp_thds*sizeof(int));
    for (int i = 0; i < comp_thds; ++i) {
       blockSize[i] = blockSizeBasic; 
    }
    blockSize[comp_thds-1] = ((csrnaiveG.getNumVertices()%comp_thds == 0) ) ? blockSizeBasic : 
        (blockSizeBasic + (csrnaiveG.getNumVertices()%comp_thds)); 

    float** blockPtrDst = (float**) malloc(comp_thds*sizeof(float*));
    float** blockPtrA = (float**) malloc(comp_thds*sizeof(float*));
    float** blockPtrB = (float**) malloc(comp_thds*sizeof(float*));

    for (int i = 0; i < comp_thds; ++i) {
       blockPtrDst[i] = nullptr; 
       blockPtrA[i] = nullptr; 
       blockPtrB[i] = nullptr; 
    }

    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        for (int k = 0; k < 16; ++k) {
            flushLlc(bufToFlushLlc);
        }

        startTime = utility::timer();
        arrayWiseFMAAVX(blockPtrDst, blockPtrA, blockPtrB, blockSize, blockSizeBasic, 
                zMat, xMat, yMat, comp_thds);

        timeElapsed += (utility::timer() - startTime);
    }

    printf("EMA using %f secs\n", timeElapsed/numCols);
    printf("EMA Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("EMA Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("EMA Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);
    std::fflush(stdout);           

    _mm_free(xMat);
    _mm_free(yMat);
    _mm_free(zMat);
    _mm_free(bufToFlushLlc);

    free(blockPtrDst);
    free(blockPtrA);
    free(blockPtrB);
    free(blockSize);

}

// for check reordering
bool checkPerm(const int *perm, int n)
{
  int *temp = new int[n];
  std::memcpy(temp, perm, sizeof(int)*n);
  sort(temp, temp + n);
  int *last = unique(temp, temp + n);
  if (last != temp + n) {
    memcpy(temp, perm, sizeof(int)*n);
    sort(temp, temp + n);

    for (int i = 0; i < n; ++i) {
      if (temp[i] == i - 1) {
        printf("%d duplicated\n", i - 1);
        assert(false);
        return false;
      }
      else if (temp[i] != i) {
        printf("%d missed\n", i);
        assert(false);
        return false;
      }
    }
  }
  delete[] temp;
  return true;
}

void SpMVSpMP(int m, int* rowPtr, int* colPtr, float* vals, float* x, float* y, int comp_thds)
{

#pragma omp parallel for num_threads(comp_thds)
    for(int i = 0; i<m; i++)
    {
        float sum = 0.0;

        int rowLen = (rowPtr[i+1] - rowPtr[i]); 
        int* rowColIdx = colPtr + rowPtr[i];
        // float* rowElem = vals + rowPtr[i]; 

        #pragma omp simd reduction(+:sum) 
        for(int j=0; j<rowLen;j++)
            sum += (x[rowColIdx[j]]);
            // sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void SpMVSpMPFull(int m, int* rowPtr, int* colPtr, float* vals, float* x, float* y, int comp_thds)
{

#pragma omp parallel for num_threads(comp_thds)
    for(int i = 0; i<m; i++)
    {
        float sum = 0.0;

        int rowLen = (rowPtr[i+1] - rowPtr[i]); 
        int* rowColIdx = colPtr + rowPtr[i];
        float* rowElem = vals + rowPtr[i]; 

        #pragma omp simd reduction(+:sum) 
        for(int j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}
void benchmarkSpMP(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
    double startTime = 0.0;
    double timeElapsed = 0.0;
    printf("Start debug SpMP RCM SpMV\n");
    std::fflush(stdout);

    CSRGraph csrg;
    csrg.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, true, true);

    double flopsTotal =  csrg.getNNZ();
    double bytesTotal = sizeof(float)*2*csrg.getNumVertices() + sizeof(int)*(csrg.getNNZ() + csrg.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    // create SpMP::CSR
    int csrRows = csrg.getNumVertices();
    // length csrRows+1
    int* csrRowIdx = csrg.getIndexRow(); 
    int* csrColIdx = csrg.getIndexCol();
    float* csrVals = csrg.getNNZVal();

    // create CSR (not own data)
    SpMP::CSR spmpcsr(csrRows, csrRows, csrRowIdx, csrColIdx, csrVals);

    // RCM reordering
    printf("Start SpMP RCM reordering\n");
    std::fflush(stdout);

    int *perm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);
    int *inversePerm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);

    spmpcsr.getRCMPermutation(perm, inversePerm);

    // check the permutation
    if (checkPerm(perm, spmpcsr.m ));
    {
        printf("Reordering coloum sccuess\n");
        std::fflush(stdout);
        // for (int i = 0; i < 10; ++i) {
        //     printf("permcol: %d is %d\n", i, perm[i]);
        //     std::fflush(stdout);
        // }
    }

    if (checkPerm(inversePerm,  spmpcsr.m));
    {
        printf("Reordering row sccuess\n");
        std::fflush(stdout);
        // for (int i = 0; i < 10; ++i) {
        //     printf("permrow: %d is %d\n", i, inversePerm[i]);
        //     std::fflush(stdout);
        // }
    }

    // data allocated at APerm
    SpMP::CSR *APerm = spmpcsr.permute(perm, inversePerm, false, true);

    // do a new SpMV 
    float* xArray = (float*) _mm_malloc(APerm->m*sizeof(float), 64);
    float* yArray = (float*) _mm_malloc(APerm->m*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < APerm->m; ++i) {
        xArray[i] = 2.0; 
        yArray[i] = 0.0; 
    }

    // float* yArray = (float*) malloc(APerm->m*sizeof(float));
    // std::memset(yArray, 0, APerm->m*sizeof(float));
    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        for (int k = 0; k < 16; ++k) {
            flushLlc(bufToFlushLlc);
        }

        startTime = utility::timer();
        SpMVSpMP(APerm->m, APerm->rowptr, APerm->colidx, APerm->svalues, xArray, yArray, comp_thds);
        timeElapsed += (utility::timer() - startTime);
    }

    printf("SpMP RCM SpMV using %f secs\n", timeElapsed/numCols);
    printf("SpMP RCM SpMV Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("SpMP RCM SpMV Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("SpMP RCM SpMV Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           

    // check yMat
    // for (int i = 0; i < APerm->m; ++i) {
    //
    //     if (inversePerm[i] >= 0 && inversePerm[i] < 10)
    //     {
    //         printf("Elem: %d is: %f\n", inversePerm[i], yArray[i]); 
    //         std::fflush(stdout);
    //     }
    //         
    // }

    delete APerm;
    _mm_free(xArray); 
    _mm_free(yArray);
    _mm_free(bufToFlushLlc);
    _mm_free(perm);
    _mm_free(inversePerm);

    printf("Finish SpMP RCM reordering\n");
    std::fflush(stdout);

}

void benchmarkSpMPFull(int argc, char** argv, EdgeList& elist, int numCols, int comp_thds)
{
    double startTime = 0.0;
    double timeElapsed = 0.0;
    printf("Start debug SpMP RCM SpMV\n");
    std::fflush(stdout);

    CSRGraph csrg;
    csrg.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false, true, true);

    double flopsTotal =  2*csrg.getNNZ();
    double bytesTotal = sizeof(float)*(csrg.getNNZ() + 2*csrg.getNumVertices()) + sizeof(int)*(csrg.getNNZ() + csrg.getNumVertices()); 
    flopsTotal /= (1024*1024*1024);
    bytesTotal /= (1024*1024*1024);

    // create SpMP::CSR
    int csrRows = csrg.getNumVertices();
    // length csrRows+1
    int* csrRowIdx = csrg.getIndexRow(); 
    int* csrColIdx = csrg.getIndexCol();
    float* csrVals = csrg.getNNZVal();

    // create CSR (not own data)
    SpMP::CSR spmpcsr(csrRows, csrRows, csrRowIdx, csrColIdx, csrVals);

    // RCM reordering
    printf("Start SpMP RCM reordering\n");
    std::fflush(stdout);

    int *perm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);
    int *inversePerm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);

    spmpcsr.getRCMPermutation(perm, inversePerm);

    // check the permutation
    if (checkPerm(perm, spmpcsr.m ));
    {
        printf("Reordering coloum sccuess\n");
        std::fflush(stdout);
        // for (int i = 0; i < 10; ++i) {
        //     printf("permcol: %d is %d\n", i, perm[i]);
        //     std::fflush(stdout);
        // }
    }

    if (checkPerm(inversePerm,  spmpcsr.m));
    {
        printf("Reordering row sccuess\n");
        std::fflush(stdout);
        // for (int i = 0; i < 10; ++i) {
        //     printf("permrow: %d is %d\n", i, inversePerm[i]);
        //     std::fflush(stdout);
        // }
    }

    // data allocated at APerm
    SpMP::CSR *APerm = spmpcsr.permute(perm, inversePerm, false, true);

    // do a new SpMV 
    float* xArray = (float*) _mm_malloc(APerm->m*sizeof(float), 64);
    float* yArray = (float*) _mm_malloc(APerm->m*sizeof(float), 64);

#pragma omp parallel for
    for (int i = 0; i < APerm->m; ++i) {
        xArray[i] = 2.0; 
        yArray[i] = 0.0; 
    }

    // float* yArray = (float*) malloc(APerm->m*sizeof(float));
    // std::memset(yArray, 0, APerm->m*sizeof(float));
    float* bufToFlushLlc = (float*)_mm_malloc(LLC_CAPACITY, 64);

    for (int j = 0; j < numCols; ++j) {

        // flush out LLC
        for (int k = 0; k < 16; ++k) {
            flushLlc(bufToFlushLlc);
        }

        startTime = utility::timer();
        SpMVSpMPFull(APerm->m, APerm->rowptr, APerm->colidx, APerm->svalues, xArray, yArray, comp_thds);
        timeElapsed += (utility::timer() - startTime);
    }

    printf("SpMP RCM SpMVFull using %f secs\n", timeElapsed/numCols);
    printf("SpMP RCM SpMVFull Arith Intensity %f\n", (flopsTotal/bytesTotal));
    printf("SpMP RCM SpMVFull Bd: %f GBytes/sec\n", bytesTotal*numCols/timeElapsed);
    printf("SpMP RCM SpMVFull Tht: %f GFLOP/sec\n", flopsTotal*numCols/timeElapsed);

    std::fflush(stdout);           

    // check yMat
    // for (int i = 0; i < APerm->m; ++i) {
    //
    //     if (inversePerm[i] >= 0 && inversePerm[i] < 10)
    //     {
    //         printf("Elem: %d is: %f\n", inversePerm[i], yArray[i]); 
    //         std::fflush(stdout);
    //     }
    //         
    // }

    delete APerm;
    _mm_free(xArray); 
    _mm_free(yArray);
    _mm_free(bufToFlushLlc);
    _mm_free(perm);
    _mm_free(inversePerm);

    printf("Finish SpMP RCM reordering\n");
    std::fflush(stdout);

}

int main(int argc, char** argv)
{
   
    int load_binary = 0;
    int write_binary = 0;
    string graph_name;
    string template_name;
    int iterations;
    int comp_thds;
    int isPruned = 1;
    int vtuneStart = -1;
    // bool calculate_automorphism = true;
    bool calculate_automorphism = false;

    int useSPMM = 0;
    // bool useMKL = true;
    bool useMKL = false;
    // bool useRcm = true;
    bool useRcm = false;
    bool useCSC = true;
    // bool useCSC = false;

    bool isBenchmark = true;
    // bool isBenchmark = false;
    // turn on this to estimate flops and memory bytes 
    // without running the codes
    bool isEstimate = false;
    // bool isEstimate = true;

    graph_name = argv[1];
    template_name = argv[2];
    iterations = atoi(argv[3]);
    comp_thds = atoi(argv[4]);
    load_binary = atoi(argv[5]);
    write_binary = atoi(argv[6]); 

    if (argc > 7)
        isPruned = atoi(argv[7]); 

    if (argc > 8)
        useSPMM = atoi(argv[8]);

    if (argc > 9)
        vtuneStart = atoi(argv[9]);

    // end of arguments
    
    // SPMM in CSR uses MKL
    if (useSPMM && (!useCSC))
        useMKL = true;

    if (useCSC)
    {
        useRcm = false;
        useMKL = false;
    }

#ifdef VERBOSE 
    if (isPruned) {
        printf("Use Pruned Mat Algorithm Impl\n");
        std::fflush(stdout);   
    }
    if (useSPMM) {
        printf("Use SPMM Impl\n");
        std::fflush(stdout);          
    }
#ifdef __AVX512F__
    printf("AVX512 available\n");
    std::fflush(stdout);          
#else
#ifdef __AVX2__
    printf("AVX2 available\n");
    std::fflush(stdout);                 
#else
    printf("No avx available\n");
    std::fflush(stdout);                 
#endif
#endif
#endif

    CSRGraph* csrInputG = nullptr;
    CSCGraph<int32_t, float>* cscInputG = nullptr;

    if (!useCSC)
        csrInputG = new CSRGraph();
    else
        cscInputG = new CSCGraph<int32_t, float>();
        
    Graph input_template;
    double startTime = utility::timer();

    // read in graph file and make 
    printf("Start loading datasets\n");
    std::fflush(stdout);

    startTime = utility::timer();

    // load input graph 
    if (load_binary)
    {

#ifdef VERBOSE 
         printf("Start the loading graph data in binary format\n");
         std::fflush(stdout);   
#endif

        ifstream input_file(graph_name.c_str(), ios::binary);
        if (csrInputG != nullptr)
            csrInputG->deserialize(input_file, useMKL, useRcm);
        else
        {
            cscInputG->deserialize(input_file);
            cscInputG->splitCSC(4*comp_thds);
        }

        input_file.close();
    }
    else
    {
#ifdef VERBOSE
        printf("Start the loading graph data in text format\n");
        std::fflush(stdout);           
#endif

        EdgeList elist(graph_name); 
        if (isBenchmark)
        {

#ifdef VERBOSE
            printf("Start benchmarking SpMV or SpMM\n");
            std::fflush(stdout);           
#endif

            const int numCols = 64;
            // const int numCols = 100;
            // const int numCols = 10;
            
            // benchmarking SpMP RCM reordering and 0-1 SpMV
            // benchmarkSpMP(argc, argv, elist,  numCols, comp_thds );
            
            // benchmarking SpMP RCM reordering standard full SpMV
            // benchmarkSpMPFull(argc, argv, elist,  numCols, comp_thds );
           
            // benchmarking mkl SpMV (inspector executor)
            // benchmarkSpMVMKL(argc, argv, elist, numCols, comp_thds);

            // benchmarking PB SpMV 
            // benchmarkSpMVPBRadix(argc, argv, elist, numCols);

            // benchmarking mkl MM (inspector executor)
            // benchmarkMMMKL(argc, argv, elist, numCols, comp_thds);

            // benchmarking mkl SpMM
            // benchmarkSpMMMKL(argc, argv, elist, numCols, comp_thds);
            
            // benchmarking SpDM3 SpMM
            // benchmarkSpDM3(argc, argv, elist, numCols, comp_thds);

            // benchmarking Naive SpMV
            // benchmarkSpMVNaive(argc, argv, elist, numCols, comp_thds);
            
            // benchmarking Naive SpMV Full
            // benchmarkSpMVNaiveFull(argc, argv, elist, numCols, comp_thds);
            //
            // benchmarking Naive SpMV Full
            // benchmarkSpMVNaiveFullCSC(argc, argv, elist, numCols, comp_thds);

            // benchmarking CSC-Split MM
            benchmarkCSCSplitMM(argc, argv, elist, numCols, comp_thds);

            // benchmarking eMA 
            // benchmarkEMA(argc, argv, elist, numCols, comp_thds);

#ifdef VERBOSE
            printf("Finish benchmarking SpMV or SpMM\n");
            std::fflush(stdout);           
#endif

            return 0;
        }
        else
        {

            if (csrInputG != nullptr)
                csrInputG->createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), useMKL, useRcm, false);
            else
            {
                cscInputG->createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList(), false);
                cscInputG->splitCSC(4*comp_thds);
            }
        }
    }

    if (write_binary)
    {
        // save graph into binary file, graph is a data structure
        ofstream output_file("graph.data", ios::binary);

        if (csrInputG != nullptr)
            csrInputG->serialize(output_file);
        else
        {
            // TODO
            cscInputG->serialize(output_file);
        }

        output_file.close();
    }

    printf("Loading Datasets using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           
    
    // ---------------- start of computing ----------------
    // load input templates
    input_template.read_enlist(template_name);

    // start CSR mat computing
    CountMat executor;
    executor.initialization(csrInputG, cscInputG, comp_thds, iterations, isPruned, useSPMM, vtuneStart, calculate_automorphism);

    executor.compute(input_template, isEstimate);

    if (csrInputG != nullptr)
        delete csrInputG;

    if (cscInputG != nullptr)
        delete cscInputG;

    return 0;

}


