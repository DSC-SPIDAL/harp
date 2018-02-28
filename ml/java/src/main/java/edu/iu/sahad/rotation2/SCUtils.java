/*
 * This is from VT's SAHAD implementation
 */
package edu.iu.sahad.rotation2;

public class SCUtils {
    public static int power(int a, int b) {
        int x = 1;
        for (int i = 0; i < b; i++) {
            x *= a;
        }
        return x;
    }

    public static double factorial(int n) {
        if (n <= 1) // base case
            return Double.parseDouble(Integer.toString(1));
        else
            return Double.parseDouble(Integer.toString(n)) * factorial(n - 1);
    }

    public static double Prob(int colorNum, int tplSize) {
        return factorial(colorNum) / factorial(colorNum - tplSize)
                / Math.pow(Double.parseDouble(Integer.toString(colorNum)), 
						Double.parseDouble(Integer.toString(tplSize)));

    }
	public static double GetBucket(double num, int bucket){
		double pos = 0;
		while(true){
			if ((num - pos*bucket) < bucket){
				return pos*bucket;
			}
			pos ++;
		}
	}	
	
}
