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
package jpl.gds.tm.service.impl.frame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.ccsds.api.tm.frame.TelemetryFrameHeaderFactory;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.checksum.IChecksumCalculator;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.FrameChecksumComputationFactory;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSyncService;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;
import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.config.TelemetryServiceProperties;

/**
 * This class performs telemetry frame synchronization and generates transfer frame messages for
 * in-sync frames. It performs a simple byte level frame sync algorithm. It checks for patterns
 * of PN codes/ASMs that are transfer frame sizes apart. It only requires two PN codes/ASMs
 * at the correct distance apart to acquire sync.
 * 
 *
 * MPCS-7476 - 7/7/15.  Changes throughout to update debug statements and remove
 *          commented out code. Changes to fix "frame flush bug". The latter are the only
 *          changes specifically marked.
 * MPCS-7993 - 3/30/16.  Changes throughout to reflect new interfaces in
 *          the transfer frame dictionary, header, and info objects. Significant changes
 *          are individually marked.          
 *         
 */
public class FrameSyncService implements IFrameSyncService {

	private Tracer log;
	
	/* MPCS-7993 - 3/30/16. Removed access of packet extract VCID properties.
	 * This was never the correct property to use to determine valid VCIDs for framesync.
	 * Also removed MAX_ASM_LENGTH constant. That is now determined from the dictionary.
	 */

	/* MPCS-7476 - 7/7/15. Added constant for min ASM byte length. */
	private static final int MIN_ASM_LENGTH = 4;

	private final IMessagePublicationBus bus;
	private int lastcursor = 0; // last consumed position
	private int cursor = 0; // current position
	private int bufferLen = 0; // holder for length in byte buffer
	private final FrameSyncBuffer buff = new FrameSyncBuffer(); // the data
	private boolean inSync = false;
	private int frameDefIndex = 0; // index into transfer frame format list
	private int maxFrameLength = 0; // maximum transfer frame size
	/* MPCS-7039 - 7/9/10. Changed array to list */
	private List<ITransferFrameDefinition> frameFormats;
	private boolean doChecksumCheck = false;
	private ITransferFrameDefinitionProvider frameDict;
	private ITelemetryFrameInfo lastTfInfo;
	private IAccurateDateTime lastFrameErt;
	private RawDataSubscriber rawSubscriber;
	private EndOfDataSubscriber endSubscriber;

	// Summary counters
	private long numFrames = 0;
	private long numBytes = 0;
	private long frameBytes = 0;
	private long outOfSyncBytes = 0;
	private long numIdle = 0;
	private long numDead = 0;

	// List of valid VCIDS
	private final List<Integer> vcids;

	/* MPCS-7039 - 7/9/15. Map of quick-32 bit ASM longs for fast ASM checking. */
	private final Map<String, Long> quickAsmMap = new HashMap<String, Long>();
	
	/* MPCS-7993 - 3/30/16. Now cache checksum computation instance per frame type,
	 * and store max ASM size from the dictionary into non-constant member. */
	private final Map<String, IChecksumCalculator> checksumMap = new HashMap<String, IChecksumCalculator>();
	
	private int maxAsmLength;
	private int outOfSyncThreshold;
	private boolean flushed;
	private MissionProperties missionProps;
    private IFrameMessageFactory frameMsgFactory;
    private ITelemetryFrameInfoFactory frameInfoFactory;
	
	
	/**
	 * Creates a new instance of FrameSyncService.
	 * 
	 * @param serviceContext the current application context
	 */
	public FrameSyncService(final ApplicationContext serviceContext) { 
       try {
		
			this.bus = serviceContext.getBean(IMessagePublicationBus.class);
			this.frameMsgFactory = serviceContext.getBean(IFrameMessageFactory.class);
			final TelemetryServiceProperties config = serviceContext.getBean(TelemetryServiceProperties.class);
			this.doChecksumCheck = config.doFramesyncChecksum();
			this.outOfSyncThreshold = config.getOutOfSyncReportThreshold();
			this.missionProps = serviceContext.getBean(MissionProperties.class);
            this.frameInfoFactory = serviceContext.getBean(ITelemetryFrameInfoFactory.class);
			
            log = TraceManager.getTracer(serviceContext, Loggers.FRAME_SYNC);

			log.debug("Framesync out of sync threshold is " , this.outOfSyncThreshold , ", checksum flag is " , this.doChecksumCheck);

			/* MPCS-7993 - 3/30/16. Now assume checksum computation may differ
			 * by frame type, so an instance is no longer created here.
			 */
			processTransferFrameXML(serviceContext.getBean(ITransferFrameDefinitionProvider.class));

			/* MPCS-7993 - 3/30/16 - Get valid VCIDs from the mission properties,
			 * not from packet extract properties in the GDS configuration. Just because
			 * we do not extract packets does not mean we do not want to sync the frames.
			 */
			final MissionProperties missionProps = serviceContext.getBean(MissionProperties.class);
			vcids = missionProps.getAllDownlinkVcids();
			if (vcids.isEmpty()) {
				vcids.add(0);
			} 

		} catch (final Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to configure FrameSync", e.getCause());
		}
	}

	/**
	 * This starts services
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 * @return a true boolean if it can make a new rawDataSubscriber and a
	 * new EndOFDataSubscriber
	 */
	@Override
	public boolean startService() {
		this.rawSubscriber = new RawDataSubscriber();        
		this.endSubscriber = new EndOfDataSubscriber();
		return true;
	}

	/**
	 * This stops service
	 * @see jpl.gds.shared.interfaces.IService#stopService()
	 */
	@Override
	public void stopService() {
		if (this.rawSubscriber != null) {
			bus.unsubscribeAll(rawSubscriber);
			rawSubscriber = null;
		}
		if (this.endSubscriber != null) {
			bus.unsubscribeAll(endSubscriber);
			endSubscriber = null;
		}
		flush();
		log.debug("Framesync service stopped");
	}

    /**
     * Parses the transfer frame dictionary and initializes frame definitions.
     * 
     *
     */
    private void processTransferFrameXML(final ITransferFrameDefinitionProvider dict)  {
        this.frameDict = dict;
        this.maxFrameLength = this.frameDict.getMaxFrameSize();
        this.frameFormats = this.frameDict.getFrameDefinitions();

        /*
         * MPCS-7039 - 7/9/15. Populate map of quick-32 bit ASM longs for
         * fast ASM checking.
         */
        for (final ITransferFrameDefinition def : frameFormats) {

            /* MPCS-7993 - 3/30/16. Add computation of max ASM length. */
            maxAsmLength = Math.max(def.getASMSizeBytes(), maxAsmLength);
            this.quickAsmMap.put(def.getName(),
                    GDR.get_u32(def.getASM(), 0));
        }

    }

	private void dumpCursor() {
		log.debug("Current buffer cursor=" , this.cursor , ", last consumed frame cursor=" , this.lastcursor ,
				", current buffer len=" , this.bufferLen);
	}

	/* 
	 * From the given offset in the byte buffer, find the next frame type 
	 *  1. Test next ASM pattern against the byte pattern at offset until a matching ASM is found (or return -1).
	 *  2. Use the length of the matching ASM to see if an ASM can be found at offset + this.tff[i].getCADUSizeBytes()
	 *  3. If a matching ASM was found in step 2 return i as the match.
	 *  4. If all Transfer Frame types have been tried return -1.
	 *  5. Continue from 1
	 *  
	 *  MPCS-7476 - 7/7/15. Added flush argument, set to true if we are performing
	 *  a buffer flush.
	 */
	private int findNextFrameType ( final int offset, final boolean isFlush ) {
		long quickASM;
		byte[] longASM;
		int nextOffset = 0;
		int i = 0;
		int j = 0;
		final int startVal = 0;
		final int endVal   = this.frameFormats.size();
		int nextFrameDef = -1;

		// Pull out 32 bits at the current offset for quick ASM check
		quickASM = this.buff.getU32 ( offset );

		// Loop through all the frame types
		for ( i = startVal; i < endVal; ++i ) {
		    
		    /* MPCS-7476 - 7/7/15. If there is not enough space in the buffer
		     * for a frame of this type, skip checking for it.
		     */
		    if (offset + frameFormats.get(i).getCADUSizeBytes() > this.bufferLen) {
		    	log.debug("findNextFrameType is skipping check for type " , this.frameFormats.get(i).getName() 
		    			, " because not enough bytes for that frame type in the current buffer");
		        continue;
		    }

			// First check the quick ASM for a match
			if ( quickAsmCheckActual (frameFormats.get(i), quickASM ) ) {
				
				log.trace("quick ASM check succeeded for frame type " , frameFormats.get(i).getName());

				// Quick ASM matches. Check the long ASM
				longASM = this.buff.getBuffer ( offset, this.frameFormats.get(i).getASMSizeBytes());

				if ( reverseLongASMCheck (frameFormats.get(i), longASM )) {
					
					log.trace("long ASM check succeeded for frame type " , frameFormats.get(i).getName());
					
					// Long ASM Check passed.
					// At this point we have an ASM of some kind.
					// If the nextOffset is successfully matched to a
					// longASM then we have found a frame

					// This is where we must look for the next ASM if this is a valid Frame
					nextOffset = offset + this.frameFormats.get(i).getCADUSizeBytes();

					/*
					 * MPCS-7476 - 7/7/15. Special check for the very last
					 * frame in the buffer (used only when flushing). If the
					 * next offset is at the end of the buffer, then 
					 * a check for next ASM cannot be performed. This
					 * makes the assumption that if we already matched an ASM,
					 * AND the remaining buffer length exactly matches the length
					 * of this frame type, then we have a last frame of this type.
					 */
					if (isFlush && this.bufferLen == nextOffset) {
					    nextFrameDef = i;
					    break;
					}

					// Loop through all the frame types 
					for ( j = 0; j < this.frameFormats.size(); ++j) {

					    // Are there enough bytes left in the buffer to look ahead this far?
					    /* MPCS-7476 - 7/7/15. Previously this would not look for the
					     * ASM unless there was room in the buffer for the longest possible
					     * ASM. That seems unnecessary. We only need enough buffer to look
					     * for the current ASM.
					     */
					    if ( (nextOffset + frameFormats.get(j).getASMSizeBytes()) < this.bufferLen || (isFlush && nextOffset == this.bufferLen)) {

					        // Do another quick ASM check at the lookahead offset
							final long quickASM2 = this.buff.getU32 ( nextOffset );

							/* MPCS-7892 - 1/21/16. Change index from i to j */
							if ( quickAsmCheckActual (frameFormats.get(j), quickASM2 ) ) {
								// Bingo!  Offset's ASM and size yield another ASM at the nextOffset position.
								// We found what we are looking for.
								// Note that this may not be the actual ASM (could be same family of ASMs).
								// But that doesn't matter, we'll determine the actual type
								// on the next call to this routine.
								// That is why we don't need to do a longASMCheck().
								log.debug(frameFormats.get(i).getName() , " may be followed by " , frameFormats.get(j).getName());
								nextFrameDef = i;
								break;
							}
					    } else {
					    	log.debug("not enough buffer for next frame ASM check for frame type " , frameFormats.get(i).getName());
					        /* MPCS-7476 - 7/7/15. Previous comment stated that we never get here, but we do.
					         */
					        break;
					    }
					} // end for
				} else {
					log.trace("long ASM check failed for frame type " ,frameFormats.get(i).getName());
				}
			} else {
				log.trace("quick ASM check failed for frame type " , frameFormats.get(i).getName());
			}
			if ( 0 <= nextFrameDef ) {
				break;
			}
		} // end for

		return nextFrameDef;
	} // end method findNextEncodingType


	/*
	 * Sends a transfer frame and drops the frame data from the byte buffer.
	 */
	private void sendFrameAndDropBuffers(final int frameSize) {

		sendTransferFrame();
		this.cursor += frameSize;

		while (this.buff.lenFirstBuffer() < this.lastcursor) {
			this.cursor -= this.buff.lenFirstBuffer();
			this.lastcursor -= this.buff.lenFirstBuffer();
			log.debug("Dropping input buffer 0 of length " , this.buff.lenFirstBuffer());
			this.buff.dropFirstBuffer();
			this.bufferLen = this.buff.getNumBytes();
		}
		dumpCursor();
	}

	/**
	 * This is the entry point for raw data.
	 * @param rdm RawDataMessage
	 */
	private void consume(final IPresyncFrameMessage rdm) {
		// Add the data to the byte buffer
		this.buff.consume(rdm);

		// Handle rollover of byte counter
		if (this.numBytes + rdm.getNumBytes() < this.numBytes) {
			this.numBytes = rdm.getNumBytes();
		} else {
			this.numBytes += rdm.getNumBytes();
		}

		// Get the size of the last transfer frame found
		final int tfSize = this.frameFormats.get(frameDefIndex).getCADUSizeBytes();
		this.bufferLen = this.buff.getNumBytes();

		dumpCursor();

		// If we are in sync, and we have seen enough data to determine if we have
		// another frame of the same type as the last one found, call maintainSync.

		// If we are out of sync or do not have enough data yet to see if we can maintain sync,
		// call syncScan to get us back in sync.

		if (this.inSync && ((this.cursor + tfSize + 2 * maxAsmLength) < this.bufferLen) ) {
			log.debug("consume() is IN_SYNC and is invoking maintainSync()");
			this.maintainSync();
		} else {
			log.debug("consume() is invoking syncScan()");
			if (((this.cursor + this.maxFrameLength + 2 * maxAsmLength) < this.bufferLen) ) {
				this.syncScan();
			}
		}
	}

	/*
	 * Entry to this method means that inSync is true.
	 *
	 * You will stay in this routine until one of the following occurs:
	 * 
	 * 1) We drop out of Sync (we do not recognize the beginning bit pattern as any ASM pattern)
	 * 2) There are not enough bytes to complete a frame (more data must be read into the buffer)
	 * 3) We encounter the end of the raw data we have
	 */ 
	private void maintainSync() {

		// Get the size of the last transfer frame found
		int tfSize = this.frameFormats.get(frameDefIndex).getCADUSizeBytes();
		
		while((this.cursor + this.maxFrameLength + 2 * maxAsmLength) < this.bufferLen) { // enough bytes to read?

			// Check for a new frame starting at the cursor in the byte buffer
			final int currEncoding = this.findNextFrameType ( this.cursor, false );

			// We found a new frame
			if ( currEncoding >= 0 ) {
			    
				// If we have found a frame of a different type than the last one, reset the
				// frame type index and frame size.
				if ( this.frameDefIndex != currEncoding ) {
					this.frameDefIndex = currEncoding;
					tfSize = this.frameFormats.get(frameDefIndex).getCADUSizeBytes();
				}
				// We have a valid frame so put it out.
				log.debug ( "maintainSync() found a frame of type " , this.frameFormats.get(currEncoding).getName()  );
				log.debug ( "maintainSync() current buffer cursor ", this.cursor );
				log.debug ( "maintainSync() found frame has length " , this.frameFormats.get(frameDefIndex).getCADUSizeBytes() );
				this.sendFrameAndDropBuffers ( tfSize );
				this.lastcursor = this.cursor;
				dumpCursor();

				// Continue to look for another in-sync frame
				continue;
			}
			// We are out of sync now!! Send the out of sync notification.
			// We don't know what we have, so let syncScan deal with it
			this.inSync = false;
			this.loseSync("No ASM at expected location for frame type " + frameFormats.get(frameDefIndex).getName());
			break;

		} 
		    

	} // end method maintainSync()

	/*  This method is only entered only if:
	 * 
	 *  1) We are out of sync
	 *  2) There are not quite enough bytes to start looking for a Transfer Frame.
	 *  There is NO OTHER REASON to be in this method. 
	 */
	private void syncScan() {

		boolean found_pn = false;   
		int next_pn = 0;

		// Do this while there are enough bytes to hold the longest frame in the bytes buffer
		while ((this.cursor + this.maxFrameLength + 2 * maxAsmLength) < this.bufferLen) {

			// If we get 800 bytes in which no frame is found, send out an out of sync data notification
			// and remove the out of sync bytes from the byte buffer
			if ((this.cursor - this.lastcursor) >= outOfSyncThreshold) {
				log.debug("syncScan() has found more than " , outOfSyncThreshold ," out of sync bytes");
				sendOutOfSyncData();
				this.lastcursor = this.cursor; // always after send outasync data
				while (this.buff.lenFirstBuffer() < this.cursor) {
					this.cursor -= this.buff.lenFirstBuffer();
					this.lastcursor -= this.buff.lenFirstBuffer();
					this.buff.dropFirstBuffer();
					this.bufferLen = this.buff.getNumBytes();
				}
			} 
			// Search for a transfer frame in the remaining data
			found_pn = false;
			final int nextTff = this.findNextFrameType ( this.cursor, false );
			if ( nextTff >= 0) {
				this.frameDefIndex = nextTff;
				found_pn = true;
				next_pn = this.frameFormats.get(frameDefIndex).getCADUSizeBytes();
			}

			// We found a transfer frame
			if (found_pn) {
				this.inSync = true;

				// Send out out of sync data left in the buffer prior to the new frame
				sendOutOfSyncData(); // get rid of stuff before cursor
				this.lastcursor = this.cursor;

				// Notify of in sync status and create the objects to hold the transfer frame
				aquireSync(); // send acquire sync message

				log.debug("syncScan() found a frame of type " , this.frameFormats.get(frameDefIndex).getName() , " at " , this.cursor);

				// Send the transfer frame message for the new frame and drop the frame's data from
				// the byte buffer
				sendFrameAndDropBuffers(next_pn);

				// We have found and sent the first frame.
				return;

			} // end if found_pn for second check
			this.cursor += 1;

		} // end while
	}

	/*
	 * Creates data objects for a new frame and sends the in sync message.
	 */
	private void aquireSync() {

		/*
		 * 6/4/13 - MPCS-4861. Removed stuff no longer needed for
		 * substituting transfer frames and just use the frame format.
		 */	
		final int tf_size = frameFormats.get(frameDefIndex).getCADUSizeBytes();
		final byte[] tf = this.buff.getBuffer(this.cursor, tf_size);

		// Create DSN Info and adjust ert to the raw data ert for the raw data chunk 
		//that contains the first byte of the frame
		final IStationTelemInfo dsnI = this.buff.getDSNInfo(this.cursor);
		dsnI.setErt(this.buff.getErt(this.cursor)); // adjust ert to the raw data ert for the raw data

		// Create the transfer frame info object and populate it 
		/*
		 * MPCS-3923 - 11/3/14. Use new factory method to create the FrameInfo object
		 * and set the header and format.
		 * 
		 * MPCS-7993 - 3/30/16. Frame header factory now takes the transfer frame definition
		 * as argument. No longer assuming the frame header class is the same for every frame type. 
		 */
		final ITelemetryFrameHeader tfh = TelemetryFrameHeaderFactory.create(missionProps, frameFormats.get(this.frameDefIndex));
		final int asmSize =  frameFormats.get(this.frameDefIndex).getASMSizeBytes();
		tfh.load(tf, asmSize);

		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(tfh, this.frameFormats.get(frameDefIndex));
		tfInfo.setDeadCode(tfh.skipContent(tf, asmSize));

		// Send the in sync message
		final IFrameEventMessage frameMsg = frameMsgFactory.createInSyncMessage(dsnI, tfInfo);
		bus.publish(frameMsg);
        log.log(frameMsg);
	}


	private void loseSync(final String msg) {
		log.debug("loseSync() invoked for reason " , msg);
		final IStationTelemInfo dsnI = this.buff.getDSNInfo(this.cursor);
		dsnI.setErt(this.buff.getErt(this.cursor)); // adjust ert to previous ert
		final IFrameEventMessage frameMsg = frameMsgFactory.createLossOfSyncMessage(dsnI, lastTfInfo, msg, lastFrameErt);
		bus.publish(frameMsg);
        log.log(frameMsg);
	}


	private void sendTransferFrame() {

		// Get the frame size and create the frame buffer
		/*
		 * 6/4/13 - MPCS-4861. Removed stuff no longer needed for
		 * substituting transfer frames and just use the frame format.
		 * 
		 * MPCS-7993 - 3/30/16. Cache current frame def rather than
		 * indexing into the array all over the place.
		 */
	    final ITransferFrameDefinition currentFrameDef =  this.frameFormats.get(frameDefIndex);
		final int tf_size =  currentFrameDef.getCADUSizeBytes();
		// have a tf at cursor
		final byte[] tf = this.buff.getBuffer(this.cursor, tf_size);

		// Create DSNInfo and adjust the frame ERT to match the raw data ERT for the raw data chunk that 
		// contained the first frame byte
		final IStationTelemInfo dsnI = this.buff.getDSNInfo(this.cursor);
		dsnI.setErt(this.buff.getErt(this.cursor)); 

		/*
		 * MPCS-3923 - 11/3/14. Use new factory method to create the FrameInfo object
		 * and set the header and format.
		 * 
		 * MPCS-7993 - 3/30/16. Frame header factory now takes the transfer frame definition
         * as argument. No longer assuming the frame header class is the same for every frame type. 
		 */
		final ITelemetryFrameHeader tfh = TelemetryFrameHeaderFactory.create(missionProps, currentFrameDef);
		tfh.load(tf, currentFrameDef.getASMSizeBytes());  

		final ITelemetryFrameInfo tfInfo = frameInfoFactory.create(tfh, currentFrameDef);

		log.debug("sendTransferFrame() is sending frame type " , tfInfo.getType());
		log.debug("Transfer frame info for frame being sent is: " , tfInfo);


		// Adjust summary counters and mark dead/idle frames
		this.numFrames++;
		this.frameBytes += tf_size;
		if (tfh.skipContent(tf, currentFrameDef.getASMSizeBytes())) { 
			log.debug("sendTransferFrame has identified this as a deadc0de frame");
			this.numDead++;
			tfInfo.setDeadCode(true);
		}
		if (tfInfo.isIdle()) {
			this.numIdle++;
			tfInfo.setIdle(true);
		}

		// Validate the frame checksum if configured to do so and frame type supports validation
		
		/* MPCS-7993 - 3/30/16. No longer restricted to performing checksum validation
		 * on only CHECKSUM and TURBO frames, which was never a valid restriction anyway. Map
		 * stores checkum class instance per frame type, in the event it differs. Whether the
		 * frame has a checksum is now explicit in the frame definition.
		 */
		if (currentFrameDef.hasFrameErrorControl() && doChecksumCheck) {
		    
		    
		    IChecksumCalculator checksum = checksumMap.get(currentFrameDef.getName());
		    if (checksum == null) {
		        checksum = FrameChecksumComputationFactory.create(this.frameFormats.get(frameDefIndex).getFormat());
		        if (checksum != null) {
		            checksumMap.put(currentFrameDef.getName(), checksum);
		        }
		    }
		    if (checksum != null) {
				/*
                 * MPCS-8107 - 08/12/16 - checksum returns the calculated
                 * checksum now, instead of determining if the checksum is
                 * "valid". Additionally, expectedChecksum was added to be able
                 * to pull out whatever checksum is stored in the frame data.
                 * This method will do whatever steps are necessary as per the
                 * size and standard being used
                 * 
                 * MPCS-8478 - 10/04/2016 - Changed to use modified IChecksumCalculator methods
                 * 
                 * MPCS-9771 - 6/19/18. Restore the R7 code. The R8 version made an invalid assumption
                 * about the location of the CRC and did not work.
                 * 
                 */
		        final int crcDataAreaSize = currentFrameDef.getTotalHeaderSizeBytes() + currentFrameDef.getDataAreaSizeBytes();
                final int crcLocation = currentFrameDef.getASMSizeBytes() + crcDataAreaSize + currentFrameDef.getOperationalControlSizeBytes();
                final long crcChecksumCalculated = checksum.calculateChecksum(tf, currentFrameDef.getASMSizeBytes(), crcDataAreaSize);
                final long crcChecksumExpected = checksum.expectedChecksum(tf, crcLocation);

                if (crcChecksumCalculated != crcChecksumExpected) {
		            tfInfo.setBad(true);
		            tfInfo.setBadReason(InvalidFrameCode.CRC_ERROR);
		        }
		    }
		}

		if (!tfInfo.isIdle() && !tfInfo.isDeadCode() && !tfInfo.isBad() && !vcids.contains(tfh.getVirtualChannelId())) {
			tfInfo.setBad(true);
			tfInfo.setBadReason(InvalidFrameCode.BAD_VCID);
			// This one we have to publish because there is no packet extract running on an invalid VCID
			final IFrameEventMessage bfm = frameMsgFactory.createBadFrameMessage(dsnI, tfInfo);
	        this.bus.publish(bfm);
            log.log(bfm);
		}

		// Send out the transfer frame message
		// No headers or trailers.

		final ITelemetryFrameMessage tfMsg = frameMsgFactory.createTelemetryFrameMessage(dsnI, tfInfo, tf_size, tf, 0, HeaderHolder.NULL_HOLDER, TrailerHolder.NULL_HOLDER);
        this.bus.publish(tfMsg); // bus listener will log this message
		this.lastTfInfo = tfInfo;
		this.lastFrameErt = dsnI.getErt();
	}

    private void sendOutOfSyncData() {
		if (this.cursor == this.lastcursor) {
			return;
		}
		final IStationTelemInfo dsnI = this.buff.getDSNInfo(this.lastcursor);
		dsnI.setErt(this.buff.getErt(this.lastcursor)); // adjust ert to previous ert
		this.outOfSyncBytes += this.cursor-this.lastcursor;
		final IOutOfSyncDataMessage msg = frameMsgFactory.createOutOfSyncMessage(dsnI, this.buff.getBuffer(this.lastcursor, this.cursor-this.lastcursor));
		this.bus.publish(msg);
        log.log(msg);
        log.debug("sent OUT_OF_SYNC_DATA message" , msg.getMessage());
	}

	/**
	 * This flushes the remaining buffer when the data stream ends
	 * by extracting any remaining frames and reporting out of sync
	 * data for any left over at the end.
	 * 
	 * MPCS-7476 - 7/7/15. Complete re-write.
	 */
	private synchronized void flush() {
	    if (flushed) {
	        return;
	    }
		log.debug("flushing buffers due to end of data");
		dumpCursor();


        /* If we are insync, run a maintain sync to
         * extract any more frames it can find in the buffer.  
         */
		if (this.inSync) {
		    maintainSync();
		    log.debug("flush is done with maintainSync()");
		    dumpCursor();
		}
		
        /*
         * The catch is that maintainSync() stops when it no longer has enough
         * buffered data to check for EVERY frame type AND find a following
         * frame. For the last frame in the buffer, this does not work. Check to
         * see if there are other frames by calling findNextFrameType() directly
         * with the flush argument set to true. A last frame will only be
         * recognized if it can be matched to a frame type using ASM and an
         * exact match to the remaining buffer length. If it does not, there may
         * be a frame there, but we cannot identify it accurately. Even the
         * assumption that there is a last frame based solely upon ASM and
         * length is risky and a true frame synchronizer would likely not return
         * it. However, the alternative is losing the last frame every time we
         * process a RAW_TF file, which most often occurs when frames are
         * queried from the database, and I don't think it would be acceptable
         * to always discard the last frame in that situation. Nor would we be
         * able to process a file containing exactly one frame.
         */
		while (this.cursor + MIN_ASM_LENGTH < this.bufferLen) {
		    frameDefIndex = findNextFrameType(this.cursor, true);
		    if (frameDefIndex < 0) {
		        break;
		    }
		    log.debug("flush found transfer frame in the remaining buffer of type " , this.frameFormats.get(frameDefIndex).getName());
		    this.sendFrameAndDropBuffers (this.frameFormats.get(frameDefIndex).getCADUSizeBytes() );
            this.lastcursor = this.cursor;
		}
		
		/*
		 * Any data still in the buffer is considered out of sync data.
		 * This is a change to behavior because in the past we have
		 * not reported this. If we don't like it, we just need to
		 * remove this block.
		 */
		if (this.lastcursor != this.bufferLen) {
			/* MPCS-7796 - 1/5/16. Do not send loss of sync unless in sync, otherwise
			 * NPE can result.
			 */
			if (this.inSync) {
				this.loseSync("Found out of sync data at end of data stream");
			}
			this.cursor = this.bufferLen;
			sendOutOfSyncData();
		}

		log.debug ( "summary: numFrames=" , this.numFrames , ", numBytes=" ,this.numBytes , ", framesBytes=" , this.frameBytes );
		log.debug ( "summary: outOfSyncBytes=", this.outOfSyncBytes, ", numIdle=", this.numIdle, ", numDead=", this.numDead);
		log.debug ( "finished" );
		flushed = true;
	}
	
    /**
     * Performs a quick check for ASM match for 32 bit ASMs only.
     * 
     * @param format
     *            the frame definition object containing the ASM to test
     * @param testASM
     *            the ASM to test, as a long
     * @return true if the ASM matches; false otherwise
     * 
     * MPCS-7039 - 7/9/15. Moved here from TransferFrameDefinition.
     */
    private boolean quickAsmCheckActual(final ITransferFrameDefinition format,
            final long testASM) {
        // if no ASM on this frame type, we cannot match it
        if (!format.arrivesWithASM()) {
            return false;
        }
        return testASM == this.quickAsmMap.get(format.getName());
    }

    /**
     * Performs a byte by byte check of any length ASM to see if it matches the
     * ASM for this frame format. Does it in reverse order for performance.
     * 
     * @param format
     *            the frame definition object containing the ASM to test
     * @param testAsm
     *            the byte array containing the ASM bytes to check
     * @return true if the ASM matches; false otherwise
     * 
     * MPCS-7039 - 7/9/15. Moved here from TransferFrameDefinition.
     */
    private boolean reverseLongASMCheck(final ITransferFrameDefinition format,
            final byte[] testAsm) {
        
        /* MPCS-7993 - 3/30/16. There is no longer an "actual" ASM
         * vs ASM. There is a flag indicating whether the frame has an ASM.
         * Handling below adjusted accordingly.
         */
        // if no ASM on this frame type, we cannot match it
        if (!format.arrivesWithASM()) {
            return false;
        }
        if (testAsm.length != format.getASMSizeBytes()) {
            return false;
        }
        final byte[] asm = format.getASM();

        for (int i = asm.length - 1; i >= 0; --i) {
            if (testAsm[i] != asm[i]) {
                return false;
            }
        }
        return true;
    }
    
	/**
	 * Listener class for Raw Data messages.
	 *
	 */
	private class RawDataSubscriber implements MessageSubscriber {
		/**
		 * Creates a RawDataSubscriber.
		 */
		public RawDataSubscriber() {
			bus.subscribe(TmServiceMessageType.PresyncFrameData, this);	
		}

		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
		 */
		@Override
		public void handleMessage(final IMessage m) {
            log.debug("RawDataMessage received");
			consume((IPresyncFrameMessage )m);
		}
	}

	/**
	 * Listener class for End Of Data messages.
	 *
	 */
	private class EndOfDataSubscriber implements MessageSubscriber {
		/**
		 * Creates a RawDataSubscriber.
		 */
		public EndOfDataSubscriber() {
			bus.subscribe(CommonMessageType.EndOfData, this);	
		}

		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared.message.IMessage)
		 */
		@Override
		public void handleMessage(final IMessage m) {
			log.debug("EndOfDataMessage received");
			flush();
		}
	}
}

