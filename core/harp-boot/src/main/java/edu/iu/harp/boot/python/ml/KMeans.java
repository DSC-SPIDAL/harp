package edu.iu.harp.boot.python.ml;

import edu.iu.fileformat.MultiFileInputFormat;
import edu.iu.harp.boot.python.HarpSession;
import edu.iu.harp.boot.python.io.filepointers.AbstractFilePointer;
import edu.iu.harp.boot.python.io.filepointers.HDFSFilePointer;
import edu.iu.harp.boot.python.io.filepointers.LocalFilePointer;
import edu.iu.harp.boot.python.io.util.FileSynchronizer;
import edu.iu.kmeans.regroupallgather.Constants;
import edu.iu.kmeans.regroupallgather.KMeansCollectiveMapper;
import edu.iu.kmeans.regroupallgather.KMeansLauncher;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class KMeans {

    private int clusters;
    private int mappers;
    private int iterations;

    private HarpSession harpSession;

    public KMeans(HarpSession harpSession) {
        this.harpSession = harpSession;
    }

    public void setClusters(int clusters) {
        this.clusters = clusters;
    }

    public void setMappers(int mappers) {
        this.mappers = mappers;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String fit(AbstractFilePointer filePointer) throws IOException, ClassNotFoundException, InterruptedException {
        return this.fit(Collections.singletonList(filePointer));
    }

    public String fit(List<AbstractFilePointer> inputFiles) throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Starting fit");

        int numOfDataPoints = 1000;
        int numPointFiles = 10;
        int vectorSize = 10;
        int numThreads = 2;


        Configuration configuration = new Configuration();
        configuration.set("fs.defaultFS", "hdfs://localhost:9010");

        FileSystem fileSystem = FileSystem.get(configuration);
        LocalFileSystem localFileSystem = FileSystem.getLocal(configuration);

        FileSynchronizer fileSynchronizer = new FileSynchronizer(this.harpSession, localFileSystem, fileSystem);

        List<LocalFilePointer> localInputFiles = inputFiles.stream()
                .filter(AbstractFilePointer::isLocal)
                .map(fp -> (LocalFilePointer) fp).collect(Collectors.toList());

        List<HDFSFilePointer> hdfsFilePointers = fileSynchronizer.syncToHdfs(localInputFiles);


        System.out.println(fileSystem.exists(new HDFSFilePointer("/tmp").getPath()));
        System.out.println(localFileSystem.exists(new LocalFilePointer("/tmp").getPath()));


        Job job = Job.getInstance(configuration, "kmeans_job");

        Path inputPaths[] = new Path[inputFiles.size()];

        FileInputFormat.setInputPaths(job, hdfsFilePointers.stream().map(HDFSFilePointer::getPath).collect(Collectors.toList()).toArray(inputPaths));

        FileUtils.deleteDirectory(new File("/tmp/kmeans/out/"));
        FileOutputFormat.setOutputPath(job, new Path("/tmp/kmeans/out/"));

        job.setInputFormatClass(MultiFileInputFormat.class);


        job.setJarByClass(KMeansLauncher.class);
        job.setMapperClass(KMeansCollectiveMapper.class);

        JobConf jobConf = (JobConf) job.getConfiguration();
        jobConf.set("mapreduce.framework.name", "map-collective");
        jobConf.setNumMapTasks(this.mappers);
        job.setNumReduceTasks(0);
        Configuration jobConfig = job.getConfiguration();

        jobConfig.setInt(Constants.POINTS_PER_FILE, numOfDataPoints / numPointFiles);
        jobConfig.setInt(Constants.NUM_CENTROIDS, this.clusters);
        jobConfig.setInt(Constants.VECTOR_SIZE, vectorSize);
        jobConfig.setInt(Constants.NUM_MAPPERS, this.mappers);
        jobConfig.setInt(Constants.NUM_THREADS, numThreads);
        jobConfig.setInt(Constants.NUM_ITERATIONS, this.iterations);
        jobConfig.set(Constants.CEN_DIR, "/tmp/kmeans/centroids");

        boolean success = job.waitForCompletion(true);

        System.out.println(success);
        return "/tmp/kmeans/out";
    }

    public void predict() {

    }

    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        KMeans kMeans = new KMeans(new HarpSession("my_session"));
        kMeans.setClusters(10);
        kMeans.setIterations(10);
        kMeans.setMappers(10);

        LocalFilePointer localFilePointer = new LocalFilePointer("/tmp/kmeans/data_0");

        kMeans.fit(localFilePointer);
    }
}
