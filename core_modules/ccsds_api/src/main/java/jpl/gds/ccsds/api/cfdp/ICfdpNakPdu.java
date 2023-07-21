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
package jpl.gds.ccsds.api.cfdp;

import java.util.List;

/**
 * {@code ICfdpNakPdu} represents a CFDP NAK PDU.
 * 
 */
public interface ICfdpNakPdu extends ICfdpFileDirectivePdu {

    /**
     * Retrieve the start of scope
     *
     * @return start of scope offset
     */
    public long getStartOfScope();

    /**
     * Retrieve the end of scope
     *
     * @return end of scope offset
     */
    public long getEndOfScope();

    public List<SegmentRequest> getSegmentRequests();

    public static class SegmentRequest {
        private long startOffset;
        private long endOffset;

        /**
         * Constructor that sets both start and end offsets of the new {@code SegmentRequest}.
         *
         * @param startOffset start offset to set
         * @param endOffset end offset to set
         */
        public SegmentRequest(final long startOffset, final long endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        /**
         * Get segment request's start offset
         *
         * @return start offset
         */
        public long getStartOffset() {
            return startOffset;
        }

        /**
         * Set segment request's start offset
         *
         * @param startOffset start offset to set
         */
        public void setStartOffset(final long startOffset) {
            this.startOffset = startOffset;
        }

        /**
         * Get segment request's end offset
         *
         * @return end offset
         */
        public long getEndOffset() {
            return endOffset;
        }

        /**
         * Set segment request's end offset
         *
         * @param endOffset end offset to set
         */
        public void setEndOffset(final long endOffset) {
            this.endOffset = endOffset;
        }

        /**
         * Returns a string representation of the object. In general, the
         * {@code toString} method returns a string that
         * "textually represents" this object. The result should
         * be a concise but informative representation that is easy for a
         * person to read.
         * It is recommended that all subclasses override this method.
         * <p>
         * The {@code toString} method for class {@code Object}
         * returns a string consisting of the name of the class of which the
         * object is an instance, the at-sign character `{@code @}', and
         * the unsigned hexadecimal representation of the hash code of the
         * object. In other words, this method returns a string equal to the
         * value of:
         * <blockquote>
         * <pre>
         * getClass().getName() + '@' + Integer.toHexString(hashCode())
         * </pre></blockquote>
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return startOffset + "-" + endOffset;
        }

    }

}