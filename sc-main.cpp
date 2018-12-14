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

#include "Graph.hpp"
#include "Count.hpp"
#include "Helper.hpp"

using namespace std;

int main(int argc, char** argv)
{
    int load_binary = 0;
    int write_binary = 0;
    string graph_name;
    string template_name;
    int iterations;
    int comp_thds;
    int algoMode = 0;
    int vtuneStart = -1;
    // bool calcMorphism = true;
    bool calcMorphism = false;

    bool isEstimate = false;
    // bool isEstimate = true;

    graph_name = argv[1];
    template_name = argv[2];
    iterations = atoi(argv[3]);
    comp_thds = atoi(argv[4]);
    load_binary = atoi(argv[5]);
    write_binary = atoi(argv[6]); 

    if (argc > 7)
        algoMode = atoi(argv[7]);

    if (argc > 8)
        vtuneStart = atoi(argv[8]);

    Graph input_graph;
    Graph input_template;

    double startTime = utility::timer();

    // load input graph 
    if (load_binary)
    {

#ifdef VERBOSE 
         printf("Start the loading graph data in binary format\n");
         std::fflush(stdout);   
#endif

        ifstream input_file(graph_name.c_str(), ios::binary);
        input_graph.deserialize(input_file);
        input_file.close();
    }
    else
    {
#ifdef VERBOSE
        printf("Start the loading graph data in text format\n");
        std::fflush(stdout);           
#endif
        input_graph.read_enlist(graph_name);
    }

    if (write_binary)
    {
        // save graph into binary file, graph is a data structure
        ofstream output_file("graph.data", ios::binary);
        input_graph.serialize(output_file);
        output_file.close();
    }

    // load input templates
    input_template.read_enlist(template_name);

    printf("Loading data using %f secs\n", (utility::timer() - startTime));
    std::fflush(stdout);           

    // start computing
    Count executor;
    executor.initialization(input_graph, comp_thds, iterations, algoMode, vtuneStart, calcMorphism);
    executor.compute(input_template, isEstimate);

    return 0;

}


