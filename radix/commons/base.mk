CXX = g++
CXX_FLAGS = -O3
PAR_FLAGS = -fopenmp
PC_FLAGS = -DUSE_PC -lpapi
PC_SUFFIX = _papi
GEN_LIB = ../generator.a
VER_LIB = ../verify.a

ifeq ($(PCM), 1)
	PC_SUFFIX = _pcm
	PC_FLAGS = -DUSE_PC -DUSE_PCM ../gail/pcm/pcm.a -lpthread
endif

# Platform customization
UNAME_OS := $(shell uname -s)
ifeq ($(UNAME_OS),Darwin)
	CXX = g++-7
	PC_FLAGS =
endif
ifeq ($(UNAME_OS),SunOS)
	CXX = sunCC
	CXX_FLAGS = -xO3 -DUSE_BIG_ENDIAN -m64 -xtarget=native
	PAR_FLAGS = -xopenmp
	PC_FLAGS = -DUSE_PC -lcpc
endif

# Building for RISCV
ifeq ($(RISCV), 1)
	CXX = riscv64-unknown-elf-g++
	PAR_FLAGS = -I ../fomp
	SUFFIX = .rv
	GEN_LIB = ../generator.rv
	VER_LIB = ../verify.rv
	PC_FLAGS = -DUSE_PC -DRISCV
	SERIAL = 1
endif

GEN_FLAGS = -DUSE_GEN $(GEN_LIB)
VER_FLAGS = -DUSE_VER $(VER_LIB) $(GEN_FLAGS)
RACE_FLAGS = -DRACE $(VER_FLAGS)
