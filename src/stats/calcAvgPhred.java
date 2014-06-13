/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

/**
 *
 * @author bickhart
 */
public class calcAvgPhred {
    public static double calcAvgPhred(String fstr, String rstr){
	int i = 0;
	double avg1 = 0.0d;
	double avg2 = 0.0d;
        char[] fqual = fstr.toCharArray();
        char[] rqual = rstr.toCharArray();
	
	for (i = 0; i < fqual.length; i++){
		avg1 += (double)fqual[i];
	}
        avg1 /= (double)i;
        avg1 -= (double)33;
	    
        for (i = 0; i < rqual.length; i++){
            avg2 += (double) rqual[i];
        }
        avg2 /= (double) i;
        avg2 -= (double) 33;
		
        return ((avg1 + avg2) / 2);
    }
    public static double calcAvgPhred(String fstr){
        int i = 0;
        double avg1 = 0.0d;
        char[] fqual = fstr.toCharArray();
        for(i = 0; i < fqual.length; i++){
            avg1 += (double)fqual[i];
        }
        avg1 /= (double) i;
        avg1 -= (double) 33;
        
        return avg1;
    }
}
