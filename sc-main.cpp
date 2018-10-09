

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

using namespace std;

int main(int argc, char** argv)
{
    bool load_binary = false;
    bool write_binary = false;
    string graph_name;
    string template_name;
    int iterations;
    int comp_thds;

    graph_name = argv[1];
    template_name = argv[2];
    iterations = atoi(argv[3]);
    comp_thds = atoi(argv[4]);

    Graph input_graph;
    Graph input_template;

    // load input graph 
    if (load_binary)
    {
      ifstream input_file(graph_name, ios::binary);
      input_graph.deserialize(input_file);
      input_file.close();
    }
    else
        input_graph.read_enlist(graph_name);

    if (write_binary)
    {
        // save graph into binary file, graph is a data structure
        ofstream output_file("graph.data", ios::binary);
        input_graph.serialize(output_file);
        output_file.close();
    }

    // load input templates
    input_template.read_enlist(template_name);

    return 0;

}


