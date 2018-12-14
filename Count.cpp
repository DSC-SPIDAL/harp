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

void Count::initialization(Graph& graph, int thd_num, int itr_num, int algoMode, int vtuneStart, bool 
        calculate_automorphisms)
{
    _graph = &graph;
    _vert_num = _graph->get_vert_num();
    _max_deg = _graph->get_max_deg();
    _thd_num = thd_num;
    _itr_num = itr_num;
    _algoMode = algoMode;
    _vtuneStart = vtuneStart;
    _calculate_automorphisms = calculate_automorphisms;
    _colors_local = (int*)malloc(_vert_num*sizeof(int));
    std::memset(_colors_local, 0, _vert_num*sizeof(int));
}

double Count::compute(Graph& templates, bool isEstimate)
{/*{{{*/

    _templates = &templates; 
    _color_num = _templates->get_vert_num();

    div_tp.DivideTp(*(_templates));
    div_tp.sort_tps();

    _subtmp_array = div_tp.get_subtps();
    _total_sub_num = div_tp.get_subtps_num();

#ifdef VERBOSE
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
            std::fflush(stdout);
            assert((main_leaf + aux_leaf == vert_self));

        }
    } 
#endif
   
    // create the index tables
    indexer.initialization(_color_num, _total_sub_num, &_subtmp_array, &div_tp);

#ifdef VERBOSE
    printf("Start initializaing datatable\n");
    std::fflush(stdout); 
#endif

    _dTable.initDataTable(_subtmp_array, &indexer, _total_sub_num, _color_num, _vert_num);

#ifdef VERBOSE
    printf("Finish initializaing datatable\n");
    std::fflush(stdout); 
#endif

#ifdef VERBOSE
    estimateMemComm();
    estimateMemCommPruned();
    estimateFlops();
    estimateFlopsPruned();
#endif

    if (isEstimate)
        return 0.0;

#ifdef VERBOSE
    printf("Start counting\n");
    std::fflush(stdout); 
#endif

    // start counting
    double timeStart = utility::timer();

    double iterCount = 0.0;
    for (int i = 0; i < _itr_num; ++i) {
        iterCount += colorCounting();
    }

#ifdef VERBOSE
    printf("Finish counting\n");
    std::fflush(stdout); 
#endif
  
    printf("\nTime for count per iter: %9.6lf seconds\n", (utility::timer() - timeStart)/_itr_num);
    printf("Ratio for neighbour looping: %9.6lf\% \n", (_spmvElapsedTime*100)/(utility::timer() - timeStart));
    printf("Ratio for multiplication: %9.6lf\% \n", (_fmaElapsedTime*100)/(utility::timer() - timeStart));
    std::fflush(stdout);

    // finish counting
    double finalCount = iterCount/(double)_itr_num;
    double probColorful = factorial(_color_num) / 
                ( factorial(_color_num - _templates->get_vert_num())*pow(_color_num, _templates->get_vert_num()));

    printf("Final raw count is %f\n", finalCount);
    std::fflush(stdout);

    printf("Prob is %f\n", probColorful);
    std::fflush(stdout);

    // int automoNum = 1;
    // enable calculate_automorphisms
    int automoNum = _calculate_automorphisms ? automorphismNum() : 1;  
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
            if (_algoMode == 1)
              countTotal = countNonBottomeFasciaPruned(s);
            else if (_algoMode == 2)
              countTotal = countNonBottomeFascia(s);
            else
              countTotal = countNonBottomeFascia(s);
        }

        if (mainIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(mainIdx);
        if (auxIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(auxIdx);
    }

    return countTotal;
}/*}}}*/

double Count::countNonBottomeFascia(int subsId)
{/*{{{*/

    int subSize = _subtmp_array[subsId].get_vert_num();
    int idxMain = div_tp.get_main_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];

#ifdef VERBOSE
    double startTime = utility::timer();
#endif

    if (subsId == _vtuneStart)
    {
        // for vtune
#ifdef VTUNE
        ofstream vtune_trigger;
        vtune_trigger.open("vtune-flag.txt");
        vtune_trigger << "Start training process and trigger vtune profiling.\n";
        vtune_trigger.close();
#endif
    }

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d\n", subsId, subSize, countCombNum, splitCombNum);
    std::fflush(stdout); 
#endif

    double countSum = 0.0;
    #pragma omp parallel num_threads(_thd_num)
    {
        // local vars for each omp thread
        int* nbrListValid = (int*)malloc(_max_deg*sizeof(int));
        int countNbrValid = 0;
        Graph* gLocal = _graph; 
        DataTabble* dTableLocal = &_dTable;


        int* combToCountLocal = (indexer.getCombToCountTable())[subsId];

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

                   // loop over comb of colorsets
                   for (int n = 0; n <countCombNum; ++n) 
                   {
                       double newColorCount = 0.0;
                       int* mainSplitLocal = (indexer.getSplitToCountTable())[0][subsId][n]; 
                       int* auxSplitLocal = (indexer.getSplitToCountTable())[1][subsId][n]; 
                       int auxPtr = splitCombNum -1;

                       // loop over split comb
                       for (int mainPtr = 0; mainPtr < splitCombNum; ++mainPtr, --auxPtr) {

                           float countMain = countMainVal[mainSplitLocal[mainPtr]]; 
                           if (countMain > 0)
                           {
                               // loop over nbrs
                               for (int j = 0; j < countNbrValid; ++j) {

                                   newColorCount += ((double)countMain)*dTableLocal->getAuxCell(nbrListValid[j], 
                                           auxSplitLocal[auxPtr]);
                               }
                           }
                       }

                       // update
                       if (newColorCount > 0.0)
                       {
                           countSum += newColorCount;
                           if (subsId != 0)
                               dTableLocal->setCurTableCell(v, combToCountLocal[n], (float)newColorCount);

                       }
                   }

               }

           }
        
        } // end of vert loop


        free(nbrListValid);
    }

#ifdef VERBOSE
    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout); 
#endif
#ifdef VERBOSE
    printf("Sub %d, counting time %f\n", subsId, (utility::timer() - startTime));
    std::fflush(stdout); 
#endif

    return countSum;

}/*}}}*/

double Count::countNonBottomeFasciaPruned(int subsId)
{/*{{{*/

    int subSize = _subtmp_array[subsId].get_vert_num();
    int idxMain = div_tp.get_main_node_idx(subsId);
    int idxAux = div_tp.get_aux_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];
    int auxSize = indexer.getSubsSize()[idxAux];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];
    int auxTableLen = indexer.getCombTable()[_color_num][auxSize];

#ifdef VERBOSE
    double startTime = utility::timer();
#endif

    if (subsId == _vtuneStart)
    {
        // for vtune
#ifdef VTUNE
        ofstream vtune_trigger;
        vtune_trigger.open("vtune-flag.txt");
        vtune_trigger << "Start training process and trigger vtune profiling.\n";
        vtune_trigger.close();
#endif
    }

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d, auxTableLen: %d\n", subsId, subSize, 
            countCombNum, splitCombNum, auxTableLen);
    std::fflush(stdout); 
#endif

    double countSum = 0.0;
    float* pruneBuf = (float*) malloc (_vert_num*sizeof(float));

    // decouple the valid nbrs computation
    std::vector<int>* validNbrIdx = new std::vector<int>[_vert_num]; 
#pragma omp parallel for num_threads(_thd_num)
    for (int v = 0; v < _vert_num; ++v) {
        DataTabble* dTableLocal = &_dTable;
        Graph* gLocal = _graph; 
        // not all v colculat the valid nbrs list
        if (dTableLocal->isVertInitMain(v))
        {
            int* adjList = gLocal->get_adj_list(v);
            int adjEnd = gLocal->get_out_deg(v);
            for (int j = 0; j < adjEnd; ++j) 
            {
                int adjVal = adjList[j];
                if (dTableLocal->isVertInitAux(adjVal)) {
                    validNbrIdx[v].push_back(adjVal);
                }
            }
        }
        
    }

#ifdef VERBOSE
    printf("Finish creating valid nbrs list\n");
    std::fflush(stdout); 
#endif

#ifdef VERBOSE
    double eltSpmv = 0.0;
    double eltMul = 0.0;
    double spmvStart = 0.0;
    double fmaStart = 0.0;
#endif

#ifdef VERBOSE
    spmvStart = utility::timer();
#endif

    // first loop on auxTableLen
    for (int i = 0; i < auxTableLen; ++i) {

        // clear the buffer
        std::memset(pruneBuf, 0, _vert_num*sizeof(float));

#pragma omp parallel for num_threads(_thd_num)
        for (int v = 0; v < _vert_num; ++v) 
        {
            DataTabble* dTableLocal = &_dTable;
            if (dTableLocal->isVertInitMain(v))
            {
                for (int j = 0; j < validNbrIdx[v].size(); ++j) {

                    float* countAuxValLocal = dTableLocal->getAuxArray(validNbrIdx[v][j]);
                    if (countAuxValLocal != NULL)
                        pruneBuf[v] += countAuxValLocal[i]; 
                }

             
            }
        }

#pragma omp parallel for num_threads(_thd_num)
        for (int v = 0; v < _vert_num; ++v) 
        {
            DataTabble* dTableLocal = &_dTable;
            if (dTableLocal->isVertInitMain(v))
            {
                float* countAuxValUpdate = dTableLocal->getAuxArray(v);
                if (countAuxValUpdate == NULL)
                {
                    #ifdef __INTEL_COMPILER
                        countAuxValUpdate = (float*) _mm_malloc(auxTableLen*sizeof(float), 64); 
                    #else
                        countAuxValUpdate = (float*) aligned_alloc(64, auxTableLen*sizeof(float)); 
                    #endif

                    dTableLocal->setAuxArray(v, countAuxValUpdate);
                }

                countAuxValUpdate[i] = pruneBuf[v];
            }
        }
    }

#ifdef VERBOSE
       _spmvElapsedTime += (utility::timer() - spmvStart);
       eltSpmv += (utility::timer() - spmvStart);
#endif

#ifdef VERBOSE
    printf("Finish pruned aux computing\n");
    std::fflush(stdout); 
#endif

#ifdef VERBOSE
    fmaStart = utility::timer();
#endif

    #pragma omp parallel num_threads(_thd_num)
    {
        // local vars for each omp thread
        // int* nbrListValid = (int*)malloc(_max_deg*sizeof(int));
        // int countNbrValid = 0;
        Graph* gLocal = _graph; 
        DataTabble* dTableLocal = &_dTable;


        int* combToCountLocal = (indexer.getCombToCountTable())[subsId];

        #pragma omp for schedule(static) reduction(+:countSum)
        for (int v = 0; v < _vert_num; ++v) {

           // countNbrValid = 0; 
           if (dTableLocal->isVertInitMain(v))
           {
               // select the valid nbrs
               // int* adjList = gLocal->get_adj_list(v);
               // int adjEnd = gLocal->get_out_deg(v);
               //
               // for (int j = 0; j < adjEnd; ++j) {
               //
               //     int adjVal = adjList[j];
               //     if (dTableLocal->isVertInitAux(adjVal)) {
               //         nbrListValid[countNbrValid++] = adjVal;
               //     }
               // }

               float* countMainVal = dTableLocal->getMainArray(v);

               // if (countNbrValid)
               if (validNbrIdx[v].size() > 0)
               {

                   // loop over comb of colorsets
                   for (int n = 0; n <countCombNum; ++n) 
                   {
                       double newColorCount = 0.0;
                       int* mainSplitLocal = (indexer.getSplitToCountTable())[0][subsId][n]; 
                       int* auxSplitLocal = (indexer.getSplitToCountTable())[1][subsId][n]; 
                       int auxPtr = splitCombNum -1;

                       // loop over split comb
                       for (int mainPtr = 0; mainPtr < splitCombNum; ++mainPtr, --auxPtr) {

                           float countMain = countMainVal[mainSplitLocal[mainPtr]]; 
                           if (countMain > 0)
                           {
                               // loop over nbrs
                               // for (int j = 0; j < countNbrValid; ++j) 
                               // {

                                   newColorCount += ((double)countMain)*dTableLocal->getAuxCell(v, 
                                           auxSplitLocal[auxPtr]);
                               // }
                           }
                       }

                       // update
                       if (newColorCount > 0.0)
                       {
                           countSum += newColorCount;
                           if (subsId != 0)
                               dTableLocal->setCurTableCell(v, combToCountLocal[n], (float)newColorCount);

                       }
                   }

               }

           }
        
        } // end of vert loop


        // free(nbrListValid);
    }

#ifdef VERBOSE
       _fmaElapsedTime += (utility::timer() - fmaStart);
       eltMul += (utility::timer() - fmaStart);
#endif

#ifdef VERBOSE
    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout); 
#endif
#ifdef VERBOSE
    double subsTime = (utility::timer() - startTime);
    printf("Sub %d, counting time %f, loop neighbours time: %f, ratio: %f, Mul time %f, ratio %f\n", subsId, 
            subsTime, eltSpmv, (eltSpmv*100)/subsTime, eltMul, (eltMul*100)/subsTime);
    std::fflush(stdout); 
#endif

    free(pruneBuf);
    delete[] validNbrIdx;

    return countSum;

}/*}}}*/

double Count::countNonBottomeVec(int subsId)
{/*{{{*/

    int subSize = _subtmp_array[subsId].get_vert_num();

    int idxMain = div_tp.get_main_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];
    int vecNum = countCombNum*splitCombNum;

#ifdef VERBOSE
    double startTime = utility::timer();
#endif

    if (subsId == 1)
    {
        // for vtune
#ifdef VTUNE
        ofstream vtune_trigger;
        vtune_trigger.open("vtune-flag.txt");
        vtune_trigger << "Start training process and trigger vtune profiling.\n";
        vtune_trigger.close();
#endif
    }

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d\n", subsId, subSize, 
            countCombNum, splitCombNum);
    std::fflush(stdout); 
#endif

// #ifdef VERBOSE
//     // debug check the zero-valued (skipped) vertices
//     int zerovNum = 0;
//     for (int i = 0; i < _vert_num; ++i) {
//        if (!_dTable.isVertInitMain(i)) 
//            zerovNum++;
//     }
//     printf("skipped vertice for sub %d is %d\n", subsId, zerovNum);
//     std::fflush(stdout); 
// #endif

    double countSum = 0.0;
    int validAdjs = 0;
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
        // #pragma omp for schedule(static) reduction(+:countSum) reduction(+:validAdjs)
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

               // validAdjs += countNbrValid;

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

#ifdef VERBOSE
    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout); 
    // printf("Sub %d, valid verts %d, total adjs %d\n", subsId, validAdjs, _graph->get_edge_num());
    // std::fflush(stdout); 
#endif
#ifdef VERBOSE
    printf("Sub %d, counting time %f\n", subsId, (utility::timer() - startTime));
    std::fflush(stdout); 
#endif

    return countSum;
}/*}}}*/

double Count::countNonBottomePruned(int subsId)
{/*{{{*/

    int subSize = _subtmp_array[subsId].get_vert_num();

    int idxMain = div_tp.get_main_node_idx(subsId);
    int idxAux = div_tp.get_aux_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];
    int auxSize = indexer.getSubsSize()[idxAux];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];
    int vecNum = countCombNum*splitCombNum;
    int auxTableLen = indexer.getCombTable()[_color_num][auxSize];

#ifdef VERBOSE
    double startTime = utility::timer();
#endif

    if (subsId == _vtuneStart)
    {
        // for vtune
#ifdef VTUNE
        ofstream vtune_trigger;
        vtune_trigger.open("vtune-flag.txt");
        vtune_trigger << "Start training process and trigger vtune profiling.\n";
        vtune_trigger.close();
#endif
    }

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d, auxTableLen: %d\n", subsId, subSize, 
            countCombNum, splitCombNum, auxTableLen);
    std::fflush(stdout); 
#endif

    double countSum = 0.0;
    // decouple the neighbour list looping from the computation
    // create a buffer to store P_{s,j}
    float* pruneBuf = (float*) malloc (_vert_num*sizeof(float));

    // decouple the valid nbrs computation
    std::vector<int>* validNbrIdx = new std::vector<int>[_vert_num]; 
#pragma omp parallel for num_threads(_thd_num)
    for (int v = 0; v < _vert_num; ++v) {
        DataTabble* dTableLocal = &_dTable;
        Graph* gLocal = _graph; 
        // not all v colculat the valid nbrs list
        if (dTableLocal->isVertInitMain(v))
        {
            int* adjList = gLocal->get_adj_list(v);
            int adjEnd = gLocal->get_out_deg(v);
            for (int j = 0; j < adjEnd; ++j) 
            {
                int adjVal = adjList[j];
                if (dTableLocal->isVertInitAux(adjVal)) {
                    validNbrIdx[v].push_back(adjVal);
                }
            }
        }
        
    }

#ifdef VERBOSE
    printf("Finish creating valid nbrs list\n");
    std::fflush(stdout); 
#endif

    // first loop on auxTableLen
    for (int i = 0; i < auxTableLen; ++i) {

        // clear the buffer
        std::memset(pruneBuf, 0, _vert_num*sizeof(float));

#pragma omp parallel for num_threads(_thd_num)
        for (int v = 0; v < _vert_num; ++v) 
        {
            DataTabble* dTableLocal = &_dTable;
            if (dTableLocal->isVertInitMain(v))
            {
                for (int j = 0; j < validNbrIdx[v].size(); ++j) {

                    float* countAuxValLocal = dTableLocal->getAuxArray(validNbrIdx[v][j]);
                    if (countAuxValLocal != NULL)
                        pruneBuf[v] += countAuxValLocal[i]; 
                }

             
            }
        }

#pragma omp parallel for num_threads(_thd_num)
        for (int v = 0; v < _vert_num; ++v) 
        {
            DataTabble* dTableLocal = &_dTable;
            if (dTableLocal->isVertInitMain(v))
            {
                float* countAuxValUpdate = dTableLocal->getAuxArray(v);
                if (countAuxValUpdate == NULL)
                {
                    #ifdef __INTEL_COMPILER
                        countAuxValUpdate = (float*) _mm_malloc(auxTableLen*sizeof(float), 64); 
                    #else
                        countAuxValUpdate = (float*) aligned_alloc(64, auxTableLen*sizeof(float)); 
                    #endif

                    dTableLocal->setAuxArray(v, countAuxValUpdate);
                }

                countAuxValUpdate[i] = pruneBuf[v];
            }
        }
    }

#ifdef VERBOSE
    printf("Finish pruned aux computing\n");
    std::fflush(stdout); 
#endif

    // compute the multiplication and addition
    #pragma omp parallel num_threads(_thd_num)
    {
        // local vars for each omp thread
        // int* nbrListValid = (int*)malloc(_max_deg*sizeof(int));
        // int countNbrValid = 0;
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

           // countNbrValid = 0; 
           if (dTableLocal->isVertInitMain(v))
           {
               // select the valid nbrs
               // int* adjList = gLocal->get_adj_list(v);
               // int adjEnd = gLocal->get_out_deg(v);
               float* countMainVal = dTableLocal->getMainArray(v);

               // for (int j = 0; j < adjEnd; ++j) {
               //
               //     int adjVal = adjList[j];
               //     if (dTableLocal->isVertInitAux(adjVal)) {
               //         nbrListValid[countNbrValid++] = adjVal;
               //     }
               // }
               
               float*  countAuxVal= dTableLocal->getAuxArray(v);
               if (validNbrIdx[v].size() > 0 && countAuxVal != NULL)
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
                   // for (int i = 0; i < validNbrIdx[v].size(); ++i) 
                   // {

                       // float*  countAuxVal= dTableLocal->getAuxArray(validNbrIdx[v][i]);

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
                           countBuf[j] = (countMainVal[mainSplitVecLocal[j]]*countAuxVal[auxSplitVecLocal[j]]);
                           // countBuf[j] += (countMainVal[mainSplitVecLocal[j]]*countAuxVal[auxSplitVecLocal[j]]);
                       }

                   // } // end of nbr loop

                   // reduction
                   for (int i = 0; i < vecNum; i+=splitCombNum) {
                       for (int j = 1; j < splitCombNum; ++j) {
                           countBuf[i] += countBuf[i+j];
                       }
                   }

                   // updating
                   for (int i = 0; i < countCombNum; ++i) 
                   {
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
    }

#ifdef VERBOSE
    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout); 
#endif
#ifdef VERBOSE
    printf("Sub %d, counting time %f\n", subsId, (utility::timer() - startTime));
    std::fflush(stdout); 
#endif

    free(pruneBuf);
    delete[] validNbrIdx;
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

int Count::automorphismNum()
{
    std::vector<int> mappingID;
    std::vector<int> restID;

    for (int i = 0; i < _templates->get_vert_num(); ++i) {
        restID.push_back(i);
    }

    return calcAutomorphismRecursive((*_templates), mappingID, restID);
}

int Count::calcAutomorphismZero(Graph& t, std::vector<int>& mappingID)
{
    for (int i = 0; i < mappingID.size(); ++i) {
       if (t.get_out_deg(i) != t.get_out_deg(mappingID[i])) 
           return 0;
       else
       {
           int* adjList = t.get_adj_list(i);
           int* adjListMap = t.get_adj_list(mappingID[i]);
           int end = t.get_out_deg(i);

           bool* isMatch = new bool[end];
           for (int j = 0; j < end; ++j) {
               isMatch[j] = false;
               int u = adjList[j];
               for (int k = 0; k < end; ++k) {
                   int u_map = adjListMap[k];
                   if (u == mappingID[u_map])
                       isMatch[j] = true;
               }
               
           }

           for (int k = 0; k < end; ++k) {
              
               if (!isMatch[k])
                   return 0;
           }

       }
        
    }
    return 1;
}

int Count::calcAutomorphismRecursive(Graph& t, std::vector<int>& mappingID, std::vector<int>& restID)
{
    int count = 0;
    if (!restID.size())
    {
        return calcAutomorphismZero(t, mappingID); 
    }
    else
    {
        for (int i = 0; i < restID.size(); ++i) {
            mappingID.push_back(restID[i]);
            std::vector<int> newRestID;
            for (int j = 0; j < restID.size(); ++j) {
                if (i!=j)
                    newRestID.push_back(restID[j]);
                
            }

            count += calcAutomorphismRecursive(t, mappingID, newRestID);
            newRestID.clear();
            mappingID.pop_back();
        }

    }

    return count;

}

double Count::estimateMemComm()
{
    double commBytesTotal = 0.0;

    // read and write comb value
    // val: n read n write + index: n 
    double bytesPerComb = sizeof(float)*(2*_graph->get_vert_num()) + sizeof(int)*(_graph->get_vert_num());
    // val: n active, nnz passive + index: n active, n passive 
    double bytesPerSplit = sizeof(float)*(_graph->get_vert_num() + _graph->get_edge_num()) + 
        sizeof(int)*(2*_graph->get_vert_num());

    for(int s=_total_sub_num-1;s>=0;s--)
    {
        int vert_self = _subtmp_array[s].get_vert_num();
        if (vert_self > 1) {
            
            int idxMain = div_tp.get_main_node_idx(s);
            int idxAux = div_tp.get_aux_node_idx(s);       

            commBytesTotal += (_dTable.getTableLen(s)*bytesPerComb);
            commBytesTotal += (_dTable.getTableLen(s)*
                indexer.comb_calc(vert_self, _subtmp_array[idxAux].get_vert_num())*bytesPerSplit);
            
        }
    }

    commBytesTotal /= (1024*1024*1024);

    printf("Comm Bytes Non Pruned estimated : %f GBytes\n", commBytesTotal);
    std::fflush(stdout);

    return commBytesTotal;
}

double Count::estimateFlops()
{
    double flopsTotal = 0.0;

    // read and write comb value
    double flopsPerComb = 0.0;
    double flopsPerSplit = (2*_graph->get_edge_num());

    for(int s=_total_sub_num-1;s>=0;s--)
    {
        int vert_self = _subtmp_array[s].get_vert_num();
        if (vert_self > 1) {
            
            int idxMain = div_tp.get_main_node_idx(s);
            int idxAux = div_tp.get_aux_node_idx(s);       

            flopsTotal += (_dTable.getTableLen(s)*flopsPerComb);
            flopsTotal += (_dTable.getTableLen(s)*
                indexer.comb_calc(vert_self, _subtmp_array[idxAux].get_vert_num())*flopsPerSplit);
            
        }
    }

    flopsTotal /= (1024*1024*1024);

    printf("Operations Non Pruned estimated : %f GFLOP\n", flopsTotal);
    std::fflush(stdout);

    return flopsTotal;
}

double Count::estimateMemCommPruned()
{
    double commBytesTotal = 0.0;

    // read and write comb value
    // Val: nnz read n write + Index 0
    double bytesPerNeighbor = sizeof(float)*(_graph->get_vert_num() + _graph->get_edge_num());

    // Val: n read + n write + Index n
    double bytesPerComb = sizeof(float)*(2*_graph->get_vert_num()) + sizeof(int)*(_graph->get_vert_num());
    // Val: n active + n passive + Index 2n  
    double bytesPerSplit = sizeof(float)*(2*_graph->get_vert_num()) + sizeof(int)*(2*_graph->get_vert_num());

    for(int s=_total_sub_num-1;s>=0;s--)
    {
        int vert_self = _subtmp_array[s].get_vert_num();
        if (vert_self > 1) {
            
            int idxMain = div_tp.get_main_node_idx(s);
            int idxAux = div_tp.get_aux_node_idx(s);       

            commBytesTotal += (_dTable.getTableLen(idxAux)*bytesPerNeighbor);
            commBytesTotal += (_dTable.getTableLen(s)*bytesPerComb);
            commBytesTotal += (_dTable.getTableLen(s)*
                indexer.comb_calc(vert_self, _subtmp_array[idxAux].get_vert_num())*bytesPerSplit);
            
        }
    }

    commBytesTotal /= (1024*1024*1024);

    printf("Comm Bytes Pruned estimated : %f GBytes\n", commBytesTotal);
    std::fflush(stdout);

    return commBytesTotal;
}

double Count::estimateFlopsPruned()
{
    double flopsTotal = 0.0;

    // read and write comb value
    double flopsPerNeighbor = (_graph->get_edge_num());

    double flopsPerComb = 0.0;
    double flopsPerSplit = (_graph->get_vert_num()*2) ;

    for(int s=_total_sub_num-1;s>=0;s--)
    {
        int vert_self = _subtmp_array[s].get_vert_num();
        if (vert_self > 1) {
            
            int idxMain = div_tp.get_main_node_idx(s);
            int idxAux = div_tp.get_aux_node_idx(s);       

            flopsTotal += (_dTable.getTableLen(idxAux)*flopsPerNeighbor);
            flopsTotal += (_dTable.getTableLen(s)*flopsPerComb);
            flopsTotal += (_dTable.getTableLen(s)*
                indexer.comb_calc(vert_self, _subtmp_array[idxAux].get_vert_num())*flopsPerSplit);
            
        }
    }

    flopsTotal /= (1024*1024*1024);

    printf("Operations Pruned estimated : %f GFLOP\n", flopsTotal);
    std::fflush(stdout);

    return flopsTotal;
}
