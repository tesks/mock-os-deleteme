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

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.message.IProductArrivedMessage;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;

/**
 *
 * ProductArrivedMessage is a message that is published when a product's metadata is
 * read from product storage. It is an internal message generated during product 
 * decommutation only.
 * 
 */
public class ProductArrivedMessage extends Message implements IProductArrivedMessage
{
    private final IProductMetadataProvider metadata;

    /**
     * Creates an instance of ProductArrivedMessage.
     * @param md the product metadata provider
     *
     */
    public ProductArrivedMessage(final IProductMetadataProvider md) {
        super(InternalProductMessageType.DecomProductArrived);
        this.metadata = md;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IProductMetadataProvider getProductMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "ProductArrivedMessage[ProductMetadata="
                                       + getProductMetadata() + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOneLineSummary() {
        return "Product read from storage " + metadata.getFullPath();
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