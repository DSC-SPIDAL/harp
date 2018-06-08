/* file: subgraph_types.h */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

/*
//++
//  Definition of subgraph common types.
//--
*/


#ifndef __SUBGRAPH_TYPES_H__
#define __SUBGRAPH_TYPES_H__

#include <cstdio> 
#include <vector>
#include <string>
#include <cstring>
#include <set>
#include <unordered_set>
#include <unordered_map>

#include "algorithms/algorithm.h"
#include "data_management/data/numeric_table.h"
// #include "data_management/data/harp_numeric_table.h"
#include "data_management/data/homogen_numeric_table.h"
#include "services/daal_defines.h"
// #include "services/hdfs.h"
#include "hdfs.h"
#include "harp_numeric_table.h"

using namespace daal::data_management;
using namespace daal::data_management::interface1;
using namespace daal::services;

namespace daal
{

//further change the id values
const int SERIALIZATION_SUBGRAPH_RESULT_ID = 106001; 
const int SERIALIZATION_SUBGRAPH_DISTRI_PARTIAL_RESULT_ID = 106101; 
            
const int null_val = 2147483647;  //integer max 
const int create_size = 100;

namespace algorithms
{

/**
* @defgroup color coding based subgraph counting 
* \copydoc daal::algorithms::subgraph
* @ingroup subgraph
* @{
*/
/** \brief Contains classes for computing the results of the subgraph algorithm */
namespace subgraph
{

    

/**
 * <a name="DAAL-ENUM-ALGORITHMS__subgraph__METHOD"></a>
 * Available methods for computing the subgraph algorithm
 */
enum Method
{
    defaultSC    = 0 /*!< Default Standard color coding subgraph counting */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__subgraph__INPUTID"></a>
 * Available types of input objects for the subgraph algorithm
 */
enum InputId
{
    filenames = 0,		 /* input graph files  */
	fileoffset = 1,
    localV = 2,
    tfilenames =3,       /* template file */
    tfileoffset = 4,
    VMapperId = 5,
    CommDataId = 6,
    ParcelOffsetId = 7,
    ParcelDataId = 8,
    ParcelIdxId = 9
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__subgraph__RESULTID"></a>
 * Available types of results of the subgraph algorithm
 * to Remove
 */
enum ResultId
{
    resWMat = 0,   /*!< Model W */
    resHMat = 1    /*!< Model H */
};

/**
 * <a name="DAAL-ENUM-ALGORITHMS__subgraph__DISTRIBUTED_RESULTID"></a>
 * Available types of partial results of the subgraph algorithm
 * to remove
 */
enum DistributedPartialResultId
{
    presWMat = 0,   /*!< Model W, used in distributed mode */
    presHMat = 1,   /*!< Model H, used in distributed mode*/
    presRMSE = 2,   /*!< RMSE computed from test dataset */
    presWData = 3
};


/**
 * \brief Contains version 1.0 of Intel(R) Data Analytics Acceleration Library (Intel(R) DAAL) interface.
 */
namespace interface1
{
    
    /**
     * @brief store abs v id and its adj list
	 * of input graph data
     */
    struct v_adj_elem{

        v_adj_elem(){_v_id = 0;}
        v_adj_elem(int v_id){_v_id = v_id;}
        ~v_adj_elem(){std::vector<int>().swap(_adjs);}

        int _v_id;
        std::vector<int> _adjs;
    };

	/**
	 * @brief an omp task of computing count information by local neighbors 
	 */
	struct task_nbr{

		task_nbr(): _vertex(-1), _nbr_ptr(NULL), _nbr_size(0) {}
		task_nbr(int vertex, int nbr_size, int* nbr_ptr): _vertex(vertex), _nbr_size(nbr_size), _nbr_ptr(nbr_ptr) {}
		~task_nbr(){_vertex = -1; _nbr_ptr = NULL;_nbr_size = 0;}

		int _vertex; // the updating vertex in graph
		int* _nbr_ptr; // the truncated local nbr list
		int _nbr_size; // the size of local nbr list 
	};

	/**
	 * @brief an omp task of updating count information 
	 * by received remote neighbors 
	 */
	struct task_nbr_update{

		task_nbr_update(int vertex, int adj_size, int* map_ids_atom, int* chunk_ids_atom, int* chunk_internal_offsets_atom):
			_vertex(vertex), _adj_size(adj_size), _map_ids_atom(map_ids_atom), _chunk_ids_atom(chunk_ids_atom), 
			_chunk_internal_offsets_atom(chunk_internal_offsets_atom) {}
		
		~task_nbr_update()
		{
			_vertex = -1;
			_adj_size = 0;
			_map_ids_atom = NULL;
			_chunk_ids_atom = NULL;
			_chunk_internal_offsets_atom = NULL;
		}

		int _vertex;
		int _adj_size;
		int* _map_ids_atom;
		int* _chunk_ids_atom;
		int* _chunk_internal_offsets_atom; 

	};

	/**
	 * @brief a thread taks to decompress received count data 
	 */
	struct decompressElem{

		decompressElem(): _len(-1), _data(NULL) {}
		void allocate(int len)
		{
			_len = len;
			_data = new float[len];
		}

		~decompressElem()
		{
			if (_data != NULL)
				delete[] _data;
		}

		int _len;
		float* _data;
	};

	/**
	 * @brief internal data format to hold the input graph
	 */
    struct Graph
    {
        Graph(){
            vert_num_count = 0;
            max_v_id_local = 0;
            max_v_id = 0; //global max_v_id
            adj_len = 0;
            num_edges = 0;
            max_deg = 0;
            vertex_local_ids = NULL; // mapping from absolute v_id to relative v_id
            adj_index_table = NULL; //a table to index adj list for each local vert
            isTemplate = false;
        }

        void initTemplate(int ng, int mg, int*& src, int*& dst);
        void freeMem();
        //copy the graph ( used in dynamic programing of sub-templates)
        Graph& operator= (const Graph& param);

		//get relative neighbor ids of a specified vertex
        int* adjacent_vertices(int v){return &(adj_index_table[v]->_adjs)[0];}
		//get absoluate neighbor ids of a specified vertex
        int* adjacent_vertices_abs(int v)
        {
            //from abs id to rel id
            int rel_id = vertex_local_ids[v];
            if (rel_id >= 0)
                return &(adj_index_table[rel_id]->_adjs)[0];
            else
                return NULL;
        }

		// convert an absolute vertex id to relative vertex id
        int get_relative_v_id(int v_abs){return vertex_local_ids[v_abs];}
		// obtain the out degree (neighbor number) of a specified vertex by its relative id
        int out_degree(int v){ return (adj_index_table[v]->_adjs).size();}
		// obtain the out degree (neighbor number) of a specified vertex by its absolute id
        int out_degree_abs(int v)
        {
            //from abs id to rel id
            int rel_id = vertex_local_ids[v];
            if (rel_id >=0)
                return (adj_index_table[rel_id]->_adjs).size();
            else
                return -1;
        }

		// accessor of max_degree value
        int max_degree(){return max_deg;}
		// accessor of total degree value
		int total_degree(){return total_deg;}
		// get an array of absolute id for local vertices
        int* get_abs_v_ids(){return vertex_ids.get();}
		// get num of vertices of local input graph
        int num_vertices(){ return vert_num_count;}

        int vert_num_count;
        int max_v_id_local;
        int max_v_id; //global max_v_id
        int adj_len;
        int num_edges;
        int max_deg;
		int total_deg;
        services::SharedPtr<int> vertex_ids; // absolute v_id
        int* vertex_local_ids; // mapping from absolute v_id to relative v_id
        v_adj_elem** adj_index_table; //a table to index adj list for each local vert
        bool isTemplate; //to control the copy behaviour for input graph and template files
    };
    
	/**
	 * @brief partition the template into sub-templates used by dynamic programming.  
	 */
    class partitioner {

        public:

            partitioner(){}
            partitioner(Graph& t, bool label, int*& label_map);
			// re-order the partitioned sub-templates
            void sort_subtemplates();
            void clear_temparrays();
			// get the number of sub-templates
            int get_subtemplate_count(){ return subtemplate_count;}
			// accessor to the subtemplate files
            Graph* get_subtemplates(){ return subtemplates; }
			// get number of active children for a sub-template
            int get_num_verts_active(int s){return subtemplates[active_children[s]].vert_num_count;}
			// get number of passive children for a sub-template
            int get_num_verts_passive(int s){return subtemplates[passive_children[s]].vert_num_count;}
            int get_num_verts_sub(int sub) {return subtemplates[sub].vert_num_count;}
            int get_active_index(int a){ return active_children[a];}
            int get_passive_index(int p){return passive_children[p];}

        private:

            void init_arrays(){subtemplates_create = new Graph[create_size];}
            bool sub_count_needed(int s){ return count_needed[s];}

            int* get_labels(int s)
            {
                if (labeled)
                    return label_maps[s];
                else
                    return NULL;
            }
            
            void partition_recursive(int s, int root);
            int* split(int s, int root);
            int split_sub(int s, int root, int other_root);
            void fin_arrays();
            void check_nums(int root, std::vector<int>& srcs, std::vector<int>& dsts, int*& labels, int*& labels_sub);

            void set_active_child(int s, int a)
            {
                while( active_children.size() <= s)
                    active_children.push_back(null_val);

                active_children[s] = a;
            }

            void set_passive_child(int s, int p)
            {
                while(passive_children.size() <= s)
                    passive_children.push_back(null_val);

                passive_children[s] = p;
            }

            void set_parent(int c, int p)
            {
                while(parents.size() <= c )
                    parents.push_back(null_val);

                parents[c]= p;
            }

            Graph* subtemplates_create;
            Graph* subtemplates;
            Graph subtemplate;

            std::vector<int> active_children;
            std::vector<int> passive_children;
            std::vector<int> parents;
            std::vector<int> cut_edge_labels;
            std::vector<int*> label_maps;

            int current_creation_index;
            int subtemplate_count;
            bool* count_needed;
            bool labeled;
    };

    /**
     * @brief store sub-template chains for
     * dynamic programming
     */
    class dynamic_table_array{

        public:

            dynamic_table_array();
            void free();
            void init(Graph*& subtemplates, int num_subtemplates, int num_vertices, int num_colors, int max_abs_vid);
            void init_sub(int subtemplate); 
            void init_sub(int subtemplate, int active_child, int passive_child);
            void clear_sub(int subtemplate); 
            void clear_table(); 
            float get(int subtemplate, int vertex, int comb_num_index);
            float* get_table(int subtemplate, int vertex);
            float get_active(int vertex, int comb_num_index);
            float* get_active(int vertex);
            float* get_passive(int vertex);
			float** get_passive_table();
            float get_passive(int vertex, int comb_num_index);
            void set(int subtemplate, int vertex, int comb_num_index, float count);
            void set(int vertex, int comb_num_index, float count);
            void set_init(int vertex);
            void update_comm(int vertex, int comb_num_index, float count);
            bool is_init(); 
            bool is_sub_init(int subtemplate); 
            bool is_vertex_init_active(int vertex);
            bool is_vertex_init_passive(int vertex);
            int get_num_color_set(int s); 
            void set_to_table(int src, int dst);

        private:

            void init_choose_table();
            void init_num_colorsets();

            int** choose_table;
            int* num_colorsets;

            Graph* subtemplates;

            int num_colors;
            int num_subs;
            int num_verts;

            bool is_inited;
            bool* is_sub_inited;

            float*** table;
            // vertex-colorset
            float** cur_table;
            // vertex-colorset
            float** cur_table_active;
            // vertex-colorset
            float** cur_table_passive;

            int max_abs_vid;
            int cur_sub;

    };

/**
 * <a name="DAAL-CLASS-ALGORITHMS__subgraph__INPUT"></a>
 * \brief Input objects for the subgraph algorithm in the batch and distributed modes 
 * algorithm.
 */
class DAAL_EXPORT Input : public daal::algorithms::Input
{

public:
    /** Default constructor */
    Input();
    /** Default destructor */
    virtual ~Input() {}

    /**
     * Returns input object of the subgraph algorithm
     * \param[in] id    Identifier of the input object
     * \return          Input object that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(InputId id) const;

    /**
     * Sets input object for the subgraph algorithm
     * \param[in] id    Identifier of the input object
     * \param[in] value Pointer to the input object
     */
    void set(InputId id, const data_management::NumericTablePtr &value);

	/**
	 * @brief get the column num of NumericTable associated to an inputid
	 *
	 * @param[in] id of input table
	 * @return column num of input table 
	 */
    size_t getNumberOfColumns(InputId id) const;

	/**
	 * @brief get the column num of NumericTable associated to an inputid
	 *
	 * @param[in]  id of input table
	 *
	 * @return row num of input table 
	 */
    size_t getNumberOfRows(InputId id) const;

    daal::services::interface1::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    // input func for read in data from HDFS
    void readGraph();
    void readTemplate();
    void free_input();

    void init_Graph();
    void init_Template();
    void init_Partitioner();
    void init_DTTable();

    size_t getReadInThd();
    size_t getLocalVNum();
    size_t getLocalMaxV();
    size_t getLocalADJLen();
    size_t getTVNum();
    size_t getTENum();
    void setGlobalMaxV(size_t id);
    size_t getSubtemplateCount();

    // store vert of each template
    int* num_verts_table;
    int subtemplate_count;
    Graph* subtemplates;

    // record comb num values for each subtemplate and each color combination
    int** comb_num_indexes_set;
    // record comb num values for active and passive children 
    int**** comb_num_indexes;
     //stores the combination of color sets
    int** choose_table;

    void create_tables();
    void delete_tables();
    void create_num_verts_table();
    //create color index for each combination of different set size
    void create_all_index_sets();
    //create color sets for all subtemplates
    void create_all_color_sets();
    //convert colorset combination into numeric values (hash function)
    void create_comb_num_system_indexes();
    //free up memory space 
    void delete_all_color_sets();
    //free up memory space
    void delete_all_index_sets();
    void delete_comb_num_system_indexes();

    void sampleGraph();
    int getSubVertN(int sub_itr);

    void initDtSub(int s);
    void clearDtSub(int s);
    void setToTable(int src, int dst);
    
    partitioner* getPartitioner() {return part;}
    dynamic_table_array* getDTTable(){return dt;}
    int* getColorsG() {return colors_g;}

    int getMaxDeg() {return g.max_deg;}
	int getTotalDeg() {return g.total_degree();}

    Graph* getGraphPtr() {return &g; }
    int getColorNum() {return num_colors;}
    int computeMorphism();

	double getPeakMem() {return peak_mem;}
	void resetPeakMem() { peak_mem = 0.0;}

	int getCombLen(int subid) {return dt->get_num_color_set(part->get_passive_index(subid));}

    // for inter-node communication 
    void init_comm(int mapper_num_par, int local_mapper_id_par, long send_array_limit_par, bool rotation_pipeline_par);
    void init_comm_omp_kernel(int mapper_num_par, int local_mapper_id_par, long send_array_limit_par, bool rotation_pipeline_par);
    void init_comm_tbb_kernel(int mapper_num_par, int local_mapper_id_par, long send_array_limit_par, bool rotation_pipeline_par);

    void init_comm_prepare(int update_id);
    void upload_prep_comm();
    void setSendVertexSize(int size);
    void setSendVertexArray(int dst);

    void init_comm_final();
    int sendCommParcelInit(int sub_id, int send_id);
    void sendCommParcelPrep(int parcel_id);
    void sendCommParcelLoad();
    void sendCommParcelLoadOld();

    void updateRecvParcelInit(int comm_id); 
    void updateRecvParcel();
    void updateRecvParcel2();
    void freeRecvParcel();
    void freeRecvParcelPip(int pipId);

    void calculate_update_ids(int sub_id);
    void calculate_update_ids_omp_kernel(int sub_id);
    void calculate_update_ids_tbb_kernel(int sub_id);

    void release_update_ids();

	void clear_task_update_list();
	// retrieve the comb number for each sub-templates
	int getCombCur(int s){
		if(part->get_num_verts_sub(s) == 1) 
			return 0;
		else
			return choose_table[num_colors][part->get_num_verts_sub(s)];
	}

	// retrieve the Active comb number for each sub-templates
	int getCombActiveCur(int s){
		if (part->get_num_verts_sub(s) == 1)
			return 0;
		else
			return choose_table[part->get_num_verts_sub(s)][part->get_num_verts_active(s)];
	}

    int mapper_num;
    int local_mapper_id;
    long send_array_limit;
    bool rotation_pipeline;
    int update_mapper_id;
    long daal_table_size; // -1 means no need to do data copy 
    int* daal_table_int_ptr; // tmp array to hold int data
    float* daal_table_float_ptr; //tmp array to hold float data
    services::SharedPtr<int>* update_map;
    services::SharedPtr<int> update_map_size;

    int cur_sub_id_comm;
    int cur_comb_len_comm;
    long cur_parcel_num;
    std::vector<int>* cur_send_id_data;
    std::vector<int>* cur_send_chunks;
    int cur_parcel_id;
    int cur_parcel_v_num;
    int cur_parcel_count_num;
    int* cur_parcel_v_offset; // v_num+1
    float* cur_parcel_v_counts_data; //count_num
    int* cur_parcel_v_counts_index; //count_num
    int cur_upd_mapper_id;
    int cur_upd_parcel_id;
    int cur_sub_id_compute;
    int cur_comb_len_compute;

	// for inter-node distributed usage
    int send_vertex_array_size;
    std::vector<int> send_vertex_array_dst;
    std::unordered_map<int, std::vector<int> > send_vertex_array;

    services::SharedPtr<int> update_mapper_len;
    services::SharedPtr<int>* map_ids_cache_pip;
    services::SharedPtr<int>* chunk_ids_cache_pip;
    services::SharedPtr<int>* chunk_internal_offsets_cache_pip;
    
    HarpBlockDescriptor<int>** update_queue_pos;
    HarpBlockDescriptor<float>** update_queue_counts;
	decompressElem** update_queue_counts_decompress;
    HarpBlockDescriptor<int>** update_queue_index;

    //for neighborlist partitioning
	std::vector<task_nbr*> task_list;
	std::vector<task_nbr_update*> task_list_update;
 
	// record peak mem usage
	// used in experiments
	double peak_mem;
	double peak_mem_comm;

	//record avg and stdev of thread-level workload
	// used in experiments
	double* thdwork_record;
	double thdwork_avg;
	double thdwork_stdev;
   
private:

    int thread_num;// thread in read in graph
    std::vector<v_adj_elem*>* v_adj; //store data from reading data

    Graph g; // graph data
    Graph t; // template data

    int num_colors; //num of colors used in coloring
    int* colors_g; // color values of local graph vertices
    
    // for template data
    size_t t_ng;
    size_t t_mg;
    std::vector<int> t_src;
    std::vector<int> t_dst;

	// read in data from HDFS
    hdfsFS* fs;
    services::SharedPtr<int> fileOffsetPtr;
    services::SharedPtr<int> fileNamesPtr;

    // table for dynamic programming
    partitioner* part;
    dynamic_table_array* dt;
 
    // temp index sets to construct comb_num_indexes 
    int**** index_sets;
    // temp index sets to construct comb_num_indexes 
    int***** color_sets;

    bool isTableCreated;

    std::vector<int>* comm_mapper_vertex;
    BlockDescriptor<int> abs_v_to_mapper;
    int* abs_v_to_queue;
};


/**
 * <a name="DAAL-CLASS-ALGORITHMS__subgraph__RESULT"></a>
 * \brief Provides methods to access results obtained with the compute() method of the subgraph algorithm
 *        in the batch processing mode 
 *        deprecated
 */
class DAAL_EXPORT Result : public daal::algorithms::Result
{
public:
    /** Default constructor */
    Result();
    /** Default destructor */
    virtual ~Result() {}

    /**
     * Returns the result of the subgraph algorithm
     * \param[in] id    Identifier of the result
     * \return          Result that corresponds to the given identifier
     */
    data_management::NumericTablePtr get(ResultId id) const;

    /**
    * Sets an input object for the subgraph algorithm
    * \param[in] id    Identifier of the result
    * \param[in] value Pointer to the result
    */
    void set(ResultId id, const data_management::NumericTablePtr &value);

    /**
       * Checks final results of the algorithm
      * \param[in] input  Pointer to input objects
      * \param[in] par    Pointer to parameters
      * \param[in] method Computation method
      */
    daal::services::interface1::Status check(const daal::algorithms::Input *input, const daal::algorithms::Parameter *par, int method) const DAAL_C11_OVERRIDE;

	/**
	 * @brief get a serialization tag for result
	 *
	 * @return serilization code  
	 */
    // int getSerializationTag() const DAAL_C11_OVERRIDE  { return SERIALIZATION_SUBGRAPH_RESULT_ID; }

    
protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        daal::algorithms::Result::serialImpl<Archive, onDeserialize>(arch);
        return services::Status();
    }

    /**
    *  Serializes the object
    *  \param[in]  arch  Storage for the serialized object or data structure
    */
    services::Status serializeImpl(data_management::InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(arch);
        return services::Status();
    }

    /**
    *  Deserializes the object
    *  \param[in]  arch  Storage for the deserialized object or data structure
    */
    services::Status deserializeImpl(const data_management::OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(arch);
        return services::Status();
    }

};

/**
 * <a name="DAAL-CLASS-ALGORITHMS__subgraph__RESULT"></a>
 * \brief Provides methods to access results obtained with the compute() method of the subgraph algorithm
 *        in the batch processing mode or finalizeCompute() method of algorithm in the online processing mode
 *        or on the second and third steps of the algorithm in the distributed processing mode
 */
class DAAL_EXPORT DistributedPartialResult : public daal::algorithms::PartialResult
{
public:
    /** Default constructor */
    DistributedPartialResult();
    /** Default destructor */
    virtual ~DistributedPartialResult() {}

    /**
     * Returns the result of the subgraph algorithm 
     * \param[in] id    Identifier of the result
     * \return          Result that corresponds to the given identifier
     */
	data_management::NumericTablePtr get(DistributedPartialResultId id) const;

    /**
     * Sets Result object to store the result of the subgraph algorithm
     * \param[in] id    Identifier of the result
     * \param[in] value Pointer to the Result object
     */
    void set(DistributedPartialResultId id, const data_management::NumericTablePtr &value);


	/**
	 * Checks partial results of the algorithm
	 * \param[in] parameter Pointer to parameters
	 * \param[in] method Computation method
	 */
    daal::services::interface1::Status check(const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

    /**
      * Checks final results of the algorithm
      * \param[in] input      Pointer to input objects
      * \param[in] parameter  Pointer to parameters
      * \param[in] method     Computation method
      */
    daal::services::interface1::Status check(const daal::algorithms::Input* input, const daal::algorithms::Parameter *parameter, int method) const DAAL_C11_OVERRIDE;

	/**
	 * @brief get serilization tag for partial result
	 *
	 * @return serilization code for partial result
	 */
    // int getSerializationTag() const DAAL_C11_OVERRIDE  { return SERIALIZATION_SUBGRAPH_DISTRI_PARTIAL_RESULT_ID;}

    void init_model(int threads);

    // member for counts container for threads
    // must use double to avoid overflow of count num of large datasets/templates
    // len is thread number
    int thread_num;
    double* cc_ato;
    double* count_local_root;
    double* count_comm_root;

    
protected:
    /** \private */
    template<typename Archive, bool onDeserialize>
    services::Status serialImpl(Archive *arch)
    {
        daal::algorithms::PartialResult::serialImpl<Archive, onDeserialize>(arch);
        return services::Status();
    }

    /**
    *  Serializes the object
    *  \param[in]  arch  Storage for the serialized object or data structure
    */
    services::Status serializeImpl(data_management::InputDataArchive  *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<data_management::InputDataArchive, false>(arch);
        return services::Status();
    }

    /**
    *  Deserializes the object
    *  \param[in]  arch  Storage for the deserialized object or data structure
    */
    services::Status deserializeImpl(const data_management::OutputDataArchive *arch) DAAL_C11_OVERRIDE
    {
        serialImpl<const data_management::OutputDataArchive, true>(arch);
        return services::Status();
    }
};

/**
 * <a name="DAAL-STRUCT-ALGORITHMS__subgraph__PARAMETER"></a>
 * \brief Parameters for the subgraph compute method
 * used in both of batch mode and distributed mode
 */
struct DAAL_EXPORT Parameter : public daal::algorithms::Parameter
{

	/* default constructor */
    Parameter() 
    {
        _thread_num = 1;		//  specify used in computation 
        _core_num = 1;			// the core num used in affinity setting
        _tpc = 1;				// the threads per core
        _affinity = 0;			// the affinity (default 0: compact, 1: scatter)
        _verbose = 0;
        _vert_num_sub = 0;		// the vert number of current subtemplate
        _stage = 0;				// 0: bottom subtemplate, 1: non-last subtemplate, 2: last subtemplate
        _sub_itr = 0;
        _pip_id=0;
        _total_counts = 0.0;
        _update_counts = 0.0;
        _count_time = 0.0;
		_nbr_task_len = 0;
    }

    virtual ~Parameter() {}

	/**
	 * @brief set the parameters in both of batch mode and distributed mode 
	 *
	 */
    void setParameter(size_t thread_num, 
                      size_t core_num,
                      size_t tpc,
                      size_t affinity,
					  size_t nbr_task_len,
                      size_t verbose)
    {
        _thread_num = thread_num;
        _core_num = core_num;
        _tpc = tpc;
        _affinity = affinity;
        _verbose = verbose;
		_nbr_task_len = nbr_task_len;
		_omp_schedule = "guided";
    }

    void setStage(size_t stage)
    {
        _stage = stage;
    }

    void setSubItr(size_t sub_itr)
    {
        _sub_itr = sub_itr;
    }

	void setNbrTaskLen(size_t nbr_task_len)
	{
		_nbr_task_len = nbr_task_len;
	}

    void setPipId(size_t pip_id)
    {
        _pip_id = pip_id;
    }

    size_t _thread_num;			//  specify used in computation 
    size_t _core_num;			// the core num used in affinity setting
    size_t _tpc;				// the threads per core
    size_t _affinity;			// the affinity (default 0: compact, 1: scatter)
    size_t _verbose;            // verbose mode to print out more runtime information
    size_t _vert_num_sub;		// the vert number of current subtemplate
    size_t _stage;				// 0: bottom subtemplate, 1: non-bottom subtemplate
    size_t _sub_itr;
    size_t _pip_id;
    double _total_counts;
    double _update_counts;
    double _count_time;
	int _nbr_task_len;
	std::string _omp_schedule;

};

/** @} */
/** @} */
} // namespace interface1

using interface1::Input;
using interface1::Result;
using interface1::DistributedPartialResult;
using interface1::Parameter;

} // namespace daal::algorithms::subgraph
} // namespace daal::algorithms
} // namespace daal

#endif
