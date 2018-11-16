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

#include "mkl.h"
#include "Graph.hpp"
#include "CSRGraph.hpp"
#include "CountMat.hpp"
#include "Helper.hpp"
#include "EdgeList.hpp"

using namespace std;

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
    printf("Loading CSR data using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // // test SpMV naive, mkl, and csr5 using double
    // // yMat = CSRGraph*xMat
    // printf("Start debug CSR SpMM \n");
    // std::fflush(stdout);
    // //
    // int csrNNZA = csrInpuG.getNNZ(); 
    // int csrRows = csrInpuG.getNumVertices();
    // int* csrRowIdx = csrInpuG.getIndexRow();
    // int* csrColIdx = csrInpuG.getIndexCol();
    // float* csrVals = csrInpuG.getNNZVal();
    //
    // int testCols = 100;
    // int testLen = testCols*csrRows;
    //
    // float* xMat = (float*) malloc(testLen*sizeof(float));
    // float* yMat = (float*) malloc(testLen*sizeof(float));
    // std::memset(yMat, 0, testLen*sizeof(float));
    //
    // for (int i = 0; i < testLen; ++i) {
    //    xMat[i] = 2.0; 
    // }
    //
    // // invoke mkl scsrmm
    // char transa = 'n';
    // MKL_INT m = csrRows;
    // MKL_INT n = testCols;
    // MKL_INT k = csrRows;
    //
    // float alpha = 1.0;
    // float beta = 0.0;
    //
    // char matdescra[5];
    // matdescra[0] = 'g';
    // matdescra[3] = 'f'; /*one-based indexing is used*/
    //
    // mkl_scsrmm(&transa, &m, &n, &k, &alpha, matdescra, csrVals, csrColIdx, csrRowIdx, &(csrRowIdx[1]), xMat, &k, &beta, yMat, &k);
    //
    // // check yMat
    // for (int i = 0; i < 10; ++i) {
    //    printf("Elem: %d is: %f\n", i, yMat[i]); 
    //    std::fflush(stdout);
    // }
    //
    // // free test mem
    // free(xMat);
    // free(yMat);
    //
    // printf("Finish debug CSR SpMM\n");
    // std::fflush(stdout);
   
    // startTime = utility::timer();
    // const char tran = 'N';
    // // mkl_cspblas_dcsrgemv(&tran, &csrRows, csrValsDouble, csrRowIdx, csrColIdx, xArray, yArray);
    // mkl_cspblas_scsrgemv(&tran, &csrRows, csrVals, csrRowIdx, csrColIdx, xArray, yArray);
    // printf("Compute CSR SpMV MKL using %f secs\n", (utility::timer() - startTime));
    // std::fflush(stdout);           
    // std::memset(yArray,0, csrRows*sizeof(float));

    // anonymouslibHandle<int, unsigned int, double> csr5Mat(csrRows, csrRows);
    // csr5Mat.inputCSR(csrNNZA, csrRowIdx, csrColIdx, csrValsDouble);
    // // csr5Mat.inputCSR(csrNNZA, csrRowIdx, csrColIdx, csrVals);
    // csr5Mat.setX(xArray);
    // int sigma = ANONYMOUSLIB_CSR5_SIGMA;
    // csr5Mat.setSigma(sigma);
    //
    // startTime = utility::timer();
    // csr5Mat.asCSR5();
    // printf("Convert CSR to CSR5 using %f secs\n", (utility::timer() - startTime));
    // std::fflush(stdout);           
    //
    // for (int j = 0; j < 100; ++j) {
    //     csr5Mat.spmv(1.0, yArray);
    // }
    //
    // startTime = utility::timer();
    // csr5Mat.spmv(1.0, yArray);
    // printf("CSR5 SpMV using %f secs\n", (utility::timer() - startTime));
    // std::fflush(stdout);           

    // delete[] xArray;
    // delete[] yArray;
    //
    // printf("Finish debug CSR SpMV\n");
    // std::fflush(stdout);
    
    // ---------------- start of computing ----------------
    // load input templates
    input_template.read_enlist(template_name);

    // start CSR mat computing
    CountMat executor;
    executor.initialization(csrInpuG, comp_thds, iterations, isPruned, useSPMM);
    executor.compute(input_template);

    return 0;

}


