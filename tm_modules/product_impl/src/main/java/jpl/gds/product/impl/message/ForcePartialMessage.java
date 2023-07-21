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
 * ForcePartialMessage is used to indicate that a partial product is to be generated
 * for anything other than the expected reasons (aging timeout or product change in the 
 * telemetry stream). Generally, it is published as a result of end of data/test run 
 * or exit of the current application. Currently, this is an internal-only message.
 *
 */
public class ForcePartialMessage extends Message 
{	
   
    private final IProductPartProvider part;
    
    /**
     * Creates an instance of ForcePartialMessage.
     *
     * @param part the originating product part
      */
    public ForcePartialMessage(final IProductPartProvider part) {
        super(InternalProductMessageType.ForcePartial);
        this.part = part;
    }
    
    /**
     * Gets the product part associated with this message.
     * @return product part 
	 */
	public IProductPartProvider getPart() {
		return part;
	}

    /**
     * Gets the VCID on which this part was received (from the part itself).
     * @return VCID
	 */
	public int getVcid() {
        return this.part.getVcid();
    }


    /**
     * Gets the product builder transaction ID this message is associated with.
     *  
     *  @return transaction ID
	 */
	public String getTransactionId() {
        return part.getTransactionId();
    }

	/**
	 * Gets the product filename (as relative path).
	 * 
	 * @return filename 
	 */
	public String getFilename() {
        return part.getFilename();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "ForcePartialMessage["
               + "transactionId=" + part.getTransactionId() + "]";
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
	public String getOneLineSummary() {
        return "Force Partial Product";
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