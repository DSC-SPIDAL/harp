package com.rf.fast;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.lang.System;

public class DescribeTrees {
  //method to take the txt fle as input and pass those values to random forests
	BufferedReader BR = null;
	String path;
	String layout;
	Random rand;

	public DescribeTrees() {

		rand = new Random(System.currentTimeMillis());

	}

	public DescribeTrees(String path, String layout){
		this.path=path;
		this.layout = layout;
	}

	public void readDataPoints(ArrayList<ArrayList<String>> DataInput, String path) {

		try {

			String sCurrentLine;
			BR = new BufferedReader(new FileReader(path));

			while ((sCurrentLine = BR.readLine()) != null) {
				//System.out.println("Reading a line from the file...");
				//System.out.println(sCurrentLine);
				// This holds the index of all commas in the data line from the input
				// file
				ArrayList<Integer> Sp=new ArrayList<Integer>();int i;
				// Pad the data line with commas on both ends and then find the location
				// of each comma so that feature values can be grabbed based on the
				// characters that fall between the commas. Within this function, the
				// data is represented as a list of strings. Even the number based
				// features are represented as strings.
				if(sCurrentLine!=null){
					if(sCurrentLine.indexOf(",")>=0){
						//has comma

						// Pad the line with commas at the start and the end of the comma
						// separated line of values
						sCurrentLine=","+sCurrentLine+",";
						// Convert type so able to iterate through
						char[] c =sCurrentLine.toCharArray();
						// Find the indices of all of the comma characters
						for(i=0;i<sCurrentLine.length();i++){
							if(c[i]==',')
								Sp.add(i);
						}
						ArrayList<String> DataPoint=new ArrayList<String>();
						// Move through the data string using the location of the commas
						// as guides to where individual pieces of attribute data start
						// and end
						for(i=0;i<Sp.size()-1;i++){
							// Get the data that falls between this comma and the next comma
							DataPoint.add(sCurrentLine.substring(Sp.get(i)+1, Sp.get(i+1)).trim());
						}
						// Add the data point to the list of data strings.
						// All features are treated as strings here, even the number of
						// features
						//System.out.println(DataPoint);
						DataInput.add(DataPoint);
					}
					// Split each data line in the data file where there are white spaces.
					// This is the same method used above for commas.
					// The location of all white spaces are found, and data elements are
					// add to the list of string elements based on what falls between
					// two subsequent white spaces.
					else if(sCurrentLine.indexOf(" ")>=0){
						//has spaces
						sCurrentLine=" "+sCurrentLine+" ";
						for(i=0;i<sCurrentLine.length();i++){
							if(Character.isWhitespace(sCurrentLine.charAt(i)))
								Sp.add(i);
						}ArrayList<String> DataPoint=new ArrayList<String>();
						for(i=0;i<Sp.size()-1;i++){
							DataPoint.add(sCurrentLine.substring(Sp.get(i), Sp.get(i+1)).trim());
						}DataInput.add(DataPoint);//System.out.println(DataPoint);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (BR != null)BR.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	public ArrayList<ArrayList<String>> DoBootStrapSampling(ArrayList<ArrayList<String>> allData) {

		int size = (int) (0.7 * allData.size()), maxSize = allData.size(), i, index;
		//int i;

		// This array list represents the data points that have not yet been sampled
		ArrayList<Integer> available = new ArrayList<Integer>();
		// Populate available with the indices of data points in allData
		for (i=0; i < allData.size(); i++) { available.add(i); }

		// The subset of the data that will be passed to a mapper for it to build
		// a subset of the random forest classifier from
		ArrayList<ArrayList<String>> newSample = new ArrayList<ArrayList<String>>();

		for(i = 0; i < size; i ++) {
			// Randomly selet a data point by randomly selecting an index from available
			index = rand.nextInt(available.size());
			//newSample.add(allData.get(rand.nextInt(maxSize)));
			// Add the data point referred to at index of available
			newSample.add(allData.get(available.get(index)));
			// Remove the value in avaible that points to the data point in allData
			// referenced at index in available
			available.remove(index);
		}

		available = null;


		return newSample;

	}

	public void createMapperFiles(ArrayList<ArrayList<String>> trainSampleFile, ArrayList<ArrayList<String>> testData, FileSystem fs,  String localDirStr, int i) throws IOException {		

		//Write data to localDir
			try
			{
				String filename =Integer.toString(i);
				// Create the file
				File file = new File(localDirStr + File.separator + "data_" + filename);
				// Create the objects that will be used to write the data to the file
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);

				//write Train file
				for(ArrayList<String> dataPoint: trainSampleFile) {
					int position = dataPoint.size();
					for(String value: dataPoint) {
						if(position == 1){
							 // Write the point to the file
							 bw.write(value+"");
							 // Move ontp the next line, ie. the next point
							 bw.newLine();
						 }else{
							 // Write the point to the file
							 bw.write(value+" ");
						 }
						 position --;
					}
				}

				//empty line separating train and test data
				bw.newLine();

				//write the test file
				for(ArrayList<String> dataPoint: testData) {
					int position = dataPoint.size();
					for(String value: dataPoint) {
						if(position == 1){
							 // Write the point to the file
							 bw.write(value+"");
							 // Move ontp the next line, ie. the next point
							 bw.newLine();
						 }else{
							 // Write the point to the file
							 bw.write(value+" ");
						 }
						position --;
					}
				}

				// Close the writer
				bw.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}		
	}

		public ArrayList<ArrayList<String>> CreateInputCateg(String layout, String... path){

			ArrayList<ArrayList<String>> DataInput = new ArrayList<ArrayList<String>>();
			int i;
			for(i = 0; i< path.length; i ++) {
				readDataPoints(DataInput,path[i]);
			}

			/**
			 * checking with the layout parameters
			 *
			 */
			char[] datalayout = CreateLayout(layout);
			if(datalayout.length!=DataInput.get(0).size()){
				System.out.print("Data Layout is incorrect. "+datalayout.length+" "+DataInput.get(0).size());
				return null;
			}else{
				ArrayList<Character> FinalPin = new ArrayList<Character>();
				for(char c:datalayout)
					FinalPin.add(c);
				for(i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='I'){
						FinalPin.remove(i);
						for(ArrayList<String>DP:DataInput)
							DP.remove(i);
						i=i-1;
					}
				}
				for(i=0;i<FinalPin.size();i++){
					if(FinalPin.get(i)=='L'){
						for(ArrayList<String> DP:DataInput){
							String swap = DP.get(i);
							DP.set(i, DP.get(DP.size()-1));
							DP.set(DP.size()-1, swap);
						}
					}break;
				}
				FinalPin = null;
			}
		return DataInput;
	}

	/**
	 * Breaks the run length code for data layout
	  	 * N-Nominal/Number/Real
		 * C-Categorical/Alphabetical/Numerical
		 * I-Ignore Attribute
		 * L-Label - last of the fields
	 *
	 * @param dataInfo
	 * @return
	 */
	public char[] CreateLayout(String dataIn){
		char[] lay =dataIn.trim().toCharArray();
		ArrayList<Character> layo = new ArrayList<Character>();
		ArrayList<Character> LaySet = new ArrayList<Character>();
		LaySet.add('N');LaySet.add('C');LaySet.add('I');LaySet.add('L');
		ArrayList<Integer> number = new ArrayList<Integer>();
		for(char ch:lay){
			if(ch!=',')
				layo.add(ch);
		}
		lay = new char[layo.size()];
		for(int i=0;i<layo.size();i++)
			lay[i] = layo.get(i);
		layo.clear();
		for(int i=0;i<lay.length;i++){
			if(LaySet.contains(lay[i])){
				if(convertonumb(number)<=0)
				layo.add(lay[i]);
				else{
					for(int j=0;j<convertonumb(number);j++){
						layo.add(lay[i]);
					}
				}
				number.clear();
			}
			else
				number.add(Character.getNumericValue(lay[i]));
		}
		lay = new char[layo.size()];
		for(int i=0;i<layo.size();i++)
			lay[i] = layo.get(i);
		return lay;
	}

	/**
	 * converts arraylist to numbers
	 *
	 * @param number
	 * @return
	 */
	private int convertonumb(ArrayList<Integer> number) {
		// TODO Auto-generated method stub
		int numb=0;
		if(number!=null){
			for(int ij=0;ij<number.size();ij++){
				numb=numb*10+number.get(ij);
			}
		}
		return numb;
	}

	/**
	 * Creates final list that Forest will use as reference...
	 * @param dataIn
	 * @return
	 */
	public ArrayList<Character> CreateFinalLayout(String dataIn) {
		// "N 3 C 2 N C 4 N C 8 N 2 C 19 N L I";
					char[] lay =dataIn.toCharArray();
					ArrayList<Character> layo = new ArrayList<Character>();
					ArrayList<Character> LaySet = new ArrayList<Character>();
					LaySet.add('N');LaySet.add('C');LaySet.add('I');LaySet.add('L');
					ArrayList<Integer> number = new ArrayList<Integer>();
					for(char ch:lay){
						if(ch!=',')
							layo.add(ch);
					}
					lay = new char[layo.size()];
					for(int i=0;i<layo.size();i++)
						lay[i] = layo.get(i);
					layo.clear();
					for(int i=0;i<lay.length;i++){
						if(LaySet.contains(lay[i])){
							if(convertonumb(number)<=0)
							layo.add(lay[i]);
							else{
								for(int j=0;j<convertonumb(number);j++){
									layo.add(lay[i]);
								}
							}
							number.clear();
						}
						else
							number.add(Character.getNumericValue(lay[i]));
					}
					for(int i=0;i<layo.size();i++)
						if(layo.get(i)=='I'||layo.get(i)=='L'){
							layo.remove(i);
							i=i-1;
						}
					layo.add('L');
					System.out.println("Final Data Layout Parameters (DescribeTrees.java ln 230)"+layo);
					return layo;
	}
}
