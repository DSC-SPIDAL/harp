#ifndef PERF_COUNT_PAPI_H
#define PERF_COUNT_PAPI_H

#include <iomanip>
#include <iostream>
#include <papi.h>
#include <stdlib.h>
#include <string>
#include <vector>



struct PerfCountEvent {
  PerfCountEvent(std::string given_label, int given_code) :
    label(given_label), event_code(given_code) {}

  std::string label;
  int event_code;
};


class PerfCount {
 public:
  PerfCount() {
    if (PAPI_library_init(PAPI_VER_CURRENT) != PAPI_VER_CURRENT) {
      std::cout << "PAPI couldn't init library properly" << std::endl;
      exit(1);
    }
    if (PAPI_thread_init(pthread_self) != PAPI_OK) {
      std::cout << "PAPI couldn't init thread properly" << std::endl;
      exit(1);
    }
    AddCounters();
    Reset();
  }

  void Start(bool reset=true) {
    if (reset)
      Reset();
    start_usecs = PAPI_get_real_usec();
    const int num_events = events.size();
    int event_codes[events.size()];
    for (int e=0; e<num_events; e++)
      event_codes[e] = events[e].event_code;
    #pragma omp parallel
    HandleError(PAPI_start_counters(event_codes, num_events), "Start");
  }

  // for compatability with old API
  void StartParallel(bool reset=true) {
    Start(reset);
  }

  void Stop() {
    total_secs += (PAPI_get_real_usec() - start_usecs) * 1e-6;
    #pragma omp parallel
    {
      std::vector<long long> lcounts(events.size());
      HandleError(PAPI_stop_counters(&lcounts[0], events.size()), "Stop");
      for (size_t e=0; e<events.size(); e++)
        #pragma omp atomic
        counts[e] += lcounts[e];
    }
  }

  // for compatability with old API
  void StopParallel() {
    Stop();
  }

  void PrintAll() {
    int num_events = events.size();
    std::cout << std::setprecision(2) << std::fixed;
    std::cout << "Total Seconds:" << std::setw(29) << total_secs << std::endl;
    for (int e=0; e<num_events; e++) {
      std::cout << std::left << std::setw(24) << events[e].label + ":";
      std::cout << std::right << std::setw(16) << counts[e] << std::endl;
    }
  }

  void Reset() {
    total_secs = 0;
    counts.assign(events.size(), 0);
  }

  void AddCounters() {
    PerfCountEvent PCE_Cycles("Cycles", PAPI_TOT_CYC);
    PerfCountEvent PCE_Insts("Instructions", PAPI_TOT_INS);
    PerfCountEvent PCE_Loads("Loads", PAPI_LD_INS);
    PerfCountEvent PCE_L1D("L1D$ Misses", PAPI_L1_DCM);
    PerfCountEvent PCE_L3("L3 Misses", PAPI_L3_TCM);
    PerfCountEvent PCE_L3A("L3 Accesses", PAPI_L3_TCA);
    // PerfCountEvent PCE_Pref("Prefetch", ConvertCode("HW_PRE_REQ"));

    PerfCountEvent PCE_DTLB("DTLB Misses", PAPI_TLB_DM);
    PerfCountEvent PCE_DTLB_MISS("DTLB Load Miss",
      ConvertCode("DTLB_LOAD_MISSES:MISS_CAUSES_A_WALK"));
    PerfCountEvent PCE_DTLB_WALK("Walk Cycles",
      ConvertCode("DTLB_LOAD_MISSES:WALK_DURATION"));

    PerfCountEvent PCE_BRANCH("Branches", PAPI_BR_INS);
    PerfCountEvent PCE_MISP("Mispredicts", PAPI_BR_MSP);

    // PerfCountEvent PCE_L1DMRep("L1D M Replaced", ConvertCode("L1D:ALL_M_REPLACEMENT"));
    // PerfCountEvent PCE_L1DMEv("L1D M Evicts", ConvertCode("L1D:M_EVICT"));
    // PerfCountEvent PCE_L2STL("L2 Store Locks Misses", ConvertCode("L2_STORE_LOCK_RQSTS:MISS"));


    events.push_back(PCE_Cycles);
    events.push_back(PCE_Insts);
    // events.push_back(PCE_Loads);
    events.push_back(PCE_L3);
    events.push_back(PCE_L3A);
    // events.push_back(PCE_Pref);
    // events.push_back(PCE_DTLB);
    // events.push_back(PCE_DTLB_MISS);
    // events.push_back(PCE_DTLB_WALK);
    events.push_back(PCE_BRANCH);
    events.push_back(PCE_MISP);
    // events.push_back(PCE_L1DMRep);
    // events.push_back(PCE_L1DMEv);
    // events.push_back(PCE_L2STL);
  }

  static int ConvertCode(const char* native_event_name) {
    int event_code;
    int ret = PAPI_event_name_to_code((char*) native_event_name, &event_code);
    if (ret != PAPI_OK)
      std::cout << event_code << " got code "  << ret << std::endl;
    return event_code;
  }

 protected:
  std::vector<PerfCountEvent> events;
  std::vector<long long> counts;
  long long start_usecs;
  double total_secs;

 private:
  void HandleError(int ret, const std::string location) {
    if (ret == PAPI_OK)
      return;
    std::cout << "PAPI Error: " << PAPI_strerror(ret);
    std::cout << " (" << ret << ")" << std::endl;
    std::cout << "  occurred in " << location << std::endl;
    exit(1);
  }
};

#endif // PERF_COUNT_PAPI_H
