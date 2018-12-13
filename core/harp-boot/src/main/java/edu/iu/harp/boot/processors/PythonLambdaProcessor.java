package edu.iu.harp.boot.processors;

import jep.Jep;
import jep.JepException;

public class PythonLambdaProcessor extends AbstractHarpProcessor<String> {

    private Jep jep;

    public PythonLambdaProcessor(byte[] lambda) {
        try {
            this.jep = new Jep();
            this.jep.eval("import cloudpickle as cp");
            this.jep.eval("import base64");
            this.jep.set("func_bin", lambda);
            this.jep.eval("print(func_bin[23])");
            this.jep.eval("func = cp.loads(func_bin)");
            this.jep.eval("print(func)");
        } catch (JepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(String data) {
        try {
            Object result = this.jep.invoke("func", data);
            System.out.println(result);
        } catch (JepException e) {
            e.printStackTrace();
        }
    }
}
