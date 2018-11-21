#ifndef PERF_COUNT_SPARC_H
#define PERF_COUNT_SPARC_H

#include <iomanip>
#include <iostream>
#include <omp.h>
#include <libcpc.h>


using namespace std;


int MB(int x) {
  return x / (1<<20);
}


class PerfCount {
 public:
  long long insts;
  long long loads;
  // long long l2l3_hits;
  long long local_hits;
  long long remote_hits;
  double total_secs;

  PerfCount() {
    num_threads = omp_get_max_threads();

    uint_t flags = CPC_COUNT_USER|CPC_COUNT_SYSTEM;
    cpc_rcw = cpc_open(CPC_VER_CURRENT);
    cpc_my_set = cpc_set_create(cpc_rcw);
    cpc_set_add_request(cpc_rcw, cpc_my_set, "PAPI_tot_ins", 0, flags, 0, NULL);
    cpc_set_add_request(cpc_rcw, cpc_my_set, "PAPI_ld_ins", 0, flags, 0, NULL);
    // cpc_set_add_request(cpc_rcw, cpc_my_set, "DC_miss_L2_L3_hit", 0, flags, 0, NULL);
    // cpc_set_add_request(cpc_rcw, cpc_my_set, "Branches", 0, flags, 0, NULL);
    // cpc_set_add_request(cpc_rcw, cpc_my_set, "Br_mispred", 0, flags, 0, NULL);
    cpc_set_add_request(cpc_rcw, cpc_my_set, "DC_miss_local_hit", 0, flags, 0, NULL);
    cpc_set_add_request(cpc_rcw, cpc_my_set, "DC_miss_remote_L3_hit", 0, flags, 0, NULL);

    cpc_before = new cpc_buf_t*[num_threads];
    cpc_after = new cpc_buf_t*[num_threads];
    cpc_diff = new cpc_buf_t*[num_threads];
    for (int t=0; t<num_threads; t++) {
      cpc_before[t] = cpc_buf_create(cpc_rcw, cpc_my_set);
      cpc_after[t] = cpc_buf_create(cpc_rcw, cpc_my_set);
      cpc_diff[t] = cpc_buf_create(cpc_rcw, cpc_my_set);
    }
  }

  inline
  void Start(bool reset=true) {
    if (reset)
      Reset();
    #pragma omp parallel
    {
      cpc_bind_curlwp(cpc_rcw, cpc_my_set, CPC_BIND_LWP_INHERIT);
      cpc_set_sample(cpc_rcw, cpc_my_set, cpc_before[omp_get_thread_num()]);
    }
  }

  inline
  void StartParallel(bool reset=true) {
    Start(reset);
  }

  inline
  void Stop() {
    #pragma omp parallel
    {
      unsigned long value0=0, value1=0, value2=0, value3=0;
      int tid = omp_get_thread_num();
      cpc_set_sample(cpc_rcw, cpc_my_set, cpc_after[tid]);
      cpc_buf_sub(cpc_rcw, cpc_diff[tid], cpc_after[tid], cpc_before[tid]);
      cpc_buf_get(cpc_rcw, cpc_diff[tid], 0, &value0);
      cpc_buf_get(cpc_rcw, cpc_diff[tid], 1, &value1);
      cpc_buf_get(cpc_rcw, cpc_diff[tid], 2, &value2);
      cpc_buf_get(cpc_rcw, cpc_diff[tid], 3, &value3);

      if (omp_get_thread_num() == 0) {
        hrtime_t start_hrt = cpc_buf_hrtime(cpc_rcw, cpc_before[tid]);
        hrtime_t end_hrt = cpc_buf_hrtime(cpc_rcw, cpc_after[tid]);
        total_secs += (end_hrt - start_hrt) * 1e-9;
      }
      #pragma omp critical
      {
        insts += value0;
        loads += value1;
        // l2l3_hits += value2;
        local_hits += value2;
        remote_hits += value3;
      }
    }
  }

  inline
  void StopParallel() {
    Stop();
  }

  void PrintAll() {
    double cache_hit = (double) (loads - local_hits) / loads;
    double memreq_p_s = (double) (local_hits) / total_secs;
    double act_trans = (double) (local_hits)*64;
    double act_bw = act_trans/total_secs;
    double ips = (double) insts / total_secs;

    cout << setprecision(2) << fixed;
    cout << "Total Time:" << setw(15) << total_secs << " s" << endl;
    cout << "Insts:     " << setw(15) << insts << endl;
    cout << "Loads:     " << setw(15) << loads << endl;
    // cout << "L2/L3 Hits:" << setw(15) << l2l3_hits << endl;
    cout << "Local Hits:" << setw(15) << local_hits << endl;
    cout << "Remote Hits:" << setw(14) << remote_hits << endl;

    cout << "IPS:       " << setw(15) << ips*1e-9 << " BIPS" << endl;
    cout << "Act. Trans:" << setw(15) << MB(act_trans) << " MB" <<endl;
    cout << "Cache Hit: " << setw(15) << cache_hit * 100 << " %" << endl;
    cout << "MemReqs/s: " << setw(15) << memreq_p_s / 1e6 << " M" << endl;
    cout << "s/MemReq:  " << setw(15) << 1/memreq_p_s*1e9 << " ns" << endl;
    cout << "Bandwidth: " << setw(15) << MB(act_bw) << " MB/s" << endl;
  }

  void Reset() {
    insts = 0;
    loads = 0;
    // l2l3_hits = 0;
    local_hits = 0;
    remote_hits = 0;
    total_secs = 0;
  }

 protected:
  int num_threads;

  cpc_t *cpc_rcw;
  cpc_set_t *cpc_my_set;

  cpc_buf_t **cpc_before;
  cpc_buf_t **cpc_after;
  cpc_buf_t **cpc_diff;
};

#endif //PERF_COUNT_SPARC_H
