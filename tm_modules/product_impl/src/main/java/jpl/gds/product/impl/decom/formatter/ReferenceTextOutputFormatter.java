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
package jpl.gds.product.impl.decom.formatter;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;

/**
 * ReferenceTextOutputFormatter is an OutputFormatter that formats product decom
 * information as plain text.
 * 
 *
 */
public class ReferenceTextOutputFormatter extends TextOutputFormatter {

    /**
     * Constructor
     * @param missionProperties current mission properties object
     * @param appContext the current application context
     * @param format context-aware print format object to use
     */
    public ReferenceTextOutputFormatter(final MissionProperties missionProperties, final ApplicationContext appContext, final SprintfFormat format) {
		super(missionProperties, appContext, format);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void headerStart(final IProductMetadataProvider metadata) {
        final String filename = metadata.getFullPath();
        final int scid = metadata.getScid();
        final int apid = metadata.getApid();
        final String type = getProductType(apid);
        final String scet = metadata.getScetStr();
        final String sclk = metadata.getSclkStr();
        out.println();
        Indent.print(out);
        out.println("Filename:   " + filename);
        Indent.print(out);
        out.println("Type:       " + type);
        Indent.print(out);
        out.println("Spacecraft: " + missionProperties.mapScidToMnemonic(scid)
                + " (SCID=" + scid + ")");
        Indent.print(out);
        out.println("SCLK:        " + sclk);
        Indent.print(out);
        out.println("SCET:        " + scet);
        Indent.print(out);
        if (appContext.getBean(EnableLstContextFlag.class).isLstEnabled()) {
        	  out.println("LST:        " + metadata.getSolStr());
              Indent.print(out);
        }
        out.println();
        out.println("Product APID=" + apid);
        Indent.incr();
    }
}
