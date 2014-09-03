/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructs;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author bickhart
 */
public class readNameMappings {
    private ConcurrentHashMap<String, Integer> readTable;
    
    public readNameMappings(){
        this.readTable = new ConcurrentHashMap<>();
    }
    
    public void addRead(String read){
        if(this.readTable.containsKey(read)){
            this.readTable.put(read, this.readTable.get(read) + 1);
        }else{
            this.readTable.put(read, 1);
        }
    }
    public int retMap(String read){
        if(this.readTable.containsKey(read)){
            return this.readTable.get(read);
        }else{
            return 0;
        }
    }
}
