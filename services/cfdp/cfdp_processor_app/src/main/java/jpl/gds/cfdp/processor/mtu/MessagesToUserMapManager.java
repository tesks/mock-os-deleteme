/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.processor.mtu;

import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.util.CfdpFileUtil;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

/**
 * Manager for Messages to User mappings. Loads the mappings from a properties file and serves those mappings to rest
 * of the application.
 *
 * @since 8.2.0
 */
@Service
@DependsOn("configurationManager")
public class MessagesToUserMapManager {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    private Properties mtuMapProperties;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private CfdpFileUtil cfdpFileUtil;

    private String mtuMapFile;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);

        mtuMapFile = configurationManager.getMessagesToUserMapFile();
        log.info("CFDP Processor Messages to User map file: " + mtuMapFile);

        // If the file doesn't exist, create it
        final java.io.File f = new File(mtuMapFile);

        if (!f.exists() || !f.isFile()) {
            log.warn("Messages to User map file " + mtuMapFile + " doesn't exist");

            try {
                cfdpFileUtil.createParentDirectoriesIfNotExist(mtuMapFile);
                f.createNewFile();
                log.info("Created a new Messages to User map file " + mtuMapFile);
            } catch (final IOException ie) {
                log.error("Failed to create new Messages to User map file: "
                        + ExceptionTools.getMessage(ie), ie);
            }

        }

        // Load and get all mappings from the file
        try (InputStream is = new FileInputStream(mtuMapFile)) {
            mtuMapProperties = new OrderedProperties();
            mtuMapProperties.load(is);
        } catch (IOException e) {
            log.error("Error loading Messages to User mappings from " + mtuMapFile);
            e.printStackTrace();
        }

        Enumeration<String> mnemonics = (Enumeration<String>) mtuMapProperties.propertyNames();

        while (mnemonics.hasMoreElements()) {
            String mnemonic = mnemonics.nextElement();
            String hexMapping = mtuMapProperties.getProperty(mnemonic);
            log.debug("Loaded Message to User mapping:", mnemonic, "=", hexMapping);
        }

    }

    /**
     * @return set of all mapped Message to User key strings
     */
    public Set<String> getAllMenmonics() {
        return mtuMapProperties.stringPropertyNames();
    }

    /**
     * Look up the mnemonic's Message to User mapping and return the byte array.
     *
     * @param mnemonic Message to User key string for the mapping
     * @return byte array of the actual Message to User content, null if no mapping found
     */
    public byte[] getBytesForMnemonic(final String mnemonic) {

        if (mtuMapProperties.containsKey(mnemonic)) {

            try {
                return BinOctHexUtility.toBytesFromHex(mtuMapProperties.getProperty(mnemonic));
            } catch (final Exception e) {
                log.error("Message to User mapped value", mtuMapProperties.getProperty(mnemonic), "is not a valid " +
                        "hexadecimal string:", ExceptionTools.rollUpMessages(e));
            }

        } else {
            log.debug("MTU mnemonic", mnemonic, "not mapped");
        }

        return null;
    }

    /**
     * Getter for Message to User map properties.
     *
     * @return Message to User map properties
     */
    public Properties getMtuMapProperties() {
        return mtuMapProperties;
    }

}