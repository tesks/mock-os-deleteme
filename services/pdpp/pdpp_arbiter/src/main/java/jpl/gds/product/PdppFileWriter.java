/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.product;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.thread.SleepUtilities;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.springframework.context.ApplicationContext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * File creation tool to allow PDPP to create new child products and metadata files after processing
 */
public class PdppFileWriter implements IPdppFileWriter {
    private final IAutomationLogger log;


    private static final Namespace				NAMESPACE_EMD	= Namespace.getNamespace("http://dsms.jpl.nasa.gov/mpcs_mm-core_emd");
    private static final Namespace				NAMESPACE_MPCS	= Namespace.getNamespace("http://dsms.jpl.nasa.gov/mpcs");

    private final IContextConfiguration srcSessionConfig;
    private final IContextConfiguration		dstSessionConfig;
    private final IProductMetadata md;

    private final ApplicationContext appContext;

    /**
     * @param srcSessionConfig the original session of the product
     * @param dstSessionConfig the new session of the product
     * @param md the metadata object of the file
     */
    public PdppFileWriter(
                             final IContextConfiguration srcSessionConfig,
                             final IContextConfiguration dstSessionConfig,
                             final IProductMetadata md,
                             final ApplicationContext appContext) {

        this.srcSessionConfig = srcSessionConfig;
        log = appContext.getBean(IAutomationLogger.class);
        this.dstSessionConfig = dstSessionConfig;
        this.md = md;
        this.appContext = appContext;

    }

    /**
     * Copy a product entry from the source session to the destination session.
     *
     * @param srcPfn the file that is being written
     * @throws ProductException an error is encountered while writing the product
     */
    @Override
    public IProductFilename writeFile(final IProductFilename srcPfn) throws ProductException {
        try {
            final File srcDataFile = new File(srcPfn.getDataFilePath());
            final File srcMetadataFile = new File(srcPfn.getMetadataFilePath());

            /*
             * Create DOM of source metadata in preparation for morphing
             */
            final Document doc = new SAXBuilder().build(srcMetadataFile);

            /*
             * Perform the metadata translation.
             */
            final IProductFilename reservedDstPfn = copyAndWriteMetadata(md, doc);

            /*
             * Retrieve the destination data file name from the PFN created when morphing the metadata
             */
            final File dstDataFile = reservedDstPfn.reserve().getReservedDataFile();

            /*
             * Copy data file if and only if source and destination are different
             */
            if (!srcDataFile.equals(dstDataFile)) {
                /**
                 * 05/21/2012  - MPCS-3719 Allow user configuration of how many retry attempts MPCS will make when attempting to
                 * verify that a written file actually exists on the file system. (default = 10, disable = 0).
                 */
                int retry;
                log.info("Copying data file to            : " + FileUtility.createFilePathLogMessage(dstDataFile));
                FileUtility.copyFile(srcDataFile, dstDataFile);
                for (retry = 0; retry < 10; retry++) {
                    if (dstDataFile.exists())
                        break;
                    log.warn("File: \"" + FileUtility.createFilePathLogMessage(dstDataFile) + "\" does not exist after writing: Try #" + retry + " of " + 10);
                    SleepUtilities.fullSleep(100);
                }
                if (retry == 10) {
                    log.warn("File existance verification failure: Retry Count (" + retry + ") exceeded while attempting to verify write of file: \"" + FileUtility.createFilePathLogMessage(dstDataFile) + "\"");
                }
            }
            return reservedDstPfn;
        }
        catch (final JDOMException e) {
            throw new ProductException("Malformed EMD file. Could not writeFile product \"" + md.getFilename() + "\" from Session " + srcSessionConfig.getContextId().getNumber() + " (" + srcSessionConfig.getContextId().getHost() + ") to Session " + dstSessionConfig.getContextId().getNumber() + " ("
                    + dstSessionConfig.getContextId().getHost() + "): " + e, e);
        }
        catch (final IOException e) {
            throw new ProductException("I/O Exception. Could not writeFile product \"" + md.getFilename() + "\" from Session " + srcSessionConfig.getContextId().getNumber() + " (" + srcSessionConfig.getContextId().getHost() + ") to Session " + dstSessionConfig.getContextId().getNumber() + " ("
                    + dstSessionConfig.getContextId().getHost() + "): " + e, e);
        }
        catch (final ExcessiveInterruptException e) {
            throw new ProductException("Could not sleep when retrying morphing product \"" + md.getFilename() + "\" from Session " + srcSessionConfig.getContextId().getNumber() + " (" + srcSessionConfig.getContextId().getHost() + ") to Session " + dstSessionConfig.getContextId().getNumber()
                    + " (" + dstSessionConfig.getContextId().getHost() + "): " + e, e);
        }
        catch (final SessionException e) {
            throw new ProductException("Could not retrieve destination Session when morphing product \"" + md.getFilename() + "\" from Session " + srcSessionConfig.getContextId().getNumber() + " (" + srcSessionConfig.getContextId().getHost() + ") to Session "
                    + dstSessionConfig.getContextId().getNumber() + " (" + dstSessionConfig.getContextId().getHost() + "): " + e, e);
        }
        catch (final NullPointerException e) {
            throw new ProductException("Unknown error morphing product \"" + md.getFilename() + "\" from Session " + srcSessionConfig.getContextId().getNumber() + " (" + srcSessionConfig.getContextId().getHost() + ") to Session " + dstSessionConfig.getContextId().getNumber() + " (" + dstSessionConfig.getContextId().getHost()
                    + "): " + e, e);
        }
    }

    /**
     * Copy a product metadata entry from the source session to the destination
     * session and commit it to a file
     *
     * @param md
     *            the metadata to be morphed
     * @param doc
     *            the destination file for the morphed metadata
     * @throws SessionException
     *             an error was encountered while morphing the metadata
     * @throws JDOMException
     *             a JDOM error was encountered while morphing the metadata
     * @throws IOException
     *             an I/O error was encountered while writing the metadata to
     *             file
     * @throws ExcessiveInterruptException
     *             an interruption was encountered while waiting to attempt to
     *             write to the metadata file
     */
    private IProductFilename copyAndWriteMetadata(final IProductMetadata md, final Document doc) throws SessionException, JDOMException, IOException, ExcessiveInterruptException {
        /*
         * Output new XML for child .emd file.
         */
        final IProductFilename reservedDstPfn = copyMetadata(md, doc);
        final XMLOutputter outputter = new XMLOutputter();
        final File f = reservedDstPfn.getReservedMetadataFile();
        final FileOutputStream fos = new FileOutputStream(f);
        log.info("Writing processed file to               : " + f);
        outputter.output(doc, new BufferedOutputStream(fos));
        fos.flush();
        fos.close();

        /**
         * 05/21/2012  - MPCS-3719 Allow user configuration of how many retry attempts MPCS will make when attempting to
         * verify that a written file actually exists on the file system. (default = 10, disable = 0).
         */
        int retry;
        for (retry = 0; retry < 10; retry++) {
            if (f.exists())
                break;
            log.warn("File: \"" + FileUtility.createFilePathLogMessage(f) + "\" does not exist after writing: Try #" + retry + " of " + 10);
            SleepUtilities.fullSleep(100);
        }
        if (10 == retry) {
            log.warn("File existance verification failure: Retry Count (" + retry + ") exceeded while attempting to verify write of file: \"" + FileUtility.createFilePathLogMessage(f) + "\"");
        }
        return reservedDstPfn;
    }

    /**
     * Copy a product metadata entry from the source session to the destination
     * session
     *
     * @param md
     *            the metadata to be written
     * @param doc
     *            the destination file for the metadata
     * @throws IOException
     *             an I/O error occurred while writing the metadata
     */
    private IProductFilename copyMetadata(final IProductMetadata md, final Document doc) throws IOException {
        final Element rootElement = doc.getRootElement();
        final Element sessionDictionaryVersionElement = rootElement.getChild("SessionInformation", NAMESPACE_EMD).getChild("SessionId", NAMESPACE_MPCS)
                .getChild("FswDictionaryVersion", NAMESPACE_MPCS);
        final Element sessionIdElement = rootElement.getChild("SessionInformation", NAMESPACE_EMD).getChild("SessionId", NAMESPACE_MPCS).getChild("Number", NAMESPACE_MPCS);
        final Element sessionNameElement = rootElement.getChild("SessionInformation", NAMESPACE_EMD).getChild("SessionId", NAMESPACE_MPCS).getChild("Name", NAMESPACE_MPCS);

        /*
         * Modify original values.
         *
         * Set Session info.
         */
        sessionDictionaryVersionElement.setText(appContext.getBean(DictionaryProperties.class).getFswVersion());
        sessionIdElement.setText(Long.toString(dstSessionConfig.getContextId().getNumber()));
        md.setSessionId(dstSessionConfig.getContextId().getNumber());
        sessionNameElement.setText(dstSessionConfig.getContextId().getName());
        //outputDirectoryElement.setText(dstSessionConfig.getGeneralInfo().getOutputDir());

        /*
         * Set Product info.
         */
        // MPCS-8180 07/14/16 - updated file naming process to AMPCS's process

        md.setDataFileName(md.isPartial());
        final IProductFilename reservedDstPfn = (appContext.getBean(PdppApiBeans.PDPP_PRODUCT_FILENAME_BUILDER_FACTORY,IProductFilenameBuilderFactory.class)
                .createBuilder())
                .addVenueAppropriateFullyQualifiedDataFilename(dstSessionConfig, md)
                .build().reserve();

        final String dstDataFileName = reservedDstPfn.getReservedDataFile().getAbsolutePath();
        md.setFullPath(dstDataFileName);

        return reservedDstPfn;
    }
}
