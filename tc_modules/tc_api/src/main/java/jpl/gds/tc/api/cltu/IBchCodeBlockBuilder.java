package jpl.gds.tc.api.cltu;

import jpl.gds.tc.api.IBchCodeblock;

public interface IBchCodeBlockBuilder {
    /**
     * Return the code block being built.
     *
     * @return MPS BCH code block
     */
    IBchCodeblock build();

    /**
     * Set the fill byte hex string
     *
     * @param fillByteHex fill byte hex string
     * @return builder
     */
    IBchCodeBlockBuilder setFillByteHex(final String fillByteHex);

    /**
     * Set the length of the data portion
     *
     * @param dataLength data length
     * @return builder
     */
    IBchCodeBlockBuilder setDataLength(final int dataLength);

    /**
     * Set the length of the EDAC portion
     *
     * @param edacLength EDAC length
     * @return builder
     */
    IBchCodeBlockBuilder setEdacLength(final int edacLength);

    /**
     * Set the EDAC into the BCH codeblock
     *
     * @param edac BCH EDAC
     * @return builder
     */
    IBchCodeBlockBuilder setEdac(final byte[] edac);

    /**
     * Set the data into the BCH codeblock
     *
     * @param data BCH data
     * @return builder
     */
    IBchCodeBlockBuilder setData(final byte[] data);
}
