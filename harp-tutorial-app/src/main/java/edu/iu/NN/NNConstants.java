package edu.iu.NN;

/*
 * Define some constants you might use
 */
public class NNConstants { 

	public static final String WEIGHTS_FILE = "weights_file";
	public static final String WEIGHTS_VEC_SIZE = "weights_vec_size";
	public static final String INPUT_VEC_SIZE = "input_vec_size";
	public static final String OUTPUT_VEC_SIZE = "output_vec_size";
	public static final String NUM_TASKS = "num_tasks";
	public static final String EPOCHS = "epochs";
    //do model average every ITER_PERSYNC iterations
	public static final String SYNC_ITERNUM = "sync_iternum";
    //define the hidden layers neuron numbers, 
    //in string like 100,32
	public static final String HIDDEN_LAYERS = "hidden_layers";
	public static final String MINIBATCH_SIZE= "minibatch_size";
	public static final String LAMBDA = "lambda";
	
}
