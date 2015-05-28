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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecordIterator;
import workers.BamMetadataGeneration;
import workers.MrsFastRuntimeFactory;

/**
 *
 * @author bickhart
 */
public class PreprocessMode {
    private boolean checkRG = false;
    private final String outbase;
    private int samplimit = 10000;
    private int maxdist = 1000000;
    private final String input;
    private int threads = 1;
    private final String reference;
    private final boolean debug;
    private final boolean adjustRL;
    private int baselineRL = 0;
    
    private static final Logger log = Logger.getLogger(PreprocessMode.class.getName());
    
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
        
        if(values.HasOpt("readLen")){
            if(Integer.parseInt(values.GetValue("readLen")) < 50){
                log.log(Level.SEVERE, "[PREPROCESS] Error with input command! Cannot process read length argument (-l) with value less than 50bp!");
                log.log(Level.SEVERE, "[PREPROCESS] Split read alignment requires read lengths of 50bp or larger, please use a larger -l value.");
                System.exit(1);
            }
            this.adjustRL = true;
            this.baselineRL = Integer.parseInt(values.GetValue("readLen"));
        }else
            this.adjustRL = false;
        
        debug = values.GetValue("debug").equals("true");
    }
    
    public void run(){
        // Generate BAM file metadata class
        log.log(Level.FINE, "[PREPROCESS] Beginning BAM metadata sampling...");
        BamMetadataGeneration metadata;
        if(this.adjustRL)
            metadata = new BamMetadataGeneration(checkRG, this.baselineRL);
        else
            metadata = new BamMetadataGeneration(checkRG);
        metadata.ScanFile(input, samplimit);
        
        final Map<String, Integer[]> values = metadata.getThresholds(maxdist);
        Map<String, DivetOutputHandle> divets = metadata.generateDivetOuts(outbase);
        Map<String, SplitOutputHandle> splits = metadata.generateSplitOuts(outbase);
        
        System.err.println("[PREPROCESS] Read input file and calculated sample thresholds.");
        metadata.getSampleIDs().stream().forEach((s) -> {
            System.err.println("Sample: " + s + " Avg Ins size: " + metadata.getSampleInsSize(s) +
                    " Stdev Ins size: " + metadata.getSampleInsStd(s));
            log.log(Level.INFO, "[PREPROCESS] Sample: " + s + " Avg Ins size: " + metadata.getSampleInsSize(s) +
                    " Stdev Ins size: " + metadata.getSampleInsStd(s));
        });
        
        // Run through the BAM file generating split and divet data
        final SAMFileReader reader = new SAMFileReader(new File(input));
        reader.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        
        SAMFileHeader h = reader.getFileHeader();
        List<BedSimple> coords = this.getSamIntervals(h);
        
        List<SamRecordMatcher> collect = coords.parallelStream()
            .map((b) -> {
                SamRecordMatcher w = new SamRecordMatcher(samplimit, checkRG, utilities.GetBaseName.getBaseName(outbase) + ".tmp", values, debug);
                try{
                    SAMFileReader temp = new SAMFileReader(new File(input));
                    temp.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
                    SAMRecordIterator itr = temp.queryContained(b.Chr(), b.Start(), b.End());
                    itr.forEachRemaining((k) -> w.bufferedAdd(k));
                    temp.close();
                    System.out.print("                                                                                        \r");
                    System.out.print("[SAMRECORD] Working on SAM chunk: " + b.Chr() + "\t" + b.Start() + "\t" + b.End() + "\r");
                    log.log(Level.INFO, "[SAMRECORD] Working on SAM chunk: " + b.Chr() + "\t" + b.Start() + "\t" + b.End());
                }catch(Exception ex){
                    System.err.println("[SAMRECORD] Error with SAM query for: " + b.Chr() + "\t" + b.Start() + "\t" + b.End());
                    log.log(Level.SEVERE, "[SAMRECORD] Error with SAM chunk: " + b.Chr() + "\t" + b.Start() + "\t" + b.End());
                    ex.printStackTrace();
                }
                return w;
            }).collect(Collectors.toList());
                //.reduce(new SamRecordMatcher(samplimit, checkRG, outbase + ".tmp", values, debug), (SamRecordMatcher a, SamRecordMatcher b) -> {a.combineRecordMatcher(b); return a;});
        
        
        SamRecordMatcher worker = new SamRecordMatcher(samplimit, checkRG, utilities.GetBaseName.getBaseName(outbase) + ".tmp", values, debug);
        collect.stream().forEachOrdered((s) -> {
            worker.combineRecordMatcher(s);
        });
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
        next.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
        worker.convertToVariant(divets, splits);
        worker.RetrieveMissingAnchors(splits, next.iterator());
        
        System.err.println("[PREPROCESS] Generated initial split and divet data.");
        log.log(Level.INFO, "[PREPROCESS] Generated initial split and divet data.");
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
        log.log(Level.INFO, "[PREPROCESS] Generated initial split and divet data.");
        splits.keySet().stream().forEach((s) -> {
            try{
                if(!this.debug){
                    Files.deleteIfExists(Paths.get(splits.get(s).fq1File()));
                    log.log(Level.FINE, "[PREPROCESS] Deleting file: " + splits.get(s).fq1File().toString());
                }
            }catch(IOException ex){
                log.log(Level.SEVERE, "[PREPROCESS] Could not delete file: " + splits.get(s).fq1File().toString(), ex);
            }
        });
        mfact.getSams().keySet().stream().forEach((s) -> {
            try{
                if(!this.debug){
                    Files.deleteIfExists(Paths.get(mfact.getSams().get(s)));
                    log.log(Level.FINE, "[PREPROCESS] Deleting file: " + mfact.getSams().get(s));
                }
            }catch(IOException ex){
                log.log(Level.SEVERE, "[PREPROCESS] Could not delete file: " + mfact.getSams().get(s), ex);
            }
        });
    }
    
    private List<BedSimple> getSamIntervals(SAMFileHeader h){
        List<BedSimple> coords = new ArrayList<>();
        coords = h.getSequenceDictionary().getSequences().stream().map((s) -> {
            String chr = s.getSequenceName();
            int len = s.getSequenceLength();
            //List<BedSimple> temp = new ArrayList<>();
            /*if(len < 1000000){
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
            }*/
            // I changed this to chromosome lengths in order to avoid instances where
            // reads that mapped on the peripheries of query sites would be lost
            // in the queried samiterator object
            log.log(Level.FINE, "[PREPROCESS] Sam interval detected: " + chr + "\t" + 0 + "\t" + len);
            return new BedSimple(chr, 0, len);
        }).collect(Collectors.toList());
        return coords;
    }
}
