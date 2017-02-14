package edu.iu.svm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.HashSet;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CollectiveMapper;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import libsvm.*;

public class SVMMapper extends CollectiveMapper<String, String, Object, Object> {
	private int vectorSize;
	private int iteration;
	private String cFile;
    private svm_problem svmProblem;
    private svm_parameter svmParameter;
    private svm_model svmModel;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
        //initialization
		Configuration configuration = context.getConfiguration();
        iteration = configuration.getInt(SVMConstants.NUM_OF_ITERATON, 10);
	}

	protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
		List<String> dataFiles = new ArrayList<String>();
        while (reader.nextKeyValue()) {
            String key = reader.getCurrentKey();
            String value = reader.getCurrentValue();
            dataFiles.add(value);
        }
        Configuration configuration = context.getConfiguration();
        runSVM(configuration, dataFiles, context);
	}
	private void runSVM(Configuration configuration, List<String> dataFiles, Context context) throws IOException {
        //store labels and features
        Vector<Double> vy = new Vector<Double>();
        Vector<svm_node[]> vx = new Vector<svm_node[]>();
        Vector<Double> svy = new Vector<Double>();
        Vector<svm_node[]> svx = new Vector<svm_node[]>();
        int maxIndex = 0;

        //read data from HDFS
        for (String dataFile : dataFiles) {
            FileSystem fs = FileSystem.get(configuration);
            Path dataPath = new Path(dataFile);
            FSDataInputStream in = fs.open(dataPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
                vy.addElement(Double.valueOf(st.nextToken()).doubleValue());
                int m = st.countTokens() / 2;
                svm_node[] x = new svm_node[m];
                for (int i = 0;i < m;i++) {
                    x[i] = new svm_node(); 
                    x[i].index = Integer.parseInt(st.nextToken());
                    x[i].value = Double.valueOf(st.nextToken()).doubleValue();
                }
                if (m > 0) maxIndex = Math.max(maxIndex,x[m - 1].index);
                vx.addElement(x);
            }
            br.close();
        }

        HashSet<String> originTrainingData = new HashSet<String>();
        for (int i = 0;i < vy.size();i++) {
            String line = "";
            line += vy.get(i) + " ";
            for (int j = 0;j < vx.get(i).length - 1;j++) {
                line += vx.get(i)[j].index + ":" + vx.get(i)[j].value + " ";
            }
            line += vx.get(i)[vx.get(i).length - 1].index + ":" + vx.get(i)[vx.get(i).length - 1].value;
            originTrainingData.add(line);
        }

        //initial svm paramter
        svmParameter = new svm_parameter();
        svmParameter.svm_type = svm_parameter.C_SVC;
        svmParameter.kernel_type = svm_parameter.RBF;
        svmParameter.degree = 3;
        svmParameter.gamma = 0;
        svmParameter.coef0 = 0;
        svmParameter.nu = 0.5;
        svmParameter.cache_size = 100;
        svmParameter.C = 1;
        svmParameter.eps = 1e-3;
        svmParameter.p = 0.1;
        svmParameter.shrinking = 1;
        svmParameter.probability = 0;
        svmParameter.nr_weight = 0;
        svmParameter.weight_label = new int[0];
        svmParameter.weight = new double[0];

        HashSet<String> supportVectors = new HashSet<String>();
        HashSet<String> currentTrainingData = new HashSet<String>();

		for (int iter = 0;iter < iteration;iter++) {
            currentTrainingData = originTrainingData;
            for (String line : supportVectors) {
                if (!currentTrainingData.contains(line)) {
                    currentTrainingData.add(line);
                }
            }

            //initial svm problem
            svmProblem = new svm_problem();
            svmProblem.l = currentTrainingData.size();
            svmProblem.x = new svm_node[svmProblem.l][];
            svmProblem.y = new double[svmProblem.l];
            int id = 0;
            for (String line : currentTrainingData) {
                StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");
                svmProblem.y[id] = Double.valueOf(st.nextToken()).doubleValue();
                int m = st.countTokens() / 2;
                svm_node[] x = new svm_node[m];
                for (int i = 0;i < m;i++) {
                    x[i] = new svm_node(); 
                    x[i].index = Integer.parseInt(st.nextToken());
                    x[i].value = Double.valueOf(st.nextToken()).doubleValue();
                }
                svmProblem.x[id] = x;
                id++;
            }

            //compute model
            svmModel = svm.svm_train(svmProblem, svmParameter);

            Table<HarpString> svTable = new Table(0, new HarpStringPlus());

            HarpString harpString = new HarpString();
            harpString.s = "";
            for (int i = 0;i < svmModel.l;i++) {
                harpString.s += svmProblem.y[svmModel.sv_indices[i] - 1] + " ";
                for (int j = 0;j < svmModel.SV[i].length - 1;j++) {
                    harpString.s += svmModel.SV[i][j].index + ":" + svmModel.SV[i][j].value + " ";
                }
                harpString.s += svmModel.SV[i][svmModel.SV[i].length - 1].index + ":" + svmModel.SV[i][svmModel.SV[i].length - 1].value + "\n";
            }
            Partition<HarpString> pa = new Partition<HarpString>(0, harpString);
            svTable.addPartition(pa);

			allreduce("main", "allreduce_" + iter, svTable);

            supportVectors = new HashSet<String>();
            String[] svString = svTable.getPartition(0).get().get().split("\n");
            for (String line : svString) {
                if (!supportVectors.contains(line)) {
                    supportVectors.add(line);
                }
            }
		}

        //output support vectors
		if (this.isMaster()) {
			outputResults(configuration, context, supportVectors);
		}
	}

    private void outputResults(Configuration configuration, Context context, HashSet<String> supportVectors) {
        String outputString = "";
        for (String line : supportVectors) {
            outputString += line + "\n";
        }
        try {
            context.write(null, new Text(outputString));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}