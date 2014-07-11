/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package stats;

/**
 *
 * @author bickhart
 */
public class ReadNameUtility {
    
    public String GetCloneName(String readName, int readFlags){
        String name = null;
        if(readFlags == 0 || readFlags == 16){
            // Inferred MrsFast mode or unmapped BWA read
            if(readName.matches(".+[_/][12]$")){
                name = readName.substring(0, readName.length() - 2);
            }else
                name = readName; // Turns out it was a BWA unmapped read
        }else{
            if(readName.matches(".+[_/][12]$")){
                name = readName.substring(0, readName.length() - 2);
            }else
                return readName;
        }
        return name;
    }
    
    public short GetCloneNum(String readName, int readFlags){
        short num = 0;
        if(readFlags == 0 || readFlags == 16){
            // Inferred MrsFast mode or unmapped BWA read
            if(readName.matches(".+[_/][12]$")){
                String[] nameSplit = readName.split("[/_]");
                num = Short.valueOf(nameSplit[1]);
            }else{
                // It was an unmapped BWA read afterall
                if((readFlags & 0x80) == 0x80)
                    num = 2;
                else
                    num = 1;
            }
        }else{
            if((readFlags & 0x40) == 0x40)
                num = 1;
            else if((readFlags & 0x80) == 0x80)
                num = 2;
        }
        return num;
    }
}
