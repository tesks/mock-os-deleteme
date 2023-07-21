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
package jpl.gds.product.impl.decom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import jpl.gds.dictionary.api.DecomHandler;
import jpl.gds.dictionary.api.IDecomHandlerSupport;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.string.Parser;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.shared.types.HexDump;

/**
 * StreamField represents the definition of a decom definition whose value is an
 * unstructured stream of bytes.
 * 
 */
public class StreamField extends AbstractDecomField implements IDecomHandlerSupport {

    private final Tracer log     = TraceManager.getDefaultTracer();
    private final int maxlength;
    private String display = "none";
    private DecomHandler viewer = null;

    /**
     * Creates an instance of StreamField.
     * 
     * @param name the name of the field
     * @param maxlength the maximum length of the field in bytes
     * @param display the display preference for the field: should be "text",
     *            "hexdump", or "none".
     */
    @Deprecated
    public StreamField(final String name, final int maxlength, final String display) {
        super(ProductDecomFieldType.STREAM_FIELD);
        this.name = name;
        this.maxlength = maxlength;
        this.display = display;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int printValue(final ByteStream stream, final IDecomOutputFormatter out, final int depth)
                                                                            throws IOException {
        int len = maxlength;
        if (maxlength == -1) {
            len = (int) stream.remainingBytes();
        }
        if (display.equalsIgnoreCase("none")) {
            // printType(out, depth);
            if (viewer == null) {
                stream.skip(len);
            }
        } else if (display.equalsIgnoreCase("text")) {
            final ByteArraySlice slice = stream.read(len);
            final String val = GDR.stringValue(slice.array, slice.offset, len);
            out.nameValue(name, val);
        } else if (display.equalsIgnoreCase("hexdump")) {
            final HexDump hexwriter = new HexDump(new OutputStreamWriter(out
                    .getPrintStream()));
            final ByteArraySlice slice = stream.read(len);
            hexwriter.handleBytes(slice.array, slice.offset, len);
            hexwriter.handleEof();
        }
        if (viewer != null) {
            final File tmpfile = File.createTempFile("stream", ".dat");
            tmpfile.deleteOnExit();
            final FileOutputStream outstream = new FileOutputStream(tmpfile);
            stream.write(outstream, len);
            outstream.close();
            final StringBuffer v = new StringBuffer(viewer.getHandlerName());
            v.append(" " + tmpfile.getPath());
            final Parser p = new Parser();
            final String[] command = p.getParsedStrings(new String(v), " ");
            final ProcessLauncher launcher = new ProcessLauncher();
            launcher.setOutputHandler(new StdoutLineHandler());
            launcher.setErrorHandler(new StderrLineHandler());
            try {
                launcher.launch(command);
            } catch (final IOException e) {
                log.error("Error running process", e);
                return len;
            }
            launcher.waitForExit();
            if (tmpfile.exists()) {
                tmpfile.delete();
            }
        }
        return len;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.decom.AbstractDecomField#printType(java.io.PrintStream, int)
     */
    @Override
    public void printType(final PrintStream out, final int depth) throws IOException {
        printIndent(out, depth);
        out.println("(Stream of " + maxlength + ") " + name + " display="
                + display);
    }


    /**
     * {@inheritDoc}
     * 
     * For stream field the value size is the maximum stream length, or -1 if
     * no maximum length is set.
     * 
     * @see jpl.gds.product.impl.decom.AbstractDecomField#getValueSize()
     */
    @Override
    public int getValueSize() {
        return maxlength;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#getExternalHandler()
     */
    @Override
	public DecomHandler getExternalHandler() {
        return null;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#getInternalHandler()
     */
    @Override
	public DecomHandler getInternalHandler() {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#hasExternalHandler()
     */
    @Override
	public boolean hasExternalHandler() {
        return viewer != null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#hasInternalHandler()
     */
    @Override
	public boolean hasInternalHandler() {
        return false;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#setExternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setExternalHandler(final DecomHandler handler) {
        viewer = handler;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IDecomHandlerSupport#setInternalHandler(jpl.gds.dictionary.impl.impl.api.DecomHandler)
     */
    @Override
	public void setInternalHandler(final DecomHandler handler) {
        throw new UnsupportedOperationException(
                "Stream Field does not support an internal handler");
    }
}
