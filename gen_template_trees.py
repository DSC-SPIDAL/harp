#!/usr/bin/env python
from sage.all import *
from sage.graphs.trees import TreeIterator

def gen_trees(n):
    trees = []
    for t in TreeIterator(n):
        if not t.is_tree():
            return False
        if t.num_verts() != n:
            return False
        if t.num_edges() != n - 1:
            return False
        for tree in trees:
            if tree.is_isomorphic(t):
                return False
        trees.append(t)
    return trees

def gen_templates(n):
    trees = gen_trees(n)
    cnt = 0
    for tree in trees:
        cnt += 1
        name = "Tree_%s_%s" % (n, cnt)

        # write teamplates as fasica files used by subgraph2vec
        with open("%s.fascia" % name, "w") as f:
            # write # of vertices
            f.write("%s\n" % n)
            # write # of edges
            f.write("%s\n" % (n-1))
            for edge in tree.edges(labels=0):
                f.write("%s %s\n" % (edge[0], edge[1]))

        # each tree is saved to a PNG image
        tree.plot(vertex_size=400,
                  edge_thickness=5,
                  vertex_color='black',
                  tree_orientation='down',
                  layout='circular',
                  vertex_labels=False).save("%s.png" % name)

def main():
    argc = len(sys.argv) - 1
    if argc != 1:
        print ("This program generates all trees with nVerts nodes")
        print ("USAGE: PROG nVerts")
        exit (1)
    else:
        nVerts = int(sys.argv[1])
        gen_templates(nVerts)

if __name__ == "__main__":
    main()

