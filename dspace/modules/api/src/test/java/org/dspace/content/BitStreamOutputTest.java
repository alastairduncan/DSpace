package org.dspace.content;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BitStreamOutputTest {
	
	@Before
	public void setup() throws Exception{
		
	}
	@After
	public void teardown() throws Exception{
		
	}
	
	@Test
	public void testBitstreamOutput() throws Exception{
		
		File tempFile = File.createTempFile("test", "");
		tempFile.delete();
		tempFile.mkdir();
		String home = tempFile.getPath();
		FileOutputStream fos = new FileOutputStream(home + File.separator + "Messages.properties");
		BitStreamOutput bso = new BitStreamOutput(fos, MessageDigest.getInstance("MD5"));
		
		// load in a test file
		FileInputStream fis = new FileInputStream("src/main/resources/Messages.properties");
		DigestInputStream dis = new DigestInputStream(fis, MessageDigest.getInstance("MD5"));
		byte[] bytes = new byte[1024]; 
		
		Map<Long,Byte[]> map =  new HashMap<Long,Byte[]>();
		
		Long counter = 0L;
		int noRead = dis.read(bytes);
		while( noRead != -1){
			List<Byte> byts = new ArrayList<Byte>(); 
			byts.clear();
			for(int i = 0; i < noRead; i++){
				byts.add(bytes[i]);
			}
			counter++;
			noRead = dis.read(bytes);
			
			// convert to a Byte[] and store in a hash table so that it can be read
			// from for a couple of tests with the arrays added in different orders
			map.put(counter, byts.toArray(new Byte[byts.size()]));
			
		}
		
		// put all of the keys in a list and ensure that they are ordered.
		// Need to reorder the list and see if the test comes out correctly with the reordered list
		List<Long> keys = new ArrayList<Long>();
		keys.addAll(map.keySet());
		
		Collections.sort(keys);
		
		boolean last = false;
		for(Long key: keys){
			
			Byte[] bts = map.get(key);
			
			last = false;
			if(key.equals(counter)){
				last = true;
			}
			
			bso.addByteArray(key, bts, last);			
		}
		
		//dis.close();
		MessageDigest inputDigest = dis.getMessageDigest();
		MessageDigest outputDigest = bso.getChecksum();
		
		byte[] idbytes = inputDigest.digest();
		byte[] odbytes = outputDigest.digest();
		
		assertEquals(DatatypeConverter.printHexBinary( idbytes ), (DatatypeConverter.printHexBinary( odbytes )));
		
		tempFile.deleteOnExit();
		FileOutputStream fos2 = new FileOutputStream(home + File.separator + "Messages2.properties");
		BitStreamOutput bso2 = new BitStreamOutput(fos2, MessageDigest.getInstance("MD5"));
		
		Collections.reverse(keys);
		
		last = false;
		for(Long key: keys){
			
			Byte[] bts = map.get(key);
			
			last = false;
			if(key.equals(counter)){
				last = true;
			}
			
			bso2.addByteArray(key, bts, last);			
		}
		
		MessageDigest outputDigest2 = bso2.getChecksum();
		byte[] odbytes2 = outputDigest2.digest();
		
		assertEquals(DatatypeConverter.printHexBinary( idbytes ), (DatatypeConverter.printHexBinary( odbytes2 )));
		
	}

}
