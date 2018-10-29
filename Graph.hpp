// This is the data structure to hold the 
// input graph and template data
#ifndef __GRAPH_H__
#define __GRAPH_H__

#include <fstream>
#include <string>

using namespace std;

class Graph {

    public:
        
        Graph()
        {
            vert_num = 0;
            edge_num = 0;
            max_deg = 0;
            adj_list = NULL;
            deg_list = NULL;
            src_edge = NULL;
            dst_edge = NULL;
        }

        ~Graph() { release(); }

        // read txt data in enlist format
        void read_enlist(string file_name);
        void build_graph(int vert, int edge, int* srclist, int* dstlist);

        // serialization 
        void serialize(ofstream& output);
        void deserialize(ifstream& input);

        // copy operator
        Graph& operator= (const Graph& g);

        // accessors
        int get_vert_num() const {return vert_num;}
        int get_edge_num() const {return edge_num;}

        int get_max_deg() const {return max_deg;}
        int get_out_deg(int vert) {return deg_list[vert+1] - deg_list[vert];}
        unsigned* get_deg_list() const {return deg_list;}

        int* get_adj_list() const {return adj_list;}
        int* get_adj_list(int vert) {return &adj_list[deg_list[vert]];}

        // release the memory of adj_list and deg_list
        void release();

    private:

        int vert_num;
        unsigned edge_file;
        unsigned edge_num;
        unsigned  max_deg;
        int* adj_list;
        unsigned* deg_list;
        int* src_edge;
        int* dst_edge;

};

#endif
