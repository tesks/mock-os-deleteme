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

import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbRawData;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;
import jpl.gds.telem.input.impl.message.RawDatabaseMessage;

/**
 * This class is an implementation of <code>IRawStreamProcessor</code> that
 * processes <code>DatabaseInputStream</code>
 * 
 *
 */
public class DatabaseStreamProcessor extends AbstractRawStreamProcessor {
	private static final long PAUSE_SLEEP = 5L * 1000L; // 5 sec
	private static final String ME = "DatabaseStreamProcessor ";
	
	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	public DatabaseStreamProcessor(final ApplicationContext serviceContext) {
	    super(serviceContext);
	}

	/**
	 * Sleep, ignoring interrupt.
	 * 
	 * @param interval Sleep interval
	 */
	protected void sleep(final long interval) {
		if (interval >= 1L) {
			SleepUtilities.checkedSleep(interval);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.stream.IRawStreamProcessor#processRawData(jpl.gds.telem.input.api.stream.IRawInputStream, jpl.gds.telem.input.api.message.RawInputMetadata)
	 */
	@Override
	public void processRawData(final IRawInputStream inputStream,
	        final RawInputMetadata metadata) throws RawInputException, IOException {
		if (!(inputStream instanceof DatabaseInputStream)) {
			throw new RawInputException("Invalid input stream. Needs DatabaseInputStream.");
		}

		logger.debug(ME + ": begin database stream processing");
		final DatabaseInputStream dis = (DatabaseInputStream) inputStream;

        if (isRemoteMode)
        {
            dis.setRemote();
        }

		try {

            if (stopped) {
                setAwaitingFirstData(true);
                return;
            }

            for (final IDbRawData drd : dis) {
                RawInputMetadata metadataClone = null;
                try {
                    metadataClone = metadata.clone();
                } catch (final CloneNotSupportedException e) {
                    metadataClone = new RawInputMetadata();
                }

                if (!isPaused()) {
                    if (awaitingFirstData()) {
                        metadataClone.setNeedInSyncMessage(true);
                        messenger.sendStartOfDataMessage();
                        setAwaitingFirstData(false);
                    }
                }

                if (paused) {
                    sleep(PAUSE_SLEEP);
                }

                // If the database says it's bad, it's bad
                final InvalidFrameCode bad = drd.getBadReason();

                metadataClone.setIsBad((bad != null), bad);

                metadataClone.setErt(drd.getErt());
                
                // 07/07/14 - MPCS-6349 : DSS ID not set properly
                final IStationTelemInfo dsnInfo = stationInfoFactory.create(drd.getBitRate(), drd.getRecordLength()
                        * Byte.SIZE, drd.getErt(), drd.getRecordDssIdAsInt());

                metadataClone.setDsnInfo(dsnInfo);

                final RawDatabaseMessage drdm = new RawDatabaseMessage(metadataClone, drd);
                context.publish(drdm);
                messenger.incrementReadCount();

                doMetering();
            }

            dis.close();
		}
        catch (final Exception e)
        {
			logger.error(ME + "Unable to get frames/packets: " +
                         ExceptionTools.rollUpMessages(e));
		}
        finally
        {
			logger.debug(ME + ": end database stream processing");
			setAwaitingFirstData(true);
		}
	}

}
