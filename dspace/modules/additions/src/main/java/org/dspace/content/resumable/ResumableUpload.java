package org.dspace.content.resumable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitstreamStorageManager;

public class ResumableUpload {

	Logger LOG = Logger.getLogger(ResumableUpload.class);

	private static ResumableUpload uploader = null;
	
	private String checksum = null;

	private Map<String, ResumableInfo> uploads = new HashMap<String, ResumableInfo>();

	private ResumableUpload() {
		uploads = new HashMap<String, ResumableInfo>();
	}

	public synchronized static ResumableUpload getInstance() {
		if (uploader == null) {
			uploader = new ResumableUpload();
		}
		return uploader;
	}
	
	public String getChecksum(){
		return checksum;
	}

	
	/**
	 * this is used by resumable js to see if the chunk has already been uploaded
	 * its a cheap get operation
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int resumableChunkNumber = getResumableChunkNumber(request);

		ResumableInfo info = getResumableInfo(request);

		if (info.getUploadedChunks().keySet().contains(resumableChunkNumber)) {
			response.getWriter().print(info.getBitstreamId()); // This Chunk has been
														// Uploaded.
		} else {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	/**
	 * main method to upload the file chunk
	 * @param context - the dspace context
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public boolean doUpload(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean complete = false;
		int resumableChunkNumber = getResumableChunkNumber(request);

		LOG.debug("doUpload: dealing with resumableChunkNumber: " + resumableChunkNumber);
		
		
		ResumableInfo info = getResumableInfo(request);
		RandomAccessFile raf = null;
		// don't want the hashmap changing size when reading or writing to it
		synchronized (uploads) {
			raf = info.getRaf();
			if (raf == null) {
				LOG.debug("doUpload: creating new random access file " + info.getResumableFilePath());

				raf = new RandomAccessFile(info.getResumableFilePath(), "rw");
				info.setRaf(raf);
			} else {
				LOG.debug("doUpload: retrieved random access file ");
			}

		}
		// don't want the random access file written to by more than one request
		// at the same time
		synchronized (raf) {
			// Seek to position
			raf.seek((resumableChunkNumber - 1) * (long) info.getResumableChunkSize());

			// Save to file
			InputStream is = request.getInputStream();
			long readed = 0;
			long content_length = request.getContentLength();
			byte[] bytes = new byte[1024 * 100];
			while (readed < content_length) {
				int r = is.read(bytes);
				if (r < 0) {
					break;
				}
				raf.write(bytes, 0, r);
				readed += r;
			}

			info.getUploadedChunks().put(resumableChunkNumber,readed);
			if (info.checkIfUploadFinished()) {

				// Check if all chunks uploaded, and
				// change filename

				
				LOG.debug("doUpload: All finished. ");
				// send the bitstreamid
				response.getWriter().print(info.getBitstreamId());
				raf.close();

				String filePath = info.getResumableFilePath();

				filePath = filePath.replace(".temp", "");

				File f = new File(info.getResumableFilePath());
				File newFile = new File(filePath);
				f.renameTo(newFile);

				uploads.remove(info.getResumableIdentifier());
				
				// Calculate the checksum asynchronously
				File checksumFile = new File(filePath + ".md5");
				Thread t = new Thread(new ChecksumCalculator(newFile, checksumFile));
				t.start();
			} else {
				LOG.debug("doUpload: Upload Not finished dealing with: " + resumableChunkNumber);
				// return the bitstreamid so that the client can send this to the database.
				response.getWriter().print(info.getBitstreamId());
			}
		}

		return complete;
	}

	private int getResumableChunkNumber(HttpServletRequest request) {
		return HttpUtils.toInt(request.getParameter("resumableChunkNumber"), -1);
	}

	private ResumableInfo getResumableInfo(HttpServletRequest request) throws ServletException {

		int resumableChunkSize = HttpUtils.toInt(request.getParameter("resumableChunkSize"), -1);
		long resumableTotalSize = HttpUtils.toLong(request.getParameter("resumableTotalSize"), -1);
		String resumableIdentifier = request.getParameter("resumableIdentifier");
		String resumableFilename = request.getParameter("resumableFilename");
		String resumableRelativePath = request.getParameter("resumableRelativePath");
		int resumableChunkNumber = HttpUtils.toInt(request.getParameter("resumableChunkNumber"), -1);
		ResumableInfo info = null;
		synchronized (uploads) {

			info = uploads.get(resumableIdentifier);

			if (info == null) {
				LOG.debug("getResumableInfo: No info object creating a new one ");
				info = new ResumableInfo();
				info.setResumableChunkSize(resumableChunkSize);
				info.setResumableFilename(resumableFilename);

				info.setResumableIdentifier(resumableIdentifier);
				info.setResumableRelativePath(resumableRelativePath);
				info.setResumableTotalSize(resumableTotalSize);

				// need to put the id into the database.
				String id = Utils.generateKey();
				info.setBitstreamId(id);
				String path = BitstreamStorageManager.getIntermediatePath(id);
				String base_dir = ConfigurationManager.getProperty("assetstore.dir");

				path = base_dir + "/" + path;

				// check if the directory structure exists if not create it
				File dirs = new File(path);
				if (!dirs.exists()) {
					dirs.mkdirs();
				}

				path = path + "/" + id;

				String resumableFilePath = new File(path).getAbsolutePath() + ".temp";
				info.setResumableFilePath(resumableFilePath);
				LOG.debug("getResumableInfo: This is a GET request not adding to uploads");
				uploads.put(resumableIdentifier, info);

			}
		}

		LOG.debug("getResumableInfo: id: " + resumableIdentifier + " total size: " + resumableTotalSize + " resumableChunkNumber: " + resumableChunkNumber + " resumableChunkSize: "
				+ resumableChunkSize + " resumableFilePath: " + info.getResumableFilePath());

		if (!info.vaild()) {
			uploads.remove(info);
			throw new ServletException("Invalid request params.");
		}
		return info;
	}

	private class ChecksumCalculator implements Runnable
	{
		private Boolean initialised = false;
		private File file;
		private File checksumFile;

		public ChecksumCalculator(File file, File checksumFile) {
			this.file = file;
			this.checksumFile = checksumFile;
			synchronized (initialised) {
				initialised = true;
			}
			LOG.debug("ChecksumCalculator created and initialised a ChecksumCalculator");
		}

		@Override
		public void run() {
			LOG.debug("ChecksumCalculator created and running a ChecksumCalculator");
			synchronized (initialised) {
				if (initialised){
					LOG.debug("ChecksumCalculator has been initialised");
					PrintWriter pw = null;
					try {
						pw = new PrintWriter(checksumFile);
						String checksum = org.dspace.curate.Utils.checksum(file, "MD5");
						pw.println(checksum);
						LOG.debug("ChecksumCalculator.run: Checksum generated for " + file.getAbsolutePath() + " " + checksum);
					} catch (IOException e) {
						LOG.error("ChecksumCalculator.run: ", e);
					} finally {
						if (pw != null) {
							pw.close();
						}
					}
				} else {
					LOG.error("ChecksumCalculator.run: Not initialised can't generate checksum");
				}
			}
			LOG.debug("ChecksumCalculator has completed");
		}
	}
}
