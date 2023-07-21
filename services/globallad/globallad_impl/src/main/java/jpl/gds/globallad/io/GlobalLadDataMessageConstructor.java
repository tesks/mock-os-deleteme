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
package jpl.gds.globallad.io;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.lmax.disruptor.EventHandler;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.IGlobalLadJsonable;
import jpl.gds.globallad.data.AbstractGlobalLadData;
import jpl.gds.globallad.data.GlobalLadDataException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory;
import jpl.gds.globallad.disruptor.ByteBufferEvent;
import jpl.gds.globallad.disruptor.IDisruptorProducer;
import jpl.gds.shared.log.Tracer;

/**
 * Disruptor ByteBufferEvent handler.  Creates the global lad data objects from the 
 * byte arrays received from the clients.  Once the objects are created calls onData to pass the new objects
 * to the global lad disruptor to be inserted into the global lad.
 */
public class GlobalLadDataMessageConstructor implements EventHandler<ByteBufferEvent>, IGlobalLadJsonable {
	private static final Tracer log = GlobalLadProperties.getTracer();

    /** The size of a word (4 bytes) */
	public static final int WORD_LENGTH_SIZE = 4;
	private static final String DM_BASE_NAME = "DataMessageConstructor";
	
	private long numBytes;
	private long numEvents;
	private long numMessages;
	private final List<ByteBuffer> data;
	private final List<Integer> sizes;
	
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
	
	private long constructionTimeNS;
	
	private final IDisruptorProducer<IGlobalLADData> dataProducer;
	
	/**
	 * MPCS-8065 triviski 3/28/2016 - Streamlining the inserts.  There is no 
	 * reason to create more than one of these.
	 */
	private final IndexAdjuster adjuster;
	
	/**
	 * 
	 * @param factory Factory to create the global lad data objects.
	 * @param dataProducer Called when a message is created.  Expected to pass the data to the 
	 * global lad disruptor to be inserted into the global lad.
	 */
	public GlobalLadDataMessageConstructor(final IGlobalLadDataFactory factory, 
			final IDisruptorProducer<IGlobalLADData> dataProducer) {
		this.factory = factory;
		this.dataProducer = dataProducer;
		
        data = new ArrayList<>(2);
        sizes = new ArrayList<>(2);
		
		pointerAlignedAfterMarker = false;
		currentOffset = 0;
		maxOffset = -1;
		
		numMessages = 0;
		constructionTimeNS = 0;
		numEvents = 0;
		numBytes = 0;
		
		adjuster = new IndexAdjuster();
	}

	/**
	 * Event handler used by the global lad socket client disruptor to handle input byte buffers. 
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long, boolean)
	 */
	@Override
	public void onEvent(final ByteBufferEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		final long start = System.nanoTime();
		final int remaining = event.buffer.remaining();
		try {
			
			data.add(event.buffer);
			sizes.add(remaining);
			
			/**
			 * On startup the maxOffset is = -1, so that every add will get the value to the correct position.  
			 */
			maxOffset += remaining;
			
			dataAdded();
		} finally {
			constructionTimeNS += System.nanoTime() - start;
			numBytes += remaining;
			numEvents++;
			
			/**
			 * Once we are done with this event we need to set the buffer to null so it can be GC'd.  If we don't 
			 * the buffers can end up in old gen space because the events are never GC'd and this causes out of memory
			 * issues if the ring buffer size is large.  We are guaranteed to be the only handler working on this event at 
			 * any time so this is a safe procedure.
			 */
			event.set(null);
		}
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
            final List<Integer> stale = new ArrayList<>();
			int adjust = 0;
			
			// Figure out what lists can be removed based on the offset.
			for (int index = 0; index < data.size(); index++) {
				final int size = sizes.get(index);
				
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
	 * @return new index adjuster for offset
	 */
	private IndexAdjuster getIndexForOffset(final int offset) {
		int adjust = 0;
		
		/**
		 * MPCS-8065 triviski 3/28/2016 - Reset the adjuster.
		 */
		adjuster.adjust(-1, -1, -1);

		for (int index = 0; index < data.size(); index++) {
			final int size = sizes.get(index);
			
			if (offset >= adjust + size) {
				adjust += size;
			} else {
				adjuster.adjust(index, adjust, offset);
				break;
			}
		}	
		
		return adjuster;
	}
		
	/**
	 * Goes to the internal lists and finds the value at the given offset.  If the offset is greater than 
	 * the amount of data we have, returns null;
	 * 
	 * @param offset
	 * @return byte at offset, null if out of range
	 */
	private Byte getByte(final int offset) {
		/*
		 * 1.  Go through all the size values in the sizes array.  Add those up to find the 
		 * 	   index of the array with the value in it. 
		 * 2.  Get the array and return the byte at the adjusted position.
		 */
		final IndexAdjuster ia = getIndexForOffset(offset);
		
		if (ia.isSet()) {
			return data.get(ia.getIndex()).get(ia.getAdjustedOffset());
		} else {
			return null;
		}
 	}
	
	/**
	 * Pass in the offset value and the length of the sync word to check.  
	 * 
	 * @param offset offset to start the search
	 * @param marker pattern to search for
	 * @return
	 */
	private boolean checkForMarker(final int offset, final byte[] marker) {
		boolean found = false;
		
		for (int adjust = marker.length - 1; adjust >= 0; adjust--) {
			final Byte b = getByte(offset + adjust);
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
	private int findMarker(final byte[] marker) {
		do {
			if (checkForMarker(currentOffset, marker)) {
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
	 * @throws GlobalLadDataException If there is an issue building a message.
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
		
		if (!pointerAlignedAfterMarker && 
			maxOffset < currentOffset + AbstractGlobalLadData.GLAD_PACKET_START_WORD.length) {
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
		 * 
		 * MPCS-9427 - triviski 4/1/2018 - Was getting errors when there were enough bytes for the sync word but
		 * not the full amount for the word length.  Update this to make sure there are enough bytes for the word 
		 * size.
		 */
		if (currentOffset + WORD_LENGTH_SIZE - 1 >= maxOffset) {
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
			} catch (final Exception e) {
				throw new GlobalLadDataException(String.format("Failed to extract a byte slice from the current data for the last %d bytes of the data word: %s",
						remainingBytes, e.getMessage()));
			}
			
			IGlobalLADData glad;
			
			try {
				glad = factory.loadLadData(slice);
			} catch (final Exception e) {
				pointerAlignedAfterMarker = false;
				throw new GlobalLadDataException(e);
			}

			// Move past the sync word.
			currentOffset += AbstractGlobalLadData.GLAD_PACKET_START_WORD.length;
			return glad;
		}
	}
	
	private void printDataToStderr() {
		// MPCS-9775 - the data dump below can produce TONS of data (as in gigs). The values aren't useful to
		//             a normal user, but it may perhaps be to a global lad genius (e.g. Matt), so we'll wrap
		//             this in a isDebugEnabled() check, but leave the output to error so if it is enabled, it
		//             still goes to the same error-associated endpoints.
		if (log.isDebugEnabled()) {
			log.error("SIZES: " + Arrays.toString(sizes.toArray()));
			int count = 1;
			for (final ByteBuffer d : this.data) {
				log.error(String.format("%d: %s", count++, Arrays.toString(d.array())));
			}
		}
	}
	/**
	 * Processes all of the available data to create all of the available global lad data objects.  
	 */
	private void dataAdded() {
        IGlobalLADData gladData = null;
		do {
			try {
                gladData = getNextMessage();
			} catch (final GlobalLadDataException e) {
				log.error("Failed to create data message: " + ExceptionUtils.getStackTrace(e), e.getCause());
				printDataToStderr();
				pointerAlignedAfterMarker = false;
				continue;
			}
			
            if (gladData != null) {
				/**
				 * Use the producer to add the data to the global lad data ring buffer.
				 */
                dataProducer.onData(gladData);
				numMessages++;
			}
        } while (gladData != null);
	
		cleanData();
	}
	
	/**
	 * Get the word size for the data, which will be a U16.  This class does not change the currentOffset
	 * so be sure to update the pointer if necessary.
	 * 
	 * @param offset offset to work with
	 * @return U16 for the size of the word or -1 if not enough bytes to create word size.
	 */
	private Integer getWordSize(final int offset) {
		if (maxOffset < offset + WORD_LENGTH_SIZE - 1) {
			return -1;
		} else {
			// Don't want to advance the pointer here, so just set the bytes.
			final ByteBuffer wb = ByteBuffer.allocate(WORD_LENGTH_SIZE);
			
			for (int i = 0; i < WORD_LENGTH_SIZE; i++) {
				wb.put(getByte(offset+i));
			}
			wb.rewind();
			
			return wb.getInt();
		}
	}
		
	/**
	 *This uses and updates currentOffset to get the data get get a slice of data with len bytes.
	 * 
	 * @param len number of bytes for the slice
	 * @return byte[] with len bytes from the current offset, empty array if not enough bytes left for the slice.
	 */
	private byte[] getSlice(final int len)  {
		final int endOffset = currentOffset + len - 1;
		
		if (endOffset > maxOffset) {
			return new byte[0];
		} else {
			final ByteBuffer buffer = ByteBuffer.allocate(len);
			int bytesCopied = 0;

			do {
				final IndexAdjuster index = getIndexForOffset(currentOffset);
				
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
		
	/**
     * Gets the average construction time as ns
     * 
     * @return the total time constructing messages
     */
	public double averageConstructionTimeNS() {
		return numMessages > 0 ? constructionTimeNS / numMessages : 0;
	}

    /**
     * Gets the record creation time per second
     * 
     * @return the number of creations per second
     */
	public long createsPerSecond() {
		final double avgNano = averageConstructionTimeNS();
		return avgNano > 0 ? (long) (1 / (avgNano / 1E9)) : 0;
	}
	
	@Override
	public JsonObject getStats() {
		return Json.createObjectBuilder()
				.add("numDisruptorEvents", numEvents)
				.add("numBytesReceived", numBytes)
				.add("numMessages", numMessages)
				.add("internalBuffersSize", sizes.size())
				.add("constructionTimeNS", constructionTimeNS)
				.add("createsPerSecond", createsPerSecond())
				.build();
	}

	@Override
	public String getJsonId() {
		return DM_BASE_NAME;
	}

	@Override
	public String toString() {
		final StringBuilder b = new StringBuilder();
		b.append("CurrentOffset="+currentOffset);
		b.append(" MaxOffset="+maxOffset);
		b.append(" messageCount="+this.numMessages);
		b.append(" sizes="+Arrays.toString(sizes.toArray()));
		return b.toString();
	}

	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		return getStats();
	}
}
