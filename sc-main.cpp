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

#include "Graph.hpp"
#include "CSRGraph.hpp"
#include "Count.hpp"
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

    graph_name = argv[1];
    template_name = argv[2];
    iterations = atoi(argv[3]);
    comp_thds = atoi(argv[4]);
    load_binary = atoi(argv[5]);
    write_binary = atoi(argv[6]); 

    Graph input_graph;
    Graph input_template;

    double startTime = utility::timer();

//     // load input graph 
//     if (load_binary)
//     {
//
// #ifdef VERBOSE 
//          printf("Start the loading graph data in binary format\n");
//          std::fflush(stdout);   
// #endif
//
//         ifstream input_file(graph_name.c_str(), ios::binary);
//         input_graph.deserialize(input_file);
//         input_file.close();
//     }
//     else
//     {
// #ifdef VERBOSE
//         printf("Start the loading graph data in text format\n");
//         std::fflush(stdout);           
// #endif
//         input_graph.read_enlist(graph_name);
//     }
//
//     if (write_binary)
//     {
//         // save graph into binary file, graph is a data structure
//         ofstream output_file("graph.data", ios::binary);
//         input_graph.serialize(output_file);
//         output_file.close();
//     }
//
//     printf("Loading data using %f secs\n", (utility::timer() - startTime));
//     std::fflush(stdout);           

    // read in graph file and make CSR format
    printf("Start debug CSR format\n");
    std::fflush(stdout);

    startTime = utility::timer();

    EdgeList elist(graph_name); 
    CSRGraph csrInpuG(elist.getNumVertices(), elist.getNumEdges(), elist.getSrcList(), elist.getDstList());

    printf("Finish debug CSR format\n");
    std::fflush(stdout);
    printf("Loading CSR data using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // printf("Start debug CSR SpMV len %d\n", elist.getNumVertices());
    // std::fflush(stdout);

    // // test SpMV sequential
    // // y = CSRGraph*x
    // float* xArray = new float[elist.getNumVertices()];
    // float* yArray = new float[elist.getNumVertices()];
    // for(int i=0;i<elist.getNumVertices(); i++)
    // {
    //     xArray[i] = 2.0;
    //     yArray[i] = 0.0;
    // }
    //
    // startTime = utility::timer();
    // csrInpuG.SpMVNaive(xArray, yArray);
    // printf("Compute CSR SpMV using %f secs\n", (utility::timer() - startTime));
    // std::fflush(stdout);           
    //
    // //debug
    // for(int i=0;i<10;i++)
    // {
    //     printf("yarray %d is %f\n", i, yArray[i]);
    //     std::fflush(stdout);
    // }
    //
    // delete[] xArray;
    // delete[] yArray;
    //
    // printf("Finish debug CSR SpMV\n");
    // std::fflush(stdout);
    
    // load input templates
    input_template.read_enlist(template_name);

    // start CSR mat computing
    CountMat executor;

    printf("Start initialize CountMat\n");
    std::fflush(stdout);

    executor.initialization(csrInpuG, comp_thds, iterations);

    printf("Finish initialize CountMat\n");
    std::fflush(stdout);

    executor.compute(input_template);
    
    return 0;

}


