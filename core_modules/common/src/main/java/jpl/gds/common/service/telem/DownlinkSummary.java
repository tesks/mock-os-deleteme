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
package jpl.gds.common.service.telem;


/**
 * 
 * DownlinkSummary contains statistics collected for a downlink run.
 * This class serves as the source of information for the downlink processing to display
 * to the console at the end of command line downlink sessions or in the summary window of the downlink GUI.
 *
 *
 */
public class DownlinkSummary extends AbstractTelemetrySummary implements IDownlinkSummary {

    private long inSyncFrames;
    private long outOfSyncData;
    private long outOfSyncCount;
    private long packets;
    private long stationPackets;

    private long ehaPackets;

    private long badFrames;
    private long fillPackets;
    private long idleFrames;
    private long deadFrames;
    private long frameGaps;
    private long frameRegressions;
    private long frameRepeats;
    private long badPackets;
    private long evrPackets;
    private long productPackets;
    private long cfdpPackets;

    private long products;
    private long partialProducts;
    private long productDataBytes;

    @Override
    public long getPartialProducts() {
        return this.partialProducts;
    }

    /**
     * Sets the count of partial products.
     *
     * @param partials
     *            The partial products count to set.
     */
    public void setPartialProducts(final long partials) {
        this.partialProducts = partials;
    }

    @Override
    public long getProductDataBytes() {
        return this.productDataBytes;
    }

    /**
     * Sets the total bytes of product data processed.
     *
     * @param productDataBytes
     *            The number of bytes to set.
     */
    public void setProductDataBytes(final long productDataBytes) {
        this.productDataBytes = productDataBytes;
    }

    @Override
    public long getProducts() {
        return this.products;
    }

    /**
     * Sets the complete product count.
     *
     * @param products
     *            The product count to set.
     */
    public void setProducts(final long products) {
        this.products = products;
    }

    @Override
    public long getEhaPackets() {
        return this.ehaPackets;
    }

    /**
     * Sets the channelized packet count.
     *
     * @param ehaPackets
     *            The packet count to set.
     */
    public void setEhaPackets(final long ehaPackets) {
        this.ehaPackets = ehaPackets;
    }

    @Override
    public long getEvrPackets() {
        return this.evrPackets;
    }

    /**
     * Sets the EVR packet count.
     *
     * @param evrPackets
     *            The packet count to set.
     */
    public void setEvrPackets(final long evrPackets) {
        this.evrPackets = evrPackets;
    }

    @Override
    public long getCfdpPackets() {
        return this.cfdpPackets;
    }

    /**
     * Sets the CFDP packet count.
     *
     * @param cfdpPackets
     *            The packet count to set.
     */
    public void setCfdpPackets(final long cfdpPackets) {
        this.cfdpPackets = cfdpPackets;
    }

    @Override
    public long getInSyncFrames() {
        return this.inSyncFrames;
    }

    /**
     * Sets the count of in-sync frames.
     *
     * @param inSyncFrames
     *            the in-sync frame count to set.
     */
    public void setInSyncFrames(final long inSyncFrames) {
        this.inSyncFrames = inSyncFrames;
    }

    @Override
    public long getOutOfSyncData() {
        return this.outOfSyncData;
    }

    /**
     * Sets the total out of sync byte count.
     *
     * @param outOfSyncData
     *            The byte count to set.
     */
    public void setOutOfSyncData(final long outOfSyncData) {
        this.outOfSyncData = outOfSyncData;
    }

    @Override
    public long getPackets() {
        return this.packets;
    }

    /**
     * Sets the station monitor packet count.
     *
     * @param packets
     *            The station packet count to set.
     * 
     */
    public void setStationPackets(final long packets) {
        this.stationPackets = packets;
    }

    @Override
    public long getStationPackets() {
        return this.stationPackets;
    }

    /**
     * Sets the valid packet count.
     *
     * @param packets
     *            The packet count to set.
     */
    public void setPackets(final long packets) {
        this.packets = packets;
    }

    @Override
    public long getBadFrames() {
        return this.badFrames;
    }

    /**
     * Sets the invalid frame count.
     *
     * @param badFrames
     *            The count to set
     */
    public void setBadFrames(final long badFrames) {
        this.badFrames = badFrames;
    }

    @Override
    public long getBadPackets() {
        return this.badPackets;
    }

    /**
     * Sets the invalid packet count.
     *
     * @param badPackets
     *            The count to set
     */
    public void setBadPackets(final long badPackets) {
        this.badPackets = badPackets;
    }

    @Override
    public long getDeadFrames() {
        return this.deadFrames;
    }

    /**
     * Sets the dead frame count.
     *
     * @param deadFrames
     *            The count to set
     */
    public void setDeadFrames(final long deadFrames) {
        this.deadFrames = deadFrames;
    }

    @Override
    public long getFrameGaps() {
        return this.frameGaps;
    }

    /**
     * Sets the frame sequence gap count.
     *
     * @param frameGaps
     *            The count to set
     */
    public void setFrameGaps(final long frameGaps) {
        this.frameGaps = frameGaps;
    }

    @Override
    public long getFrameRegressions() {
        return this.frameRegressions;
    }

    /**
     * Sets the frame sequence regression count.
     *
     * @param frameRegressions
     *            The count to set
     */
    public void setFrameRegressions(final long frameRegressions) {
        this.frameRegressions = frameRegressions;
    }

    @Override
    public long getFrameRepeats() {
        return this.frameRepeats;
    }

    /**
     * Sets the frame sequence repeat count.
     *
     * @param frameRepeats
     *            The count to set.
     */
    public void setFrameRepeats(final long frameRepeats) {
        this.frameRepeats = frameRepeats;
    }

    @Override
    public long getIdleFrames() {
        return this.idleFrames;
    }

    /**
     * Sets the idle frames count.
     *
     * @param idleFrames
     *            The count to set.
     */
    public void setIdleFrames(final long idleFrames) {
        this.idleFrames = idleFrames;
    }

    @Override
    public long getFillPackets() {
        return this.fillPackets;
    }

    /**
     * Sets the fill packets count.
     *
     * @param fillPackets
     *            The count to set.
     */
    public void setIdlePackets(final long fillPackets) {
        this.fillPackets = fillPackets;
    }

    @Override
    public long getProductPackets() {
        return this.productPackets;
    }

    /**
     * Sets the product packets count.
     *
     * @param productPackets
     *            The count to set.
     */
    public void setProductPackets(final long productPackets) {
        this.productPackets = productPackets;
    }

    @Override
    public long getOutOfSyncCount() {
        return outOfSyncCount;
    }

    /**
     * Sets the out of sync event count.
     * 
     * @param outOfSyncCount
     *            the count to set
     */
    public void setOutOfSyncCount(final long outOfSyncCount) {
        this.outOfSyncCount = outOfSyncCount;
    }

    @Override
    public String toString() {
        return getTelemetrySummary(true);
    }

    @Override
    public String toStringNoBlanks(final String prefix) {
        return getTelemetrySummary(false);
    }

    String getTelemetrySummary(final boolean newline) {

        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        appendSummary(sb, newline).append("In Sync Frames Processed: ");
        sb.append(inSyncFrames);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Out Of Sync Bytes: ");
        sb.append(outOfSyncData);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Out Of Sync Count: ");
        sb.append(outOfSyncCount);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Frame Gaps: ");
        sb.append(frameGaps);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Frame Regressions: ");
        sb.append(frameRegressions);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Frame Repeats: ");
        sb.append(frameRepeats);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Idle Frames: ");
        sb.append(idleFrames);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Dead Frames: ");
        sb.append(deadFrames);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Bad Frames: ");
        sb.append(badFrames);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Valid Packets Processed: ");
        sb.append(packets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("EVR Packets: ");
        sb.append(evrPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("EHA Packets: ");
        sb.append(ehaPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Product Packets: ");
        sb.append(productPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Bad Packets: ");
        sb.append(badPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Fill Packets: ");
        sb.append(fillPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Station Packets: ");
        sb.append(stationPackets);
        appendSeparator(sb, newline);


        appendSummary(sb, newline).append("CFDP Packets: ");
        sb.append(cfdpPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Partial Products Generated: ");
        sb.append(partialProducts);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Complete Products Generated: ");
        sb.append(products);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("Product Data Bytes: ");
        sb.append(productDataBytes);

        return sb.toString();
    }

}
