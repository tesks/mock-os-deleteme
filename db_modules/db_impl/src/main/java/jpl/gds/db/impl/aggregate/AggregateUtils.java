package jpl.gds.db.impl.aggregate;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;

public class AggregateUtils {
    
    public static final int BUFFER_SIZE = 10000;
    
    /**
     * Compress byte array.
     *
     * @param data Input byte array
     *
     * @return Compressed byte array
     */
    public static byte[] compress(final byte[] data) {
        
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        final byte[] buffer = new byte[BUFFER_SIZE];
        
        // Add a few bytes in case it gets BIGGER
        final ByteArrayOutputStream outputStream =
            new ByteArrayOutputStream(data.length + 100);

        deflater.reset();

        deflater.setInput(data);

        deflater.finish();

        while (! deflater.finished())
        {
            outputStream.write(buffer, 0, deflater.deflate(buffer));
        }

        return outputStream.toByteArray();
    }
    
    public static byte[] decompress(final byte[] byteArray) throws DataFormatException {
        final Inflater inflater = new Inflater();
        final byte[] buffer = new byte[BUFFER_SIZE];
        inflater.reset();
        inflater.setInput(byteArray);
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while (!inflater.finished()) {
            baos.write(buffer, 0, inflater.inflate(buffer));
        }
        
        return baos.toByteArray();
    }
    
    public static Proto3EhaAggregatedGroup deserializeAggregate(final byte[] byteArray) throws InvalidProtocolBufferException {
        return Proto3EhaAggregatedGroup.parseFrom(byteArray);
    }
    
}
