/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package modes;

import GetCmdOpt.SimpleModeCmdLineParser;
import dataStructs.DivetOutputHandle;
import dataStructs.SamRecordMatcher;
import dataStructs.SplitOutputHandle;
import file.BedSimple;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;
import workers.BamMetadataGeneration;
import workers.MrsFastRuntimeFactory;

/**
 *
 * @author bickhart
 */
public class PreprocessMode {
    private boolean checkRG = false;
    private final String outbase;
    private int samplimit = 1000;
    private int maxdist = 100000;
    private final String input;
    private int threads = 1;
    private final String reference;
    private final boolean debug;
    
    public PreprocessMode(SimpleModeCmdLineParser values){
        outbase = values.GetValue("output");
        input = values.GetValue("input");
        reference = values.GetValue("reference");
        
        // Optional values
        if(values.HasOpt("checkRG"))
            if(values.GetValue("checkRG").equals("true"))
                checkRG = true;
            
        if(values.HasOpt("threads"))
            threads = Integer.parseInt(values.GetValue("threads"));
        
        if(values.HasOpt("maxdist"))
            maxdist = Integer.parseInt(values.GetValue("maxdist"));
        
        if(values.HasOpt("samplimit"))
            samplimit = Integer.parseInt(values.GetValue("samplimit"));
        
        debug = values.GetValue("debug").equals("true");
    }
    
    public void run(){
        // Generate BAM file metadata class
        BamMetadataGeneration metadata = new BamMetadataGeneration(checkRG);
        metadata.ScanFile(input, samplimit);
        
        final Map<String, Integer[]> values = metadata.getThresholds(maxdist);
        Map<String, DivetOutputHandle> divets = metadata.generateDivetOuts(outbase);
        Map<String, SplitOutputHandle> splits = metadata.generateSplitOuts(outbase);
        
        System.err.println("[PREPROCESS] Read input file and calculated sample thresholds.");
        /*metadata.getSampleIDs().stream().forEach((s) -> {
        System.err.println("Sample: " + s + " Avg Ins size: " + metadata.getSampleInsSize(s) +
        " Stdev Ins size: " + metadata.getSampleInsStd(s));
        });*/
        
        for(String s : metadata.getSampleIDs()){
            System.err.println("Sample: " + s + " Avg Ins size: " + metadata.getSampleInsSize(s) +
                    " Stdev Ins size: " + metadata.getSampleInsStd(s));
        }
        
        // Run through the BAM file generating split and divet data
        final SAMFileReader reader = new SAMFileReader(new File(input));
        reader.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        
        SAMFileHeader h = reader.getFileHeader();
        List<BedSimple> coords = this.getSamIntervals(h);
        
        /*List<SamRecordMatcher> collect = coords.parallelStream()
        .map((b) -> {
        SamRecordMatcher w = new SamRecordMatcher(samplimit, checkRG, utilities.GetBaseName.getBaseName(outbase) + ".tmp", values, debug);
        try{
        SAMFileReader temp = new SAMFileReader(new File(input));
        temp.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        SAMRecordIterator itr = temp.queryContained(b.Chr(), b.Start(), b.End());
        itr.forEachRemaining((k) -> w.bufferedAdd(k));
        temp.close();
        System.out.print("                                                                                        \r");
        System.out.print("[SAMRECORD] Working on SAM chunk: " + b.Chr() + "\t" + b.Start() + "\t" + b.End() + "\r");
        }catch(Exception ex){
        System.err.println("[SAMRECORD] Error with SAM query for: " + b.Chr() + "\t" + b.Start() + "\t" + b.End());
        ex.printStackTrace();
        }
        return w;
        }).collect(Collectors.toList());*/
                //.reduce(new SamRecordMatcher(samplimit, checkRG, outbase + ".tmp", values, debug), (SamRecordMatcher a, SamRecordMatcher b) -> {a.combineRecordMatcher(b); return a;});
        
        List<SamRecordMatcher> collect = new ArrayList<SamRecordMatcher>();
        for(BedSimple b : coords){
            SamRecordMatcher w = new SamRecordMatcher(samplimit, checkRG, utilities.GetBaseName.getBaseName(outbase) + ".tmp", values, debug);
                try{
                    SAMFileReader temp = new SAMFileReader(new File(input));
                    temp.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
                    SAMRecordIterator itr = temp.queryContained(b.Chr(), b.Start(), b.End());
                    //itr.forEachRemaining((k) -> w.bufferedAdd(k));
                    while(itr.hasNext()){
                        w.bufferedAdd(itr.next());
                    }
                    temp.close();
                    System.out.print("                                                                                        \r");
                    System.out.print("[SAMRECORD] Working on SAM chunk: " + b.Chr() + "\t" + b.Start() + "\t" + b.End() + "\r");
                }catch(Exception ex){
                    System.err.println("[SAMRECORD] Error with SAM query for: " + b.Chr() + "\t" + b.Start() + "\t" + b.End());
                    ex.printStackTrace();
                }
                collect.add(w);
        }
        
        SamRecordMatcher worker = new SamRecordMatcher(samplimit, checkRG, utilities.GetBaseName.getBaseName(outbase) + ".tmp", values, debug);
        /*collect.stream().forEachOrdered((s) -> {
        worker.combineRecordMatcher(s);
        });*/
        for(SamRecordMatcher s : collect){
            worker.combineRecordMatcher(s);
        }
        reader.close();
        System.out.println(System.lineSeparator() + "[PREPROCESS] Working on Variant calling from SAM data.");
        /*SamRecordMatcher worker = new SamRecordMatcher(samplimit, checkRG, outbase + "_tmp_", values, debug);
        SAMRecordIterator itr = reader.iterator();
        while(itr.hasNext()){
        SAMRecord s;
        try{
        s = itr.next();
        }catch(SAMFormatException ex){
        // this should ignore sam validation errors for crap reads
        System.err.println(ex.getMessage());
        continue;
        }
        worker.bufferedAdd(s);
        }
        itr.close();*/
        
        SAMFileReader next = new SAMFileReader(new File(input));
        next.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        worker.convertToVariant(divets, splits);
        worker.RetrieveMissingAnchors(splits, next.iterator());
        
        System.err.println("[PREPROCESS] Generated initial split and divet data.");
        // Run MrsFAST on the split fastqs and generate bam files
        MrsFastRuntimeFactory mfact = new MrsFastRuntimeFactory(threads, metadata.getSamFileHeader());
        mfact.ProcessSplitFastqs(splits, reference, outbase);
        Map<String, String> bams = mfact.getBams();
        
        // Capture all output into a flatfile format and prepare to print it
        try(BufferedWriter out = Files.newBufferedWriter(Paths.get(outbase + ".flat"), Charset.defaultCharset())){
            for(String s : values.keySet()){
                out.write(bams.get(s) + "\t" + divets.get(s).getDivetFileStr() + "\t");
                out.write(splits.get(s).getAnchorFileStr() + "\t" + String.valueOf(metadata.getSampleInsSize(s)) + "\t");
                out.write(String.valueOf(metadata.getSampleInsStd(s)));
                out.newLine();                
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        System.err.println("[PREPROCESS] Cleaning up temporary files...");
        /*splits.keySet().stream().forEach((s) -> {
        try{
        Files.deleteIfExists(Paths.get(splits.get(s).fq1File()));
        }catch(IOException ex){
        ex.printStackTrace();
        }
        });*/
        for(String s : splits.keySet()){
            try{
                Files.deleteIfExists(Paths.get(splits.get(s).fq1File()));
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
        
        /*mfact.getSams().keySet().stream().forEach((s) -> {
        try{
        Files.deleteIfExists(Paths.get(s));
        }catch(IOException ex){
        ex.printStackTrace();
        }
        });*/
        
        for(String s : mfact.getSams().keySet()){
            try{
                Files.deleteIfExists(Paths.get(s));
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }
    
    private List<BedSimple> getSamIntervals(SAMFileHeader h){
        List<BedSimple> coords = new ArrayList<>();
        /*coords = h.getSequenceDictionary().getSequences().stream().map((s) -> {
            String chr = s.getSequenceName();
            int len = s.getSequenceLength();
            //List<BedSimple> temp = new ArrayList<>();
            if(len < 1000000){
            temp.add(new BedSimple(chr, 0, len));
            return temp.stream();
            }else{
            for(int x = 0; x < len; x += 1000000){
            if(x + 1000000 > len)
            temp.add(new BedSimple(chr, x, len));
            else
            temp.add(new BedSimple(chr, x, x + 1000000));
            }
            return temp.stream();
            }
            // I changed this to chromosome lengths in order to avoid instances where
            // reads that mapped on the peripheries of query sites would be lost
            // in the queried samiterator object
            return new BedSimple(chr, 0, len);
        }).collect(Collectors.toList());*/
        
        for(SAMSequenceRecord s : h.getSequenceDictionary().getSequences()){
            String chr = s.getSequenceName();
            int len = s.getSequenceLength();
            coords.add(new BedSimple(chr, 0,len));
        }
        
        return coords;
    }
}
