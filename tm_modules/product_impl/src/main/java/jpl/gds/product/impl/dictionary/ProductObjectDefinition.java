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
/**
 * 
 */
package jpl.gds.product.impl.dictionary;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.product.api.decom.formatter.IProductDecomOutputFormatter;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;
import jpl.gds.product.impl.decom.AbstractFieldContainer;
import jpl.gds.product.impl.decom.SimpleField;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.ByteStream;

/**
 * The DataProductObjectDefinition class is a product dictionary
 * DefinitionElement that represents a pre-defined data structure within a data
 * product.
 * 
 *
 */
public class ProductObjectDefinition extends AbstractFieldContainer implements IProductObjectDefinition {

    private static Tracer log = TraceManager.getDefaultTracer();

    private int dpoId;
    private String operationalCategory;
    private String module;
    private DecomHandler internalHandler;
    private DecomHandler externalHandler;
    private long fswVersionId;
    private final Categories categories = new Categories();
   

    /**
     * Creates a new ProductObjectDefinition.
     */
    public ProductObjectDefinition() {
        super(ProductDecomFieldType.DPO);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#print(java.io.PrintStream)
     */
    @Override
	public void print(final PrintStream out) throws IOException {
        printType(out, 0);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {

        printIndent(out, depth);

        if (dpoId != 0) {
            out.println("Data Product Object: " + dpoId + " ("
                    + new SprintfFormat().anCsprintf("0x%08X", dpoId) + ")");
        } else {
            out.println("Data Product Object: NO VID");
        }
        Indent.incr();

        printIndent(out, depth + 1);

        out.println("Name: " + name);
        if (getFswDescription() != null) {
            printIndent(out, depth + 1);
            out.println("Description: " + getFswDescription());
        }

        if (operationalCategory != null) {
            printIndent(out, depth + 1);
            out.println("Operational category: " + module);
        }

        if (module != null) {
            printIndent(out, depth + 1);
            out.println("Module: " + module);
        }

        if (internalHandler != null) {
            printIndent(out, depth + 1);
            out.println(internalHandler.toString());
        }

        if (externalHandler != null) {
            printIndent(out, depth + 1);
            out.println(externalHandler.toString());
        }
        
        if (isChannelize()) {
            printIndent(out, depth + 1);
            out.println("DPO contains channels");
        }

        for (final IProductDecomField e : elements) {
            e.printType(out, depth + 1);
        }
        Indent.decr();
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#getDpoId()
     */
    @Override
	public int getDpoId() {
        return dpoId;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#setDpoId(int)
     */
    @Override
	public void setDpoId(final int dpoId) {
        this.dpoId = dpoId;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#getOperationalCategory()
     */
    @Override
	public String getOperationalCategory() {
        return operationalCategory;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#setOperationalCategory(java.lang.String)
     */
    @Override
	public void setOperationalCategory(final String operationalCategory) {
        this.operationalCategory = operationalCategory;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#getModule()
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
    public String getModule() {
        return module;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#setModule(java.lang.String)
     */
    @Override
    @Deprecated /* Replaced with ICategorySupport. */
	public void setModule(final String module) {
        this.module = module;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#getExternalHandler()
     */
    @Override
	public DecomHandler getExternalHandler() {
        return externalHandler;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#getInternalHandler()
     */
    @Override
	public DecomHandler getInternalHandler() {
        return internalHandler;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#hasExternalHandler()
     */
    @Override
	public boolean hasExternalHandler() {
        return externalHandler != null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#hasInternalHandler()
     */
    @Override
	public boolean hasInternalHandler() {
        return internalHandler != null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#setExternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setExternalHandler(final DecomHandler handler) {
        externalHandler = handler;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#setInternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setInternalHandler(final DecomHandler handler) {
        internalHandler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
                                                                            throws IOException {
        long bytesRead = 0;

        ((IProductDecomOutputFormatter) out).dpoStart(this.getName(), this.getDpoId());

        // if the output format is not null, use the specified format

        if (getPrintFormat() != null) {
            bytesRead += printFormattedDpo(stream, out, depth);

        } else {
            for (final IProductDecomField element : elements) {
                // has potential to go past end of field
                if (!stream.hasMore()) {
                    log.warn("DataProductObjectDefinition ran out of data");
                    break;
                }
                bytesRead += element.printValue(stream, out, depth + 1);
            }
        }
        ((IProductDecomOutputFormatter) out).dpoEnd();

        return (int) bytesRead;
    }

    private int printFormattedDpo(final ByteStream data,
            final IDecomOutputFormatter out, final int depth) throws IOException {

        final ArrayList<Object> objectsToPrint = new ArrayList<Object>();

        final SprintfFormat dataFormatter = out.getApplicationContext().getBean(SprintfFormat.class);
        // now get the values and convert them to Java objects

        int bytesUsed = 0;

        for (final IProductDecomField element : elements) {
            // has potential to go past end of field
            if (!data.hasMore()) {
                log.warn("DataProductObjectDefinition ran out of data");
                break;
            }
            final SimpleField df = (SimpleField) element;
            final Object currentObject = df.getValue(df.getDataType(), data);
            bytesUsed += df.getDataType().getByteLength();
            objectsToPrint.add(currentObject);
        }
        printFormattedLine(objectsToPrint, out, depth + 1, dataFormatter);
        return bytesUsed;
    }

    /**
     * Utility for printing a formatted line.
     * 
     * @param objectsToPrint list of objects to print
     * @param out output formatter to write data to
     * @param dataFormatter data formatter in use
     */
    private void printFormattedLine(final ArrayList<Object> objectsToPrint,
            final IDecomOutputFormatter out, final int depth,
            final SprintfFormat dataFormatter) throws IOException {

        String formattedData = "";
        if (objectsToPrint.size() == 1) {
            formattedData = dataFormatter.anCsprintf(getPrintFormat(),
                    objectsToPrint.get(0));
        } else if (objectsToPrint.size() < getItemsToPrint()) {
            final String newFormat = adjustOutputFormatterForCount(objectsToPrint
                    .size());
            formattedData = dataFormatter.sprintf(newFormat, objectsToPrint
                    .toArray());
        } else {
            formattedData = dataFormatter.sprintf(getPrintFormat(), objectsToPrint
                    .toArray());
        }

        out.structureValue(formattedData);
    }

    private String adjustOutputFormatterForCount(final int count) {
        final int diff = getItemsToPrint() - count;
        final String format = getPrintFormat();
        int lastPercentIndex = format.length() - 1;
        for (int i = 0; i < diff; i++) {
            lastPercentIndex = format.lastIndexOf('%', lastPercentIndex);
        }
        final String result = format.substring(0, lastPercentIndex);
        return result;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#hasChannels()
     */
    @Override
    public boolean hasChannels() {
    	return isChannelize();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#setFswVersionId(long)
     */
	@Override
	public void setFswVersionId(final long fswVersionId) {
		this.fswVersionId = fswVersionId;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductObjectDefinition#getFswVersionId()
     */
	@Override
	public long getFswVersionId() {
		return fswVersionId;
	}
    /** {@inheritDoc}
    * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategories(jpl.gds.dictionary.impl.impl.api.Categories)
    */
   @Override
   public void setCategories(final Categories map) {
       categories.copyFrom(map);
   }

   /** {@inheritDoc}
    * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#getCategories()
    */
   @Override
   public Categories getCategories() {
       return categories;
   }

   /** {@inheritDoc}
    * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategory(java.lang.String, java.lang.String)
    */
   @Override
   public void setCategory(final String catName, final String catValue) {
       categories.setCategory(catName, catValue);        
   }

   /** {@inheritDoc}
    * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#getCategory(java.lang.String)
    */
   @Override
   public String getCategory(final String name) {
       return categories.getCategory(name);
   }
	
}
