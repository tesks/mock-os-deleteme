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
package jpl.gds.time.impl.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.time.api.config.TimeCorrelationProperties;
import jpl.gds.time.api.message.IFswTimeCorrelationMessage;
import jpl.gds.time.api.message.ITimeCorrelationMessageFactory;
import jpl.gds.time.api.service.ITimeCorrelationParser;
import jpl.gds.time.api.service.ITimeCorrelationService;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * This is the common time correlation adapter service, an instance of a
 * DownlinkService that watches incoming packet messages for those matching a
 * configured time correlation packet APID. When it sees them, it parses the
 * information out of the TC packet. It also keeps a rotating buffer created
 * from incoming transfer frame messages. Once it parses the incoming TC packet,
 * it searches this circular buffer for the matching reference frame. It then
 * publishes a FswTimeCorrelationMessage containing both the packet and frame
 * information.
 * 
 * This adapter is known to work only with the SMAP and MSL missions. There is
 * no guarantee it will work for any other mission.
 * 
 *
 */
public class MultimissionTimeCorrelationService implements ITimeCorrelationService {
	private final Tracer tracer; 


	private PacketSubscriber packetSubscriber;
	private FrameSubscriber frameSubscriber;
	private int timeApid;
	private CircularFrameBuffer frameHistory;
	private ITimeCorrelationParser pktParser;
	private final ITransferFrameDefinitionProvider frameDictionary;
	private final IMessagePublicationBus bus;
	private final ApplicationContext appContext;
    private final TimeCorrelationProperties tcProperties;
    private final ITimeCorrelationMessageFactory tcMessageFactory;


    private final IContextConfiguration contextConfig;
	
	/**
	 * Constructor.
	 * 
	 * @param serveContext the current application context
	 */
	public MultimissionTimeCorrelationService(final ApplicationContext serveContext) {
		this.appContext = serveContext;
        this.tracer = TraceManager.getDefaultTracer(serveContext);
	    this.bus = serveContext.getBean(IMessagePublicationBus.class);
	    frameDictionary = serveContext.getBean(ITransferFrameDefinitionProvider.class);
	    tcProperties = appContext.getBean(TimeCorrelationProperties.class);
	    tcMessageFactory = appContext.getBean(ITimeCorrelationMessageFactory.class);
	    contextConfig = appContext.getBean(IContextConfiguration.class);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.time.api.service.ITimeCorrelationService#startService()
	 */
	@Override
	public boolean startService() {

		this.timeApid = tcProperties.getTcPacketApid(false);
		this.packetSubscriber = new PacketSubscriber();
		this.frameSubscriber = new FrameSubscriber();
		final int bufferLen = tcProperties.getReferenceFrameBufferLength();
		this.frameHistory = new CircularFrameBuffer(bufferLen);

		try {
			this.pktParser = appContext.getBean(ITimeCorrelationParser.class);
		} catch (final Exception e) {
			tracer.error("Unable to instantiate time correlation packet parser: "
					+ e.toString());
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.time.api.service.ITimeCorrelationService#stopService()
	 */
	@Override
	public void stopService() {

		if (this.packetSubscriber != null) {
			bus.unsubscribeAll(
					this.packetSubscriber);
		}
		if (this.frameSubscriber != null) {
			bus.unsubscribeAll(
					this.frameSubscriber);
		}
	}

	/**
	 * Attempts to locate the reference frame given the input information from
	 * the TC packet. Whether it locates the frame or not, publishes a
	 * FswTimeCorrelationMessage.
	 * 
	 * @param pm
	 *            the IPacketMessage containing the TC packet
	 * @param vcid
	 *            the virtual channel of the reference frame
	 * @param vcfc
	 *            the virtual sequence count of the reference frame
	 * @param corrSclk
	 *            the expected SCLK from inside the TC packet
	 * @param enc
	 *            the reference frame encoding type
	 * @param rateIndex
	 *            the bit rate index from the TC packet
	 * 7/16/13 Added publication of encoding type
	 *                        and bit rate index to the TC message                      
	 */
	private void publishCorrelationMessage(final ITelemetryPacketMessage pm,
			final int vcid, final long vcfc, final ISclk corrSclk,
			final EncodingType enc, final long rateIndex) {

		/*
		 * 06/20/2011 - MPCS-2344: chill_down NullPointerException with
		 * EDL re-xmit data set
		 */
		double frameSize;
	
		final ITransferFrameDefinition tfFrameFormat = this.frameDictionary.findFrameDefinition(pm.getPacketInfo().getFrameType());
		if (null != tfFrameFormat) {
			frameSize = tfFrameFormat.getEncodedCADUSizeBytes();
			if (frameSize <= 0.0) {
				frameSize = tfFrameFormat.getCADUSizeBytes();
			}
		} else {
			tracer.error("Could not determine Transfer Frame Format for PacketMessage.");
			return;
		}


		// Search the frame buffer for the reference frame
		final FrameBufferEntry entry = this.frameHistory.findFrame(pm
				.getPacketInfo().getDssId(), vcid, vcfc);

		double bitrate = 0.0;
		IAccurateDateTime ert = null;
		
		final boolean found = entry != null;
		if (found) {

			// Need to calculate the bit rate between the reference frame and
			// that previous to it
			final FrameBufferEntry prevFrame = this.frameHistory
					.getPreviousEntry(entry);
			final FrameBufferEntry nextFrame = this.frameHistory
					.getNextEntry(entry);
			

			if (prevFrame != null
					&& prevFrame.getEncoding().equals(entry.getEncoding())) {
				bitrate = calculateBitRate(prevFrame.getErt(),
						entry.getErt(), frameSize);
			} else if (nextFrame != null
					&& nextFrame.getEncoding().equals(entry.getEncoding())) {
				bitrate = calculateBitRate(entry.getErt(),
						nextFrame.getErt(), frameSize);
			} else {
				tracer.error("Could not calculate TC bitrate for "
						+ pm.getPacketInfo().getIdentifierString());
			}

			if (bitrate == 0.0) {
				tracer.warn("No bit rate for TC calculation. Using packet bitrate.");
				bitrate = pm.getPacketInfo().getBitRate();
			}
			ert = entry.getErt();

		} else {
			tracer.warn("Abandoning FSW TC msg publishing: Unable to locate reference frame VC="
					+ vcid
					+ " VCFC="
					+ vcfc
					+ "matching TC packet "
					+ "("
					+ pm.getPacketInfo().getIdentifierString() + ")");
			return;
		}


		final IFswTimeCorrelationMessage m = tcMessageFactory.createFswTimeCorrelationMessage(
		        corrSclk, 
		        pm.getPacketInfo().getSclk(), 
		        ert, 
		        pm.getPacketInfo().getErt(), 
		        vcfc, 
		        vcid, 
		        (int) frameSize, 
		        found, 
		        enc, 
		        bitrate, 
		        rateIndex);
		
	    m.setFromSse(false);
        
		bus.publish(m);
	}

	/**
	 * Attempts to calculate bit rate based upon the ERT interval between two
	 * frames.
	 * 
	 * @param a
	 *            ERT time on the first frame
	 * @param b
	 *            ERT time on the second frame
	 * @param len
	 *            length of the first frame
	 * @return the calculated bit rate. Will be 0.0 if too little time has
	 *         elapsed between the ERTs to perform the calculation.
	 */
	private double calculateBitRate(final IAccurateDateTime a,
			final IAccurateDateTime b, final double len) {

		final BigDecimal lenBitsBd = BigDecimal.valueOf(len * 8.0);

		final IAccurateDateTime elapsedTime = b.roll(a.getTime(),
				a.getNanoseconds(), false);
		final BigDecimal elapsedSecs1Bd = (BigDecimal.valueOf(elapsedTime
				.getTime())).movePointLeft(3);
		final BigDecimal elapsedSecs2Bd = (BigDecimal.valueOf(elapsedTime
				.getMicroTenths())).movePointLeft(7);
		final BigDecimal totalElapsedSecBd = elapsedSecs1Bd.add(elapsedSecs2Bd);
		if (totalElapsedSecBd.longValue() == 0) {
			tracer.warn("Elapsed time in TC bit rate calculation is 0 in CommonTimeCorrelationAdaptor. Could not calculate bitrate");
			return 0.0;
		}
		return lenBitsBd.divide(totalElapsedSecBd, new MathContext(20))
				.doubleValue();
	}

	/**
	 * Subscriber class for internal PacketMessage objects.
	 * 
	 *
	 */
	private class PacketSubscriber implements MessageSubscriber {
		public PacketSubscriber() {

			bus.subscribe(
					TmServiceMessageType.TelemetryPacket, this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void handleMessage(final IMessage message) {

			final ITelemetryPacketMessage pm = (ITelemetryPacketMessage) message;
			if (!contextConfig.accept(pm)
					|| pm.getPacketInfo().isFill()
					|| pm.getPacketInfo().getApid() != MultimissionTimeCorrelationService.this.timeApid) {
				return;
			}
			tracer.debug("Received FSW time correlation packet with sequence number "
					+ pm.getPacketInfo().getSeqCount());

			// Parse the TC packet
			final byte[] packetData = pm.getPacket();
			MultimissionTimeCorrelationService.this.pktParser.parse(packetData);

			// Attempt to locate the reference frame and publish a time
			// correlation message
			// 7/16/13 - MPCS-5055. Added arguments per the new TC message
			publishCorrelationMessage(pm,
					MultimissionTimeCorrelationService.this.pktParser.getVcid(),
					MultimissionTimeCorrelationService.this.pktParser.getVcfc(),
					MultimissionTimeCorrelationService.this.pktParser.getSclk(),
					MultimissionTimeCorrelationService.this.pktParser.getEncType(),
					MultimissionTimeCorrelationService.this.pktParser.getRateIndex());
		}
	}

	/**
	 * Subscriber class for internal TransferFrameMessage objects.
	 * 
	 *
	 */
	private class FrameSubscriber implements MessageSubscriber {
	    
		public FrameSubscriber() {

			bus.subscribe(TmServiceMessageType.TelemetryFrame, this);
			               			
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void handleMessage(final IMessage message) {

			final ITelemetryFrameMessage tfm = (ITelemetryFrameMessage) message;
			
			// Filter out frames with wrong VCID, station, or SCID 
			if (!contextConfig.accept(tfm)) {
				return;
			}
			// Add the frame to the circular history buffer
			MultimissionTimeCorrelationService.this.frameHistory.addEntry(tfm);
		}
	}

	/**
	 * This class manages a circular buffer of recently received transfer
	 * frames.
	 * 
	 *
	 */
	private static class CircularFrameBuffer {
		private final int bufferLen;
		private final List<FrameBufferEntry> entries;
		private int currentInsertPos;

		/**
		 * Constructs a frame buffer with the given length (frame count).
		 * 
		 * @param bufferLen
		 *            number of frames to keep in the buffer
		 */
		public CircularFrameBuffer(final int bufferLen) {

			this.bufferLen = bufferLen;
			this.entries = new ArrayList<>(this.bufferLen);
		}

		/**
		 * Adds a new transfer frame to the buffer, dropping older entries if
		 * needed.
		 * 
		 * @param message
		 *            TransferFrameMessage containing the frame to add
		 */
		public synchronized void addEntry(final ITelemetryFrameMessage message) {

			final int vcid = message.getFrameInfo().getVcid();
			final long vcfc = message.getFrameInfo().getSeqCount();
			final IAccurateDateTime ert = message.getStationInfo().getErt();
			final int dss = message.getStationInfo().getDssId();
			final EncodingType et = message.getFrameInfo().getFrameFormat()
					.getEncoding();

			final FrameBufferEntry addMe = new FrameBufferEntry(vcid, dss,
					vcfc, ert, et);
			if (this.entries.size() < this.bufferLen) {
				this.entries.add(addMe);
				this.currentInsertPos++;
			} else {
				if (this.currentInsertPos >= this.bufferLen) {
					this.currentInsertPos = 0;
				}
				this.entries.set(this.currentInsertPos, addMe);
				this.currentInsertPos++;
			}
		}

		/**
		 * Finds a specific frame in the buffer.
		 * 
		 * @param dss
		 *            frame station ID
		 * @param vcid
		 *            frame virtual channel ID
		 * @param vcfc
		 *            frame sequence counter
		 * 
		 * @return a matching FrameBufferEntry, or null if no match found
		 */
		public synchronized FrameBufferEntry findFrame(final int dss,
				final int vcid, final long vcfc) {

			for (final FrameBufferEntry entry : this.entries) {
				if (entry.getDss() == dss && entry.getVcfc() == vcfc
						&& entry.getVcid() == vcid) {
					return entry;
				}
			}
			return null;
		}

		/**
		 * Retrieves the FrameBufferEntry that came previous to the specified
		 * FrameBufferEntry.
		 * 
		 * @param frame
		 *            FrameBufferEntry to search for
		 * @return FrameBufferEntry previous to the input frame in the buffer
		 */
		private synchronized FrameBufferEntry getPreviousEntry(
				final FrameBufferEntry frame) {

			final int frameIndex = this.entries.indexOf(frame);

			int prevIndex = frameIndex - 1;
			if (prevIndex == -1) {
				prevIndex = this.entries.size() - 1;
			}

			return this.entries.get(prevIndex);
		}

		/**
		 * Retrieves the FrameBufferEntry that came after to the specified
		 * FrameBufferEntry.
		 * 
		 * @param frame
		 *            FrameBufferEntry to search for
		 * @return FrameBufferEntry after the input frame in the buffer
		 */
		private synchronized FrameBufferEntry getNextEntry(
				final FrameBufferEntry frame) {

			final int frameIndex = this.entries.indexOf(frame);

			int nextIndex = frameIndex + 1;
			if (nextIndex == this.entries.size()) {
				nextIndex = 0;
			}

			return this.entries.get(nextIndex);
		}
	}

	/**
	 * Simple holder class for frame information.
	 * 
	 *
	 */
	private static class FrameBufferEntry {

		private final int vcid;
		private final long vcfc;
		private final IAccurateDateTime ert;
		private final int dss;
		private final EncodingType encoding;

		/**
		 * Constructor.
		 * 
		 * @param vcid
		 *            frame virtual channel ID
		 * @param dss
		 *            frame station ID
		 * @param seq
		 *            frame sequence number
		 * @param ert
		 *            frame ERT
		 * @param et
		 *            frame encoding type
		 */
		public FrameBufferEntry(final int vcid, final int dss, final long seq,
				final IAccurateDateTime ert, final EncodingType et) {

			this.vcid = vcid;
			this.dss = dss;
			this.vcfc = seq;
			this.ert = ert;
			this.encoding = et;
		}

		/**
		 * Gets the frame virtual channel ID.
		 * 
		 * @return VCID
		 */
		public int getVcid() {

			return this.vcid;
		}

		/**
		 * Gets the frame virtual channel sequence counter.
		 * 
		 * @return VCFC
		 */
		public long getVcfc() {

			return this.vcfc;
		}

		/**
		 * Gets the frame ERT.
		 * 
		 * @return ERT
		 */
		public IAccurateDateTime getErt() {

			return this.ert;
		}

		/**
		 * Gets the frame station ID.
		 * 
		 * @return station ID (DSS ID)
		 */
		public int getDss() {

			return this.dss;
		}

		/**
		 * Gets the frame encoding type.
		 * 
		 * @return EncodingType
		 */
		public EncodingType getEncoding() {

			return this.encoding;
		}

	}

}
