#include "Graph.hpp"
#include <stdlib.h>
#include <stdio.h>
#include <cstring>


using namespace std;

void Graph::read_enlist(string file_name)
{/*{{{*/

    string line;
    ifstream file_strm;
    file_strm.open(file_name.c_str());

    // get the vert num
    std::getline(file_strm, line);
    int verts = atoi(line.c_str());
    // get the edge num
    std::getline(file_strm, line);
    edge_file = atoi(line.c_str());

    src_edge = new int[edge_file];
    dst_edge = new int[edge_file];

    int max_id = 0;
    for(unsigned j=0;j<edge_file;j++)
    {
        std::getline(file_strm, line, ' ');
        src_edge[j] = atoi(line.c_str());
        max_id = (src_edge[j] > max_id) ? src_edge[j]: max_id;

        std::getline(file_strm, line);
        dst_edge[j] = atoi(line.c_str());
        max_id = (dst_edge[j] > max_id) ? dst_edge[j]: max_id;
    }

    file_strm.close();

    if (max_id != verts - 1)
    {
#ifdef VERBOSE
        // remove "holes"
        printf("Start remove holes; max_id: %d, n_g: %d\n", max_id, verts); 
        std::fflush(stdout);
#endif
   
        int* v_id = new int[max_id+1];
        std::memset(v_id, 0, (max_id+1)*sizeof(int));

        for(int i=0;i<edge_file;i++)
        {
            v_id[src_edge[i]] = 1;
            v_id[dst_edge[i]] = 1;
        }

        int itr = 0;
        for(int i=0;i<max_id+1;i++)
        {
            if (v_id[i] == 1)
                v_id[i] = (itr++);
        }

        for(int i=0;i<edge_file;i++)
        {
            src_edge[i] = v_id[src_edge[i]];
            dst_edge[i] = v_id[dst_edge[i]];
        }
#ifdef VERBOSE
        printf("Finish remove holes\n");
        std::fflush(stdout);
#endif
        delete[] v_id;
    }
    
    // build the internal graph datastructure
    // change it to undirected graph
    build_graph(verts, edge_file, src_edge, dst_edge);

}/*}}}*/

void Graph::build_graph(int verts, int edges, int* src_edge, int* dst_edge)
{
#ifdef VERBOSE
    printf("Start building graph\n");
    std::fflush(stdout);
#endif

    vert_num = verts;
    edge_file = edges;
    edge_num = 2*edges;
    max_deg = 0;

    adj_list = new int[edge_num];
    deg_list = new unsigned[vert_num+1];
    deg_list[0] = 0;

    unsigned* tmp_list = new unsigned[vert_num];
    std::memset(tmp_list, 0, vert_num*sizeof(unsigned));

    for(unsigned j=0;j<edge_file;j++)
    {
        tmp_list[src_edge[j]]++;
        tmp_list[dst_edge[j]]++;
    }

    for(int j=0;j<vert_num;j++)
        max_deg = tmp_list[j] > max_deg ? tmp_list[j] : max_deg;

    for(int j=0;j<vert_num;j++)
        deg_list[j+1] = deg_list[j] + tmp_list[j];

    std::memcpy(tmp_list, deg_list, vert_num*sizeof(unsigned));

    for(unsigned j=0;j< edge_file; j++)
    {
        adj_list[tmp_list[src_edge[j]]++] = dst_edge[j];
        adj_list[tmp_list[dst_edge[j]]++] = src_edge[j];
    }
#ifdef VERBOSE
    printf("Total vertices is : %d\n", vert_num);
    printf("Total Edges is : %d\n", edge_num);
    printf("Max Deg is : %d\n", max_deg);
    printf("Avg Deg is : %d\n", (deg_list[vert_num]/vert_num));   
    std::fflush(stdout);
#endif

    delete[] tmp_list;
}

Graph& Graph::operator= (const Graph& obj)
{
    vert_num = obj.get_vert_num();
    edge_num = obj.get_edge_num();
    max_deg = obj.get_max_deg();

    adj_list = new int[edge_num];
    deg_list = new unsigned[vert_num+1];

    std::memcpy(adj_list, obj.get_adj_list(), edge_num*sizeof(int));
    std::memcpy(deg_list, obj.get_deg_list(), (vert_num+1)*sizeof(unsigned));
}

void Graph::serialize(ofstream& output)
{
    output.write((char*)&vert_num, sizeof(int));
    output.write((char*)&edge_num, sizeof(unsigned));
    output.write((char*)&max_deg, sizeof(int));
    output.write((char*)adj_list, edge_num*sizeof(int));
    output.write((char*)deg_list, (vert_num+1)*sizeof(unsigned));
}

void Graph::deserialize(ifstream& input)
{
    input.read((char*)&vert_num, sizeof(int));
    input.read((char*)&edge_num, sizeof(unsigned));
    input.read((char*)&max_deg, sizeof(int));
    adj_list = new int[edge_num];
    input.read((char*)adj_list, edge_num*sizeof(int));
    deg_list = new unsigned[vert_num+1];
    input.read((char*)deg_list, (vert_num+1)*sizeof(unsigned));
#ifdef VERBOSE
    printf("Total vertices is : %d\n", vert_num);
    printf("Total Edges is : %d\n", edge_num);
    printf("Max Deg is : %d\n", max_deg);
    printf("Avg Deg is : %d\n", (deg_list[vert_num]/vert_num)); 
    std::fflush(stdout); 
#endif
   
}

void Graph::release()
{
    if (adj_list != NULL)
        delete[] adj_list;

    if (deg_list != NULL)
        delete[] deg_list;

    if (src_edge != NULL)
        delete[] src_edge;

    if (dst_edge != NULL)
        delete[] dst_edge;

    src_edge = NULL;
    dst_edge = NULL;
    adj_list = NULL;
    deg_list = NULL;
}
