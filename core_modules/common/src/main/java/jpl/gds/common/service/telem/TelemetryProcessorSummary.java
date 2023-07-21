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
 * TelemetryProcessorSummary contains statistics collected for a Telemetry Processor run.
 * This class serves as the source of information to the console at the end of command line TP sessions.
 *
 *
 */
public class TelemetryProcessorSummary extends AbstractTelemetrySummary implements ITelemetryProcessorSummary {

    private long products;
    private long partialProducts;
    private long productDataBytes;

    private long evrPackets;
    private long ehaPackets;

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

        appendSummary(sb, newline).append("EVR Packets: ");
        sb.append(evrPackets);
        appendSeparator(sb, newline);

        appendSummary(sb, newline).append("EHA Packets: ");
        sb.append(ehaPackets);
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
