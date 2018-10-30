/*
Copyright (c) 2011, Washington University in St. Louis
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY 
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#ifndef SPLITSBUFFER_H
#define SPLITSBUFFER_H

#include "FeatureData.h"
#include "StaticTree.h"
#include <mpi.h>

class SplitsBuffer {
    public:
        SplitsBuffer(int n);
        ~SplitsBuffer();

        void clear();
        void updateSingleCore(FeatureData* data, StaticTree* tree);
        void updateFromData(FeatureData* data, StaticTree* tree);
        void updateFromBuffer(unsigned int* buffer);
        void exchange(); // global reduce, for splits distributed across many processors
        void broadcast(int root); // broadcast, for splits contained entirely on a single processor (i.e. tree root)
        void broadcastReduce(int root, int myid); // broadcast-based reduction, for splits distributed across few processors (but more than one)
        void applyToData(FeatureData* data);
        
        // TODO : necessary?
        int getBufferSize();
        unsigned int* getBuffer();

    private:
        int N, buffersize;
        unsigned int* buffer;
};

SplitsBuffer::SplitsBuffer(int n) {
	// record number of data instances
	N = n;

	// instantiate buffer (one bit per instance)
	buffersize = (int) ceil(N/32.);
	buffer = new unsigned int[buffersize];
	clear();
}

SplitsBuffer::~SplitsBuffer() {
	delete [] buffer;
}

void SplitsBuffer::clear() {
    // localize references to promote vectorization
    unsigned int* b = buffer;
    int n = buffersize;
    
    // clear buffer
	for (int i=0; i<n; i++)
		b[i] = 0U;
}

void SplitsBuffer::updateSingleCore(FeatureData* data, StaticTree* tree) {
	// iterate over data instances
	for (int i=0; i<N; i++) {
		// get previous node
		int node = data->getNode(i);
		
		// determine new node
        int feature; float split;
        tree->getSplit(node, feature, split);
		data->setNode(i,data->getNode(i) << 1); // node[i] *= 2
        if (feature >= 0 and data->getFeature(feature, i) >= split) // TODO : eliminate branch
            data->setNode(i,data->getNode(i) | 1U); // node[i] += 1
	}
}

void SplitsBuffer::updateFromData(FeatureData* data, StaticTree* tree) {
	// iterate over buffer cells
    int i=0; // data instance index
    for (int j=0; j<buffersize; j++) {
        // initialize mask
        unsigned int mask = 1U;
        buffer[j] = 0U;
        // iterate over positions in buffer cell
        for (int k=0; k<32 and i<N; k++, i++, mask<<=1) {
            // get node in previous level
            int node = data->getNode(i);
            
            // get split at next level
            int feature; float split;
            tree->getSplit(node, feature, split);
            
            // determine new node
            bool newnode = (data->isLocalFeature(feature) and data->getFeature(data->localFeatureIndex(feature), i) >= split);
                // TODO : to eliminate the branch in the first part of this statement, make a "missing feature" with all zeros 
                // and set the node feature:split to missing:0.f
                // TODO : consider eliminating subtraction from the second statement
            // buffer[j] <<= 1;
            //             buffer[j] += newnode;
            // update buffer cell
            buffer[j] = (buffer[j] & ~mask) | (-newnode & mask);
        }
    }
}

void SplitsBuffer::updateFromBuffer(unsigned int* nbuffer) {
	// localize references to promote vectorization
    unsigned int* b = buffer;
    int n = buffersize;
    
    // merge two buffers
	for (int i=0; i<n; i++)
		b[i] |= nbuffer[i];
}

void SplitsBuffer::applyToData(FeatureData* data) {
    for (int i=0, j=0; j<buffersize; j++) {
        for (int k=0; k<32 and i<N; k++, i++) {
            int node = data->getNode(i);
            node = (node << 1) | (buffer[j] >> k & 1U); // * 2 + 0/1
            data->setNode(i,node);
        }
    }
}

int SplitsBuffer::getBufferSize() {
	return buffersize;
}

unsigned int* SplitsBuffer::getBuffer() {
	return buffer;
}

void SplitsBuffer::exchange() {
    // global reduce on buffer
    unsigned int* newBuffer = new unsigned int[buffersize];
    MPI_Allreduce(buffer, newBuffer, buffersize, MPI_UNSIGNED, MPI_BXOR, MPI_COMM_WORLD);
    
    delete [] buffer;
    buffer = newBuffer;
}

void SplitsBuffer::broadcast(int root) {
    MPI_Bcast(buffer, buffersize, MPI_UNSIGNED, root, MPI_COMM_WORLD);
}

void SplitsBuffer::broadcastReduce(int root, int myid) {
    // init newBuffer
    unsigned int* newBuffer = NULL;
    if (root == myid)
        newBuffer = buffer;
    else newBuffer = new unsigned int[buffersize];
    
    // broadcast/receive buffer
    MPI_Bcast(newBuffer, buffersize, MPI_UNSIGNED, root, MPI_COMM_WORLD);
    
    // update local buffer
    if (root != myid) {
        updateFromBuffer(newBuffer);
        delete [] newBuffer;
    }
}

#endif
