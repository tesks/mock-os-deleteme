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

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.LineIterator;

import jpl.gds.db.api.sql.fetch.aggregate.ComparableIndexItem;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileIndexProvider;

/**
 * This class is used to read temporary batch index files 
 * with the use of a LineIterator
 *
 */
public class BatchFileIndexReader implements IBatchFileIndexProvider {
    
    private final LineIterator lineIterator;
    private final String batchId;
    
    /**
     * Constructs a Batch File Index Provider
     * 
     * @param lineIterator The LineIterator used to read the index file
     * @param batchId The batch ID
     */
    public BatchFileIndexReader(final LineIterator lineIterator, final String batchId) {
        this.lineIterator = lineIterator;
        this.batchId = batchId;
    }
    
    @Override
    public Iterator<ComparableIndexItem<String>> iterator() {
        return new IndexIterator(lineIterator, batchId);
    }

    @Override
    public void close() throws IOException {
        if (lineIterator == null) {
            throw new IllegalStateException("LineIterator is undefined");
        }
        lineIterator.close();
    }
    
    private static final class IndexIterator implements Iterator<ComparableIndexItem<String>> {

        private final LineIterator lineIterator;
        private final String batchId;

        public IndexIterator(final LineIterator lineIterator, String batchId) {
            this.lineIterator = lineIterator;
            this.batchId = batchId;
        }

        @Override
        public boolean hasNext() {
            return lineIterator.hasNext();
        }

        @Override
        public ComparableIndexItem<String> next() {
            return new ComparableIndexItem<String>(batchId, 0, (String) lineIterator.next());
        }  
    }
}
