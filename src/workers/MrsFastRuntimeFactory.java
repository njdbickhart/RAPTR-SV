/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package workers;

import dataStructs.SplitOutputHandle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordFactory;
import htsjdk.samtools.TextCigarCodec;

/**
 * This program runs MrsFAST and returns the output BAM file name
 * @author bickhart
 */
public class MrsFastRuntimeFactory{
    private final int threads;
    private final SAMFileHeader FileHeader;
    private final Map<String, String> bamfiles = new HashMap<>();
    private final Map<String, String> samfiles = new HashMap<>();
    
    private static final Logger log = Logger.getLogger(MrsFastRuntimeFactory.class.getName());
    
    public MrsFastRuntimeFactory(int numthreads, SAMFileHeader head){
        threads = numthreads;
        // Setting sort order to queryname to make the clustering program's job easier
        head.setSortOrder(SAMFileHeader.SortOrder.queryname);
        FileHeader = head;
    }
    
    public void ProcessSplitFastqs(Map<String, SplitOutputHandle> handles, String refgenome, String outbase){
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        Map<String, Future<String>> sams = new HashMap<>();
        
        // Process the fastqs in a threaded fashion
        for(String rg : handles.keySet()){
            MrsFastExecutable runner = new MrsFastExecutable(refgenome, handles.get(rg).fq1File(), outbase + "." + rg + ".sam", rg);
            Future<String> temp = ex.submit(runner);
            sams.put(rg, temp);
        }
        
        ex.shutdown();
        while(!ex.isTerminated()){}
        
        // Now convert all of those sams to bams in a threaded fashion
        //ex = Executors.newFixedThreadPool(threads);
        //SAMFileWriterFactory sfact = new SAMFileWriterFactory();
        for(String rg : handles.keySet()){
            try {
                String samstr = sams.get(rg).get();
                samfiles.put(rg, samstr);
                Runnable r = processStringsToRecords(FileHeader, outbase, rg, samstr);
                r.run();
                
                // This logic should give the absolute path name when creating the flatfile
                bamfiles.put(rg, Paths.get(outbase).toAbsolutePath().toString() + "." + rg + ".bam");
                //ex.submit(r);
            } catch (InterruptedException ex1) {
                log.log(Level.SEVERE, "[MRSFASTFACT] Error with MrsFAST runtime bam creation!", ex1);
            } catch (ExecutionException ex2){
                log.log(Level.SEVERE, ex2.getCause().getMessage(), ex2.getCause());
                log.log(Level.SEVERE, ex2.getMessage(), ex2);
            }
        }
        
        //ex.shutdown();
        //while(!ex.isTerminated()){}
    }
    
    protected Runnable processStringsToRecords(SAMFileHeader header, String outbase, String rg, String samstr){
        Runnable r = () ->{
            SAMRecordFactory recordCreator = new DefaultSAMRecordFactory();
            SAMFileWriterFactory sfact = new SAMFileWriterFactory();
            SAMFileWriter bam = sfact.makeBAMWriter(header, false, new File(outbase + "." + rg + ".bam"));
            try(BufferedReader input = Files.newBufferedReader(Paths.get(samstr), Charset.defaultCharset())){
                String line = null;
                while((line = input.readLine()) != null){
                    line = line.trim();
                    if(line.length() < 10)
                        continue; // This was an empty line from flawed logic
                    String[] segs = line.split("\t");
                    if(segs.length < 9)
                        continue;
                    if(segs[9].length() != segs[10].length())
                        continue; // This was an alignment error.
                    SAMRecord sam = recordCreator.createSAMRecord(header);
                    sam.setReadName(segs[0]);
                    sam.setFlags(Integer.valueOf(segs[1]));
                    sam.setReferenceName(segs[2]);
                    sam.setAlignmentStart(Integer.valueOf(segs[3]));
                    sam.setMappingQuality(Integer.valueOf(segs[4]));
                    sam.setCigar(TextCigarCodec.decode(segs[5]));
                    sam.setMateReferenceName(segs[6]);
                    sam.setMateAlignmentStart(Integer.valueOf(segs[7]));
                    sam.setInferredInsertSize(Integer.valueOf(segs[8]));
                    sam.setReadString(segs[9]);
                    sam.setBaseQualityString(segs[10]);
                    for(int i = 11; i < segs.length; i++){
                        String[] tags = segs[i].split(":");
                        if(StrUtils.NumericCheck.isNumeric(tags[2]) && !tags[0].equals("MD"))
                            sam.setAttribute(tags[0], Integer.parseInt(tags[2]));
                        else if(StrUtils.NumericCheck.isFloating(tags[2]))
                            sam.setAttribute(tags[0], Float.parseFloat(tags[2]));
                        else
                            sam.setAttribute(tags[0], tags[2]);
                    }
                    bam.addAlignment(sam);
                }
                bam.close();
            }catch(IOException ex){
                log.log(Level.SEVERE, "[MRSFASTFACT] Error creating MrsFAST split BAM files!", ex);
            }
        };
        return r;
    }
    
    public Map<String, String> getBams(){
        return this.bamfiles;
    }
    public Map<String, String> getSams(){
        return this.samfiles;
    }
}
