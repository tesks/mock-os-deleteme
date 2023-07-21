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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.gds.product.api.decom.IChannelBlockSupport;
import jpl.gds.product.api.decom.IChannelSupport;
import jpl.gds.product.api.decom.IFieldContainer;
import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.types.ByteStream;

/**
 * AbstractFieldContainer is the base class for all IFieldContainer objects. These objects
 * are decom fields, such as arrays and structures, that contain other fields.
 * 
 *
 */
public abstract class AbstractFieldContainer extends AbstractDecomField implements IFieldContainer {
    
	/**
	 * List of subfields in this container.
	 */
    protected List<IProductDecomField> elements = new ArrayList<>();
    
    private int valueSize;
    private boolean channelize = false;
    private boolean hasChannelsDone = false;
    private boolean hasEmbeddedChannels = false;
    private int elementsToPrint;
    
    
    /**
     * Creates a decom field container with the given decom field type.
     * 
     * @param type the decom field type being created
     */
    public AbstractFieldContainer(final ProductDecomFieldType type) {
        super(type);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void addField(final IProductDecomField element) {
        if (element == null) {
            System.out.println("");
        } else if (this == element) {
            tracer.debug("Skipping adding ", element.getName() , " to list size=", elements.size(),
                         " because it shouldn't be added to itself (infinite loop)");
            return;
        }
        elements.add(element);
    }


    /**
     * {@inheritDoc}
     */
    @Override
	@SuppressWarnings("unchecked")
    public List<IProductDecomField> getFields() {
        return (List<IProductDecomField>) ((ArrayList<IProductDecomField>)elements).clone();
    }
    
    /**
     * Indicates whether this repeat contains fields that should be channelized. Currently, this involves
     * a comparison of every primitive field name with the channel dictionary.
     * @return true if DPO contains valid channel IDs
     */
    @Override
	public boolean hasChannels() {
        if (hasChannelsDone) {
            return hasEmbeddedChannels;
        }
        for (final IProductDecomField element : elements) {
            if ( (element instanceof IChannelBlockSupport && ((IChannelBlockSupport) element).hasChannels())
                    || (element instanceof IChannelSupport && ((IChannelSupport) element).isChannel()) ) {
                hasEmbeddedChannels = true;
                break;
            }
        }
        hasChannelsDone = true;
        return hasEmbeddedChannels;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#getValueSize()
     */
    @Override
    public int getValueSize() {
        if (valueSize == 0) {
            for (final IProductDecomField element : elements) {
                valueSize += element.getValueSize();
            }
        }
        return valueSize;
    }
    

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#isChannelize()
     */
    @Override
	public boolean isChannelize() {
        return channelize;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#setChannelize(boolean)
     */
    @Override
	public void setChannelize(final boolean channelize) {
        this.channelize = channelize;
    }
    
    /**
     * Sets the printFormat.
     * 
     * @param printFormat the printFormat to set
     */
    @Override
    public void setPrintFormat(final String printFormat) {
        super.setPrintFormat(printFormat);
        countItemsToPrint();
    }

    /**
     * Count the number of items to print per output statement --at the moment
     * this is a simple count of the number of % characters in the format
     * statement
     */
    protected void countItemsToPrint() {
        elementsToPrint = 0;
        if (printFormat == null) {
            return;
        }
        final Pattern percentPattern = Pattern.compile("%", Pattern.CASE_INSENSITIVE);
        final Matcher percentMatcher = percentPattern.matcher(printFormat);
        while (percentMatcher.find()) {
            elementsToPrint++;
        }
    }
    
    /**
     * Gets the number of % specifiers in the current format string for this container.
     * 
     * @return formatter count
     */
    protected int getItemsToPrint() {
        return elementsToPrint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out,
            final int depth) throws IOException {
        return 0;
    }


}
