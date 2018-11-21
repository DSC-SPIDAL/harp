#ifndef LOADER_H
#define LOADER_H

#include <algorithm>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <map>
#include <stdint.h>

#include "timer.h"


inline
uint32_t Swap32(uint32_t x) {
  #if defined __GNUC__
  return __builtin_bswap32(x);
  #else
  return ((x>>24) |
         ((x<<8) & 0x00ff0000) |
         ((x>>8) & 0x0000ff00) |
          (x<<24));
  #endif
}

inline
uint64_t Swap64(uint64_t x) {
  #if defined __GNUC__
  return __builtin_bswap64(x);
  #else
  return ((x>>56) |
         ((x<<40) & 0x00ff000000000000) |
         ((x<<24) & 0x0000ff0000000000) |
         ((x<<8)  & 0x000000ff00000000) |
         ((x>>8)  & 0x00000000ff000000) |
         ((x>>24) & 0x0000000000ff0000) |
         ((x>>40) & 0x000000000000ff00) |
          (x<<56));
  #endif
}


using namespace std;

template <typename T>
struct EdgePair {
  T u;
  T v;

  EdgePair() {}
  
  EdgePair(T x1, T x2) : u(x1), v(x2) {}
};

typedef EdgePair<uint32_t> FileEdge;

template <typename id_t, typename w_t>
struct WeightedEdgePair {
  id_t u;
  id_t v;
  w_t w;

  WeightedEdgePair() {}

  WeightedEdgePair(id_t x1, id_t x2, w_t z) : u(x1), v(x2), w(z) {}
};

void FlipEndianness(uint32_t *to_flip, uint64_t length) {
  for (long i=0; i<length; i++)
    to_flip[i] = Swap32(to_flip[i]);
}

void BlockedWrite(fstream &out, char* data, long num_bytes) {
  const long block_size = 1<<30;
  for (long i=0; i<num_bytes; i+=block_size) {
    int write_size = min(num_bytes-i, block_size);
    out.write(data + i, write_size);
  }
}

void BlockedRead(ifstream &in, char* data, long num_bytes) {
  const long block_size = 1<<30;
  for (long i=0; i<num_bytes; i+=block_size) {
    int read_size = min(num_bytes-i, block_size);
    in.read(data + i, read_size);
  }
}

FileEdge* ReadInEdgesASCII(char* filename, long &num_edges) {
  uint32_t a, b;
  ifstream edge_list_file(filename);
  if (!edge_list_file.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  edge_list_file >> num_edges;
  FileEdge* edge_list = new FileEdge[num_edges];
  for (uint64_t i=0; i<num_edges; i++) {
    edge_list_file >> a;
    edge_list_file >> b;
    edge_list[i] = FileEdge(a,b);
  }
  edge_list_file.close();
  return edge_list;
}

FileEdge* ReadInEdgesASCIISquish(char* filename, long &num_edges) {
  string orig_a, orig_b;
  uint32_t conv_a, conv_b, next_node=0;
  map<string, uint32_t> squisher;
  ifstream edge_list_file(filename);
  if (!edge_list_file.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  edge_list_file >> num_edges;
  FileEdge* edge_list = new FileEdge[num_edges];
  for (uint64_t i=0; i<num_edges; i++) {
    edge_list_file >> orig_a;
    edge_list_file >> orig_b;
    if (!squisher.count(orig_a))
      squisher[orig_a] = next_node++;
    conv_a = squisher[orig_a];
    if (!squisher.count(orig_b))
      squisher[orig_b] = next_node++;
    conv_b = squisher[orig_b];
    edge_list[i] = FileEdge(conv_a, conv_b);
  }
  edge_list_file.close();
  return edge_list;
}

FileEdge* ReadInMTX(char* filename, long &num_nodes, long &num_edges) {
  uint32_t a, b;
  ifstream mtx_file(filename);
  if (!mtx_file.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  mtx_file >> num_nodes >> num_nodes >> num_edges;
  FileEdge* edge_list = new FileEdge[num_edges];
  for (uint64_t i=0; i<num_edges; i++) {
    mtx_file >> a >> b;
    edge_list[i] = FileEdge(a,b);
  }
  mtx_file.close();
  return edge_list;
}

WeightedEdgePair<int, float>* ReadInWeightedMTX(char* filename,
    long &num_nodes, long &num_edges) {
  uint32_t a, b;
  float w;
  ifstream mtx_file(filename);
  if (!mtx_file.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  mtx_file >> num_nodes >> num_nodes >> num_edges;
  WeightedEdgePair<int, float>* edge_list =
    new WeightedEdgePair<int, float>[num_edges];
  for (long i=0; i<num_edges; i++) {
    mtx_file >> a >> b >> w;
    edge_list[i] = WeightedEdgePair<int, float>(a,b,w);
  }
  mtx_file.close();
  return edge_list;
}

WeightedEdgePair<int, int>* ReadInGR(char* filename,
    long &num_nodes, long &num_edges) {
  char c, sp[2];
  uint32_t a, b, w;
  ifstream gr_file(filename);
  if (!gr_file.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  while(true) {
    c = gr_file.peek();
    if (c == 'c')
      gr_file.ignore(200, '\n');
    else {
      gr_file >> c >> sp >> num_nodes >> num_edges;
      break;
    }
  }
  num_nodes++; // .gr format is starts from 1
  WeightedEdgePair<int, int>* edge_list =
    new WeightedEdgePair<int, int>[num_edges];
  for (uint64_t e=0; e<num_edges; e++) {
    c = gr_file.peek();
    if (c == 'a') {
      gr_file >> c >> a >> b >> w;
      edge_list[e] = WeightedEdgePair<int, int>(a,b,w);
    } else {
      gr_file.ignore(200, '\n');
      e--;
    }
  }
  gr_file.close();
  return edge_list;
}

bool WriteOutEdgesBinary(char* filename, FileEdge* edge_list, long num_edges) {
  fstream out(filename, ios::out | ios::binary);
  if (!out) {
    cout << "Couldn't write to file " << filename << endl;
    return false;
  }
  #ifdef USE_BIG_ENDIAN
  FlipEndianness((uint32_t*) edge_list, num_edges*2);
  uint64_t size_little = Swap64(num_edges);
  out.write((char*) &size_little, sizeof(uint64_t));
  #else
  out.write((char*) &num_edges, sizeof(uint64_t));
  #endif
  BlockedWrite(out, (char*) edge_list, num_edges*sizeof(FileEdge));
  out.close();
  return true;
}

FileEdge* ReadInEdgesBinary(char* filename, long &num_edges) {
  ifstream in(filename);
  if (!in.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return 0;
  }
  in.read((char*) &num_edges, sizeof(uint64_t));
  #ifdef USE_BIG_ENDIAN
  num_edges = Swap64(num_edges);
  #endif
  FileEdge* edge_list = new FileEdge[num_edges];
  in.read((char*) edge_list, num_edges * sizeof(FileEdge));
  #ifdef USE_BIG_ENDIAN
  FlipEndianness((uint32_t*) edge_list, num_edges*2);
  #endif
  return edge_list;
}

void RemoveVertex(uint32_t v, FileEdge* &edge_list, long &num_edges) {
  long total_occurrences = 0;
  for (long e=0; e<num_edges; e++)
    if ((edge_list[e].u == v) || (edge_list[e].v == v))
      total_occurrences++;
  long new_num_edges = num_edges - total_occurrences;
  FileEdge *new_edge_list = new FileEdge[new_num_edges];
  long new_in=0;
  for (long e=0; e<num_edges; e++)
    if ((edge_list[e].u != v) && (edge_list[e].v != v))
      new_edge_list[new_in++] = edge_list[e];
  swap(edge_list, new_edge_list);
  swap(num_edges, new_num_edges);
  delete[] new_edge_list;
}

bool WriteOutRawGraph(char* filename, long num_nodes, long num_edges,
                      long *g_index, int *g_neigh, bool directed) {
  #if USE64ID
  cout << "Raw graph format not supported for 64-bit IDs" << endl;
  return false;
  #endif
  fstream out(filename, ios::out | ios::binary);
  if (!out) {
    cout << "Couldn't write to file " << filename << endl;
    return false;
  }
  long num_index_bytes = sizeof(long) * (num_nodes+1);
  long num_neigh_bytes = sizeof(int) * g_index[num_nodes];
  #ifdef USE_BIG_ENDIAN
  Timer flip_timer;
  flip_timer.Start();
  FlipEndianness((uint32_t*) g_neigh, g_index[num_nodes]);
  FlipEndianness((uint32_t*) g_index, num_nodes+1);
  num_nodes = Swap64(num_nodes);
  num_edges = Swap64(num_edges);
  flip_timer.Stop();
  cout << "Flip Time: " << setw(10) << flip_timer.Seconds() << endl;
  #endif
  out.write((char*) &directed, sizeof(bool));
  out.write((char*) &num_edges, sizeof(uint64_t));
  out.write((char*) &num_nodes, sizeof(uint64_t));
  BlockedWrite(out, (char*) g_index, num_index_bytes);
  BlockedWrite(out, (char*) g_neigh, num_neigh_bytes);
  out.close();
  return true;
}

bool ReadInRawGraph(char* filename, long &num_nodes, long &num_edges,
                    long* &g_index, int* &g_neigh, bool &directed) {
  ifstream in(filename);
  if (!in.is_open()) {
    cout << "Couldn't open file " << filename << endl;
    return false;
  }
  in.read((char*) &directed, sizeof(bool));
  in.read((char*) &num_edges, sizeof(uint64_t));
  in.read((char*) &num_nodes, sizeof(uint64_t));
  #ifdef USE_BIG_ENDIAN
  num_edges = Swap64(num_edges);
  num_nodes = Swap64(num_nodes);
  #endif
  g_index = new long[num_nodes+1];
  BlockedRead(in, (char*) g_index, sizeof(uint64_t)*(num_nodes+1));
  #ifdef USE_BIG_ENDIAN
  FlipEndianness((uint32_t*) g_index, num_nodes+1);
  #endif
  g_neigh = new int[g_index[num_nodes]];
  BlockedRead(in, (char*) g_neigh, sizeof(uint32_t)*g_index[num_nodes]);
  #ifdef USE_BIG_ENDIAN
  FlipEndianness((uint32_t*) g_neigh, g_index[num_nodes]);
  #endif
  in.close();
  return true;
}

bool PrintCSR(long num_nodes, long num_edges, long *g_index, int *g_neigh) {
  cout << "AdjacencyGraph" << endl;
  cout << num_nodes << endl << g_index[num_nodes] << endl;
  for (long n=0; n<num_nodes; n++)
    cout << g_index[n] << endl;
  for (long m=0; m<g_index[num_nodes]; m++)
    cout << g_neigh[m] << endl;
  return true;
}

bool WriteLigraBinary(char* filename, long num_nodes, long num_edges,
                      long* g_index, int* g_neigh) {
  string config_name = string(filename) + ".config";
  string adj_name = string(filename) + ".adj";
  string idx_name = string(filename) + ".idx";
  ofstream config_file;
  config_file.open(config_name.c_str());
  config_file << num_nodes << "\n";
  config_file.close();
  ofstream adj_file;
  adj_file.open(adj_name.c_str(), ios::out | ios::binary);
  for (long m=0; m<g_index[num_nodes]; m++)
    adj_file.write((char*) &g_neigh[m], sizeof(unsigned int));
  adj_file.close();
  ofstream idx_file;
  idx_file.open(idx_name.c_str(), ios::out | ios::binary);
  for (long n=0; n<num_nodes; n++) {
    unsigned int uint_off = g_index[n];
    idx_file.write((char*) &g_index[n], sizeof(unsigned long));
  }
  idx_file.close();
  return true;
}

#endif // LOADER_H
