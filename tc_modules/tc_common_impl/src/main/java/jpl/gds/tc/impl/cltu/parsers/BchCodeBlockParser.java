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

import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.config.CltuProperties;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes a byte array and converts it into one or more BCH code blocks
 *
 */
public class BchCodeBlockParser {

    private final int dataLength;
    private final int edacLength;
    private final int bchLength;

    /**
     * Constructor that takes the ApplicationContext
     * @param appContext the current application context
     */
    public BchCodeBlockParser(final ApplicationContext appContext) {
        this(appContext.getBean(CltuProperties.class));
    }

    /**
     * Constructor that takes CltuProperties
     * @param cltuProperties the current application's CltuProperties
     */
    public BchCodeBlockParser(final CltuProperties cltuProperties) {
        this.dataLength = cltuProperties.getCodeBlockDataByteLength();
        this.edacLength = (cltuProperties.getCodeblockEdacBitLength() + cltuProperties.getCodeblockFillBitLength())/8;
        this.bchLength = cltuProperties.getCodeblockByteLength();
    }

    /**
     * Parse a single IBchCodeBlock from a byte array
     * <br> If more bytes are supplied than necessary, the IBchCodeBlock is parsed from the first bytes needed and the rest are ignored.
     * @param bchBytes the byte array containing an IBchCodeBlock
     * @return the IBchCodeblock that was parsed
     * @throws  IllegalArgumentException if not enough bytes were supplied to create an IBchCodeblock
     */
    public IBchCodeblock parse(byte[] bchBytes) {
        if(bchBytes.length < bchLength) {
            throw new IllegalArgumentException("A single BCH is " + bchLength + " bytes long, but only " + bchBytes.length + " have been supplied");
        }
        byte[] data = ArrayUtils.subarray(bchBytes, 0, dataLength);
        byte[] edac = ArrayUtils.subarray(bchBytes, dataLength, dataLength + edacLength);
        return new BchCodeBlockBuilder().setDataLength(dataLength).setData(data)
                                           .setEdacLength(edacLength).setEdac(edac)
                .build();
    }

    /**
     * Parses as many IBchCodeBlocks as possible from a byte array
     * @param bchBytes the byte array containing at least one IBchCodeBlock
     * @return a List of the IBchCodeBlocks that were parsed from the byte array
     * @throws IllegalArgumentException if not enough bytes were supplied to create a single IBchCodeblock
     */
    public List<IBchCodeblock> parseList(byte[] bchBytes) {
        List<IBchCodeblock> bchBlocks = new ArrayList<>();

        for(int i = 0 ; i < bchBytes.length ; i += bchLength) {
            try {
                bchBlocks.add(parse(ArrayUtils.subarray(bchBytes, i, i + bchLength)));
            } catch (IllegalArgumentException e) {
                if(i + bchLength < bchBytes.length) {
                    throw e;
                } // else we just have some trailing bytes, ignore the error and return the list
            }
        }

        return bchBlocks;
    }
}
