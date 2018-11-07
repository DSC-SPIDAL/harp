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

    graph_name = argv[1];
    template_name = argv[2];
    iterations = atoi(argv[3]);
    comp_thds = atoi(argv[4]);
    load_binary = atoi(argv[5]);
    write_binary = atoi(argv[6]); 

    if (argc > 7)
        isPruned = atoi(argv[7]); 

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
    // // y = CSRGraph*x
    // printf("Start debug CSR SpMV len %d\n", elist.getNumVertices());
    // std::fflush(stdout);
    //
    // int csrNNZA = csrInpuG.getNNZ(); 
    // int csrRows = csrInpuG.getNumVertices();
    // int* csrRowIdx = csrInpuG.getIndexRow();
    // int* csrColIdx = csrInpuG.getIndexCol();
    // float* csrVals = csrInpuG.getNNZVal();
    // double* csrValsDouble = (double*) malloc(csrNNZA*sizeof(double)); 
    // for (int i = 0; i < csrNNZA; ++i) {
    //    csrValsDouble[i] = csrVals[i]; 
    // }
    //
    // // double* xArray = new double[csrRows];
    // // double* yArray = new double[csrRows];
    // float* xArray = new float[csrRows];
    // float* yArray = new float[csrRows];
    //
    // for(int i=0;i<csrRows; i++)
    // {
    //     xArray[i] = 2.0;
    //     yArray[i] = 0.0;
    // }
    //
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
    
    // load input templates
    input_template.read_enlist(template_name);

    // start CSR mat computing
    CountMat executor;
    executor.initialization(csrInpuG, comp_thds, iterations, isPruned);
    executor.compute(input_template);

    return 0;

}


