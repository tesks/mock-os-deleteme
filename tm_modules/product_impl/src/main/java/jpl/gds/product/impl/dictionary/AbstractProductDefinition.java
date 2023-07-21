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
package jpl.gds.product.impl.dictionary;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.IDecomHandlerSupport;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionKey;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;


/**
 * ProductDefinition represents the contents of the XML product dictionary definition
 * for a single product type. It is the base class for all project-specific product
 * definition classes.
 *
 */
public abstract class AbstractProductDefinition implements IDecomHandlerSupport, IProductDefinition {

    private int apid;
    private String version;
    private String name;
    private String description;
    private String command;
    private DecomHandler externalHandler;
    
    private final ArrayList<IProductObjectDefinition> dpos = new ArrayList<IProductObjectDefinition>();
    
    /**
     * Key for this data product definition; shared with subclasses.
     */
    protected IProductDefinitionKey key;

    /**
     * Creates an instance of ProductDefinition.
     */
    protected AbstractProductDefinition() { 
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setApid(int)
     */
    @Override
	public void setApid(final int id) {
        apid= id;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getApid()
     */
    @Override
	public int getApid() {
        return apid;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#hasInternalHandler()
     */
    @Override
	public boolean hasInternalHandler() {
        return false;
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
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#getInternalHandler()
     */
    @Override
	public DecomHandler getInternalHandler() {
        return null;
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
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#setInternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setInternalHandler(final DecomHandler handler) { 
        throw new UnsupportedOperationException("Product definition does not support an internal product handler");
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setExternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setExternalHandler(final DecomHandler handler) {
        externalHandler = handler;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setVersion(java.lang.String)
     */
    @Override
	public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getVersion()
     */
    @Override
	public String getVersion() {
        return version;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setName(java.lang.String)
     */
    @Override
	public void setName(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getName()
     */
    @Override
	public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setDescription(java.lang.String)
     */
    @Override
	public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getDescription()
     */
    @Override
	public String getDescription() {
        return description;
    }

   
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#print(java.io.PrintStream)
     */
    @Override
	public void print(final PrintStream out) throws IOException {
        out.println("Product " + key.toOutputString());
 
        out.println(name);
        if (description != null) {
            out.println(description);
        }

        Iterator<IProductObjectDefinition> i = dpos.iterator();
        while (i.hasNext()) {
            IProductObjectDefinition e = i.next();
            e.printType(out, 1);
        }
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#addProductObject(jpl.gds.product.api.dictionary.IProductObjectDefinition)
     */
    @Override
	public void addProductObject(final IProductObjectDefinition def) {
        dpos.add(def);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getProductObjects()
     */
    @Override
	public List<IProductObjectDefinition> getProductObjects() {
        return dpos;
    }
      
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getKey()
     */
    @Override
	public abstract IProductDefinitionKey getKey();

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#getCommand()
     */
    @Override
	public String getCommand() {
        return command;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDefinition#setCommand(java.lang.String)
     */
    @Override
	public void setCommand(final String stem) {
        command = stem;
    }
}
