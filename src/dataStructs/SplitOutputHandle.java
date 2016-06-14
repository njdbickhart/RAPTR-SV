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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordFactory;
import htsjdk.samtools.TextCigarCodec;

/**
 *
 * @author bickhart
 */
public class SplitOutputHandle {
    private final Path fq1path;
    private final Path anchorpath;
    private BufferedWriter fq1;
    private final SAMFileWriterFactory anchor = new SAMFileWriterFactory();
    private final SAMRecordFactory recordCreator = new DefaultSAMRecordFactory();
    private SAMFileWriter anchorOut;
    private final SAMFileHeader header;
    private boolean fileopen = false;
    private final int splitreadlen;
    private AtomicInteger discardCounter = new AtomicInteger(0);
    private AtomicInteger errorCounter = new AtomicInteger(0);
    private AtomicInteger trimCounter = new AtomicInteger(0);
    private AtomicInteger totalSplits = new AtomicInteger(0);
    
    private static final Logger log = Logger.getLogger(SplitOutputHandle.class.getName());
    
    /**
     * This class generates the temporary output file handle for split read generation
     * @param file The temporary fq file for alignment
     * @param file2 The output handle for the anchoring reads
     * @param sam A Samheader class object
     * @param readlen The read length chosen for read trimming
     */
    public SplitOutputHandle(String file, String file2, SAMFileHeader sam, int readlen){
        fq1path = Paths.get(file);
        anchorpath = Paths.get(file2);
        header = sam;
        //this.OpenFQHandle();
        
        if(readlen % 2 != 0)
            readlen -= 1; // accounting for odd number of bases in a read
        
        this.splitreadlen = readlen / 2;
        
        //TODO: make this a buffer in order and close file handles in an orderly fashion
    }
    
    /**
     * This adds an anchor to the temp anchor file
     * @param segs String representation of a SAM record
     */
    public synchronized void AddAnchor(String[] segs){
        if(!fileopen)
            this.OpenAnchorHandle();
        SAMRecord sam = recordCreator.createSAMRecord(header);
        sam.setReadName(segs[2]);
        sam.setFlags(Integer.valueOf(segs[3]));
        sam.setReferenceName(segs[4]);
        sam.setAlignmentStart(Integer.valueOf(segs[5]));
        //sam.setAlignmentEnd(Integer.valueOf(segs[6]) + segs[12].length());
        sam.setMappingQuality(Integer.valueOf(segs[6]));
        sam.setCigarString(segs[7]);
        sam.setMateReferenceName(segs[8]);
        if(segs[8].equals("*"))
            sam.setMateAlignmentStart(0);
        else
            sam.setMateAlignmentStart(Integer.valueOf(segs[9]));
        sam.setInferredInsertSize(Integer.valueOf(segs[10]));
        sam.setReadString(segs[11]);
        sam.setBaseQualityString(segs[12]);
        for(int i = 13; i < segs.length; i++){
            String[] tags = segs[i].split(":");
            if(tags[0].equals("OQ"))
                continue; // This is a pretty extraneous field and it serves just to bulk up the BAM file
            switch(tags[1]){
                case "A":
                case "Z":
                case "B":
                    sam.setAttribute(tags[0], tags[2]);
                    break;
                case "i":
                    sam.setAttribute(tags[0], Integer.parseInt(tags[2]));
                    break;
                case "f":
                    sam.setAttribute(tags[0], Float.parseFloat(tags[2]));
                    break;
            }
        }
        anchorOut.addAlignment(sam);
    }
    
    /**
     * This adds an anchor to the temp anchor file
     * @param s A SAMRecord object
     */
    public synchronized void AddAnchor(SAMRecord s){
        if(!fileopen)
            this.OpenAnchorHandle();
        anchorOut.addAlignment(s);
    }
    
    /**
     * This adds an alignment to the split read temporary file output
     * @param segs String representation of a temporary SAMRecord object (first two columns have the bin and RG ids)
     */
    public synchronized void AddSplit(String[] segs){
        if(!fileopen)
            this.OpenFQHandle();
        String nl = System.lineSeparator();
        String rn1 = "@" + segs[2] + "_1";
        String rn2 = "@" + segs[2] + "_2";
        
        
        int len = segs[11].length();
        //int splitter = firstSplitSeg(segs[8], len);
        // TODO: Unfortunately, Mrsfast does not allow for split read alignment of
        // variable length reads. I will have to implement a side method that 
        // processes the reads differently based on their length
        
        if(len % 2 != 0)
            len -= 1;
        int splitter = len / 2;
        
        String tS1, tS2, tQ1, tQ2;
        
        if(segs[11].length() != segs[12].length()){
            // This was a malformed read and should be counted as an error
            this.errorCounter.getAndIncrement();
            return;
        }else if(splitter < this.splitreadlen){
            // This read is far too small, and should be discarded
            this.discardCounter.incrementAndGet();
            return;
        }else if(splitter > this.splitreadlen){
            // The read is large enough that we can work with it!
            // We split the read in the middle, but we remove the bases on the 5' and 3' ends
            int diff = splitter - this.splitreadlen;
            this.trimCounter.incrementAndGet();
            tS1 = segs[11].substring(diff, splitter);
            tS2 = segs[11].substring(splitter, (splitter * 2) - diff);
            
            tQ1 = segs[12].substring(diff, splitter);
            tQ2 = segs[12].substring(splitter, (splitter * 2) - diff);
        }else{
            // The read is the right size, so the splitting is really easy
            tS1 = segs[11].substring(0, splitter);
            tS2 = segs[11].substring(splitter, splitter * 2);
            
            tQ1 = segs[12].substring(0, splitter);
            tQ2 = segs[12].substring(splitter, splitter * 2);
        }      
        
        this.totalSplits.incrementAndGet();
        
        try {
            fq1.write(rn1 + nl + tS1 + nl + "+" + nl + tQ1 + nl);
            fq1.write(rn2 + nl + tS2 + nl + "+" + nl + tQ2 + nl);
            fq1.flush();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "[SPLITOUT] Error writing split file!", ex);
        }
    }
    
    private int firstSplitSeg(String cigar, int readlen){
        int start = 0;
        for(CigarElement e : TextCigarCodec.decode(cigar).getCigarElements()){
            if(e.getOperator().equals(CigarOperator.S) && e.getLength() > readlen * 0.20){
                if (start == 0)
                    return e.getLength(); // We have a softclip area at the beginning
                else if(start + e.getLength() >= readlen * 0.90)
                    return start; // This softclipping is at the end
                else
                    return start + e.getLength();
            }else 
                start += e.getLength();
        }
        return readlen / 2; // We couldn't find a good softclip, so let's just return half the read length
    }
    
    /**
     * Opens the temp FQ handle file for writing. 
     */
    public void OpenFQHandle(){
        try{
            fq1 = Files.newBufferedWriter(fq1path, Charset.defaultCharset());
        }catch(IOException ex){
            log.log(Level.SEVERE, "[SPLITOUT] Error opening FQ handle!", ex);
        }
        //fileopen = true;
    }
    
    /**
     * Closes the temp FQ handle.
     */
    public void CloseFQHandle(){
        try{           
            fq1.close();
        }catch(IOException ex){
            log.log(Level.SEVERE, "[SPLITOUT] Error closing FQ handle!");
        }
        //fileopen = false;
    }
    
    /**
     * Opens the temp Anchor handle file for writing.
     */
    public void OpenAnchorHandle(){
        anchorOut = anchor.makeBAMWriter(header, false, anchorpath.toFile());
        fileopen = true;
    }
    
    /**
     * Closes the temp Anchor handle file for writing.
     */
    public void CloseAnchorHandle(){
        
        if(fileopen){
            anchorOut.close();
        }
        fileopen = false;
    }
        
    /**
     * Checks if the temporary files are open for writing.
     * @return true if the handles are open. 
     */
    public boolean fileIsOpen(){
        return fileopen;
    }
    
    /**
     * 
     * @return returns the absolute path of the temp FQ file
     */
    public String fq1File(){
        return this.fq1path.toString();
    }
    
    /**
     *
     * @return returns the absolute path of the temp Anchor file
     */
    public String getAnchorFileStr(){
        return this.anchorpath.toAbsolutePath().toString();
    }
    
    /**
     *
     * @return returns the count of split reads that were too short for processing
     */
    public int getDiscards(){
        return this.discardCounter.intValue();
    }
    
    /**
     *
     * @return returns the count of split reads that were too long and were trimmed.
     */
    public int getTrims(){
        return this.trimCounter.intValue();
    }
    
    /**
     *
     * @return returns the number of malformed sam record entries
     */
    public int getErrors(){
        return this.errorCounter.intValue();
    }
    
    /**
     *
     * @return returns the total number of processed (trimmed and normal) split reads.
     */
    public int getTotalSplits(){
        return this.totalSplits.intValue();
    }
}
