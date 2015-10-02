package org.dspace.content.resumable;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * by fanxu
 */
public class ResumableInfo {
	
	private static Logger LOG = Logger.getLogger(ResumableInfo.class);

    private int      resumableChunkSize;
    private long     resumableTotalSize;
    private String   resumableIdentifier;
    private String   resumableFilename;
    private String   resumableRelativePath;
    private String bitstreamId;
    private RandomAccessFile raf;
  //Chunks uploaded
    private Map<Integer, Long> uploadedChunks = new HashMap<Integer, Long>();
    private String resumableFilePath;

    
    public String getBitstreamId() {
		return bitstreamId;
	}

	public void setBitstreamId(String bitstreamId) {
		this.bitstreamId = bitstreamId;
	}

	public void setResumableChunkSize(int chunkSize){
    	resumableChunkSize = chunkSize;
    }
    
    public int getResumableChunkSize(){
    	return resumableChunkSize;
    }
    
    public void setResumableTotalSize(long resumableTotalSize){
    	this.resumableTotalSize = resumableTotalSize;
    }
    
    public long getResumableTotalSize(){
    	return resumableTotalSize;
    }
    
    public void setResumableIdentifier(String resumableIdentifier){
    	this.resumableIdentifier = resumableIdentifier;
    }
    
    public String getResumableIdentifier(){
    	return resumableIdentifier;
    }
    
    public void setResumableFilename(String resumableFilename){
    	this.resumableFilename = resumableFilename;
    }
    
    public String getResumableFilename(){
    	return resumableFilename;
    }
    public void setUploadedChunks(Map<Integer, Long> uploadedChunks){
    	this.uploadedChunks = uploadedChunks;
    }
    
    public Map<Integer, Long> getUploadedChunks(){
    	return uploadedChunks;
    }
    public void setResumableFilePath(String resumableFilePath){
    	this.resumableFilePath = resumableFilePath;
    }
    
    public String getResumableFilePath(){
    	return resumableFilePath;
    }
    public void setResumableRelativePath(String resumableRelativePath){
    	this.resumableRelativePath = resumableRelativePath;
    }
    
    public String getResumableRelativeFilePath(){
    	return resumableRelativePath;
    }
    
    public RandomAccessFile getRaf() {
		return raf;
	}

	public void setRaf(RandomAccessFile raf) {
		this.raf = raf;
	}

	public boolean vaild(){
        if (resumableChunkSize < 0 || resumableTotalSize < 0
                || HttpUtils.isEmpty(resumableIdentifier)
                || HttpUtils.isEmpty(resumableFilename)
                || HttpUtils.isEmpty(resumableRelativePath)) {
            return false;
        } else {
            return true;
        }
    }
	
    public boolean checkIfUploadFinished() {
        //check if upload finished
        int count = (int) Math.ceil(((double) resumableTotalSize) / ((double) resumableChunkSize));
        
        LOG.debug("checkIfUploadFinished: Toatal number of chunks " + count + " uploaded chunks size: " + uploadedChunks.size());
        
        Set<Integer> keys = uploadedChunks.keySet();
        
        long total = 0L;
        
        for(Integer key: keys){
        	total += uploadedChunks.get(key);
        }
        
        if(total == resumableTotalSize){
        	return true;
        }else{
        	return false;
        }
    
    }
}
