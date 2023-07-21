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
package jpl.gds.cfdp.processor.ampcs.product;

import cfdp.engine.TransStatus;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;

/**
 * {@code CfdpMultimissionDvtExtractor} extracts the Data Validity Time from the product's filename, based on the scheme
 * defined in the AMPCS Multimission Data Product Software Interface Software (SIS) (DOC-000693 Rev A).
 *
 * @since 8.1.0
 */
public class CfdpMultimissionDvtExtractor implements ICfdpDvtExtractor {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private IProductPropertiesProvider productPropertiesProvider;

    private Tracer log;
    private String filenameDvtMarker;
    private String filenameDvtSeparator;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        filenameDvtMarker = productPropertiesProvider.getFileDvtMarker();
        filenameDvtSeparator = productPropertiesProvider.getFileDvtSeparator();
    }

    @Override
    public long extractDvtCoarse(final TransStatus status) {
        return Long.parseUnsignedLong(splitFilenameIntoDvtPortions(status)[0]);
    }

    @Override
    public long extractDvtFine(final TransStatus status) {
        return Long.parseUnsignedLong(splitFilenameIntoDvtPortions(status)[1]);
    }

    private String[] splitFilenameIntoDvtPortions(final TransStatus status) {
        if (status.getDestinationFile() == null || "".equals(status.getDestinationFile())) {
            throw new IllegalArgumentException("TransStatus missing a valid destination filename. Cannot extract DVT.");
        }

        final String filename = Paths.get(status.getDestinationFile()).getFileName().toString();

        // <basename>_<DVT-SCLK-coarse>-<DVT-SCLK-file>

        final int dvtStart = filename.indexOf(filenameDvtMarker);
        final String dvtPortion = filename.substring(dvtStart + 1);
        final String[] portions = dvtPortion.split(filenameDvtSeparator);

        if (dvtStart == -1 || portions.length < 2) {
            throw new IllegalArgumentException("Product filename " + filename + " does not match the expected format. Cannot extract DVT.");
        }

        return portions;
    }

}