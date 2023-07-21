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
/**
 * 
 */
package jpl.gds.product.impl.decom.formatter;

import java.io.PrintStream;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.formatting.SprintfFormat;

/**
 * The NullOutputFormatter class is an output formatter that throws everything away. Yes, believe it 
 * or not, it throws everything away. This allows us to traverse a product as if decomming it 
 * without producing any output.
 *
 *
 */
public class NullOutputFormatter implements IDecomOutputFormatter {

    private PrintStream out;
    /** Current application context */
	protected final ApplicationContext appContext;
	/** Context-aware print formatter object */
	protected final SprintfFormat format;


    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param format print formatter object to use
     */
    public NullOutputFormatter(final ApplicationContext appContext, final SprintfFormat format) {
		super();
		this.appContext = appContext;
		this.format = format;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.formatter.IDecomOutputFormatter#getApplicationContext()
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.decom.formatter.IDecomOutputFormatter#getPrintFormatter()
	 */
	@Override
	public SprintfFormat getPrintFormatter() {
		return format;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public void addressValue(final int address, final String value) {
         // Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayEnd() {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndex(final String name) {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayIndexEnd() {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void arrayStart(final String name, final int length) {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void endOutput() {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public PrintStream getPrintStream() {
        return out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value) {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void nameValue(final String name, final String value, final String units) {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setPrintStream(final PrintStream ps) {
        out = ps;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void startOutput() {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureEnd() {
    	// Deliberately empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void structureStart(final String name) {
    	// Deliberately empty
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
	public void structureValue(final String value) {
    	// Deliberately empty
    }
}
