/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

import dataStructs.SplitOutputHandle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecordIterator;

/**
 * This program runs MrsFAST and returns the output BAM file name
 * @author bickhart
 */
public class MrsFastRuntimeFactory{
    private final int threads;
    private SAMFileHeader SAMFileHeader;
    
    public MrsFastRuntimeFactory(int numthreads){
        threads = numthreads;
    }
    
    public void ProcessSplitFastqs(Map<String, SplitOutputHandle> handles, String refgenome, String outbase){
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        Map<String, Future<String>> sams = new HashMap<>();
        
        // Process the fastqs in a threaded fashion
        for(String rg : handles.keySet()){
            MrsFastExecutable runner = new MrsFastExecutable(refgenome, handles.get(rg).fq1File(), outbase + "." + rg + ".sam");
            Future<String> temp = ex.submit(runner);
            sams.put(rg, temp);
        }
        
        ex.shutdown();
        while(!ex.isTerminated()){}
        
        // Now convert all of those sams to bams in a threaded fashion
        ex = Executors.newFixedThreadPool(threads);
        SAMFileWriterFactory sfact = new SAMFileWriterFactory();
        for(String rg : handles.keySet()){
            try {
                String samstr = sams.get(rg).get();
                Runnable r = () -> {
                        SAMFileReader reader = new SAMFileReader(new File(samstr));
                        SAMFileHeader head = reader.getFileHeader();
                        SAMFileWriter bam = sfact.makeBAMWriter(head, false, new File(outbase + "." + rg + ".bam"));
                        SAMRecordIterator itr = reader.iterator();
                        while(itr.hasNext()){
                            bam.addAlignment(itr.next());
                        }
                        reader.close();
                        bam.close();
                    };
                ex.submit(r);
            } catch (InterruptedException | ExecutionException ex1) {
                Logger.getLogger(MrsFastRuntimeFactory.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        
        ex.shutdown();
        while(!ex.isTerminated()){}
    }
}
