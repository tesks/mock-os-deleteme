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

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.time.DataValidityTime;

/**
 * ReferenceXmlOutputFormatter is an generic product OutputFormatter that formats product
 * decom information as XML output.
 *
 *
 */
public class ReferenceXmlOutputFormatter extends XmlOutputFormatter {
    
    /**
     * Constructor
     * @param appContext the current application context
     * @param format context-aware print format object to use
     */
    public ReferenceXmlOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super(appContext, format);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void headerStart(final IProductMetadataProvider metadata) {
        final String filename = metadata.getFullPath();
        final String dvt = new DataValidityTime(metadata.getDvtCoarse(), metadata.getDvtFine()).toString();
        final int apid = metadata.getApid();
        final String scet = metadata.getScetStr();
        final String sclk = metadata.getSclkStr();
        Indent.print(out);
        out.print("<product filename=\"" + filename + "\"");
        out.print(" dvt=\"" + dvt + "\"");
        out.print(" apid=\"" + apid + "\"");
        out.print(" type=\"" + getProductType(apid) + "\"");
        out.print(" scid=\"" + metadata.getScid() + "\"");
        out.print(">\n");
        Indent.incr();
        Indent.print(out);
        out.println("<scet>" + scet + "</scet>");
        Indent.print(out);
        out.println("<sclk>" + sclk + "</sclk>");
        if (appContext.getBean(EnableLstContextFlag.class).isLstEnabled()) {
        	out.println("<lst>" + metadata.getSolStr() + "</lst>");
        }
    }
    
}
