/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.message.impl.spill;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import jpl.gds.shared.log.Tracer;

/**
 * Utility class supporting conversions of Serializable to/from bytes.
 * @param <T> Type of Object to encode from and decode to.
 */
public class SpillSerializer<T extends Serializable> extends Object {
    /**
     * Only exception thrown by SpillSerializer (on purpose, anyway.).
     */
    public static final class SpillSerializerException extends Exception {
        /**
         * Default Serializable version ID.
         */
        private static final long serialVersionUID = 1L;


        /**
         * Constructor.
         *
         * @param message Message text
         */
        public SpillSerializerException(final String message) {
            super(message);
        }


        /**
         * Constructor.
         *
         * @param message Message text
         * @param cause   Underlying cause
         */
        public SpillSerializerException(final String message,
                final Throwable cause) {
            super(message, cause);
        }
    }

    private static final byte[] _empty = new byte[0];
    private final Class<T> _clss;

    
    /**
     * Constructor for spill serializer used to encode and decode bytes of data.
     * @param clss Class type returned in decoded serialized messages.
     * @param trace Custom Tracer or null for JmsFastTracer.
     * @throws SpillSerializerException Thrown if clss is null.
     */
    public SpillSerializer(final Class<T> clss, final Tracer trace)
            throws SpillSerializerException {
        super();

        if (clss == null) {
            throw new SpillSerializerException("Null class");
        }

        this._clss = clss;
    }

    /**
     * Decodes an array of bytes into a Message.
     * @param bytes Bytes to reconstruct into a Message.
     * @return T Reconstructed Message.
     * @throws SpillSerializerException Thrown if unable to reconstruct the
     *             message.
     */
    public T decode(final byte[] bytes) throws SpillSerializerException {
        if ((bytes == null) || (bytes.length == 0)) {
            return null;
        }

        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(bais);
        } catch (final IOException ioe) {
            throw new SpillSerializerException("Unable to decode", ioe);
        }

        Object result = null;
        boolean exception = false;

        try {
            result = ois.readObject();
        } catch (final ClassNotFoundException cnfe) {
            exception = true;

            throw new SpillSerializerException("Unable to decode", cnfe);
        } catch (final IOException ioe) {
            exception = true;

            throw new SpillSerializerException("Unable to decode", ioe);
        } finally {
            try {
                ois.close();
            } catch (final IOException ioe) {
                if (!exception) {
                    throw new SpillSerializerException("Unable to close", ioe);
                }
            }
        }

        if ((result != null) && !this._clss.isInstance(result)) {
            throw new SpillSerializerException("Unable to decode, got: "
                    + result.getClass().getName());
        }

        return this._clss.cast(result);
    }

    /**
     * Encodes a Message into a string of bytes.
     * @param object Message to encode.
     * @return byte[] String of bytes representing the Message.
     * @throws SpillSerializerException Throw if unable to encode the Message.
     */
    public byte[] encode(final T object) throws SpillSerializerException {
        if (object == null) {
            return _empty;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;

        try {
            oos = new ObjectOutputStream(baos);
        } catch (final IOException ioe) {
            throw new SpillSerializerException("Unable to encode", ioe);
        }

        boolean exception = false;

        try {
            oos.writeObject(object);
        } catch (final IOException ioe) {
            exception = true;

            throw new SpillSerializerException("Unable to encode", ioe);
        } finally {
            try {
                oos.close();
            } catch (final IOException ioe) {
                if (!exception) {
                    throw new SpillSerializerException("Unable to close", ioe);
                }
            }
        }

        return baos.toByteArray();
    }
}
