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
package jpl.gds.product.app.decom;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.decom.IProductDecom;
import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionKey;
import jpl.gds.product.api.dictionary.IProductObjectDefinition;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.string.Parser;
import jpl.gds.shared.types.ByteStream;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * ReferenceProductDecom is the generic class for product decommutation.
 *
 */
public class ReferenceProductDecom extends AbstractProductDecom {

    /**
     * Creates an instance of ReferenceProductDecom.
     * @param context the current application context
     *
     */
    public ReferenceProductDecom(final ApplicationContext context) { 
    	super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean handleProduct(final IProductMetadataProvider metadata, final ByteStream bytestream) {
 
        boolean success = true;
        try {
            loadDictionary(metadata.getEmdDictionaryDir(), metadata.getEmdDictionaryVersion());
        } catch (final Exception e) {
            /* 
             * Used to catch all exceptions.
             * Spring throws BeanException wrapped around DictionaryException.
             * Do not want to log stack trace here.  We just one one error message.
             * Then return a new error code indicating there is no product definition.
             */
            log.error("Unable to load correct dictionaries: " + ExceptionTools.getMessage(e.getCause()));
            
            setReturnCode(IProductDecom.NO_PRODUCT_DEF);
            return false;
        }
        
        final int apid = metadata.getApid();
        final int xmlVersion = metadata.getXmlVersion();
        boolean hasValue = false;


        final IProductDefinitionKey key = this.definitionFactory.createDefinitionKey(apid, xmlVersion);
        final IProductDefinition def = dictionary.getDefinition(key);

        try {
            outf.headerStart(metadata);
            List<IProductObjectDefinition> productObjects = null;
            if (def != null) {
                productObjects = def.getProductObjects();
            }
            if (def != null && productObjects != null && !productObjects.isEmpty()) {
                if (productObjects.size() > 1) {
                    log.error("Too many data product objects in product definition. Only the first will be processed.");
                }
                final List< IProductDecomField > fields = productObjects.get(0).getFields();
                for (final IProductDecomField el: fields) {

                    el.printValue(bytestream, outf);
                    hasValue = true;
                }
                if (hasValue && bytestream.hasMore()) {
                    final String units = (bytestream.remainingBytes() == 1) ? "byte" : "bytes";
                    log.warn("Product data has ", bytestream.remainingBytes(), " extra ", units);
                }
            }
            else {
                /*
                 * The output stream can be null if
                 * no text was requested. Same with hexdump.
                 */
                if (out != null) {
                    out.println("Product dictionary definition not found");
                } else {
                    log.error("Product dictionary definition not found");
                }
                setReturnCode(IProductDecom.NO_PRODUCT_DEF);
                if (hexdump != null) {
                    hexdump.reset();
                    hexdump.handleByteStream(bytestream);
                    hexdump.handleEof();
                }
                success = false;
            }
        }

        catch (final IOException ignore) { }

        outf.headerEnd();

        DecomHandler view = null;
        if (def != null) { 
            view = (def).getExternalHandler() ;
        }
        if (view != null && showProductViewer) {
            final String fullPath = metadata.getFullPath();
            String name = view.getHandlerName();
            if (!name.startsWith(File.separator) && viewerDir != null) {
                name = viewerDir + File.separator + name;
            }
            final String v = name + " " + fullPath;
            final Parser p = new Parser();
            final String[] command = p.getParsedStrings(v, " ");
            final ProcessLauncher launcher = new ProcessLauncher();
            launcher.setOutputHandler(new StdoutLineHandler());
            launcher.setErrorHandler(new StderrLineHandler());
            if (showLaunch) {
                final StringBuilder commandStr = new StringBuilder();
                for (final String s: command) {
                    commandStr.append(s).append(" ");
                }
                System.out.println("Launching " + commandStr.toString());
            }
            try {
                launcher.launchSync(command);
            }
            catch (final IOException e) {
                log.error("Error running process ", command[0], ": ", e.toString(), e.getCause());
                setReturnCode(IProductDecom.FAILURE);
                success = false;
            }
        } else if (view == null && showDpoViewer) {
            log.warn("No defined product viewer for APID: ", metadata.getApid());
            if (suppressText) {
                setReturnCode(IProductDecom.NO_PROD_VIEWER);
                success = false;
            }
        }
        
        return success;
    }
}

