/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Prints out Divet File information from SAMRecords
 * @author bickhart
 */
public class DivetOutputHandle {
    private final Path path;
    private BufferedWriter out;
    private boolean fileopen = false;
    
    public DivetOutputHandle(String file){
        path = Paths.get(file);
    }
    
    public void OpenHandle(){
        try{
            out = Files.newBufferedWriter(path, Charset.defaultCharset());
        }catch(IOException ex){
            ex.printStackTrace();
        }
        fileopen = true;
    }
    
    public void CloseHandle(){
        try{
            if(fileopen)
                out.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        fileopen = false;
    }
    
    public synchronized void PrintDivetOut(ArrayList<divet> d){
        try{
            
            for(divet a : d){
                out.write(StrUtils.StrArray.Join(a.divout, "\t"));
                out.newLine();
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }
        
    public boolean fileIsOpen(){
        return fileopen;
    }
    
    public String getDivetFileStr(){
        return this.path.toAbsolutePath().toString();
    }
}
