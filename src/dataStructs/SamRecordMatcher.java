/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dataStructs;

import TempFiles.TempDataClass;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.TextCigarCodec;
import stats.ReadNameUtility;
import workers.TextFileQuickSort;

/**
 * TODO: rewrite this class so that it does not extend TempDataClass
 * @author bickhart
 */
public class SamRecordMatcher extends TempDataClass {
    private final Map<SAMReadGroupRecord, Map<String, Map<Short, ArrayList<SAMRecord>>>> buffer = new HashMap<>();
    // Changed SamTemp to separate output files by read name hash as well for faster sorting
    // Samtemp-> readgroup -> rnhashbin -> samoutputhandle
    private final Map<String, Map<Long, SamOutputHandle>> SamTemp = new ConcurrentHashMap<>();
    private int overhead = 0;
    private final int threshold;
    private final Map<String, Integer[]> thresholds;
    private final boolean checkRGs;
    private final String defId = "D";
    private final ReadNameUtility rn = new ReadNameUtility();
    private final boolean debug;
    private final String tempOutBase;
    private Path debugOut;
    private BufferedWriter debugWriter;
    private Map<String, Map<String, Short>> anchorlookup;
    
    private static final Logger log = Logger.getLogger(SamRecordMatcher.class.getName());
    
    /**
     * Creates a factory that processes sam entries for discordant and split reads.
     * @param threshold The number of alignments to save in memory before spilling to disk
     * @param checkRGs true if the user wants the samheader read groups treated separately
     * @param tmpoutname The base temporary file output name
     * @param thresholds Obtained from the BamMetadataGeneration class -- [0] = lower insert size threshold [1] = upper insert size threshold
     * @param debug true if debug "samsupport.tab" file should be generated 
     */
    public SamRecordMatcher(int threshold, boolean checkRGs, String tmpoutname, Map<String, Integer[]> thresholds, boolean debug){
        this.threshold = threshold;
        this.checkRGs = checkRGs;
        this.thresholds = thresholds;
        //this.createTemp(Paths.get(tmpoutname));
        this.debug = debug;
        if(debug){
            debugOut = Paths.get("SamSupport.tab");
            try {
                this.debugWriter = Files.newBufferedWriter(this.debugOut, Charset.defaultCharset());
            } catch (IOException ex) {
                log.log(Level.SEVERE, "[SAMMATCH] Error creating SamSupport.tab file!", ex);
            }
        }    
        this.tempOutBase = tmpoutname;
    }
    
    /**
     * Buffered method for adding SAMRecords for processing. Will spill to disk automatically when threshold is reached.
     * @param a a SAMRecord object
     */
    public void bufferedAdd(SAMRecord a) {
        // Check if we should add this one
        // We want to avoid optical duplicates and otherwise marked "bad" reads
        String rname = a.getReadName();
        int rgflags = a.getFlags();
        if(((rgflags & 0x4) == 0x4 && (rgflags & 0x8) == 0x8))
            return; // read pair did not map at all
       
        if((rgflags & 0x100) == 0x100)
            return; // this read was a secondary alignment. I'm ignoring these for now
        
        SAMReadGroupRecord r;
        if(this.checkRGs)
            r = a.getReadGroup();
        else
            r = new SAMReadGroupRecord(defId);
        
        Integer[] t = this.thresholds.get(r.getId());
        int insert = Math.abs(a.getInferredInsertSize());
        
        int softclips = 0;
        if(a.getCigarString().contains("S"))
            softclips = this.getCigarSoftClips(a.getCigar());
        double softthresh = (double)a.getReadLength() * 0.20d;
        boolean properMDist = false;
        boolean properOrient = true;
        if((rgflags & 0x1) == 0x1)
            if(insert > t[0] && insert < t[1] && softclips < softthresh)
                properMDist = true; // This entry has proper spacing
        
        if((rgflags & 0x2) != 0x2)
            properOrient = false; // A simplified metric to determine if there is an anomaly with read pairing
        
        if(properMDist && properOrient)
            return; // This read had both proper orientation and was within expected size distributions, so nothing to see here!
        
        // Remove any unwanted characters in the read name prior to our use of the clone function
        if(a.getReadName().matches("[_/]")){
            String initread = a.getReadName().replaceAll("[_/]", "-");
            a.setReadName(initread);
        }
        
        String clone = rn.GetCloneName(a.getReadName(), a.getFlags());
        short num = rn.GetCloneNum(a.getReadName(), a.getFlags());
        
        // The hash value takes up less space and should still be unique
        long rnHash = rn.ReadHash(clone);
        long bin = readNameHashBin(rnHash);
        //a.setReadName(String.valueOf(rnHash));
        
        if(!SamTemp.containsKey(r.getId()))
            SamTemp.put(r.getId(), new ConcurrentHashMap<>());
        if(!SamTemp.get(r.getId()).containsKey(bin))
            SamTemp.get(r.getId()).put(bin, new SamOutputHandle(this.threshold, r.getId(), this.tempOutBase));
        SamTemp.get(r.getId()).get(bin).bufferedAdd(a, rnHash, num);
    }

    private long readNameHashBin(long hash){
        return hash >> 60;
    }
    
    /**
     * Part of interface. Ignored in this iteration.
     */
    @Override
    public void readSequentialFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Automatically spills all reads to disk from buffer.
     */
    @Override
    public void dumpDataToDisk() {
        this.openTemp('A');
        try{
            for(SAMReadGroupRecord r : buffer.keySet()){
                for(String clone : buffer.get(r).keySet()){
                    for(short num : buffer.get(r).get(clone).keySet()){
                        for(SAMRecord sam : buffer.get(r).get(clone).get(num)){
                            this.output.write(r.getId() + "\t" + clone + "\t" + num + "\t" + sam.getSAMString());
                            //this.output.newLine();
                        }
                    }
                }
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }finally{
            this.closeTemp('A');
        }
        this.buffer.clear();
    }
    
    /**
     * Method that merges several child SamRecordMatcher objects with a parent object.
     * @param s SamRecordMatcher class
     */
    public synchronized void combineRecordMatcher(SamRecordMatcher s){
        s.SamTemp.keySet().forEach((k) -> {
            if(!this.SamTemp.containsKey(k)){
                this.SamTemp.put(k, new ConcurrentHashMap<>());
            }
            s.SamTemp.get(k).keySet().forEach((c) ->{
                if(!this.SamTemp.get(k).containsKey(c))
                    this.SamTemp.get(k).put(c, s.SamTemp.get(k).get(c));
                else{
                    log.log(Level.FINE, "[SAMMATCH] Had to combine records for rg: " + k + " and bin: " + c);
                    this.SamTemp.get(k).get(c).combineTempFiles(s.SamTemp.get(k).get(c));
                }
            });
            
        });
    }
    
    /**
     * Main routine that takes all identified discordant alignments and ships them to appropriate temp output handles. 
     * @param divets the temporary discordant read output handle
     * @param splits the temporary split read output handle
     */
    public void convertToVariant(Map<String, DivetOutputHandle> divets, Map<String, SplitOutputHandle> splits){
        if(!this.buffer.isEmpty())
            this.dumpDataToDisk();
        // Here I need to change the sorting to be a separate step
        // I can do easy file concatenation with the files sorted in bins, as the read names will all be in the same files
        final Map<String, List<Path>> sortHolder = new ConcurrentHashMap<>();
        this.SamTemp.entrySet().stream().forEach((e) -> {
            e.getValue().entrySet().stream().forEach((l) -> {
                // Ensuring that all data is currently spilled to disk before sort routine.
                l.getValue().dumpDataToDisk();
            });
            if(!sortHolder.containsKey(e.getKey()))
                sortHolder.put(e.getKey(), Collections.synchronizedList(new ArrayList<>()));
            e.getValue().entrySet().parallelStream().forEach((l) -> {
                TextFileQuickSort t = new TextFileQuickSort("\t", new int[]{0,1}, this.tempOutBase);
                try {
                    if(!l.getValue().getTempFile().toFile().canRead())
                        throw new FileNotFoundException("Error reading file!");
                    t.splitChunks(new FileInputStream(l.getValue().getTempFile().toFile()), String.valueOf(l.getKey()));
                    t.mergeChunks();
                } catch (FileNotFoundException ex) {
                    log.log(Level.SEVERE, "[SAMMATCH] Could not open temp file for sorting!", ex);
                } catch (IOException ex) {
                    log.log(Level.SEVERE, "[SAMMATCH] Error with temporary file merging!", ex);
                } catch (Exception ex){
                    log.log(Level.SEVERE, "[SAMMATCH] Error! Caught exception!", ex);
                }
                sortHolder.get(e.getKey()).add(t.getTemp());
            });
        });
        
        // OK, so the files should be concatenated after this!
        Map<String, TempSortFileHolder> mergeHolder = sortHolder.entrySet().stream()
                .collect(Collectors.toConcurrentMap(e -> e.getKey(), (e) -> {
                    TempSortFileHolder t = new TempSortFileHolder(Paths.get(this.tempOutBase));
                    t.ConcatenateFiles(e.getValue());
                    return t;
                }));
        
        anchorlookup = new ConcurrentHashMap<>();
        try {
            mergeHolder.keySet().parallelStream().forEach((String s) -> {
                try{
                    // Orderly open files
                    splits.get(s).OpenAnchorHandle();
                    splits.get(s).OpenFQHandle();
                    divets.get(s).OpenHandle();
                    
                    // Here we try to stream each chromosome in order but dump it in the same temp file for the readgroup
                    
                    
                    TempFileConverter(splits.get(s), divets.get(s), s, mergeHolder.get(s));
                    log.log(Level.FINE, "[SAMMATCH] finished sorting temp file for rg: " + s);
                    mergeHolder.get(s).close();                       
                    
                    // Close files to reduce number of open file handles later
                    divets.get(s).CloseHandle();
                    splits.get(s).CloseAnchorHandle();
                    splits.get(s).CloseFQHandle();
                }catch(Exception ex){
                    log.log(Level.SEVERE, "[SAMMATCH] Caught unknown exception (inner loop!)", ex);
                }
            });
            if(debug)
                this.debugWriter.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[SAMMATCH] Error with opening file!", ex);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "[SAMMATCH] Caught unknown exception (outer loop!)", ex);
        }
    }
    
    private void TempFileConverter(SplitOutputHandle splits, DivetOutputHandle divets, String rg, TempSortFileHolder sam)throws InterruptedException, IOException, Exception {
        String line, last = "none";
        String[] lastsegs = null;
        ArrayList<String[]> records = new ArrayList<>();
        BufferedReader input = sam.GetTempReader();
        
        // This is a new counter designed to hold the divet state (ie. first clone or second clone missing)
        Map<Integer, Integer> readState = new HashMap<>();
        readState.put(0, 0);
        readState.put(1, 0);
        readState.put(2, 0);
        readState.put(3, 0);
        
        while((line = input.readLine()) != null){
            line = line.trim();
            String[] segs = line.split("\t");
            
            // clone name is not the same as the last one
            if(!segs[0].equals(last)){
                if(!records.isEmpty()){
                    if(records.stream().anyMatch((s) -> isSplit(s))){
                        int scount = 0, acount = 0;
                        for(String[] r : records){
                            if(this.isAnchor(r)){
                                splits.AddAnchor(r);
                                if(debug)
                                    this.debugWriter.write("Anchor\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                                acount++;
                            }else{
                                splits.AddSplit(r);
                                if(debug)
                                    this.debugWriter.write("Split\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                                scount++;
                            }
                        }
                        if(acount == 0 && scount > 0){
                            // We had more than one split read, but no anchors
                            // Store the information we need to get the anchors next round
                            String[] temp = records.get(0);
                            if(!anchorlookup.containsKey(rg))
                                anchorlookup.put(rg, new HashMap<>());
                            anchorlookup.get(rg).put(temp[0], this.flipCloneNum(Short.parseShort(temp[1])));
                        }
                    }
                    // I changed this to allow for discordant read detection after split read detection
                    if(records.size() > 1){
                        Integer[] t = thresholds.get(rg);
                        SamToDivet converter = new SamToDivet(last, t[0], t[1], t[2]);
                        records.stream().forEach((r) -> {
                            // Process the XAZTag if it exists
                            processXAZTag(r).stream().forEach((n) -> {converter.addLines(n);});
                            converter.addLines(r);
                            try{
                                if(debug)
                                    this.debugWriter.write("Disc\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                            }catch(IOException ex){
                                ex.printStackTrace();
                            }
                        });
                        converter.processLinesToDivets();
                        int state = converter.getState();
                        if(state != 0){
                            readState.put(state, readState.get(state) + 1);
                        }else{
                            readState.put(state, readState.get(state) + 1);
                            divets.PrintDivetOut(converter.getDivets());
                        }
                    }
                    records.clear();
                }
            }
            lastsegs = segs;
            records.add(segs);
            last = segs[0];
        }
        log.log(Level.FINE, "[SAMMATCH] identified: " + readState.get(0) + " read pairs that were proper discordant pairs in " + rg + " readname.");
        if(readState.get(1) > 0 || readState.get(2) > 0 || readState.get(3) > 0){
            if(readState.get(1) > 0)
                log.log(Level.WARNING, "WARNING! Found " + readState.get(1) + " read pairs that were missing the first read in read group: " + rg);
            if(readState.get(2) > 0)
                log.log(Level.WARNING, "WARNING! Found " + readState.get(2) + " read pairs that were missing the second read in read group: " + rg);
            if(readState.get(3) > 0)
                log.log(Level.WARNING, "WARNING! Found " + readState.get(3) + " read pairs that were missing BOTH identification tags in read group: " + rg  + "! Please check your input BAM file!");
        }
        
        int splitTrims = splits.getTrims();
        int splitDiscards = splits.getDiscards();
        int splitErrors = splits.getErrors();
        log.log(Level.INFO, "[SAMMATCH] While creating split reads, discarded: " + splitDiscards + " small reads and trimmed: " + splitTrims + " long reads out of: " + splits.getTotalSplits() + " total viable split reads.");
        if(splitErrors > 0){
            log.log(Level.INFO, "[SAMMATCH] Warning! Found " + splitErrors + " alignments with mismatching quality score and sequence lengths!");
        }
        
        divets.CloseHandle();
        splits.CloseFQHandle();
        input.close();
        
    }

    private void TempFileSorter(SplitOutputHandle splits, DivetOutputHandle divets, String rg, SamOutputHandle sam) throws InterruptedException, IOException, Exception {
        // Use Unix sort to sort the file by the multiple beginning columns
        ProcessBuilder p = new ProcessBuilder("sort", "-k1,1", "-k2,2n", sam.getTempFile().toString());
        p.redirectError(new File("sort.error.log"));
        Process sort = p.start();
        String line, last = "none";
        String[] lastsegs = null;
        ArrayList<String[]> records = new ArrayList<>();
        BufferedReader input = new BufferedReader(new InputStreamReader(sort.getInputStream()));
        /*
        TODO: This is an area where I can speed up processing by running each read group in a separate thread
        I would need to rewrite the class so that SamRecordMatcher is not a temp data file storage class, but rather
        a separate class stores the data.
         */
        while((line = input.readLine()) != null){
            line = line.trim();
            String[] segs = line.split("\t");
            
            // clone name is not the same as the last one
            if(!segs[0].equals(last)){
                if(!records.isEmpty()){
                    if(records.stream().anyMatch((s) -> isSplit(s))){
                        int scount = 0, acount = 0;
                        for(String[] r : records){
                            if(this.isAnchor(r)){
                                splits.AddAnchor(r);
                                if(debug)
                                    this.debugWriter.write("Anchor\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                                acount++;
                            }else{
                                splits.AddSplit(r);
                                if(debug)
                                    this.debugWriter.write("Split\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                                scount++;
                            }
                        }
                        if(acount == 0 && scount > 0){
                            // We had more than one split read, but no anchors
                            // Store the information we need to get the anchors next round
                            String[] temp = records.get(0);
                            if(!anchorlookup.containsKey(rg))
                                anchorlookup.put(rg, new HashMap<>());
                            anchorlookup.get(rg).put(temp[0], this.flipCloneNum(Short.parseShort(temp[1])));
                        }
                    }
                    // I changed this to allow for discordant read detection after split read detection
                    if(records.size() > 1){
                        Integer[] t = thresholds.get(rg);
                        SamToDivet converter = new SamToDivet(last, t[0], t[1], t[2]);
                        records.stream().forEach((r) -> {
                            // Process the XAZTag if it exists
                            processXAZTag(r).stream().forEach((n) -> {converter.addLines(n);});
                            converter.addLines(r);
                            try{
                                if(debug)
                                    this.debugWriter.write("Disc\t" + StrUtils.StrArray.Join(r, "\t") + System.lineSeparator());
                            }catch(IOException ex){
                                ex.printStackTrace();
                            }
                        });
                        converter.processLinesToDivets();
                        divets.PrintDivetOut(converter.getDivets());
                    }
                    records.clear();
                }
            }
            lastsegs = segs;
            records.add(segs);
            last = segs[0];
        }
        divets.CloseHandle();
        splits.CloseFQHandle();
        input.close();
        sort.waitFor();
        
        sort.getErrorStream().close();
        sort.getInputStream().close();
        sort.getOutputStream().close();
    }
    
    /**
     * This is a recursive iteration over the bam file to grab anchor reads that were not associated with split reads the first time. Could
     * be avoided if the 0x8 flag was set on the parent anchor read (you can pull them out in the first pass)
     * @param splits the temp split output handle
     * @param samItr a samjdk iterator over the input bam file.
     */
    public void RetrieveMissingAnchors(Map<String, SplitOutputHandle> splits, SAMRecordIterator samItr){
        if(!this.anchorlookup.isEmpty()){
            int anchorcount = this.anchorlookup.keySet().stream().map((s) -> anchorlookup.get(s).keySet().size()).reduce(0,Integer::sum);
            System.err.println("[RECORD MATCHER] Identified " + anchorcount + " soft clipped reads that need anchors identified.");
            //Map<String, Boolean> anchorfound = anchorlookup.values().stream()
            //    .map(Map::keySet)
            //    .flatMap(Set::stream)
            //    .collect(Collectors.toMap(s -> s, s -> false));
            Map<String, Boolean> anchorfound = new HashMap<>();
            for(String s : anchorlookup.keySet()){
                anchorfound.put(s, false);
            }
            while(samItr.hasNext()){
                SAMRecord s;
                try{
                    s = samItr.next();
                }catch(SAMFormatException ex){
                    // This should ignore sam validation errors for crap reads
                    System.err.println(ex.getMessage());
                    continue;
                }
                SAMReadGroupRecord r;
                if(this.checkRGs)
                    r = s.getReadGroup();
                else
                    r = new SAMReadGroupRecord(defId);
                String rg = r.getId();
                if(this.anchorlookup.containsKey(rg)){
                    if(s.getReadName().matches("[_/]")){
                        String initread = s.getReadName().replaceAll("[_/]", "-");
                        s.setReadName(initread);
                    }
                    String clone = rn.GetCloneName(s.getReadName(), s.getFlags());   
                    if(this.anchorlookup.get(rg).containsKey(clone)){
                        short num = rn.GetCloneNum(s.getReadName(), s.getFlags());
                        if(this.anchorlookup.get(rg).get(clone) == num){
                            splits.get(rg).AddAnchor(s);
                            anchorfound.put(clone, true);
                        }
                    }
                }
            }
            long found = anchorfound.keySet().stream().filter((e) -> anchorfound.get(e)).count();
            long notfound = (long)anchorfound.size() - found;
            System.err.println("[RECORD MATCHER] Collected: " + found + " anchor reads and missed " + notfound + " out of " + anchorfound.size() + " original values");
        }
        samItr.close();
        // We close the anchor handle here just in case there were additional anchor entries to add.
        splits.keySet().stream().forEach((s) -> splits.get(s).CloseAnchorHandle());
    }
    
    // Wrapper class to merge sorted text files
    private class TempSortFileHolder extends TempDataClass{
        
        public TempSortFileHolder(Path tmpdir){
            this.createTemp(tmpdir);
        } 
        
        public void ConcatenateFiles(List<Path> files){
            this.openTemp('W');
            for(Path p : files){
                try(BufferedReader in = Files.newBufferedReader(p, Charset.defaultCharset())){
                    String line;
                    while((line = in.readLine()) != null){
                        this.output.write(line);
                        this.output.write(System.lineSeparator());
                    }
                    p.toFile().delete();
                }catch(IOException ex){
                    log.log(Level.SEVERE, "[TEMPSORTMERGE] Error merging files: " + files.toString(), ex);
                }
            }
            this.closeTemp('W');
        }
        
        public BufferedReader GetTempReader() throws IOException{
            this.openTemp('R');
            return this.handle;
        }
        
        public void close(){
            this.closeTemp('R');
        }
        
        @Override
        public void readSequentialFile() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void dumpDataToDisk() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    private short flipCloneNum(short a){
        if(a == 1)
            return 2;
        else
            return 1;
    }
    
    private boolean isOverSoftClipThreshold(Cigar c, int readLength){
        return getCigarSoftClips(c) > readLength * 0.30d;
    }
    
    private int getCigarSoftClips(Cigar c){
        return c.getCigarElements()
                .stream()
                .filter((s) -> s.getOperator().equals(CigarOperator.S) || s.getOperator().equals(CigarOperator.SOFT_CLIP))
                .map((s) -> s.getLength()).reduce(0, Integer::sum);
    }
    
    // TODO: instead of ignoring secondary aligments (0x100) try to generate split alignments from them
    private boolean isSplit(String[] segs){
        int fflags = Integer.parseInt(segs[3]);
        Cigar c = TextCigarCodec.getSingleton().decode(segs[7]);
        boolean segmentUnmapped = (fflags & 0x4) == 0x4 && (fflags & 0x8) != 0x8 && (fflags & 0x100) != 0x100;
        boolean softclipThresh = isOverSoftClipThreshold(c, segs[11].length()) && (fflags & 0x100) != 0x100;
        return segmentUnmapped || softclipThresh;
    }
    
    private boolean isAnchor(String[] segs){
        int fflags = Integer.parseInt(segs[3]);
        Cigar c = TextCigarCodec.getSingleton().decode(segs[7]);
        return (fflags & 0x4) != 0x4 && (fflags & 0x8) == 0x8 && !segs[4].equals("*") && !isOverSoftClipThreshold(c, segs[11].length());
    }
    
    private ArrayList<String[]> processXAZTag(String[] record){
        ArrayList<String[]> entries = new ArrayList<>();
        int XAZloc = hasXAZTag(record);
        
        // Check to see if the XA:Z tag is in the record
        if(XAZloc > 0){
            Pattern xaz = Pattern.compile("XA:Z:(.+)");
            Pattern plus = Pattern.compile("([+-])(\\d+)");
            Matcher match = xaz.matcher(record[XAZloc]);
            if(match.find()){
                // Each XA:Z group
                String[] segs = match.group(1).split(";");
                for(String s : segs){
                    // The individual components of the XA:Z group
                    String[] tsegs = s.split(",");
                    String[] e = new String[record.length];
                    System.arraycopy(record, 0, e, 0, record.length);
                    
                    // Check if the alignment is in the forward or reverse direction
                    Matcher pmatch = plus.matcher(tsegs[1]);
                    String sign, pos;
                    if(pmatch.find()){
                        sign = pmatch.group(1);
                        pos = pmatch.group(2);
                        // If the coordinate should be reversed, then subtract the read length from it
                        if(sign.equals("-"))
                            pos = String.valueOf(Integer.valueOf(pos) - record[11].length());
                        
                        e[4] = tsegs[0];
                        e[5] = pos;
                        // Add this alternative match to the array for return to the main program
                        entries.add(e);
                    }
                }
            }
        }
        return entries;
    }
    
    private int hasXAZTag(String[] record){
        for(int x = 0; x < record.length; x++){
            if(record[x].matches("XA:Z:.+"))
                return x;
        }
        return 0;
    }
}
