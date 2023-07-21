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

package jpl.gds.shared.process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The FileLineHandler class writes handled lines to a file.
 * 
 */
public class FileLineHandler implements LineHandler {

    private final FileWriter outputWriter;
    private final String filepath;

    /**
     * Constructor which creates a new file writer for the handler.
     * 
     * @param filepath
     *            path to the file
     * @throws IOException
     *             thrown if unable to create a file writer to the file
     */
    public FileLineHandler(File filepath) throws IOException {
        this.filepath = filepath.getAbsolutePath();
        this.outputWriter = new FileWriter(filepath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.process.LineHandler#handleLine(java.lang.String)
     */
    public void handleLine(String line) throws IOException {
        this.outputWriter.write(line + "\n");
        this.outputWriter.flush();
    }

    /**
     * Closes the output writer.
     */
    public void shutdown() {
        if (this.outputWriter != null) {
            try {
                this.outputWriter.close();
            } catch (IOException e) {
                // nothing we can do now. if it ain't closed, it ain't closed
            }
        }
    }

    /**
     * Returns the file path.
     * 
     * @return file path
     */
    public String getFilePath() {
        return (this.filepath);
    }
}
