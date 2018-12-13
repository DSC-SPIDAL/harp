package edu.iu.harp.boot.python;

import edu.iu.harp.boot.processors.PythonLambdaProcessor;
import edu.iu.harp.boot.python.io.filepointers.HDFSFilePointer;
import edu.iu.harp.boot.python.io.filepointers.LocalFilePointer;
import edu.iu.harp.boot.python.ml.MLKernels;
import jep.DirectNDArray;
import jep.Jep;
import jep.JepConfig;
import jep.JepException;

import java.nio.ByteBuffer;

public class HarpSession {

    private String name;
    private MLKernels ml;

    public HarpSession(String name) {
        this.name = name;
        this.ml = new MLKernels(this);
    }

    public MLKernels ml() {
        return ml;
    }

    public String getName() {
        return name;
    }

    public LocalFilePointer fromLocalFile(String absoluteFilePath) {
        return new LocalFilePointer(absoluteFilePath);
    }

    public HDFSFilePointer fromHDFS(String absoluteFilePath) {
        return new HDFSFilePointer(absoluteFilePath);
    }

    public String getSessionRootPath() {
        return "/harp/" + this.name + "/";
    }

    public void byteTest(byte[] arr) {
        try (Jep jep = new Jep(new JepConfig().addSharedModules("numpy"))) {
            System.out.println(Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory());
            jep.eval("import cloudpickle");
            jep.eval("import numpy as np");
            long t1 = System.currentTimeMillis();
            jep.set("x", arr);
            jep.eval("y = cloudpickle.loads(x[0])");
            jep.eval("print(x[0])");
            System.out.println("Took : " + (System.currentTimeMillis() - t1));
            System.out.println(Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory());
        } catch (JepException jex) {
            jex.printStackTrace();
            System.out.println("Jep exception occurred");
        }
    }

    public void calc(byte[] func, String str) {
        PythonLambdaProcessor pythonLambdaProcessor = new PythonLambdaProcessor(func);
        pythonLambdaProcessor.process(str);
    }

    public void close() {
        //do cleanup
    }
}
