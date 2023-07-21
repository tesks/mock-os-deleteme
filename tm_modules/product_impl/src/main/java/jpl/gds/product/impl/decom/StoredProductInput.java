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
package jpl.gds.product.impl.decom;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.decom.IStoredProductInput;
import jpl.gds.product.api.message.IProductArrivedMessage;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.types.ByteArraySlice;

/**
 *  This component reads  a product that has been stored by the
 *  product generator and publishes a "product arrived" message. This is a
 *  base class that must be extended for specific missions.
 *  
 */
public abstract class StoredProductInput implements IStoredProductInput {

    private IMessagePublicationBus messageContext;
    /** The current application context */
    protected final ApplicationContext appContext;
    
    /**
     * Creates an instance of StoredProductInput.
     * @param appContext the current application context
     */
    public StoredProductInput(final ApplicationContext appContext) {
    	this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessageContext(final IMessagePublicationBus messageContext) {
        this.messageContext = messageContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void read(String filename) throws IOException, ProductException;

    /**
     * Reads the product data and returns it as a ByteArraySlice.
     * @param filename the full path to the product data file
     * @return a ByteArraySlice containing the product data
     * @throws IOException if there is a file I/IO error reading the product data
     * @throws ProductException if there is a problem parsing or interpreting the product data
     */
    protected ByteArraySlice readData(final String filename)
        throws IOException, ProductException
    {
        final File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File not found: " + filename);
        }
        if (!file.canRead()) {
            throw new IOException("Read permission denied: " + filename);
        }

        final long bigLength = file.length();
        if (bigLength > Integer.MAX_VALUE) {
            throw new ProductException("File bigger than "
                                       + Integer.MAX_VALUE + " bytes: "
                                       + filename);
        }
        final int length = (int) bigLength;
        final byte[] bytes = new byte[length];

        final DataInputStream dis = new DataInputStream(new FileInputStream(file));
        dis.readFully(bytes);
        dis.close();
        return new ByteArraySlice(bytes, 0, length);
    }

    /**
     * Publishes a "product arrived message" for the product just loaded.
     * @param filename the full path to the product data file
     * @param product the metadata for the product
     */
    protected void publishProduct(final String filename, final IProductMetadataUpdater product) {
        product.setFullPath(filename);
        final IProductArrivedMessage message = appContext.getBean(IProductMessageFactory.class).
                createProductArrivedMessage(product);
        messageContext.publish(message);
    }
    
    /**
     * Checks to see that product data files exist.
     * @param filenames an array of product data file name and product metadata file name
     * @throws FileNotFoundException if either file does not exist
     */
    protected void checkFiles(final String[] filenames) throws FileNotFoundException {
       if (!new File(filenames[0]).exists()) {
    	   throw new FileNotFoundException("Product data file " + filenames[0] + " does not exist");
       }
       if (!new File(filenames[1]).exists()) {
    	   throw new FileNotFoundException("Product metadata file " + filenames[1] + " does not exist");
       }
    }
}
