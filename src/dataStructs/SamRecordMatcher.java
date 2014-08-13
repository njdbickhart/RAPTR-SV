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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.TextCigarCodec;
import stats.ReadNameUtility;

/**
 * TODO: rewrite this class so that it does not extend TempDataClass
 * @author bickhart
 */
public class SamRecordMatcher extends TempDataClass {
    private final Map<SAMReadGroupRecord, Map<String, Map<Short, ArrayList<SAMRecord>>>> buffer = new HashMap<>();
    private final Map<String, SamOutputHandle> SamTemp = new HashMap<>();
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
                Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }    
        this.tempOutBase = tmpoutname;
    }
    
    public void bufferedAdd(SAMRecord a) {
        // Check if we should add this one
        // We want to avoid optical duplicates and otherwise marked "bad" reads
        int rgflags = a.getFlags();
        if(((rgflags & 0x4) == 0x4 && (rgflags & 0x8) == 0x8))
            return; // read pair did not map at all
       
        
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
        if((rgflags & 0x1) == 0x1)
            if(insert > t[0] && insert < t[1] && softclips < softthresh)
                return; // This entry was properly mated and was not discordant; we don't need it
        
        // Remove any unwanted characters in the read name prior to our use of the clone function
        if(a.getReadName().matches("[_/]")){
            String initread = a.getReadName().replaceAll("[_/]", "-");
            a.setReadName(initread);
        }
        
        String clone = rn.GetCloneName(a.getReadName(), a.getFlags());
        short num = rn.GetCloneNum(a.getReadName(), a.getFlags());
        
        if(!SamTemp.containsKey(r.getId()))
            SamTemp.put(r.getId(), new SamOutputHandle(this.threshold, r.getId(), this.tempOutBase));
        SamTemp.get(r.getId()).bufferedAdd(a, clone, num);
    }

    @Override
    public void readSequentialFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

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
    
    public synchronized void combineRecordMatcher(SamRecordMatcher s){
        s.SamTemp.keySet().forEach((k) -> {
            if(!this.SamTemp.containsKey(k)){
                this.SamTemp.put(k, new SamOutputHandle(this.threshold, k, this.tempOutBase));
            }
            this.SamTemp.get(k).combineTempFiles(s.SamTemp.get(k));
        });
    }
    
    public void convertToVariant(Map<String, DivetOutputHandle> divets, Map<String, SplitOutputHandle> splits){
        if(!this.buffer.isEmpty())
            this.dumpDataToDisk();
        
        anchorlookup = new ConcurrentHashMap<>();
        try {
            this.SamTemp.keySet().parallelStream().forEach((String s) -> {
                try{
                    TempFileSorter(splits.get(s), divets.get(s), s, SamTemp.get(s));
                }catch(InterruptedException | IOException ex){
                    Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
                }catch(Exception ex){
                    Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            if(debug)
                this.debugWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(SamRecordMatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                    }else if(records.size() > 1){
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
    }
    
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
    
    private boolean isSplit(String[] segs){
        int fflags = Integer.parseInt(segs[3]);
        Cigar c = TextCigarCodec.getSingleton().decode(segs[7]);
        return (((fflags & 0x4) == 0x4 && (fflags & 0x8) != 0x8)|| isOverSoftClipThreshold(c, segs[11].length()));
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
