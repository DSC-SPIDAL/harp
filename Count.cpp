// the implementation of the color-coding algorithms
// Author: Langshi Chen

#include <cassert>
#include <math.h>
#include<cstdlib>
#include<ctime>
#include <cstring>
#include <omp.h>

#include "Count.hpp"

#include "Helper.hpp"

using namespace std;

void Count::initialization(Graph& graph, int thd_num, int itr_num)
{
    _graph = &graph;
    _vert_num = _graph->get_vert_num();
    _max_deg = _graph->get_max_deg();
    _thd_num = thd_num;
    _itr_num = itr_num;
    _colors_local = (int*)malloc(_vert_num*sizeof(int));
    std::memset(_colors_local, 0, _vert_num*sizeof(int));
}

double Count::compute(Graph& templates)
{/*{{{*/

    _templates = &templates; 
    _color_num = _templates->get_vert_num();

    printf("Start subtemplates Dividing\n");
    std::fflush(stdout);

    div_tp.DivideTp(*(_templates));
    div_tp.sort_tps();

    _subtmp_array = div_tp.get_subtps();
    _total_sub_num = div_tp.get_subtps_num();

    //check the sub vert num
    // debug
    for(int s=0;s<_total_sub_num;s++)
    {
        int vert_self = _subtmp_array[s].get_vert_num();
        if (vert_self > 1)
        {
            int main_leaf = div_tp.get_main_node_vert_num(s);
            int aux_leaf = div_tp.get_aux_node_vert_num(s);
            printf("Vert: %d, main: %d, aux: %d\n", vert_self, main_leaf, aux_leaf);
            // printf("Vert: %d, main: %d, aux: %d\n", s, main_idx, aux_idx);
            std::fflush(stdout);
            assert((main_leaf + aux_leaf == vert_self));

        }
    }
    
    printf("Finish subtemplates Dividing\n");
    std::fflush(stdout);

    // create the index tables
    indexer.initialization(_color_num, _total_sub_num, &_subtmp_array, &div_tp);

    printf("Finish creating indexer\n");
    std::fflush(stdout);

    _dTable.initDataTable(_subtmp_array, &indexer, _total_sub_num, _color_num, _vert_num);

    printf("Finish initializaing datatable\n");
    std::fflush(stdout);

    // start counting
    double timeStart = utility::timer();

    double iterCount = 0.0;
    for (int i = 0; i < _itr_num; ++i) {
        iterCount += colorCounting();
    }
   
    printf("\nTime for count per iter: %9.6lf seconds\n", (utility::timer() - timeStart)/_itr_num);
    std::fflush(stdout);

    // finish counting
    double finalCount = iterCount/(double)_itr_num;
    double probColorful = factorial(_color_num) / 
                ( factorial(_color_num - _templates->get_vert_num())*pow(_color_num, _templates->get_vert_num()));

    printf("Final raw count is %f\n", finalCount);
    std::fflush(stdout);

    printf("Prob is %f\n", probColorful);
    std::fflush(stdout);

    int automoNum = 1;
    finalCount = floor(finalCount/(probColorful*(double)automoNum) + 0.5);

    printf("Final count is %e\n", finalCount);
    std::fflush(stdout);

}/*}}}*/

double Count::colorCounting()
{/*{{{*/

    colorInit();

    double countTotal = 0.0;

    for (int s = _total_sub_num - 1; s >= 0; --s) {

        int subSize = _subtmp_array[s].get_vert_num();
        int mainIdx = div_tp.get_main_node_idx(s);
        int auxIdx = div_tp.get_aux_node_idx(s);

        _dTable.initSubTempTable(s, mainIdx, auxIdx);
        // debug
        // printf("Finish init sub templte %d, vert: %d\n", s, subSize);
        // std::fflush(stdout);
        int* idxCombToCount = (indexer.getSubCToCount())[s]; 

        if (subSize == 1) {
            // init the bottom of counts
#pragma omp parallel for num_threads(_thd_num)
            for (int v = 0; v < _vert_num; ++v) {
                _dTable.setCurTableCell(v, idxCombToCount[_colors_local[v]], 1.0); 
            }
        }
        else
        {
            //non-bottom case
            countTotal = countNonBottome(s);
        }

        if (mainIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(mainIdx);
        if (auxIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(auxIdx);
    }

    return countTotal;
}/*}}}*/

double Count::countNonBottome(int subsId)
{/*{{{*/

    int subSize = _subtmp_array[subsId].get_vert_num();

    int idxMain = div_tp.get_main_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];
    int vecNum = countCombNum*splitCombNum;

    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d\n", subsId, subSize, 
            countCombNum, splitCombNum);
    std::fflush(stdout);

    double countSum = 0.0;
    #pragma omp parallel num_threads(_thd_num)
    {
        // local vars for each omp thread
        int* nbrListValid = (int*)malloc(_max_deg*sizeof(int));
        int countNbrValid = 0;
        Graph* gLocal = _graph; 
        DataTabble* dTableLocal = &_dTable;
        int* mainSplitVecLocal = (indexer.getSplitToCountVecTable())[0][subsId]; 
        int* auxSplitVecLocal = (indexer.getSplitToCountVecTable())[1][subsId]; 
        int* combToCountLocal = (indexer.getCombToCountTable())[subsId];

#ifdef __INTEL_COMPILER
        __assume_aligned(mainSplitVecLocal, 64);
        __assume_aligned(auxSplitVecLocal, 64);
#else
        __builtin_assume_aligned(mainSplitVecLocal, 64);
        __builtin_assume_aligned(auxSplitVecLocal, 64);
#endif
        #pragma omp for schedule(static) reduction(+:countSum)
        for (int v = 0; v < _vert_num; ++v) {

           countNbrValid = 0; 
           if (dTableLocal->isVertInitMain(v))
           {
               // select the valid nbrs
               int* adjList = gLocal->get_adj_list(v);
               int adjEnd = gLocal->get_out_deg(v);
               float* countMainVal = dTableLocal->getMainArray(v);

               for (int j = 0; j < adjEnd; ++j) {

                   int adjVal = adjList[j];
                   if (dTableLocal->isVertInitAux(adjVal)) {
                       nbrListValid[countNbrValid++] = adjVal;
                   }
               }

               if (countNbrValid)
               {
#ifdef __INTEL_COMPILER
                   float* countBuf = (float*) _mm_malloc(vecNum*sizeof(float), 64);
#else
                   float* countBuf = (float*) aligned_alloc(64, vecNum*sizeof(float));
#endif
                   std::memset(countBuf, 0, vecNum*sizeof(float));

#ifdef __INTEL_COMPILER
                   __assume_aligned(countBuf, 64);
                   __assume_aligned(countMainVal, 64);
#else
                   __builtin_assume_aligned(countBuf, 64);
                   __builtin_assume_aligned(countMainVal, 64);
#endif
                   // multiplicaiton
                   for (int i = 0; i < countNbrValid; ++i) {
                       
                       float*  countAuxVal= dTableLocal->getAuxArray(nbrListValid[i]);

#ifdef __INTEL_COMPILER
                       __assume_aligned(countAuxVal, 64);
#else
                       __builtin_assume_aligned(countAuxVal, 64);
#endif

#ifdef __INTEL_COMPILER
                       #pragma vector always
#else
                       #pragma GCC ivdep
#endif
                       for (int j = 0; j < vecNum; ++j) {
                           countBuf[j] += (countMainVal[mainSplitVecLocal[j]]*
                                   countAuxVal[auxSplitVecLocal[j]]);
                       }
                       
                   } // end of nbr loop

                   // reduction
                   for (int i = 0; i < vecNum; i+=splitCombNum) {
                       for (int j = 1; j < splitCombNum; ++j) {
                           countBuf[i] += countBuf[i+j];
                       }
                   }

                   // updating
                   for (int i = 0; i < countCombNum; ++i) {
                       float res = countBuf[i*splitCombNum];
                       countSum += (double)res;
                       if (subsId != 0 && res > 0) {
                          dTableLocal->setCurTableCell(v, combToCountLocal[i], (double)res); 
                       }
                   }
#ifdef __INTEL_COMPILER
                   _mm_free(countBuf);
#else
                   free(countBuf);
#endif

               } // end of valid nbr

           }

        } // end of vert loop

        free(nbrListValid);
    }

    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout);

    return countSum;
}/*}}}*/

void Count::colorInit()
{
#pragma omp parallel
    {
        // each thread has a seed for ranodm number generation
        srand(time(0)+ omp_get_thread_num());

#pragma omp for
        for (int i = 0; i < _vert_num; ++i) {
            _colors_local[i] = (rand()%_color_num);
        }
    }
}

int Count::factorial(int n)
{
    if (n <= 1)
        return 1;
    else
        return (n*factorial(n-1));
}
