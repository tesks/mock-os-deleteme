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

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.time.DataValidityTime;

/**
 * 
 * XmlOutputFormatter is the base class from which product decom formatters
 * that output XML text can be extended. It provides basic text formatting
 * methods.
 * 
 *
 */
public class XmlOutputFormatter extends AbstractProductOutputFormatter {

    /**
     * Constructor
     * @param appContext the current application context
     * @param format context-aware print format object to use
     */
	public XmlOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super(appContext, format);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startOutput() {
		out.println("<?xml version=\"1.0\" ?>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void headerEnd() {
		Indent.decr();
		Indent.print(out);
		out.println("</product>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nameValue(final String name, final String value) {
		Indent.print(out);
		out.println("<value name=\"" + name + "\">" + value + "</value>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nameValue(final String name, final String value, final String units) {
		Indent.print(out);
		if (units == null) {
			out.println("<value name=\"" + name + "\">" + value + "</value>");
		} else {
			out.println("<value name=\"" + name + "\" units=\"" + units + "\">"
					+ value + "</value>");
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void arrayStart(final String name, final int length) {
		Indent.print(out);
		out.println("<array name=\"" + name + "\" length=\"" + (length == -1 ? "variable" : length) + "\">");
		Indent.incr();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void arrayEnd() {
		Indent.decr();
		Indent.print(out);
		out.println("</array>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void arrayIndex(final String name) {
		Indent.print(out);
		out.println("<index name=\"" + name + "\">");
		Indent.incr();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void arrayIndexEnd() {
		Indent.decr();
		Indent.print(out);
		out.println("</index>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endOutput() {
	    // do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void structureStart(final String name) {
		Indent.print(out);
		out.println("<structure name=\"" + name + "\">");
		Indent.incr();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void structureEnd() {
		Indent.decr();
		Indent.print(out);
		out.println("</structure>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void structureValue(final String value) {
		Indent.print(out);
		out.println("<value><![CDATA[$" + value + "]]></value>");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addressValue(final int address, final String value) {
		Indent.print(out);
        final String addrString = new SprintfFormat().anCsprintf("[%08d]", address);
		out.println("<value address=\"" + addrString + "\">" + value
				+ "</value>");
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
        final String ert = metadata.getErtStr();
        
        Indent.print(out);
        out.print("<product filename=\"" + filename + "\"");
        out.print(" dvt=\"" + dvt + "\"");
        out.print(" apid=\"" + apid + "\"");
        out.print(" type=\"" + getProductType(apid) + "\"");
        out.print(" scid=\"" + metadata.getScid() + "\"");
        out.print(">\n");
        Indent.print(out);
        out.println("<scet>" + scet + "</scet>");
        Indent.print(out);
        out.println("<sclk>" + sclk + "</sclk>");
        Indent.print(out);
        out.println("<lst>" + metadata.getSolStr() + "</lst>");
        Indent.print(out);
        out.println("<ert>" + ert + "</ert>");
        Indent.incr();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoEnd() {
        Indent.decr();
        Indent.print(out);
        out.println("</dpo>");
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoStart(final String name, final int dpoId) {
        Indent.print(out);
        out.println("<dpo name=\"" + name + "\" id=\"" + dpoId + "\">");
        Indent.incr();
    }
}
