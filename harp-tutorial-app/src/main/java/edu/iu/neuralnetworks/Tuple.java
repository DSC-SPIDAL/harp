// package org.dvincent1337.neuralNet;
package edu.iu.neuralnetworks;

public class Tuple<FIRST ,SECOND>
{
	/*
	 * This class was written by thomasjungblut, and a few modifications were made.
	 * 	the original code found at: https://github.com/thomasjungblut/
	 * 
	 * [David Vincent] Changes Made:
	 * 	1)removed superclass;
	 */
	private final FIRST first;
	private final SECOND second;

	public Tuple(FIRST first, SECOND second)
	{
	    this.first = first;
	    this.second = second;
	}

	public final FIRST getFirst() 
	{
	  return first;
	}

	public final SECOND getSecond()
	{
	  return second;
	}

	  
	public int hashCode() 
	{
		  final int prime = 31;
		  int result = 1;
		  result = prime * result + ((first == null) ? 0 : first.hashCode());
		  return result;
	}

	
	public boolean equals(Object obj) 
	{
	  if (this == obj)
		  return true;
	  if (obj == null)
		  return false;
	  if (getClass() != obj.getClass())
		  return false;
    @SuppressWarnings("rawtypes")
	   Tuple other = (Tuple) obj;
	   if (first == null) 
	   {
	    	if (other.first != null)
	    		return false;
	   } 
	   else if (!first.equals(other.first))
	   	return false;
	    return true;
	}

	@SuppressWarnings("unchecked")
	public int compareTo(Tuple<FIRST, SECOND> o) 
	{
		if (o.getFirst() instanceof Comparable && getFirst() instanceof Comparable) 
	    {
	    	return ((Comparable<FIRST>) getFirst()).compareTo(o.getFirst());
	    } 
	    else 
	    {
	    	return 0;
	    }
	}

	@Override
	public String toString() 
	{
		return "Tuple [first=" + first + ", second=" + second + "]";
	}
}
