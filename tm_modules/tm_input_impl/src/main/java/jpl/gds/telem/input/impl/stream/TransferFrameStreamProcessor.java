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
package jpl.gds.telem.input.impl.stream;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.ITransferFrameDataProcessor;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawTransferFrameMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;

/**
 * This class is an implementation of <code>IRawStreamProcessor</code> that
 * processes transfer frame streams.
 * 
 *
 */
public class TransferFrameStreamProcessor extends AbstractRawStreamProcessor {
	/*  MPCS-12190 - 2021-07-27. Set initial bitrate to 1, and remove 10KB threshold for bitrate calculation */
	private static final double DEF_BIT_RATE = 1.0;
	/*  MPCS-7348 - 5/18/15. Add constants for bit rate calculation */
	private static final int RATE_TIME_THRESHOLD = 250;
    private final IFrameMessageFactory frameMsgFactory;
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public TransferFrameStreamProcessor(final ApplicationContext appContext) {
	    super(appContext);
	    this.frameMsgFactory = appContext.getBean(IFrameMessageFactory.class);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream, jpl.gds.telem.input.api.message.RawInputMetadata)
	 */
	@Override
	public void processRawData(final IRawInputStream inputStream,
	        final RawInputMetadata metadata) throws RawInputException, IOException {
		if (metadata.shouldDiscard()) {
			return;
		}

		if (inputStream == null) {
			throw new IllegalArgumentException("Null data input source");
		} 

		int len = 0;
		final int bufSize = this.rawConfig.getReadBufferSize();

		byte[] buff = new byte[bufSize];
		double bitRate = DEF_BIT_RATE;
		
		/*  MPCS-7348 - 5/18/15. Track number of bytes for bitrate calculation */
		int bitRateCalcLen = 0;

		long startTime = System.currentTimeMillis();
		final DataInputStream dis = inputStream.getDataInputStream();
		final byte[] data = inputStream.getData();

		boolean singleRead = false;

		while (!isStopped()) {
			RawInputMetadata metadataClone = null;
			try {
				metadataClone = metadata.clone();
			} catch (final CloneNotSupportedException e) {
				metadataClone = new RawInputMetadata();
				logger.error("Cannot clone RawInputMetadata object.", e);
			}

			if (data == null) {
				len = 0;
				int tmplen = 0;
				while (len < bufSize && !isStopped()) {
					/*
					 *  7/3/13 - MPCS-4696. When connection is closed,
					 * this read will throw. Allowing it to just throw up
					 * to the caller will lose what is currently in the buffer.
					 * If we are deliberately stopped, we want to process
					 * that stuff before stopping. Otherwise, re-throw.
					 */
					try {
                        // MPCS-5013 07/29/13
                        setEofOnStreamStatus(false);

						tmplen = dis.read(buff, len, bufSize - len);
					} catch (final IOException e) {
						if (this.isStopped()) {
							break;
						} else {
							throw e;
						}
					}
					if (tmplen < 0) {
                        // MPCS-5013 07/29/13 Needed for client socket input

                        // End-of-file

                        setEofOnStreamStatus(true);

						if (len > 0) {
							break;
						}

						setAwaitingFirstData(true);

						if (this.stopped) {
							return;
						}

						// MPCS-4632 10/13/16 - Changed to EOFException to properly identify it
						throw new EOFException("Error reading buffer");
					}

					len += tmplen;
					if (len > (bufSize / 2)) {
						break;
					}
				}
			} else {
				len = data.length;
				buff = data;
				singleRead = true; // we don't have a stream of data
			}

			messenger.incrementReadCount();
			
			/*  MPCS-7348 - 5/18/15. Track number of bytes for bitrate calculation */
			bitRateCalcLen += len;
			final long endTime = System.currentTimeMillis();

			IAccurateDateTime nominalErt = new AccurateDateTime();

			if (metadataClone.getErt() == null) {
				metadataClone.setErt(nominalErt);
			} else {
				nominalErt = metadata.getErt();
			}

			/*  MPCS-12190 - 2021-07-27. Remove 10KB threshold, and merge if statements */
			if (metadataClone.getBitRate() == null) {
				if (endTime > (startTime + RATE_TIME_THRESHOLD)) {

					// if this is not true, assume bit rate hasn't changed
					/*  MPCS-7348 - 5/18/15. Adjust bit rate calculation to use
					 * more frames or wait more time. This makes it more accurate
					 * and also allows it to function at very high rates.
					 */
					bitRate = (bitRateCalcLen * 8) / ((endTime - startTime) / 1000.0);
					bitRateCalcLen = 0;
					startTime = endTime;
				}
				metadataClone.setBitRate(bitRate);
			} else {
				bitRate = metadata.getBitRate();
			}

			if (!isPaused()) {
				if (awaitingFirstData()) {
					messenger.sendStartOfDataMessage();
					setAwaitingFirstData(false);
				}

                // MPCS-5013 07/03/13 Use configured station
                final IStationTelemInfo dsnInfo = stationInfoFactory.create(bitRate,
                                                    len * 8,
                                                    nominalErt,
                                                    getConfiguredStation());

				metadataClone.setDsnInfo(dsnInfo);
				metadataClone.setDataLength(len);

				IMessage msg = null;

				if (metadata.getNeedsFrameSync() == null
				        && inputType.needsFrameSync()) {
					msg = frameMsgFactory.createPresyncFrameMessage(dsnInfo, buff, 0, len * 8, nominalErt);
				} else {
					if (metadataClone.isOutOfSync()) {
						logger.debug(this.getClass().getName()
						        + ": Out of sync");
						// if it was not out of sync before, but now it is, send
						// loss of sync message
						if (!isOutOfSync) {
							String outOfSyncReason = metadataClone.getOutOfSyncReason();

                            outOfSyncReason = ((outOfSyncReason == null)
                                               ? "Received out-of-sync frame data"
                                               : outOfSyncReason);

							messenger.sendLossOfSyncMessage(dsnInfo, outOfSyncReason, 
									((ITransferFrameDataProcessor)rawDataProc).getLastTfInfo(), 
									((ITransferFrameDataProcessor)rawDataProc).getLastFrameErt());
						}

						isOutOfSync = true;
					} else if (isOutOfSync) {
						logger.debug(this.getClass().getName() + ": In sync");

						// if it was out of sync, but now it is back in sync,
						// tell
						// data processor to send in sync message
						// we need the data processor to send it because we do
						// not
						// have the TF info

						metadataClone.setNeedInSyncMessage(true);
						isOutOfSync = false;
					}

					if (isOutOfSync) {
						messenger.sendOutOfSyncBytesMessage(dsnInfo, buff);

						if (singleRead) {
							break;
						} else {
							continue;
						}
					} else {
                        msg = new RawTransferFrameMessage(
                                      metadataClone,
                                      buff,
                                      HeaderHolder.NULL_HOLDER,
                                      TrailerHolder.NULL_HOLDER);
					}
				}
                logger.debug(msg.getType() + ": " + msg.getEventTimeString());
				context.publish(msg);

			} else {
				this.bytesDiscarded += buff.length;

				/*
				 * If stopping, don't suppress "bytes discarded" message until
				 * threshold is reached, since it could stop before we reach it.
				 * But otherwise, only send message when threshold is reached,
				 * so the message bus doesn't get overloaded.
				 */
				if (isStopped()
				        || this.bytesDiscarded > bytesDiscardedWhilePausedThreshold) {
					logger.warn("Processing of raw input is paused: "
					        + this.bytesDiscarded + " bytes discarded.");
					this.bytesDiscarded = 0;
				}

			}

			doMetering();

			if (singleRead) {
				return;
			}
		}
	}
}
