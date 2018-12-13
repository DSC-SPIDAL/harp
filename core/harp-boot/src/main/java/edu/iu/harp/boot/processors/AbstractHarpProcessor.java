package edu.iu.harp.boot.processors;

public abstract class AbstractHarpProcessor<TYPE> {

    private AbstractHarpProcessor next;

    abstract public void process(TYPE data);
}
