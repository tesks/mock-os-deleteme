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
package jpl.gds.tc.impl.cltu.parsers;

import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.BchCodeblock;

/**
 * The BchCodeBlockBuilder helps ensure an IBchCodeBlock is constructed appropriately
 *
 */
public class BchCodeBlockBuilder implements IBchCodeBlockBuilder {
    private static int     defaultDataByteLength = 7;
    private static int     defaultEdacByteLength = 1;
    private        byte[]  data;
    private        String  fillByteHex;
    private        int     dataLength;
    private        int     edacLength;
    private        byte[]  edac;
    private        boolean fillByteHexSet;
    private        boolean dataLengthSet;
    private        boolean edacLengthSet;
    private        boolean edacSet;
    private        boolean dataSet;

    /**
     * Return the code block being built.
     *
     * @return MPS BCH code block
     */
    public IBchCodeblock build() {
        checkPreconditions();

        final BchCodeblock bchCodeblock = new BchCodeblock();

        if (dataLengthSet && data.length < dataLength) {
            data = BinOctHexUtility
                    .createPaddedArray(data, dataLength,
                            BinOctHexUtility.toByteFromHex(fillByteHex.charAt(0), fillByteHex.charAt(1)));
        }

        bchCodeblock.setData(data);
        bchCodeblock.setEdac(edac);

        return bchCodeblock;
    }

    private void checkPreconditions() {
        if (!dataSet || !edacSet) {
            throw new IllegalStateException("Please set data and EDAC.");
        }

        if (!dataLengthSet) {
            setDataLength(defaultDataByteLength);
        }

        if (data.length > dataLength) {
            throw new IllegalStateException(
                    "The data length has been set to " + dataLength + ", but the provided data is " + data.length + " bytes long.");
        }

        if (data.length < dataLength && !fillByteHexSet) {
            throw new IllegalStateException(
                    "The data length has been set and the provided data is less than the length, but the fill byte hex has not been set.");
        }

        if (!edacLengthSet) {
            setEdacLength(defaultEdacByteLength);
        }

        if (edac.length != edacLength) {
            throw new IllegalStateException(
                    "The EDAC length has been set to " + edacLength + ", but the provided EDAC is " + edac.length + " bytes long.");
        }

    }

    /**
     * Set the fill byte hex string
     *
     * @param fillByteHex fill byte hex string
     * @return builder
     */
    public IBchCodeBlockBuilder setFillByteHex(final String fillByteHex) {

        if (fillByteHex == null) {
            throw new IllegalArgumentException("If provided, fill byte hex must not be null.");
        }

        String tmpFillByteHex = fillByteHex;

        if (BinOctHexUtility.hasHexPrefix(tmpFillByteHex)) {
            tmpFillByteHex = BinOctHexUtility.stripHexPrefix(tmpFillByteHex);
        }

        if (tmpFillByteHex.length() != 2) {
            throw new IllegalArgumentException("Please provide 2 hex digits for the fill byte.");
        }


        if (!BinOctHexUtility.isValidHex(tmpFillByteHex)) {
            throw new IllegalArgumentException("Input hex string does not consist of valid hex letters.");
        }

        this.fillByteHex = tmpFillByteHex;
        this.fillByteHexSet = true;

        return this;
    }

    /**
     * Set the length of the data portion
     *
     * @param dataLength data length
     * @return builder
     */
    public IBchCodeBlockBuilder setDataLength(final int dataLength) {
        if(dataSet && data.length > dataLength) {
            throw new IllegalArgumentException(
                    "The length of the currently stored data is " + data.length + ", but provided data length is " + dataLength + ".");
        }
        this.dataLength = dataLength;
        this.dataLengthSet = true;
        return this;
    }

    /**
     * Set the length of the EDAC portion
     *
     * @param edacLength EDAC length
     * @return builder
     */
    public IBchCodeBlockBuilder setEdacLength(final int edacLength) {
        this.edacLength = edacLength;
        this.edacLengthSet = true;
        return this;
    }

    /**
     * Set the EDAC into the BCH codeblock
     *
     * @param edac BCH EDAC
     * @return builder
     */
    public IBchCodeBlockBuilder setEdac(final byte[] edac) {
        if (edac == null) {
            throw new IllegalArgumentException("EDAC must not be null.");
        }
        if (edacLengthSet && edac.length != edacLength) {
            throw new IllegalArgumentException(
                    "EDAC length has been set to " + edacLength + ", but provided edac is " + edac.length + ".");
        }
        this.edac = edac;
        this.edacSet = true;
        return this;
    }

    /**
     * Set the data into the BCH codeblock
     *
     * @param data BCH data
     * @return builder
     */
    public IBchCodeBlockBuilder setData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }
        if (dataLengthSet && data.length > dataLength) {
            throw new IllegalArgumentException(
                    "Data length has been set to " + dataLength + ", but provided data is " + data.length + ".");
        }
        this.data = data;
        this.dataSet = true;
        return this;
    }
}
