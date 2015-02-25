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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.DefaultSAMRecordFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordFactory;
import net.sf.samtools.TextCigarCodec;

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
    private final TextCigarCodec cd = TextCigarCodec.getSingleton();
    
    public SplitOutputHandle(String file, String file2, SAMFileHeader sam){
        fq1path = Paths.get(file);
        anchorpath = Paths.get(file2);
        header = sam;
        //this.OpenFQHandle();
        
        //TODO: make this a buffer in order and close file handles in an orderly fashion
    }
    
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
    
    public synchronized void AddAnchor(SAMRecord s){
        if(!fileopen)
            this.OpenAnchorHandle();
        anchorOut.addAlignment(s);
    }
    
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
        int splitter = Math.floorDiv(len, 2);
        
        String tS1 = segs[11].substring(0, splitter);
        String tS2 = segs[11].substring(splitter, splitter * 2);

        String tQ1 = segs[12].substring(0, splitter);
        String tQ2 = segs[12].substring(splitter, splitter * 2);
        
        try {
            fq1.write(rn1 + nl + tS1 + nl + "+" + nl + tQ1 + nl);
            fq1.write(rn2 + nl + tS2 + nl + "+" + nl + tQ2 + nl);
        } catch (IOException ex) {
            Logger.getLogger(SplitOutputHandle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private int firstSplitSeg(String cigar, int readlen){
        int start = 0;
        for(CigarElement e : cd.decode(cigar).getCigarElements()){
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
    
    public void OpenFQHandle(){
        try{
            fq1 = Files.newBufferedWriter(fq1path, Charset.defaultCharset());
        }catch(IOException ex){
            ex.printStackTrace();
        }
        //fileopen = true;
    }
    
    public void CloseFQHandle(){
        try{
            fq1.close();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        //fileopen = false;
    }
    
    public void OpenAnchorHandle(){
        anchorOut = anchor.makeBAMWriter(header, false, anchorpath.toFile());
        fileopen = true;
    }
    
    public void CloseAnchorHandle(){
        if(fileopen)
            anchorOut.close();
        fileopen = false;
    }
        
    public boolean fileIsOpen(){
        return fileopen;
    }
    
    public String fq1File(){
        return this.fq1path.toString();
    }
    
    public String getAnchorFileStr(){
        return this.anchorpath.toAbsolutePath().toString();
    }
}
