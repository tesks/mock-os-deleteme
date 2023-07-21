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
package jpl.gds.product.impl.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;

/**
 * 
 * AgingTimeoutMessage is published when the product scheduler has determined that the timeout for 
 * receiving parts for a specific product has elapsed. This means a partial product should be 
 * generated. Currently this message is internal only.
 *
 *
 */
public class AgingTimeoutMessage extends Message 
{
    private final IProductPartProvider part;

    /**
     * Creates an instance of AgingTimeoutMessage.
     * @param part the originating product part
     */
    public AgingTimeoutMessage(final IProductPartProvider part) {
        super(InternalProductMessageType.AgingTimeout);
        this.part = part;

    }

    /**
     * Gets the originating product part.
     * @return AbstractProductPart
     */
    public IProductPartProvider getPart() {
		return part;
	}

	/**
     * Gets the virtual channel ID on which this product was received.
     * @return the VCID
     */
    public int getVcid() {
        return this.part.getVcid();
    }

    /**
     * Gets the product builder transaction ID.
     * @return the transaction identifier string
     */
    public String getTransactionId() {
        return part.getTransactionId();
    }

    /**
     * Gets the product filename (not path) without extension
     * @return the filename
     */
    public String getFilename() {
        return part.getFilename();
    }

    /**
     * Gets the product sub-directory within the product builder's output directory structure.
     * @return the sub-directory name
     */
    public String getSubdirectory() {
        return part.getDirectoryName();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "TimeoutMessage["
               + "transactionId=" + part.getTransactionId() + "]";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Aging Timeout on product transaction " + part.getTransactionId();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
    }
}