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

import java.util.StringTokenizer;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.io.Indent;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * 
 * TextOutputFormatter is the base class from which product decom formatters
 * that output plain text can be extended. It provides basic text formatting
 * methods.
 * 
 *
 */
public class TextOutputFormatter extends AbstractProductOutputFormatter {
	
    /** Mission configuration properties object */
	protected final MissionProperties missionProperties;
	
    /**
     * Constructor
     * @param missionProperties current mission properties object
     * @param appContext the current application context
     * @param format context-aware print format object to use
     */
    public TextOutputFormatter(final MissionProperties missionProperties, final ApplicationContext appContext, final SprintfFormat format) {
		super(appContext, format);
		this.missionProperties = missionProperties;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void headerEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void startOutput() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value) {
        Indent.print(out);
        if (value == null) {
            out.println(name + ":");
        } else {
            out.println(name + ": " + value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value, final String units) {
        Indent.print(out);
        String vstr;
        String ustr;
        vstr = value;
        if (vstr == null) {
            vstr = "";
        }
        ustr = units;
        if (ustr == null) {
            ustr = "";
        }
        out.println(name + ": " + vstr + " " + ustr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayStart(final String name, final int length) {
        Indent.print(out);
        out.println("Array " + name + " (length="
                + (length == -1 ? "variable" : String.valueOf(length)) + ")");
        Indent.incr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndex(final String name) {
        Indent.print(out);
        out.println("[" + name + "]");
        Indent.incr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndexEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void endOutput() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureStart(final String name) {
        Indent.print(out);
        out.println("Structure " + name);
        Indent.incr();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public void structureEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void addressValue(final int address, final String value) {
        Indent.print(out);
        final String addrString = new SprintfFormat().anCsprintf("[%08d]", address);

        String vstr = value;
        if (vstr == null) {
            vstr = "";
        }
        out.println(addrString + ": " + vstr);
    }


    /**
     * {@inheritDoc}
     */
    @Override
	public void structureValue(final String value) {
        final StringTokenizer tokens = new StringTokenizer(value, "\n");
        while (tokens.hasMoreTokens()) {
            Indent.print(out);
            out.println(tokens.nextToken());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void headerStart(final IProductMetadataProvider metadata) {

        final String filename = metadata.getFullPath();
        final int scid = metadata.getScid();
        final int apid = metadata.getApid();
        final String scet = metadata.getScetStr();
        final String sclk = metadata.getSclkStr();
        final ISclk dvt = new Sclk(metadata.getDvtCoarse(), metadata.getDvtFine());
        final String ert = metadata.getErtStr();

        out.println();
        Indent.print(out);
        out.println("Filename: " + filename);
        Indent.print(out);
        out.println("Apid: " + apid);
        Indent.print(out);
        out.println("Type: " + getProductType(apid));
        Indent.print(out);
        out.println("Spacecraft: " + missionProperties.mapScidToMnemonic(scid)
                + " (Scid=" + scid + ")");
        Indent.print(out);
        out.println("Dvt: " + dvt);
        Indent.print(out);
        out.println("Scet: " + scet);
        Indent.print(out);
        out.println("Sclk: " + sclk);
        Indent.print(out);
        out.println("Lst: " + metadata.getSolStr());
        Indent.print(out);
        out.println("Ert: " + ert);
        Indent.incr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoEnd() {
        Indent.decr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void dpoStart(final String name, final int dpoId) {
        Indent.print(out);
        if (dpoId != 0) {
            out.println("DPO " + name + " (" + dpoId + ")");
        } else {
            out.println("DPO " + name + " (NO VID)");
        }
        Indent.incr();
    }
}
