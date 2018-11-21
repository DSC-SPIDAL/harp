#ifndef PERF_COUNT_RISCV_H
#define PERF_COUNT_RISCV_H

#include <iomanip>
#include <iostream>


int MB(int x) {
  return x / (1<<20);
}


using namespace std;


class PerfCount {
 public:
  // unsigned long start_rtime;
  unsigned long start_cycles;
  unsigned long start_insts;
  // unsigned long rtime;
  unsigned long cycles;
  unsigned long insts;

  PerfCount() {
    Reset();
  }

  inline
  void Start(bool reset=true) {
    if (reset)
      Reset();
    // start_rtime = GetRTime();
    start_cycles = GetCycles();
    start_insts = GetInsts();
  }

  inline
  void StartParallel(bool reset=true) {
    Start(reset);
  }

  inline
  void Stop() {
    // unsigned long end_rtime = GetRTime();
    unsigned long end_cycles = GetCycles();
    unsigned long end_insts = GetInsts();
    // rtime = end_rtime - start_rtime;
    cycles += end_cycles - start_cycles;
    insts += end_insts - start_insts;
  }

  inline
  void StopParallel() {
    Stop();
  }

  void PrintAll() {
    cout << setprecision(2) << fixed;
    // cout << "Total Time:" << setw(15) << rtime << endl;
    cout << "Insts:     " << setw(15) << insts << endl;
    cout << "Cycles:    " << setw(15) << cycles << endl;
    cout << "IPC:       " << setw(15) << (float) insts / cycles << endl;
  }

  void Reset() {
    // start_rtime = 0;
    start_cycles = 0;
    start_insts = 0;
    // rtime = 0;
    cycles = 0;
    insts = 0;
  }

  unsigned long GetRTime() {
    unsigned long time;
    asm volatile("rdtime %0" : "=r"(cycles));
    return cycles;
  }

  unsigned long GetCycles() {
    unsigned long cycles;
    asm volatile("rdcycle %0" : "=r"(cycles));
    return cycles;
  }

  unsigned long GetInsts() {
    unsigned long insts;
    asm volatile("rdinstret %0" : "=r"(insts));
    return insts;
  }
};

#endif //PERF_COUNT_RISCV_H
