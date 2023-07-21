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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionBuilder;
import jpl.gds.product.api.dictionary.IProductDefinitionKey;
import jpl.gds.product.api.dictionary.IProductDictionary;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlFileFilter;

/**
 * 
 * AbstractProductDictionary is a top-level class that can be extended to
 * implement the XML product telemetry dictionary, which describes the contents
 * and format of data products that are generated from downlink telemetry. This
 * is a multi-mission class which should be extended for specific projects.
 * Product definitions that are loaded are kept in a HashMap.
 * 
 */
public abstract class AbstractProductDictionary implements IProductDictionary {
	
    /** Shared logger instance */
    protected static final Tracer                            logger         = TraceManager
            .getTracer(Loggers.TLM_PRODUCT);

 
    private File xmldir;
    private final IProductDefinitionBuilder builder;
    /**
     * List of product definitions that have been found to be invalid. Shared with subclasses.
     */
    protected Set<IProductDefinitionKey> badDefinitions = new HashSet<IProductDefinitionKey>();
    /**
     * List of cached product definitions. Shared with subclasses.
     */
    protected Map<IProductDefinitionKey,IProductDefinition> definitions = 
        new HashMap<IProductDefinitionKey,IProductDefinition>();
    
    /**
     * Creates an instance of AbstractProductDictionary.
     * @param fieldFactory the decom field factory to use
     *
     */
    public AbstractProductDictionary(final IProductDecomFieldFactory fieldFactory) {
        builder = createBuilder(fieldFactory);
    }
    

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDictionary#getAllDefinitions()
     */
    @Override
	public List<IProductDefinition> getAllDefinitions() {
        final Collection<IProductDefinition> allDefs = definitions.values();
        final List<IProductDefinition> list = new ArrayList<IProductDefinition>(allDefs);
        return list;
    }
    
    /**
     * Sets the directory path to the XML product definitions.
     * 
     * @param directoryPath the location of the product dictionary files
     * @throws DictionaryException if the directory does not exist or is not a directory
     */
    @Override
	public void setDirectory(final String directoryPath) throws DictionaryException {
        if (directoryPath == null) {
            logger.error("Product definition file path is undefined");
            throw new DictionaryException("Product definition file path is undefined");
        }

        xmldir = new File(directoryPath);
        if (!xmldir.exists()) {
            logger.error("Product definition directory " +
                            xmldir.getAbsolutePath() + " not found");
            xmldir = null;
            throw new DictionaryException("Product definition directory " +
                   new File(directoryPath).getAbsolutePath() + " not found");
        }

        if (!xmldir.isDirectory()) {
            logger.error("Product definition directory " +
                            xmldir.getAbsolutePath() + " is not a directory");
            xmldir = null;
            throw new DictionaryException("Product definition directory " +
                    new File(directoryPath).getAbsolutePath() + " is not a directory");
        }
    }

    /**
     * Writes the product dictionary contents to the given stream.
     * @param out the PrintStream to write to
     * @throws IOException if there is a file I/O error writing to the output stream
     * @throws DictionaryException if product definitions cannot be printed/interpreted
     */
    public void printAll(final PrintStream out) throws DictionaryException, IOException {
        if (xmldir == null) {
            return;
        }

        loadAll();

        out.println("Product Dictionary " + xmldir);
        final Iterator<IProductDefinition> i = definitions.values().iterator();
        while (i.hasNext()) {
            out.println();
            final IProductDefinition def = i.next();
            def.print(out);
        }
    }

    /**
     * Loads all the product definitions in the dictionary directory.
     * @throws DictionaryException if the product definitions cannot be loaded
     */
    @Override
	public void loadAll() throws DictionaryException {

        final File[] xmlFiles = getAllFiles();
        for (int i = 0; i < xmlFiles.length; ++i) {
            loadDefinition(xmlFiles[i]);
        }
    }

    /**
     * Loads the XML product definition in the given file.
     * @param file the file to load
     * @return the IProductDefinition object that is created by the load
     * @throws DictionaryException if there is a problem loading the definition file
     */
    protected IProductDefinition loadDefinition(final File file) throws DictionaryException {
        IProductDefinition def = null;
        def = builder.buildProductDefinition(file.getPath());
        definitions.put(def.getKey(), def);
        return def;
    }

    /**
     * Gets a list of file dictionary files in the dictionary directory.
     * @return an array of File objects, one for each product dictionary file
     * @throws DictionaryException if XML directory does not exist or is empty
     */
    protected File[] getAllFiles() throws DictionaryException {
        if (xmldir == null) {
            throw new IllegalStateException("XML directory not set");
        }

        final File[] xmlFiles = xmldir.listFiles(new XmlFileFilter());
        if (xmlFiles == null) {
            logger.error("Couldn't find any product definition files in "
                            + xmldir.getAbsolutePath());
            throw new DictionaryException("Couldn't find any product definition files in "
                    + xmldir.getAbsolutePath());
        }

        if (xmlFiles.length == 0) {
            logger.warn("Couldn't find any product definition files in "
                            + xmldir.getAbsolutePath());
            throw new DictionaryException("Couldn't find any product definition files in "
                    + xmldir.getAbsolutePath());
        }

        return xmlFiles;
    }
    
    /**
     * Retrieves the XML definition directory File object, for the root directory in which the product definitions 
     * are stored.
     * @return File object
     */
    protected File getDefinitionDir() {
        return xmldir;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDictionary#clear()
     */
    @Override
	public void clear() {
        definitions.clear();
        badDefinitions.clear();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.product.api.dictionary.IProductDictionary#getDefinition(jpl.gds.product.api.dictionary.IProductDefinitionKey)
     */
    @Override
	public IProductDefinition getDefinition(final IProductDefinitionKey key) {
        return definitions.get(key);
    }
}
