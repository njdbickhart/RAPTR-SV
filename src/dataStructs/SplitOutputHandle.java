/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author bickhart
 */
public class SplitOutputHandle {
    private final Path path;
    
    public SplitOutputHandle(String file){
        path = Paths.get(file);
    }
}
