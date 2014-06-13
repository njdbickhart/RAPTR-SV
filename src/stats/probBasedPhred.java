/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stats;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author bickhart
 */
public class probBasedPhred {
        public static double calculateScore(String mis_match, String qstr, int lower){
            int i;
            char[] qual = qstr.toCharArray();
            posCounter pi = errorPosition(mis_match);
            double score = 1d; //Default returned score is 1
            int qlen = qual.length;
                    if (pi.numEvents > 1)
                    {
                        for (i=0; i < pi.numEvents; i++)
                        {
                            if(pi.positions.get(i) >= lower || pi.positions.get(i) >= qlen) {
                                break;
                            }
                            int value = qual[pi.positions.get(i)]-33;
                            score *= 0.001d + 1/Math.pow( 10d, (double)((double)(value)/10.0d) ); // This needs the quality score at the position of the error
                            // I will have to find out how to parse out the cigar score to ID this
                            // chars within arrays can be added together and they give their ascii value automatically in C
                        }
                    }
            return score;
    }

    // This function takes the MD:Z:digit tag and returns an array of positions where the read was a mismatch
    // This will enable me to cross reference the quality score of that position of the read in order to calculate the prob-based phred value for the read
    private static posCounter errorPosition(String mm){
            int i = 0;  //iterator for loop
            int pi = 0; //positions index number
            int in = 0; //boolean for values inside pre array
            int con = 0;//holder for converted atoi values
            int pri = 0;//iterator for previous digit arra
            posCounter count = new posCounter();
            String pre = null;
            //char pre[5];//previous digit characters array
            char[] mis_match = mm.toCharArray();

            for (i = 0; i < mis_match.length; i++){
                    if(isalpha(mis_match[i]) && in == 1 && pre != null){
                            con += Integer.parseInt(pre);
                            count.positions.add(con);
                            //positions[pi] = con;
                            con++;
                            //pi++;
                            count.numEvents++;
                            in = 0;
                            pre = null;
                            pri = 0;
                    }else if(isalpha(mis_match[i]) && in == 0){
                        count.positions.add(con);    
                        //positions[pi] = con;
                            con++;
                            pi++;
                    }else if(isdigit(mis_match[i])){
                        if(pre == null){
                            pre = String.valueOf(mis_match[i]);
                        }else{
                            pre = pre + mis_match[i];
                        }
                            //pre[pri] = mis_match[i];
                            pri++;
                            in = 1;
                    }
            }

            if(pre != null){
                con += Integer.parseInt(pre);
                count.positions.add(con);
                    //con += atoi(pre);
                    //positions[pi] = con;
                count.numEvents++;
                    pi++;
            }

            return count;
    }
    private static class posCounter{
        public int numEvents;
        public ArrayList<Integer> positions;
        public posCounter(){
            this.positions = new ArrayList<>();
        }
    }
    private static boolean isalpha(char a){
        boolean isa;
        Pattern p = Pattern.compile("[a-zA-Z]");
        String temp = String.valueOf(a);
        isa = p.matcher(temp).find();
        return isa;
    } 
    private static boolean isdigit(char a){
        boolean isa;
        Pattern p = Pattern.compile("[0-9]");
        String temp = String.valueOf(a);
        isa = p.matcher(temp).find();
        return isa;
    }
}
