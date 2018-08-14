//forest.h

#ifndef AM_RT_RANK_FOREST
#define AM_RT_RANK_FOREST

#include "regression_tree.h"
#include "args.h"
#include "epoch.h"
#include <cmath>
#include <sys/time.h>
#include <unistd.h>
using namespace std;

#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>
using namespace boost;


void random_forest(const data_t& train, const vec_data_t& test, vec_preds_t& test_preds, args_t& args);
void randsample(const data_t& data, data_t& s);
void single_forest(const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds, args_t& args);
void random_forest_p(const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds, args_t& args);
void multiple_forest(int trees, const data_t& train, const vec_data_t& test, preds_t& train_preds, vec_preds_t& test_preds_seg, args_t& args);


#endif //AM_RT_RANK_FOREST
