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
package jpl.gds.db.impl.aggregate.batch.write;

import java.io.BufferedWriter;
import java.io.IOException;

import jpl.gds.db.api.sql.fetch.aggregate.IStringWriter;

/**
 * Batch record writer used to write CSV based batch files
 *
 */
public class CsvBatchRecordWriter implements IStringWriter {
    private BufferedWriter writer;

    /**
     * Constructs a batch record writer using the specified BufferedWriter
     * 
     * @param writer BufferedWriter used to write the batch file
     */
    public CsvBatchRecordWriter(final BufferedWriter writer) {
        this.writer = writer;
    }

    @Override
    public void write(final String record) throws IOException {
        writer.write(record);
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }
}
