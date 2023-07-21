/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.globallad.data;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.io.IndexAdjuster;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Tracer;

/**
 * Very close to the DataMessageConstructor code.  Could not figure out a good way to make that class
 * usable outside of the coupling it has with the global lad clients.  This is a standalone class that 
 * can be used to convert binary data into global lad data objects.
 * <p>
 * The data in the stream / buffer must be one of the following forms:
 * 1.  Back to back global lad binary data words including sync words.
 * 2.  Verified delta query data.  This has a data word count followed by the data for all 
 * 	   three of the data types.  complete, incomplete and unknown.  
 * <p>
 * Using this as an iterator in verified mode is a little clunky and needs some explaining.   In order
 * to allow verified data to be streamed a few extra steps must be used by the caller.  An internal value for 
 * the delta time type enum is stored for the LAST value that has been created.  
 * <p>
 * This class in not thread safe!
 */
public class GlobalLadDataIterator implements Iterator<IGlobalLADData>, Closeable {
	private static final Tracer log = GlobalLadProperties.getTracer();
	private static final int READ_SIZE = 1024;
	public static final int WORD_LENGTH_SIZE = 4;
	
	/**
	 * Verified queries have 3 integers for complet, incomplete and unknown counts, hence 12 bytes.
	 */
	private static final int VERIFIED_QUERY_COUNT_SIZE = 12;
	private List<ByteBuffer> data;
	private List<Integer> sizes;
	
	/** 
	 * Used to tell if the current pointer is positioned at the byte after
	 * a sync word.  This will be used when the beginning of a data word
	 * has been found but there is not enough data to finish the word and we
	 * must not skip the data in the next creation cycle. 
	 * 
	 **/
	private boolean pointerAlignedAfterMarker;
	
	private Integer currentOffset;
	private Integer maxOffset;
	
	private final IGlobalLadDataFactory factory;
	private final InputStream dataStream;
	
	private final boolean isVerified;
	
	private boolean countsInitialized;
	private int completeCount;
	private int incompleteCount;
	
	@SuppressWarnings("unused")
	private int unknownCount;
	
	/**
	 * The current count for the group.  
	 */
	private int currentMessageCount;
	
	/**
	 * @param dataStream - Input stream to read data from.
	 * @param factory - Used to convert byte data toe IGlobalLadData objects.
	 * @param isVerified - If the data is a verified response data from the server set this to true.  
	 */
	public GlobalLadDataIterator(InputStream dataStream, IGlobalLadDataFactory factory, boolean isVerified) {
		this.dataStream = dataStream;
		this.factory = factory;
		this.isVerified = isVerified;
		
		this.completeCount = -1;
		this.incompleteCount = -1;
		this.unknownCount = -1;
		countsInitialized = false;
		
		currentMessageCount = 0;
		
		data = new ArrayList<ByteBuffer>(10);
		sizes = new ArrayList<Integer>(10);
		
		pointerAlignedAfterMarker = false;
		currentOffset = 0;
		maxOffset = -1;
	}
	
	/**
	 * @param data - Data to be converted to IGlobalLadData objects.
	 * @param factory - Used to convert byte data toe IGlobalLadData objects.
	 */
	public GlobalLadDataIterator(byte[] data, IGlobalLadDataFactory factory, boolean isVerified) {
		this(new ByteArrayInputStream(data), factory, isVerified);
	}

	
	/**
	 * @param buffer - Data to be converted to IGlobalLadData objects.
	 * @param factory - Used to convert byte data toe IGlobalLadData objects.
	 */
	public GlobalLadDataIterator(ByteBuffer buffer, IGlobalLadDataFactory factory, boolean isVerified) {
		this(buffer.array(), factory, isVerified);
	}

	
	/* (non-Javadoc)
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		dataStream.close();
	}
	
	/**
	 * Creates a mapping of the data grouped by status type.  If this is an non-verified stream of data 
	 * everything will be assumed to be complete.
	 * 
	 * @return Map of the data converted from the input data source.  These collections will be keyed by the DeltaQueryStatus of the collection.
	 */
	public Map<DeltaQueryStatus, Collection<IGlobalLADData>> getAllDataVerified() {
		Map<DeltaQueryStatus, Collection<IGlobalLADData>> resultMap = 
				new HashMap<DeltaQueryStatus, Collection<IGlobalLADData>>();
		
		DeltaQueryStatus status = null;
		IGlobalLADData data;
		
		while (hasNext()) {
			data = next();
			
			if (data != null) {
				status = lastStatus();
				
				if (!resultMap.containsKey(status)) {
					resultMap.put(status, new ArrayList<IGlobalLADData>());
				}
				
				resultMap.get(status).add(data);
			}
		}
		
		return resultMap;
	}

	/**
	 * Convenience method that will iterate through the data stream and create a collection of data 
	 * from the input stream.  This works with the verified flag set as well.  However in that case, no 
	 * seperation of the data will be done, it will all be added to the same collection.
	 * <p>
	 * NOTE:  If the stream has many data objects in it (meaning lots of bytes) this could cause out 
	 * of memory issues.  Should only be used when the number of objects to be created is somewhat low.
	 * 
	 * @param dataStream
	 * @return Collection of the converted data read from the input data source.
	 * @throws IOException 
	 */
	public Collection<IGlobalLADData> getAllData() throws IOException {
		Collection<IGlobalLADData> gladData = new ArrayList<IGlobalLADData>();
		IGlobalLADData data;

		while(hasNext()) {
			data = next();
			
			if (data != null) {
				gladData.add(data);
			}
		}
		
		return gladData;
	}
	
	/**
	 * Reads data from the input and will add it to the internal data.  
	 * 
	 * @return True if any data was read from the stream.
	 * @throws IOException
	 */
	private boolean readData() throws IOException {
		byte[] buffer = new byte[READ_SIZE];
		int bytesRead = this.dataStream.read(buffer);
		
		if (bytesRead > 0) {
			ByteBuffer bb = ByteBuffer.wrap(buffer, 0, bytesRead);
			data.add(bb);
			sizes.add(bb.remaining());
			
			this.maxOffset += bb.remaining();
		}
		
		return bytesRead > 0;
	}

	/**
	 * Calculates the status of the last returned value.  
	 * 
	 * @return Calculated the status of the last returned value.  
	 */
	private DeltaQueryStatus lastStatus() {
		/**
		 * Fixed logic that would always make the first incomplete
		 * message be marked as unknown due to a bogus check.  
		 */

		if (completeCount > 0 && currentMessageCount <= completeCount) {
			return DeltaQueryStatus.complete;
		} else if (incompleteCount > 0 && 
				   (completeCount == 0 || currentMessageCount > completeCount) && 
				   currentMessageCount <= completeCount + incompleteCount
				   ) {
			return DeltaQueryStatus.incomplete;
		} else {
			return DeltaQueryStatus.unknown;
		}
	}
	
	/**
	 * Reads the counts off of the data stream for the number of messages for each type.  This 
	 * should only be called when dealing with a verified set of data.
	 */
	private synchronized void initCounts() {
		if (countsInitialized) {
			/**
			 * no-op
			 */
			return;
		}
		
		try {
			while (currentOffset + VERIFIED_QUERY_COUNT_SIZE > maxOffset) {
				boolean dataRead = readData();
				
				if (!dataRead) {
					/**
					 * Not enough data to set the deals. 
					 */
					return;
				}
			}
			
			/**
			 * Set the values and be done with it.
			 */
			this.completeCount = GDR.get_i32(getSlice(4), 0);
			this.incompleteCount = GDR.get_i32(getSlice(4), 0);
			this.unknownCount = GDR.get_i32(getSlice(4), 0);
			this.countsInitialized = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		if (isVerified && !countsInitialized) {
			/**
			 * Initialize the data counts from the input stream.
			 */ 
			this.initCounts();
		}
		
		if (maxOffset > currentOffset) {
			return true;
		} else {
			/**
			 * Must do a little digging before we assume there is no data left.  This could be 
			 * called at a time when all the data in the internal store has been used 
			 * but there is more data in the input stream.  Do a read and check again.
			 */
			try {
				return readData();
			} catch (IOException e) {
				// Got an error so no more data.
				return false;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public IGlobalLADData next() {
		/**
		 * Must have this block in both the next and hasNext methods because some maniac may 
		 * call next before hasNext.
		 */
		if (isVerified && !countsInitialized) {
			/**
			 * Initialize the data counts from the input stream.
			 */ 
			this.initCounts();
		}
		
		/**
		 * Keep trying until we run out of data in the input stream or we get a 
		 * complete message to send.
		 */
		try {
			IGlobalLADData data = getNextMessage();

			while (data == null) {
				boolean dataRead = readData();
				
				data = getNextMessage();
				
				if (data != null) {
					this.currentMessageCount++;
				} else if (!dataRead) {
					/** 
					 * No more data and we can not get any more from the input.  Set max size to 
					 * -1 so that has next will return false and then break.
					 */
					this.maxOffset = -1;
					break;
				} else {
					/**
					 * We did not get a complete message but we did have a data read so 
					 * do another loop.
					 */
				}
			}
			
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			/**
			 * Once we are done clean up our mess.
			 */
			cleanData();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	/**
	 * Counts up the sizes of the data and will set the max offset.  If there is no data, will 
	 * be -1.  Will also update the currentOffset pointer in case data was removed and the 
	 * pointer needs to be updated.
	 * 
	 * Based on offset, this will drop any data from the beginning of the list and reset the offset.  
	 * This checks if the current offset is greater than the size of any arrays at the front of the list.
	 * If it is, it will drop the arrays from data as well as the size from sizes.
	 */
	private void cleanData() {
		if (currentOffset <= 0 || maxOffset < 0) {
			// Nothing has been set or it has been reset.  Do nothing.
		} else {
			List<Integer> stale = new ArrayList<Integer>();
			int adjust = 0;
			
			// Figure out what lists can be removed based on the offset.
			for (int index = 0; index < data.size(); index++) {
				int size = sizes.get(index);
				
				if (currentOffset >= adjust + size) {
					adjust += size;
					stale.add(index);
				} else {
					break;
				}
			}
				
			for (int index = stale.size() - 1; index >= 0; index--) {
				data.remove(index);
				sizes.remove(index);
			}
			
			// Adjust the current offset and max offset.
			currentOffset -= adjust;
			maxOffset -= adjust;
		}
	}
		
	/**
	 * Given the offset, this find the index of the given internal array.  
	 * 
	 * @param offset
	 * @return - IndexAdjuster for offset.
	 */
	private IndexAdjuster getIndexForOffset(int offset) {
		int adjust = 0;
		
		for (int index = 0; index < data.size(); index++) {
			int size = sizes.get(index);
			
			if (offset >= adjust + size) {
				adjust += size;
			} else {
				return new IndexAdjuster(index, adjust, offset);
			}
		}	
		
		return new IndexAdjuster();
	}
		
	/**
	 * Goes to the internal lists and finds the value at the given offset.  If the offset is greater than 
	 * the amount of data we have, returns null;
	 * 
	 * @param offset
	 * @return
	 */
	private Byte getByte(int offset) {
		/*
		 * 1.  Go through all the size values in the sizes array.  Add those up to find the 
		 * 	   index of the array with the value in it. 
		 * 2.  Get the array and return the byte at the adjusted position.
		 */
		IndexAdjuster ia = getIndexForOffset(offset);
		
		if (ia.isSet()) {
			return data.get(ia.getIndex()).get(ia.getAdjustedOffset());
		} else {
			return null;
		}
 	}
	
	/**
	 * Pass in the offset value and the length of the sync word to check.  
	 * 
	 * @param offset
	 * @param len
	 * @return
	 */
	private boolean checkForMarker(int offset, int len, byte[] marker) {
		boolean found = false;
		
		for (int adjust = len - 1; adjust >= 0; adjust--) {
			Byte b = getByte(offset + adjust);
			if (b != null && b == marker[adjust]) {
				found = true;
			} else {
				found = false;
				break;
			}
		}
		
		return found;
	}
		
	/**
	 * Base method to find a marker (pattern) of bytes.  This will update the currentOffset to either 
	 * point at the first byte of the marker, or if the marker is not found with the data that is given
	 * will point at the byte in position maxOffset - marker.length - 1.
	 * 
	 * @param marker
	 * @return index of the marker (currentOffset) or -1 if the marker was not found.  
	 */
	private int findMarker(byte[] marker) {
		do {
			if (checkForMarker(currentOffset, marker.length, marker)) {
			// We found sync marker, return the offset.
				return currentOffset;			
			}
		
			currentOffset++;
		} while (currentOffset <= maxOffset - marker.length + 1);
		
		return -1;
	}
	
	/**
	 * Assumes the pointer is set to the first byte after the sync word. 
	 * 
	 * @return IGlobalLADData or null if no messages left.
	 * 
	 * @throws GlobalLadDataException - If there is an issue building a message.
	 */
	private IGlobalLADData getNextMessage() throws GlobalLadDataException {
		/**
		 * Check if we are already at the end of the data and return quickly.  
		 * After that find the start marker.  If the marker is not found, return 
		 * quickly.
		 * 
		 * Nominally the marker step should be very simple each time assuming
		 * there are no gaps.
		 * 
		 * Check if we have enough bytes (4) to get the word size. 
		 */
		if (currentOffset + WORD_LENGTH_SIZE - 1 > maxOffset || 
			! pointerAlignedAfterMarker && currentOffset + AbstractGlobalLadData.GLAD_PACKET_START_WORD.length >= maxOffset) {
			return null;
		}
		
		if (pointerAlignedAfterMarker) {
			// Already sync'd up to the start word from previous call.  Do nothing.
		} else if (findMarker(AbstractGlobalLadData.GLAD_PACKET_START_WORD) >= 0) {
			// We found the marker and can advance the pointer to after the sync word.
			currentOffset += AbstractGlobalLadData.GLAD_PACKET_START_WORD.length;
			pointerAlignedAfterMarker = true;
		} else {
			// We are not sync'd and could not find the word.  Return null.
			log.warn("Failed to sync on the syn word: " , this);
			return null;
		}
		
		/**
		 * It is possible that the marker was found and current and max offset are now equal, meaning
		 * there is no more data.  Check this case and return null to wait for more data.
		 */
		if (currentOffset == maxOffset) {
			log.trace("Aligned after sync word with no data left: " , this);
			return null;
		}
		
		// First thing, get the size of the next data word.
		final int wordSize = getWordSize(currentOffset);

		/**
		 * It is possible that the current offset is equal to the max offset.  In that case
		 * the word size would be returned as -1.  Need to check if it is negative and the 
		 * this case it true and return null in that case.
		 */
		if (wordSize <= 0) {
			throw new GlobalLadDataException("Word size was negative");
		} else if (wordSize > Integer.MAX_VALUE) {
			throw new GlobalLadDataException("Word size was greater than maximum word size: " + wordSize);
		} else if (maxOffset - currentOffset + 1 < wordSize) {
			/**
			 * The word size fields are included in the word size.
			 */
			return null;
		} else {
			/**
			 * Adjust the offset for the word size fields.
			 */
			currentOffset += WORD_LENGTH_SIZE;
			
			final int remainingBytes = wordSize - WORD_LENGTH_SIZE;
			byte[] slice;
			try {
				slice = getSlice(remainingBytes);
			} catch (Exception e) {
				throw new GlobalLadDataException(String.format("Failed to extract a byte slice from the current data for the last %d bytes of the data word: %s",
						remainingBytes, e.getMessage()));
			}
			
			IGlobalLADData glad;
			
			try {
				glad = factory.loadLadData(slice);
			} catch (Exception e) {
				pointerAlignedAfterMarker = false;
				throw new GlobalLadDataException(e);
			}

			// Move past the sync word.
			currentOffset += AbstractGlobalLadData.GLAD_PACKET_START_WORD.length;
			return glad;
		}
	}
	
	/**
	 * Based on the offset gets the word size which is a short.  If there are not enough bytes based on the 
	 * offset -1 otherwise returns a U16 for the size of the word.  This does not adjust the current offset in 
	 * case the size is calculated and there are not enough bytes for the current message.  Make sure to adjust
	 * the current offset after this call by two if necessary.
	 * 
	 * @param os
	 * @return
	 */
	private Integer getWordSize(int os) {
		if (maxOffset < os + 1) {
			return -1;
		} else {
			// Don't want to advance the pointer here, so just set the bytes.
			ByteBuffer wb = ByteBuffer.allocate(WORD_LENGTH_SIZE);
			
			for (int i = 0; i < WORD_LENGTH_SIZE; i++) {
				wb.put(getByte(os+i));
			}
			wb.rewind();
			return wb.getInt();
		}
	}
		
	/**
	 * Give the number of bytes to return.  This uses and updates currentOffset to get the data.
	 * If there are not enough bytes to return len bytes, returns an empty byte array.
	 * 
	 * @param len
	 * @return
	 */
	private byte[] getSlice(int len)  {
		int endOffset = currentOffset + len - 1;
		
		if (endOffset > maxOffset) {
			return new byte[0];
		} else {
			ByteBuffer buffer = ByteBuffer.allocate(len);
			int bytesCopied = 0;

			do {
				IndexAdjuster index = getIndexForOffset(currentOffset);
				
				// Need to adjust the length for the adjusted offset which will be in the 
				// begining portion of the array.  The two will make it so the proper amount
				// is received for example [x x x x 1 2 3 4 5 x x x x].  
				// Also need to figure out if there is an adjust in the middle.
				
				// Think I made this too hard before.  This should be the as follows:
				// lenght_current - adjust - bytes copied.  This should work every time.  
				int length = sizes.get(index.getIndex()) - index.getAdjustedOffset();
				
				// This figures out the length based on the amount left and the end index
				// that will be copied.
				if (len - bytesCopied < length) {
					length = len - bytesCopied;
				}			
				buffer.put(data.get(index.getIndex()).array(), index.getAdjustedOffset(), length);
				
				// add to currentOffset and the deal.
				currentOffset += length;
				bytesCopied += length;
			} while (bytesCopied < len);
			
			return buffer.array();
		}
	}	
}
