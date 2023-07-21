package jpl.gds.db.impl.aggregate.batch.read;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.LineIterator;

import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileRecordProvider;

/**
 * This class is used to read temporary batch CSV record files 
 * with the use of a LineIterator
 *
 */
public class BatchFileRecordReader implements IBatchFileRecordProvider {
    private final LineIterator lineIterator;
    
    /**
     * Constructs a Batch File Record Provider with the passed in LineIterator
     * 
     * @param lineIterator The LineIterator used to read the record file 
     */
    public BatchFileRecordReader(final LineIterator lineIterator) {
        this.lineIterator = lineIterator;
    }
    
    @Override
    public Iterator<String> iterator() {
        return new RecordIterator(lineIterator);
    }

    @Override
    public void close() throws IOException {
        if (lineIterator == null) {
            throw new IllegalStateException("LineIterator is undefined");
        }
        lineIterator.close();
    }

    private static final class RecordIterator implements Iterator<String> {
        
        private final LineIterator lineIterator;
        private final String NL = System.lineSeparator();
        
        public RecordIterator(LineIterator lineIterator) {
            this.lineIterator = lineIterator;
        }

        @Override
        public boolean hasNext() {
            return lineIterator.hasNext();
        }

        @Override
        public String next() {
            return (String) lineIterator.next() + NL;
        }
        
    }
}
