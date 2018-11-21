include ../base.mk

# CXX_FLAGS += -std=c++17 $(PAR_FLAGS) -Wall -march=native
CXX_FLAGS += -std=c++11 $(PAR_FLAGS) -Wall -march=native

ifneq (,$(findstring icpc,$(CXX)))
	PAR_FLAG = -openmp
endif

ifeq ($(PC), 1)
	SUFFIX = $(PC_SUFFIX)
	LDLIBS += $(PC_FLAGS)
endif

ifeq ($(CHECKS), 1)
	CXX_FLAGS += -DCOUNT_CHECKS
endif

KERNELS = pr

.PHONY: all
all: $(SUITE)

% : %.cc ../*.h *.h
	$(CXX) $(CXX_FLAGS) $< -o $@$(SUFFIX) $(LDLIBS)

.PHONY: clean
clean:
	rm -f $(SUITE) *.rv *.s *_papi *_pcm test/out/*
