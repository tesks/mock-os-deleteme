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
package jpl.gds.telem.input.impl.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.sfdu.SfduLabel;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.telem.input.api.InternalTmInputMessageType;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.telem.input.api.message.RawInputMetadata;


/**
 * This class represents an MPCS internal message containing SFDU wrapped raw
 * (unprocessed) transfer frame data
 * 
 */

public class RawSfduTfMessage extends Message implements
        IRawDataMessage {
	private RawInputMetadata metadata;
	private boolean isIdleFrame;
	private byte[] data;

    private final HeaderHolder  header;
    private final TrailerHolder trailer;
    private IChdoSfdu sfdu;

	private SfduLabel sfduLabel;


	/**
	 * Constructor
	 * @param sfdu the IChdoSdfu object for the SFDU
	 * @param metadata the associated <code>RawInputMetadata</code>
	 * @param data the payload data
     * @param hdr  the data header
     * @param tr the data trailer
	 */
	public RawSfduTfMessage(final IChdoSfdu sfdu,
               final RawInputMetadata metadata,
               final byte[]           data,
               final HeaderHolder     hdr,
               final TrailerHolder    tr)
    {
		super(InternalTmInputMessageType.RawSfduTf);
		this.metadata = metadata;
		this.data = data;
        this.sfdu = sfdu;
        header  = HeaderHolder.getSafeHolder(hdr);
        trailer = TrailerHolder.getSafeHolder(tr);
	}


	/**
	 * Get the SFDU Label
	 * 
	 * @return the SFDU Label
	 */
	public SfduLabel getSfduLabel() {
		return sfduLabel;
	}

	/**
	 * Set the SFDU label
	 * 
	 * @param sfduLabel the SFDU label to set
	 */
	public void setSfduLabel(final SfduLabel sfduLabel) {
		this.sfduLabel = sfduLabel;
	}


	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	        throws XMLStreamException {
		writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
	}

	@Override
	public String getOneLineSummary() {
		return "SFDU Raw Data";
	}

	@Override
	public String toString() {
		return "MSG:" + getType() + " " + getEventTimeString();
	}

	/**
	 * Indicates if this message is carrying an idle frame
	 * 
	 * @return true if this message is carrying an idle frame, false otherwise
	 */
	public boolean isIdle() {
		return isIdleFrame;
	}

	/**
	 * The associated <code>RawInputMetadata</code>
	 * 
	 * @param metadata the <code>RawInputMetadata</code> to set
	 */
	public void setMetadata(final RawInputMetadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * Set to indicate if this message is carrying an idle frame
	 * 
	 * @param isIdle true if this message is carrying an idle frame, false
	 *        otherwise
	 */
	public void setIdle(final boolean isIdle) {
		this.isIdleFrame = isIdle;
	}

	/**
	 * Set the payload data
	 * 
	 * @param data the payload data
	 */
	public void setData(final byte[] data) {
		this.data = data;
	}

	@Override
	public RawInputMetadata getMetadata() {
		return metadata;
	}

	@Override
	public byte[] getData() {
		return this.data;
	}


    @Override
    public HeaderHolder getRawHeader()
    {
        return header;
    }

    @Override
    public TrailerHolder getRawTrailer()
    {
        return trailer;
    }
    
    /**
     * Gets the IChdoSfdu object associated with this message.
     * 
     * @return IChdoSfdu
     */
    public IChdoSfdu getSfdu() {
        return sfdu;
    }
}
