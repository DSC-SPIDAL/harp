CC = g++ -Wall
CCOPTS = -O3 -Wno-sign-compare 
VL_CFLAGS = -I./3rd-party/vlfeat/
VL_LFLAGS = -lvl -L./3rd-party/vlfeat/bin/g64 #depending on how vlfeat compiles on your system, you may need to change this path

all : feat_b2a vldsift padkey vq

vldsift : vldsift.o
	$(CC) $(CCOPTS) -o $@  $< $(VL_LFLAGS) 

feat_b2a : feat_b2a.o
	$(CC) $(CCOPTS) -o $@  $< 

vq : vq.o
	$(CC) $(CCOPTS) -o $@  $< 

padkey : padkey.o
	$(CC) $(CCOPTS) -o $@  $< 

%.o:%.cpp
	$(CC) $(CCOPTS) $(VL_CFLAGS) -c $< -o $@ 

clean: 
	rm *.o; rm feat_b2a vldsift padkey vq


