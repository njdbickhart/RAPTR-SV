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
import net.sf.samtools.DefaultSAMRecordFactory;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordFactory;

/**
 *
 * @author bickhart
 */
public class SplitOutputHandle {
    private final Path fq1path;
    private final Path anchorpath;
    private BufferedWriter fq1;
    private SAMFileWriterFactory anchor = new SAMFileWriterFactory();
    private SAMRecordFactory recordCreator = new DefaultSAMRecordFactory();
    private SAMFileWriter anchorOut;
    private final SAMFileHeader header;
    private boolean fileopen = false;
    
    public SplitOutputHandle(String file, String file2, SAMFileHeader sam){
        fq1path = Paths.get(file);
        anchorpath = Paths.get(file2);
        header = sam;
        this.OpenFQHandle();
    }
    
    public void AddAnchor(String[] segs){
        if(!fileopen)
            this.OpenAnchorHandle();
        SAMRecord sam = recordCreator.createSAMRecord(header);
        sam.setReadName(segs[3]);
        sam.setFlags(Integer.valueOf(segs[4]));
        sam.setReferenceName(segs[5]);
        sam.setAlignmentStart(Integer.valueOf(segs[6]));
        //sam.setAlignmentEnd(Integer.valueOf(segs[6]) + segs[12].length());
        sam.setMappingQuality(Integer.valueOf(segs[7]));
        sam.setCigarString(segs[8]);
        sam.setMateReferenceName(segs[9]);
        if(segs[9].equals("*"))
            sam.setMateAlignmentStart(0);
        else
            sam.setMateAlignmentStart(Integer.valueOf(segs[10]));
        sam.setInferredInsertSize(Integer.valueOf(segs[11]));
        sam.setReadString(segs[12]);
        sam.setBaseQualityString(segs[13]);
        for(int i = 14; i < segs.length; i++){
            String[] tags = segs[i].split(":");
            if(StrUtils.NumericCheck.isNumeric(tags[2]) && !tags[0].equals("MD"))
                sam.setAttribute(tags[0], Integer.parseInt(tags[2]));
            else if(StrUtils.NumericCheck.isFloating(tags[2]))
                sam.setAttribute(tags[0], Float.parseFloat(tags[2]));
            else
                sam.setAttribute(tags[0], tags[2]);
        }
        anchorOut.addAlignment(sam);
    }
    
    public void AddAnchor(SAMRecord s){
        anchorOut.addAlignment(s);
    }
    
    public void AddSplit(String[] segs){
        String nl = System.lineSeparator();
        String rn1 = "@" + segs[3] + "_1";
        String rn2 = "@" + segs[3] + "_2";
        
        int len = segs[12].length();
        
        String tS1 = segs[12].substring(0, len / 2);
        String tS2 = segs[12].substring(len / 2, len);

        String tQ1 = segs[13].substring(0, len / 2);
        String tQ2 = segs[13].substring(len / 2, len);
        
        try {
            fq1.write(rn1 + nl + tS1 + nl + "+" + nl + tQ1 + nl);
            fq1.write(rn2 + nl + tS2 + nl + "+" + nl + tQ2 + nl);
        } catch (IOException ex) {
            Logger.getLogger(SplitOutputHandle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void OpenFQHandle(){
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
        return this.anchorpath.toString();
    }
}
