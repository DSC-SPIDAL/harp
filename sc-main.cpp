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

using namespace std;

void benchmarkSpMVPBRadix(int argc, char** argv, EdgeList& elist)
{
    double startTime;
    int len = 100;
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

    pvector<ParGuider<int32_t, float>*> par_guides(omp_get_max_threads());
#pragma omp parallel
    // par_guides[omp_get_thread_num()] = new ParGuider<int32_t, float>(19,omp_get_thread_num(), omp_get_max_threads(), radixG);
    par_guides[omp_get_thread_num()] = new ParGuider<int32_t, float>(binSize,omp_get_thread_num(), omp_get_max_threads(), radixG);

    printf("Finish build par_parts\n");
    std::fflush(stdout);

    float* xMat = (float*) malloc(radixG.num_nodes()*sizeof(float));
    for (int i = 0; i < radixG.num_nodes(); ++i) {
        xMat[i] = 2.0; 
    }

    float* yMat = (float*) malloc(radixG.num_nodes()*sizeof(float));
    std::memset(yMat, 0, radixG.num_nodes()*sizeof(float));
    //
    startTime = utility::timer();

    // check pagerank scores
    // for (int j = 0; j < 100; ++j) {
    // SpMVRadixPar(xMat, yMat, radixG, 1, kGoalEpsilon, par_parts);
    SpMVGuidesPar(xMat, yMat, radixG, len, kGoalEpsilon, par_guides);
    // }
    //
    printf("Radix SpMV using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           
    //
    // check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    // free memory
    free(xMat);
    free(yMat);

    // -------------------- end debug the Radix SpMV ------------------------------
}

void benchmarkSpMVNaive(int argc, char** argv, EdgeList& elist, int comp_thds)
{

    double startTime;
    int len = 100;
    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    float* xMat = (float*) malloc(csrnaiveG.getNumVertices()*sizeof(float));
    for (int i = 0; i < csrnaiveG.getNumVertices(); ++i) {
        xMat[i] = 2.0; 
    }

    float* yMat = (float*) malloc(csrnaiveG.getNumVertices()*sizeof(float));
    std::memset(yMat, 0, csrnaiveG.getNumVertices()*sizeof(float));

    // test SpMV naive
    startTime = utility::timer();
    for (int j = 0; j < len; ++j) {
        csrnaiveG.SpMVNaive(xMat, yMat, comp_thds);
    }

    printf("Naive SpMV using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yMat[i]); 
        std::fflush(stdout);
    }

    free(xMat);
    free(yMat);
}

// Inspector-Executor interface in MKL 11.3+
// NOTICE: the way to invoke the mkl 11.3 inspector-executor
void benchmarkSpMVMKL(int argc, char** argv, EdgeList& elist, int comp_thds)
{
  
    double startTime;
    const int len = 100;
    CSRGraph csrGMKL;
    csrGMKL.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

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

    stat = mkl_sparse_set_mv_hint(
    mklA, SPARSE_OPERATION_NON_TRANSPOSE, descA, len);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to set mv hint\n");
        return;
    }

    stat = mkl_sparse_optimize(mklA);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to sparse optimize\n");
        return;
    }

    float* xArray = (float*) malloc(csrGMKL.getNumVertices()*sizeof(float));
    for (int i = 0; i < csrGMKL.getNumVertices(); ++i) {
        xArray[i] = 2.0; 
    }

    float* yArray = (float*) malloc(csrGMKL.getNumVertices()*sizeof(float));
    std::memset(yArray, 0, csrGMKL.getNumVertices()*sizeof(float));

    startTime = utility::timer();

    for (int j = 0; j < len; ++j) {
        mkl_sparse_s_mv(SPARSE_OPERATION_NON_TRANSPOSE, 1, mklA, descA, xArray, 0, yArray);
    }

    printf("MKL SpMV using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < 10; ++i) {
        printf("Elem: %d is: %f\n", i, yArray[i]); 
        std::fflush(stdout);
    }

    free(xArray);
    free(yArray);
}

void benchmarkSpMMMKL(int argc, char** argv, EdgeList& elist, int comp_thds)
{
    double startTime;
    printf("Start debug CSR SpMM \n");
    std::fflush(stdout);

    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    //
    int csrNNZA = csrnaiveG.getNNZ(); 
    int csrRows = csrnaiveG.getNumVertices();
    int* csrRowIdx = csrnaiveG.getIndexRow();
    int* csrColIdx = csrnaiveG.getIndexCol();
    float* csrVals = csrnaiveG.getNNZVal();

    int testCols = 100;
    int testLen = testCols*csrRows;

    float* xMat = (float*) malloc(testLen*sizeof(float));
    float* yMat = (float*) malloc(testLen*sizeof(float));
    std::memset(yMat, 0, testLen*sizeof(float));

    for (int i = 0; i < testLen; ++i) {
       xMat[i] = 2.0; 
    }

    // invoke mkl scsrmm
    char transa = 'n';
    MKL_INT m = csrRows;
    MKL_INT n = testCols;
    MKL_INT k = csrRows;

    float alpha = 1.0;
    float beta = 0.0;

    char matdescra[5];
    matdescra[0] = 'g';
    matdescra[3] = 'f'; /*one-based indexing is used*/

    mkl_scsrmm(&transa, &m, &n, &k, &alpha, matdescra, csrVals, csrColIdx, csrRowIdx, &(csrRowIdx[1]), xMat, &k, &beta, yMat, &k);

    // check yMat
    for (int i = 0; i < 10; ++i) {
       printf("Elem: %d is: %f\n", i, yMat[i]); 
       std::fflush(stdout);
    }

    // free test mem
    free(xMat);
    free(yMat);

    printf("Finish debug CSR SpMM\n");
    std::fflush(stdout);

}

void benchmarkSpDM3(int argc, char** argv, EdgeList& elist, int comp_thds)
{    
    double startTime;
    printf("Start debug Spdm3 SpMM\n");
    std::fflush(stdout);

    CSRGraph csrnaiveG;
    csrnaiveG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    spdm3::SpMat<int, float> smat(spdm3::SPARSE_CSR, 0);
    csrnaiveG.fillSpMat(smat);

    // use smat
    int rowNum = smat.dim1();
    int colNum = 100;
    int testLen = rowNum*colNum;
    float* xArray = (float*) malloc (testLen*sizeof(float));
    for (int i = 0; i < testLen; ++i) {
       xArray[i] = 2.0; 
    }   

    float* yArray = (float*) malloc (testLen*sizeof(float));
    std::memset(yArray, 0, testLen*sizeof(float));

    // data copy from xArray to xMat
    // TODO replace data copy by pointer assignment
    spdm3::DMat<int, float> xMat(rowNum, colNum, rowNum, spdm3::DENSE_COLMAJOR, xArray);
    spdm3::DMat<int, float> yMat(rowNum, colNum, rowNum, spdm3::DENSE_COLMAJOR, yArray);

    printf("Dmat: row: %d, cols: %d\n", xMat.rows_, xMat.cols_);
    std::fflush(stdout);

    startTime = utility::timer();
    // start the SpMM 
    spdm3::matmul_blas_colmajor<int>(smat, xMat, yMat);

    printf("SpDM3 SpMM using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < 10; ++i) {
       printf("Elem: %d is: %f\n", i, yMat.values_[i]); 
       std::fflush(stdout);
    }

    printf("Finish debug Spdm3 SpMM\n");
    std::fflush(stdout);

    free(xArray);
    free(yArray);
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
        float* rowElem = vals + rowPtr[i]; 

        #pragma omp simd reduction(+:sum) 
        for(int j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void benchmarkSpMP(int argc, char** argv, EdgeList& elist, int comp_thds)
{
    double startTime;
    printf("Start debug Spdm3 SpMM\n");
    std::fflush(stdout);

    CSRGraph csrg;
    csrg.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    // create SpMP::CSR
    int csrRows = csrg.getNumVertices();
    // length csrRows+1
    int* csrRowIdx = csrg.getIndexRow(); 
    int* csrColIdx = csrg.getIndexCol();
    float* csrVals = csrg.getNNZVal();

    // create CSR (not own data)
    SpMP::CSR spmpcsr(csrRows, csrRows, csrRowIdx, csrColIdx, csrVals);

    // RCM reordering
    printf("Start Spdm3 RCM reordering\n");
    std::fflush(stdout);

    int *perm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);
    int *inversePerm = (int*)_mm_malloc(spmpcsr.m*sizeof(int), 64);

    spmpcsr.getRCMPermutation(perm, inversePerm);

    // check the permutation
    if (checkPerm(perm, spmpcsr.m ));
    {
        printf("Reordering coloum sccuess\n");
        std::fflush(stdout);
        for (int i = 0; i < 10; ++i) {
            printf("permcol: %d is %d\n", i, perm[i]);
            std::fflush(stdout);
        }
    }

    if (checkPerm(inversePerm,  spmpcsr.m));
    {
        printf("Reordering row sccuess\n");
        std::fflush(stdout);
        for (int i = 0; i < 10; ++i) {
            printf("permrow: %d is %d\n", i, inversePerm[i]);
            std::fflush(stdout);
        }
    }

    // data allocated at APerm
    SpMP::CSR *APerm = spmpcsr.permute(perm, inversePerm, false, true);

    // do a new SpMV 
    float* xArray = (float*) malloc(APerm->m*sizeof(float));
    for (int i = 0; i < APerm->m; ++i) {
        xArray[i] = 2.0; 
    }

    float* yArray = (float*) malloc(APerm->m*sizeof(float));
    std::memset(yArray, 0, APerm->m*sizeof(float));

    int len = 100;
    startTime = utility::timer();
    for (int j = 0; j < len; ++j) {
        SpMVSpMP(APerm->m, APerm->rowptr, APerm->colidx, APerm->svalues, xArray, yArray, comp_thds);
    }
    printf("SpMP RCM SpMV using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // check yMat
    for (int i = 0; i < APerm->m; ++i) {

        if (inversePerm[i] >= 0 && inversePerm[i] < 10)
        {
            printf("Elem: %d is: %f\n", inversePerm[i], yArray[i]); 
            std::fflush(stdout);
        }
            
    }

    delete APerm;
    free(xArray); 
    free(yArray);
    _mm_free(perm);
    _mm_free(inversePerm);

    printf("Finish Spdm3 RCM reordering\n");
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
    int useSPMM = 0;

    bool isBenchmark = true;
    // bool isBenchmark = false;

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

    CSRGraph csrInpuG;
    Graph input_template;
    double startTime = utility::timer();

    // read in graph file and make CSR format
    printf("Start loading CSR datasets\n");
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
        csrInpuG.deserialize(input_file);
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

            // benchmarking SpMP RCM reordering
            // benchmarkSpMP(argc, argv, elist, comp_thds );
            
            // benchmarking mkl SpMV
            benchmarkSpMVMKL(argc, argv, elist, comp_thds);

            // benchmarking PB SpMV 
            // benchmarkSpMVPBRadix(argc, argv, elist);

            // benchmarking Naive SpMV
            benchmarkSpMVNaive(argc, argv, elist, comp_thds);

            // benchmarking mkl SpMM
            // benchmarkSpMMMKL(argc, argv, elist, comp_thds);
            
            // benchmarking SpDM3 SpMM
            // benchmarkSpDM3(argc, argv, elist, comp_thds);

            
#ifdef VERBOSE
            printf("Finish benchmarking SpMV or SpMM\n");
            std::fflush(stdout);           
#endif

            return 0;
        }
        else
            csrInpuG.createFromEdgeListFile(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());
    }

    if (write_binary)
    {
        // save graph into binary file, graph is a data structure
        ofstream output_file("graph.data", ios::binary);
        csrInpuG.serialize(output_file);
        output_file.close();
    }

    printf("Finish CSR format\n");
    std::fflush(stdout);
    printf("Loading CSR data using %f secs, vert: %d\n", (utility::timer() - startTime), csrInpuG.getNumVertices());
    std::fflush(stdout);           
    
    // ---------------- start of computing ----------------
    // load input templates
    input_template.read_enlist(template_name);

    // start CSR mat computing
    CountMat executor;
    executor.initialization(csrInpuG, comp_thds, iterations, isPruned, useSPMM);
    executor.compute(input_template);

    return 0;

}


