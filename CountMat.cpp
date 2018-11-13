// the implementation of the color-coding algorithms
// Author: Langshi Chen

#include <cassert>
#include <math.h>
#include<cstdlib>
#include<ctime>
#include <cstring>
#include <omp.h>
#include <vector>

#include "CountMat.hpp"
#include "Helper.hpp"

using namespace std;

void CountMat::initialization(CSRGraph& graph, int thd_num, int itr_num, int isPruned)
{
    _graph = &graph;
    _vert_num = _graph->getNumVertices();
    _thd_num = thd_num;
    _itr_num = itr_num;
    _isPruned = isPruned;
    _isScaled = 0;
    _colors_local = (int*)malloc(_vert_num*sizeof(int));
    std::memset(_colors_local, 0, _vert_num*sizeof(int));

#ifdef __INTEL_COMPILER
    _bufVec = (float*) _mm_malloc(_vert_num*sizeof(float), 64); 
#else
    _bufVec = (float*) aligned_alloc(64, _vert_num*sizeof(float)); 
#endif
    std::memset(_bufVec, 0, _vert_num*sizeof(float));

    _bufVecThd = (float**) malloc (_thd_num*sizeof(float*));

    for (int i = 0; i < _thd_num; ++i) {
       #ifdef __INTEL_COMPILER
          _bufVecThd[i] =  (float*) _mm_malloc(_vert_num*sizeof(float), 64); 
       #else
          _bufVecThd[i] =  (float*) aligned_alloc(64, _vert_num*sizeof(float)); 
       #endif 
    }
}

double CountMat::compute(Graph& templates)
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

    // check the effective aux indices
    // for (int s = 0; s < _total_sub_num; ++s) {
    //     printf("Effectiv sub %d\n", s);
    //     std::fflush(stdout);
    //     std::vector<int>* effectVector = indexer.getEffectiveAuxIndices();
    //     for (int i = 0; i < effectVector[s].size(); ++i) {
    //         printf("index: %d\n", effectVector[s][i]); 
    //         std::fflush(stdout);
    //     }
    // }
    // for (int s = 0; s < _total_sub_num; ++s) {
    //
    //     if (_subtmp_array[s].get_vert_num() > 1)
    //     {
    //         int idxAux = div_tp.get_aux_node_idx(s);
    //         int auxSize = indexer.getSubsSize()[idxAux];
    //         int auxNodesLen = indexer.getCombTable()[_color_num][auxSize];
    //         printf("Subs: %d, Aux count len: %d, effect len: %d\n", s, auxNodesLen, (indexer.getEffectiveAuxIndices())[s].size());
    //         std::fflush(stdout);
    //     }
    // }
    
#ifdef VERBOSE
    printf("Start initializaing datatable\n");
    std::fflush(stdout); 
#endif

    _dTable.initDataTable(_subtmp_array, &indexer, _total_sub_num, _color_num, _vert_num, _thd_num);

#ifdef VERBOSE
    printf("Finish initializaing datatable\n");
    std::fflush(stdout); 
#endif

#ifdef VERBOSE
    printf("Start counting\n");
    std::fflush(stdout); 
#endif

    // allocating the bufVecLeaf buffer
    _bufVecLeaf = (float**) malloc (_color_num*sizeof(float*));
    for (int i = 0; i < _color_num; ++i) {
       #ifdef __INTEL_COMPILER
          _bufVecLeaf[i] =  (float*) _mm_malloc(_vert_num*sizeof(float), 64); 
       #else
          _bufVecLeaf[i] =  (float*) aligned_alloc(64, _vert_num*sizeof(float)); 
       #endif 
    }

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

    double totalCountTime = (utility::timer() - timeStart);
    printf("\nTime for count per iter: %9.6lf seconds\n", totalCountTime/_itr_num);
    std::fflush(stdout);

#ifdef VERBOSE
    printf("spmv ratio %f\% \n", 100*(_spmvTime/totalCountTime));
    std::fflush(stdout);
    printf("eMA ratio %f\% \n", 100*(_eMATime/totalCountTime));
    std::fflush(stdout);
#endif

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

    return finalCount;

}/*}}}*/

double CountMat::colorCounting()
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
            _dTable.countCurBottom(idxCombToCount, _colors_local);          
        }
        else
        {
            //non-bottom case
            if (_isPruned == 1)
                countTotal = countNonBottomePruned(s);
            else
                countTotal = countNonBottomeOriginal(s);
        }

        if (mainIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(mainIdx, false);
        if (auxIdx != DUMMY_VAL)
            _dTable.cleanSubTempTable(auxIdx, false);
    }

    return countTotal;


}/*}}}*/

//reduce the num of SpMV
double CountMat::countNonBottomePruned(int subsId)
{/*{{{*/

    if (subsId == 3)
    {
        // for vtune miami u15-2 subids==3 takes 103 secs
#ifdef VTUNE
        ofstream vtune_trigger;
        vtune_trigger.open("vtune-flag.txt");
        vtune_trigger << "Start training process and trigger vtune profiling.\n";
        vtune_trigger.close();
#endif
    }

    int subSize = _subtmp_array[subsId].get_vert_num();

    int idxMain = div_tp.get_main_node_idx(subsId);
    int idxAux = div_tp.get_aux_node_idx(subsId);

    int mainSize = indexer.getSubsSize()[idxMain];
    int auxSize = indexer.getSubsSize()[idxAux];
   
    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];
    int auxTableLen = indexer.getCombTable()[_color_num][auxSize];

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d, isScaled: %d\n", subsId, subSize, 
            countCombNum, splitCombNum, _isScaled);
    std::fflush(stdout); 
#endif

    double countSum = 0.0;
    double subSum = 0.0;
    int** mainSplitLocal = (indexer.getSplitToCountTable())[0][subsId]; 
    int** auxSplitLocal = (indexer.getSplitToCountTable())[1][subsId]; 
    int* combToCountLocal = (indexer.getCombToCountTable())[subsId];

    double* bufLastSub = nullptr;
    float* objArray = nullptr;

    if (subsId == 0)
    {
#ifdef __INTEL_COMPILER
      bufLastSub = (double*) _mm_malloc(_vert_num*sizeof(double), 64); 
#else
      bufLastSub = (double*) aligned_alloc(64, _vert_num*sizeof(double)); 
#endif
      std::memset(bufLastSub, 0, _vert_num*sizeof(double)); 

    }

#ifdef VERBOSE
    double startTime = utility::timer();
    double startTimeComp = 0.0;
    double eltSpmv = 0.0;
    double eltMul = 0.0;
#endif

// first the precompute of SpMV results and have a in-place storage
#ifdef VERBOSE
   startTimeComp = utility::timer(); 
#endif
   for (int i = 0; i < auxTableLen; ++i) {

       float* auxObjArray = _dTable.getAuxArray(i);
       _graph->SpMVNaive(auxObjArray, _bufVec); 
       
       // check the size of auxArray
       if (auxSize > 1)
          std::memcpy(auxObjArray, _bufVec, _vert_num*sizeof(float));
       else
          std::memcpy(_bufVecLeaf[i], _bufVec, _vert_num*sizeof(float));
   }
#ifdef VERBOSE
   eltSpmv += (utility::timer() - startTimeComp); 
#endif   

#ifdef VERBOSE
   startTimeComp = utility::timer(); 
#endif
// a second part only involves element-wise multiplication and updating
    for(int i=0; i<countCombNum; i++)
    {
        int combIdx = combToCountLocal[i];

// #ifdef VERBOSE
//         printf("Sub: %d, comb: %d, combIdx: %d\n", subsId, i, combIdx);
//         std::fflush(stdout);
// #endif

        if (subsId > 0)
            objArray = _dTable.getCurTableArray(combIdx);

        for (int j = 0; j < splitCombNum; ++j) {

            int mainIdx = mainSplitLocal[i][j];
            int auxIdx = auxSplitLocal[i][j];

// #ifdef VERBOSE
//         printf("Sub: %d, mainIdx: %d, auxIdx: %d\n", subsId, mainIdx, auxIdx);
//         std::fflush(stdout);
// #endif

            // already pre-computed by SpMV
            float* auxArraySelect = nullptr;
            if (auxSize > 1)
                auxArraySelect = _dTable.getAuxArray(auxIdx);
            else
                auxArraySelect = _bufVecLeaf[auxIdx];

            // element-wise mul 
            float* mainArraySelect = _dTable.getMainArray(mainIdx);
            if (subsId > 0)
            {
                if (_isScaled == 0)
                    _dTable.arrayWiseFMAScale(objArray, auxArraySelect, mainArraySelect, 1.0e-12);
                else
                    _dTable.arrayWiseFMAAVX(objArray, auxArraySelect, mainArraySelect);
            }
            else
            {
                // the last scale use 
                _dTable.arrayWiseFMALast(bufLastSub, auxArraySelect, mainArraySelect);
            }

        }

        // if (subsId > 0)
        //     subSum += sumVec(objArray, _vert_num);

    }
#ifdef VERBOSE
   eltMul += (utility::timer() - startTimeComp); 
#endif

    _isScaled = 1;

    if (subsId == 0)
    {
        // sum the vals from bufLastSub  
        for (int k = 0; k < _vert_num; ++k) {
            countSum += bufLastSub[k];
        }

        // to recover the scale down process
        if (_isScaled == 1)
            countSum *= 1.0e+12;

#ifdef __INTEL_COMPILER
        _mm_free(bufLastSub);
#else
        free(bufLastSub);
#endif
    }

#ifdef VERBOSE
    double subsTime = (utility::timer() - startTime);
    printf("Sub %d, counting time %f, Spmv time %f: ratio: %f\%,  Mul time %f: ratio: %f\% \n", subsId, subsTime, eltSpmv, 100*(eltSpmv/subsTime), eltMul, 100*(eltMul/subsTime));
    _spmvTime += eltSpmv;
    _eMATime += eltMul;
    // printf("Sub %d, counting val %e\n", subsId, subSum);
    // std::fflush(stdout); 
#endif

// #ifdef VERBOSE
//     printf("Sub %d, NonBottom raw count %e\n", subsId, countSum);
//     std::fflush(stdout); 
// #endif

    return countSum;

}/*}}}*/

double CountMat::countNonBottomeOriginal(int subsId)
{/*{{{*/

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

    int subSize = _subtmp_array[subsId].get_vert_num();

    int idxMain = div_tp.get_main_node_idx(subsId);
    int mainSize = indexer.getSubsSize()[idxMain];

    int countCombNum = indexer.getCombTable()[_color_num][subSize];
    int splitCombNum = indexer.getCombTable()[subSize][mainSize];

#ifdef VERBOSE
    printf("Finish init sub templte %d, vert: %d, comb: %d, splitNum: %d\n", subsId, subSize, 
            countCombNum, splitCombNum);
    std::fflush(stdout); 
#endif

    double countSum = 0.0;
    int** mainSplitLocal = (indexer.getSplitToCountTable())[0][subsId]; 
    int** auxSplitLocal = (indexer.getSplitToCountTable())[1][subsId]; 
    int* combToCountLocal = (indexer.getCombToCountTable())[subsId];

    float* bufLastSub = nullptr;
    float* objArray = nullptr;

    if (subsId == 0)
    {
#ifdef __INTEL_COMPILER
      bufLastSub = (float*) _mm_malloc(_vert_num*sizeof(float), 64); 
#else
      bufLastSub = (float*) aligned_alloc(64, _vert_num*sizeof(float)); 
#endif
      std::memset(bufLastSub, 0, _vert_num*sizeof(float)); 

    }

#ifdef VERBOSE
    double startTime = utility::timer();
    double startTimeComp = 0.0;
    double eltSpmv = 0.0;
    double eltMul = 0.0;
#endif
   
    for(int i=0; i<countCombNum; i++)
    {
        int combIdx = combToCountLocal[i];

        if (subsId == 0)
            objArray = bufLastSub;
        else
            objArray = _dTable.getCurTableArray(combIdx);

        for (int j = 0; j < splitCombNum; ++j) {

            int mainIdx = mainSplitLocal[i][j];
            int auxIdx = auxSplitLocal[i][j];

            float* auxArraySelect = _dTable.getAuxArray(auxIdx);
            // spmv
#ifdef VERBOSE
            startTimeComp = utility::timer();
#endif
            _graph->SpMVNaive(auxArraySelect, _bufVec);
#ifdef VERBOSE
            eltSpmv += (utility::timer() - startTimeComp);
#endif

            // element-wise mul 
            float* mainArraySelect = _dTable.getMainArray(mainIdx);
#ifdef VERBOSE
            startTimeComp = utility::timer();
#endif

            _dTable.arrayWiseFMANaive(objArray, _bufVec, mainArraySelect);
#ifdef VERBOSE
            eltMul += (utility::timer() - startTimeComp);
#endif

        }
    }

    if (subsId == 0)
    {
        // sum the vals from bufLastSub  
        for (int k = 0; k < _vert_num; ++k) {
            countSum += bufLastSub[k];
        }
#ifdef __INTEL_COMPILER
        _mm_free(bufLastSub);
#else
        free(bufLastSub);
#endif
    }

#ifdef VERBOSE
    printf("Sub %d, counting time %f, Spmv time %f, Mul time %f \n", subsId, (utility::timer() - startTime), eltSpmv, eltMul);
    // printf("Sub %d, counting time %f\n", subsId, (utility::timer() - startTime));
    std::fflush(stdout); 
#endif

#ifdef VERBOSE
    printf("Sub %d, NonBottom raw count %f\n", subsId, countSum);
    std::fflush(stdout); 
#endif

    return countSum;

}/*}}}*/

void CountMat::colorInit()
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

int CountMat::factorial(int n)
{
    if (n <= 1)
        return 1;
    else
        return (n*factorial(n-1));
}


double CountMat::sumVec(valType* input, idxType len)
{
    double sum = 0.0;
#pragma omp parallel for reduction(+:sum) 
    for (idxType i = 0; i < len; ++i) {
        sum += input[i];
    }

    return sum;
}

void CountMat::scaleVec(valType* input, idxType len, double scale)
{
#pragma omp parallel for  
    for (idxType i = 0; i < len; ++i) {
        double tmp = scale*input[i];
        input[i] = (float)tmp;
    }

}
