package org.dspace.content;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;


/**
 * Class Which will write out a sequence of byte array to file in the correct order.
 * This is to take care of large files which have been split into manageable chunks to send over http
 * they may arrive out of order so need to be streamed to storage in the correct order.
 *
 *
 */
public class BitStreamOutput {
	// hashmap with sequence number and a list of byte arrays that will be
	// streamed onto the storage medium
	private Map<Long, Byte[]> parts = new HashMap<Long, Byte[]>();
	private DigestOutputStream dos = null;
	private Long currentSequenceNo = 0L;
	private MessageDigest checksum = null;
	private boolean updated = false;
	private boolean end = false;
	private Long size = 0L;
	private boolean complete = false;

	public BitStreamOutput(OutputStream bos, MessageDigest digest) {
		    digest.reset();
		    try {
				checksum = (MessageDigest)digest.clone();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.dos = new DigestOutputStream(bos, digest);
			
		
	}

	/**
	 * add a byte array to the bitstream
	 * 
	 * @param sequenceNo
	 *            for the ordering of this byte array within the bitstream
	 * @param bytes
	 *            the byte array part of the bitstream
	 * @param last
	 *            is this the final part of the bitstream NB. there still may be
	 *            others
	 * @return whether the final part has been written out to the storage medium
	 *         and output stream closed
	 * @throws IOException if the output has been closed and still trying to add to it
	 */
	public boolean addByteArray(Long sequenceNo, Byte[] bytes, boolean last) throws IOException {

		synchronized (parts) {
			// has the output stream already been flushed and closed

			// if this is the last byte array flag it up
			// it may be the last one but it may have be out of order
			if (bytes != null && last) {
				end = last;
			}
			if (!parts.containsKey(sequenceNo)) {
				parts.put(sequenceNo, bytes);
				updated = true;
			}
		}

		return update();
	}

	private boolean update() throws IOException {
		
		synchronized (parts) {
			if (updated && currentSequenceNo != null) {
				if (parts.containsKey(currentSequenceNo)) {
					// this should not happen, the bitstream part should have
					// been removed and the current sequence updated
					parts.remove(currentSequenceNo);
				}
				while (parts.containsKey(currentSequenceNo + 1)) {
					Byte[] byteArray = parts.remove(currentSequenceNo + 1);
					for (int i = 0; i < byteArray.length; i++) {
						checksum.update(byteArray[i]);
						dos.write(byteArray[i]);
						size++;
					}
					currentSequenceNo++;
				}
				updated = false;
			}
			if (end && parts.isEmpty()) {
				// finished
				
				dos.flush();
				dos.close();
				complete = true;
			}
		}
		return complete;
	}
	
	public Long getSize(){
		return size;
	}

	public MessageDigest getChecksum() {
		
		//System.out.println("getChecksum: " + checksum.digest());
		
		return checksum;
	}

}
