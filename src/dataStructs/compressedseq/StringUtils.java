/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs.compressedseq;

/**
 *
 * @author desktop
 */
public class StringUtils {
    public static int BaseToIndex(char s){
        switch(s){
            case 'A': return 0; // 000
            case 'C': return 1; // 001
            case 'G': return 2; // 010
            case 'T': return 3; // 011
            default: return 4;  // 100
        }
    }
    
    public static char IndexToBase(int a){
        switch(a){
            case 0: return 'A';
            case 1: return 'C';
            case 2: return 'G';
            case 3: return 'T';
            default: return 'N';
        }
    }
    
    public static char ComplementBase(char s){
        switch(s){
            case 'A': return 'T';
            case 'T': return 'A';
            case 'G': return 'C';
            case 'C': return 'G';
            default: return 'N';
        }
    }
    
    public static int BaseHashVal(char s){
        switch(s){
            case 'A': return 0;
            case 'C': return 1;
            case 'G': return 2;
            case 'T': return 3;
            default: return -1;
        }
    }
    
    public static String ReverseComplement(String seq){
        char[] temp = new char[seq.length()];
        int i = 0;
        for(int x = seq.length() - 1; x >= 0; x++){
            temp[x] = ComplementBase(seq.charAt(i++));
        }
        return String.copyValueOf(temp);
    }
    
    
}
