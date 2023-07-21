/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.tc.mps.impl.ctt.repo;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.exception.CommandFileParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * This object isolates process calls around CTS's cxml command dictionary compiler tool.
 *
 */
public class CtsCxml implements ICtsCxml {
    private static final String CTS_UNKNOWN           = "CTS_unknown";
    private static final String COMPILED_FILE_EXT     = ".proc";
    private static final String COMPILED_REV_FILE_EXT = ".rproc";

    private final Tracer tracer;
    private final String compileXmlBinary;

    /**
     * Constructor
     *
     * @param cxmlBinary
     * @param tracer
     */
    public CtsCxml(final String cxmlBinary, final Tracer tracer) {
        this.compileXmlBinary = cxmlBinary;
        this.tracer = tracer;
    }

    /**
     * Constructor, uses default uplink tracer.
     *
     * @param cxmlBinary
     */
    public CtsCxml(String cxmlBinary) {
        this(cxmlBinary, TraceManager.getTracer(Loggers.UPLINK));
    }

    @Override
    public String getCtsVersion() {
        List<String> args = new ArrayList<>();
        args.add(compileXmlBinary);
        args.add("-version");

        final ProcessBuilder pb = new ProcessBuilder(args);
        try {
            String version;

            final Process            p        = pb.start();
            final ProcessOutConsumer consumer = new ProcessOutConsumer(p, tracer);

            consumer.start();
            p.waitFor();
            consumer.join();
            String error = consumer.getStderr();
            if (stringOk(error)) {
                tracer.debug(error);
            }

            version = consumer.getStdout();
            if (stringOk(version)) {
                version = version.replace(' ', '_').trim();
                return version;
            } else {
                return CTS_UNKNOWN;
            }
        } catch (IOException e) {
            tracer.error(e);
            return CTS_UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tracer.error(e);
            return CTS_UNKNOWN;
        }
    }

    /**
     * Compile the command dictionary, returning the file path. This will return the XML file path if the dictionary was
     * unable to be compiled.
     */
    @Override
    public CommandTranslationTablePaths compileCommandDictionary(
            final String cmdDictPath,
            final boolean validateSchema,
            final String compiledBasePath,
            final String basename) throws
                                   CommandFileParseException {
        // do not compile if the file already exists.
        final File forwardFile = new File(compiledBasePath + basename + COMPILED_FILE_EXT);
        final File reverseFile = new File(compiledBasePath + basename + COMPILED_REV_FILE_EXT);
        if (filesExist(forwardFile, reverseFile)) {
            tracer.info("Forward dictionary hash file found for ", cmdDictPath,
                    ", loading compiled dictionary file from ",
                    forwardFile.getAbsolutePath());
            tracer.info("Reverse dictionary hash file found for ", cmdDictPath,
                    ", loading compiled dictionary file from ",
                    reverseFile.getAbsolutePath());
            return new CommandTranslationTablePaths(forwardFile.getAbsolutePath(),
                    reverseFile.getAbsolutePath());
        }

        tracer.info("Dictionary hash files not found for ", cmdDictPath,
                ", compiling new forward and reverse dictionaries at ", forwardFile.getAbsolutePath(), " and ",
                reverseFile.getAbsolutePath());

        List<String> args = new ArrayList<>();
        args.add(compileXmlBinary);
        args.add("-o");
        args.add(basename);
        args.add("-proc");

        // MPCS-11403 11/19/19: Add config for passing -noschema to CTS
        if (!validateSchema) {
            args.add("-noschema");
        }

        args.add(cmdDictPath);

        // run the compile process
        final ProcessBuilder pb = new ProcessBuilder(args);

        // set the working directory for the process
        pb.directory(new File(compiledBasePath));

        try {
            final Process            p        = pb.start();
            final ProcessOutConsumer consumer = new ProcessOutConsumer(p, tracer);
            consumer.start();
            final int status = p.waitFor();
            consumer.join();
            if (status != 0) {
                tracer.error("An error occurred while compiling the command dictionary.");
            }
            final String stderr = consumer.getStderr();
            if (stringOk(stderr)) {
                writeToLog(TraceSeverity.DEBUG, stderr);
            }
            final String stdout = consumer.getStdout();
            if (stringOk(stdout)) {
                writeToLog(TraceSeverity.INFO, stdout);
            } else {
                writeToLog(TraceSeverity.WARN, "Unable to retrieve CTS CXML version");
            }
        } catch (final IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new CommandFileParseException("An error has occurred compiling a command dictionary.", e.getCause());
        }

        String forwardPath = cmdDictPath;
        String reversePath = cmdDictPath;
        // sanity check, return compiled path if it exists
        if (forwardFile.exists()) {
            forwardPath = forwardFile.getAbsolutePath();
        } else {
            tracer.warn("A compiled forward command translation table could not be created.");
        }

        if (reverseFile.exists()) {
            reversePath = reverseFile.getAbsolutePath();
        } else {
            tracer.warn("A compiled reverse command translation table could not be created.");
        }
        return new CommandTranslationTablePaths(forwardPath, reversePath);
    }

    private boolean stringOk(String stderr) {
        return stderr != null && !stderr.isEmpty();
    }

    private boolean filesExist(File forwardFile, File reverseFile) {
        return forwardFile.exists() && reverseFile.exists();
    }

    private void writeToLog(TraceSeverity level, String messages) throws IOException {
        try (BufferedReader r = new BufferedReader(new StringReader(messages))) {
            String line;
            while ((line = r.readLine()) != null) {
                tracer.log(level, "cxml: " + line);
            }
        }
    }

    /**
     * Consumer for process STDERR and STDOUT
     */
    static class ProcessOutConsumer {
        final StringWriter out = new StringWriter();
        final Thread       outThread;
        final StringWriter err = new StringWriter();
        final Thread       errThread;
        final Tracer       tracer;

        private boolean ready;

        /**
         * Constructor
         *
         * @param p process
         * @param t tracer
         */
        ProcessOutConsumer(final Process p, final Tracer t) {
            outThread = new Thread(new StreamConsumer(p.getInputStream(), out, t));
            errThread = new Thread(new StreamConsumer(p.getErrorStream(), err, t));
            tracer = t;
        }

        /**
         * Start consumer threads
         */
        void start() {
            outThread.start();
            errThread.start();
        }

        /**
         * Wait for consumer threads to exit
         *
         * @throws InterruptedException
         */
        void join() throws InterruptedException {
            outThread.join();
            errThread.join();
            ready = true;
        }

        /**
         * Retrieve process STDOUT
         *
         * @return process STDOUT
         */
        String getStdout() {
            if (!ready) {
                tracer.warn("Retrieved process stdout before it became ready");
            }
            return out.toString();
        }

        /**
         * Retrieve process STDERR
         *
         * @return process STDERR
         */
        String getStderr() {
            if (!ready) {
                tracer.warn("Retrieved process stderr before it became ready");
            }
            return err.toString();
        }
    }

    /**
     * Consume an inputstream and write it to a writer
     */
    private static class StreamConsumer implements Runnable {
        private final Writer      os;
        private final InputStream is;
        private final Tracer      log;

        StreamConsumer(InputStream in, Writer out, Tracer log) {
            this.is = in;
            this.os = out;
            this.log = log;
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) {
                    os.write((char) c);
                    os.flush();
                }
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

}
