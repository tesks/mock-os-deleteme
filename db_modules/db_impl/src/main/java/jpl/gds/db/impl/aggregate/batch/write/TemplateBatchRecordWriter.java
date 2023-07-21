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

import java.io.FileOutputStream;
import java.io.IOException;

import jpl.gds.db.api.sql.fetch.aggregate.IStringWriter;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaBatchRecord;

/**
 * This class is used to write Velocity template record batch files
 *
 */
public class TemplateBatchRecordWriter implements IStringWriter {
	
	private Proto3EhaBatchRecord.Builder batchRecordBuilder;
	private Proto3EhaBatchRecord message;
	private FileOutputStream fos;
	
	/**
	 * Constructs a Template batch record writer.
	 * 
	 * @param fos The FileOutputStream used to write the batch file
	 */
	public TemplateBatchRecordWriter(final FileOutputStream fos) {
		this.fos = fos;
		this.batchRecordBuilder = Proto3EhaBatchRecord.newBuilder();
	}
	
	@Override
	public void write(final String record) throws IOException {
		message = batchRecordBuilder.setRecord(record).build();
		message.writeDelimitedTo(fos);
	}

	@Override
	public void close() throws IOException {
		fos.flush();
		fos.close();
	}
}
