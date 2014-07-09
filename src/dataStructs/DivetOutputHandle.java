/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.sf.samtools.SAMRecord;

/**
 * Prints out Divet File information from SAMRecords
 * @author bickhart
 */
public class DivetOutputHandle {
    private final Path path;
    
    public DivetOutputHandle(String file){
        path = Paths.get(file);
    }
    
    // Divet file format:
    // 20VQ5P1:104:D09KFACXX:7:2106:7435:121010:0      chr27   23699238        23699288        F       23695946        23695996        F       delinv  0       37.47  1.00000000000000000000   1
    
    
}
