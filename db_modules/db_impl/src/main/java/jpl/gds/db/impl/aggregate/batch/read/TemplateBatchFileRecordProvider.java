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
package jpl.gds.db.impl.aggregate.batch.read;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileRecordProvider;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaBatchRecord;

/**
 * This class is used to read temporary batch Velocity Template 
 * based record files 
 *
 *
 */
public class TemplateBatchFileRecordProvider implements IBatchFileRecordProvider {
    
    private final FileInputStream fis;
    
    /**
     * Constructs a Batch File Record Provider 
     * 
     * @param fis The FileInputStream used to read the batch file
     */
    public TemplateBatchFileRecordProvider(final FileInputStream fis) {
        this.fis = fis;
    }
    
    @Override
    public Iterator<String> iterator() {
        return new TemplateRecordIterator(fis);
    }

    @Override
    public void close() throws IOException {
        if (fis == null) {
            throw new IllegalStateException("FileInputStream is undefined");
        }
        fis.close();
    }
    
    private static final class TemplateRecordIterator implements Iterator<String> {
        
        private final FileInputStream fis;
        private Proto3EhaBatchRecord message;
        
        public TemplateRecordIterator(FileInputStream fis) {
            this.fis = fis;
        }

        @Override
        public boolean hasNext() {
            try {
                message = Proto3EhaBatchRecord.parseDelimitedFrom(fis);
                return (message != null);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public String next() {
            if (message != null) {
                return message.getRecord();
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
