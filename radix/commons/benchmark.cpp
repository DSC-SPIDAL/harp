#include <algorithm>
#include <cinttypes>
#include <functional>
#include <random>
#include <utility>
#include <vector>

#include "builder.h"
#include "graph.h"
#include "timer.h"
#include "util.h"
#include "writer.h"

#include "benchmark.h"

bool VerifyUnimplemented(...) {
  std::cout << "** verify unimplemented **" << std::endl;
  return false;
}

