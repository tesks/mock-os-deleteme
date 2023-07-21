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
package ammos.datagen.channel.generators;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ammos.datagen.channel.config.CustomPacket;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.TruthFile;
import ammos.datagen.util.UnsignedUtil;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.types.Pair;

/**
 * This is the data generator for channel packet bodies when configured to
 * generate custom packets using a defined list of channels and DNs per APID. It
 * creates the portion of an pre-channelized packet from the end of the
 * secondary header onwards. It must be supplied a seed object that tells it
 * which channels and APIDs to generate.
 * 
 *
 */
public class CustomChannelBodyGenerator extends AbstractChannelBodyGenerator
        implements ISeededGenerator {

    private List<CustomPacket> customPackets = null;
    private Iterator<CustomPacket> currentPacket;

    /**
     * Basic constructor.
     */
    public CustomChannelBodyGenerator() {

        super();
    }

    /**
     * Constructor that sets the truth file writer
     * 
     * @param truthFile
     *            TruthFile object for writing truth data to
     */
    public CustomChannelBodyGenerator(final TruthFile truthFile) {

        super(truthFile);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
     */
    @Override
    public void setSeedData(final ISeedData seed)
            throws InvalidSeedDataException {

        super.setSeedData(seed);
        this.customPackets = this.seedData.getCustomPackets();
        this.currentPacket = this.customPackets.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#reset()
     */
    @Override
    public void reset() {

        super.reset();
        this.customPackets = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The actual return type of this method is Pair<Integer, byte[]>, where the
     * integer is the packet APID and the byte[] is the channel packet body.
     * This method will return null when the configured list of custom packets
     * is exhausted.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getNext()
     */
    @Override
    public Object getNext() {

        if (this.seedData == null || this.chanDefs.isEmpty()) {
            throw new IllegalStateException(
                    "channel body generator is not seeded or contains no channel definitions");
        }

        /*
         * If we have exhausted the list of custom packets there is nothing else
         * to generate. Return a null.
         */
        if (!this.currentPacket.hasNext()) {
            return null;
        }

        /*
         * Otherwise, advance to the next custom packet definition and generate
         * the channel body for it.
         */
        final CustomPacket packet = this.currentPacket.next();

        final int apid = getNextApid();
        return new Pair<Integer, byte[]>(apid, getChannelBody(
                getDefsForNextPacket(packet), packet));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is not implemented in this class and will throw.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getRandom()
     */
    @Override
    public Object getRandom() {

        throw new UnsupportedOperationException(
                "Random channel body generation is not supported in CUSTOM mode");
    }

    /**
     * Selects the channel definitions required to generate the next custom
     * packet.
     * 
     * @return List of selected IChannelDefinition objects
     */
    private List<IChannelDefinition> getDefsForNextPacket(
            final CustomPacket thePacket) {

        /*
         * Get the list of desired channels for the current packet.
         */
        final List<String> desiredChans = thePacket.getChannelIds();

        /*
         * Loop through the desired channels and add all their channel
         * definitions to the result list.
         */
        final List<IChannelDefinition> selectedChans = new LinkedList<IChannelDefinition>();
        for (final String desiredChan : desiredChans) {
            selectedChans.add(this.chanMap.get(desiredChan));
        }
        return selectedChans;
    }

    /**
     * Generates a binary body in pre-channelized packet form for the given list
     * of channel definitions. If configured to do so, randomly inserts invalid
     * channel indices. Updates UsageTracker objects to reflect which samples
     * (valid and invalid) have been generated.
     * 
     * @param chanDefs
     *            the list of channel definition objects
     * @return array of bytes containing the channel data
     */
    private byte[] getChannelBody(final List<IChannelDefinition> chanDefs,
            final CustomPacket thePacket) {

        /*
         * This list will hold many byte arrays, each containing the index/DN
         * pair for one channel sample.
         */
        final List<byte[]> sampleChunks = new LinkedList<byte[]>();
        final List<Pair<String, String>> dnValues = thePacket.getDnValues();
        final Iterator<Pair<String, String>> valueIterator = dnValues
                .iterator();

        /*
         * Loop through the incoming list of channel definitions.
         */
        for (final IChannelDefinition def : chanDefs) {
            final byte[] sample = getSample(def, valueIterator.next().getTwo());

            this.stats.incrementTotalForChannelId(def.getId());

            /*
             * Either way, the sample byte array goes into the sample chunk
             * list.
             */
            sampleChunks.add(sample);
        }

        /*
         * Now compute the size we need for the whole channel body by adding up
         * the size of the samples.
         */
        int neededSize = 0;
        for (final byte[] sample : sampleChunks) {
            neededSize += sample.length;
        }

        /*
         * Allocate a byte array for the whole thing and copy all the sample
         * chunks into it, one after the other.
         */
        final byte[] chanBody = new byte[neededSize];
        int offset = 0;

        for (final byte[] sample : sampleChunks) {
            System.arraycopy(sample, 0, chanBody, offset, sample.length);
            offset += sample.length;
        }

        /*
         * Update statistics for number of channels per packet.
         */
        this.stats.updateChannelPacketStatistics(sampleChunks.size());

        return chanBody;
    }

    /**
     * Gets the bytes representing one valid channel sample (channel index and
     * DN value) for the channel with the given definition. Also writes the
     * sample to the truth file, unless the invalidIndexAdded argument is true.
     * 
     * @param def
     *            channel definition from the dictionary
     * @param invalidIndexAdded
     *            true if an invalid channel index has already been added to the
     *            channel body for this packet, false if not
     * @return byte array containing channel index and DN value
     */
    @SuppressWarnings({ "fallthrough", "PMD.SwitchDensity" })
    private byte[] getSample(final IChannelDefinition def, String dnValue) {

        /*
         * This array is bigger than we will ever need for a channel sample.
         * Copy the 16-bit channel index into it as the first thing.
         */
        final byte[] bytesForSample = new byte[1024];
        int off = GDR.set_u16(bytesForSample, 0, def.getIndex());

        try {
            /*
             * DN Is generated based upon the channel data type.
             */
            /* MPCS-6115 - 5/24/14. Added TIME case below. */
            switch (def.getChannelType()) {
            case ASCII:
                /*
                 * String channel. Use the dnValue as is and set it into the
                 * sample byte array.
                 */
                final int maxLen = def.getSize() / 8;
                final int vlen = dnValue.length();
                off += GDR.set_string_no_pad(bytesForSample, off, dnValue);
                /*
                 * String channel byte length must match dictionary length, so
                 * we pad the sample array with zeros out to that length.
                 */
                for (int i = 0; i < maxLen - vlen; i++) {
                    GDR.set_u8(bytesForSample, off++, 0);
                }
                break;

            case BOOLEAN:
                /*
                 * Boolean channel. Convert the DN string to an integer and set
                 * it into the sample byte array. Note that booleans can be a
                 * variety of sizes, so we have to check and act appropriately.
                 */
                final boolean fromDnStr = GDR.parse_boolean(dnValue);
                final Short boolVal = Short.valueOf(fromDnStr ? (short) 1
                        : (short) 0);
                dnValue = boolVal.toString();
                switch (def.getSize()) {
                case 64:
                    GDR.set_u64(bytesForSample, off, boolVal);
                    off += 8;
                    break;
                case 32:
                    GDR.set_u32(bytesForSample, off, boolVal);
                    off += 4;
                    break;
                case 16:
                    GDR.set_u16(bytesForSample, off, boolVal);
                    off += 2;
                    break;
                case 8:
                    GDR.set_u8(bytesForSample, off, boolVal);
                    off += 1;
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported boolean channel size for channel "
                                    + def.getId());
                }
                break;
            case DIGITAL:
            case UNSIGNED_INT:
            case TIME:
                /*
                 * Unsigned channel. Convert the DN string to an unsigned value
                 * of the appropriate size. Set the value into the sample byte
                 * array.
                 */
                switch (def.getSize()) {
                case 64:
                    final BigInteger u64ArgVal = new BigInteger(dnValue);
                    GDR.set_u64(bytesForSample, off, u64ArgVal.longValue());
                    off += 8;
                    dnValue = u64ArgVal.toString();
                    break;
                case 32:
                    final Long u32ArgVal = GDR.parse_long(dnValue);
                    GDR.set_u32(bytesForSample, off, u32ArgVal.intValue());
                    off += 4;
                    dnValue = UnsignedUtil.formatAsUnsigned(u32ArgVal);
                    break;
                case 24:
                    final Integer u24ArgVal = GDR.parse_int(dnValue);
                    GDR.set_u24(bytesForSample, off, u24ArgVal.intValue());
                    off += 3;
                    dnValue = UnsignedUtil.formatAsUnsigned(u24ArgVal);
                    break;
                case 16:
                    final Integer u16ArgVal = GDR.parse_int(dnValue);
                    GDR.set_u16(bytesForSample, off, u16ArgVal.shortValue());
                    off += 2;
                    dnValue = UnsignedUtil.formatAsUnsigned(u16ArgVal);
                    break;
                case 8:
                    final Short u8ArgVal = GDR.parse_short(dnValue);
                    GDR.set_u8(bytesForSample, off, u8ArgVal.byteValue());
                    off += 1;
                    dnValue = UnsignedUtil.formatAsUnsigned(u8ArgVal);
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported unsigned channel size for channel "
                                    + def.getId());
                }
                break;
            case FLOAT:
                /*
                 * MPCS-6115 - 5/23/14. Removed check for DOUBLE type
                 * above.
                 */
                /*
                 * Float channel. Get a float value of the appropriate size from
                 * the DN string. Set the value into the sample byte array.
                 */
                if (def.getSize() == 64) {
                    final Double f64ArgVal = Double.valueOf(dnValue);
                    GDR.set_double(bytesForSample, off, f64ArgVal);
                    dnValue = String.valueOf(GDR
                            .get_double(bytesForSample, off));
                    off += 8;
                } else {
                    final Float f32ArgVal = Float.valueOf(dnValue);
                    GDR.set_float(bytesForSample, off, f32ArgVal);
                    dnValue = String.valueOf((double) GDR.get_float(
                            bytesForSample, off));
                    off += 4;
                }
                break;
            case SIGNED_INT:
                /*
                 * Unsigned channel. Convert the DN string to an integer value
                 * of the appropriate size. Set the value into the sample byte
                 * array.
                 */
                switch (def.getSize()) {
                case 64:
                    final Long i64ArgVal = GDR.parse_long(dnValue);
                    GDR.set_i64(bytesForSample, off, i64ArgVal);
                    off += 8;
                    dnValue = i64ArgVal.toString();
                    break;
                case 32:
                    final Integer i32ArgVal = GDR.parse_int(dnValue);
                    GDR.set_i32(bytesForSample, off, i32ArgVal);
                    off += 4;
                    dnValue = i32ArgVal.toString();
                    break;
                case 16:
                    final Short i16ArgVal = GDR.parse_short(dnValue);
                    GDR.set_i16(bytesForSample, off, i16ArgVal);
                    off += 2;
                    dnValue = i16ArgVal.toString();
                    break;
                case 8:
                    final Byte i8ArgVal = GDR.parse_byte(dnValue);
                    GDR.set_i8(bytesForSample, off, i8ArgVal);
                    off += 1;
                    dnValue = i8ArgVal.toString();
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported integer channel size for channel "
                                    + def.getId());
                }
                break;
            case STATUS:
                /*
                 * Enum channel. Configured DN value may be numeric or symbolic.
                 * Attempt to interpret as a numeric first. If that fails,
                 * attempt to map the string value to numeric value using the
                 * enumeration table for the current channel.
                 */
                long which = -1;
                try {
                    which = GDR.parse_long(dnValue);
                } catch (final NumberFormatException e) {
                    which = def.getLookupTable().getKey(dnValue);
                    if (which == -1) {
                            TraceManager.getDefaultTracer()
                                    .error("Symbol: " + dnValue + " not found as enumeration type value -1 returned.");
                        throw new IllegalArgumentException(
                                "Configured DN Value "
                                        + dnValue
                                        + " for enum channel "
                                        + def.getId()
                                        + " cannot be mapped to a numeric value in the channel's enumeration table");
                    }
                }
                dnValue = String.valueOf(which);
                /*
                 * Now write the value into the sample byte array, taking care
                 * to write the proper number of bytes for the current channel.
                 */
                switch (def.getSize()) {
                case 64:
                    GDR.set_i64(bytesForSample, off, which);
                    off += 8;
                    break;
                case 32:
                    GDR.set_i32(bytesForSample, off, (int) which);
                    off += 4;
                    break;
                case 16:
                    GDR.set_i16(bytesForSample, off, (short) which);
                    off += 2;
                    break;
                case 8:
                    GDR.set_i8(bytesForSample, off, (byte) which);
                    off += 1;
                    break;
                default:
                    throw new IllegalStateException(
                            "Unsupported enum channel size for channel "
                                    + def.getId());
                }
                break;
            default:
                throw new IllegalStateException(
                        "Unknown channel type for channel " + def.getId());
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Configured DN Value "
                            + dnValue
                            + " for channel "
                            + def.getId()
                            + " cannot be converted to the proper data type for the channel");
        }

        /*
         * Write the sample to the truth file.
         */
        writeSampleToTruth(def.getId(), def.getName(), dnValue);

        final byte[] realBytes = new byte[off];
        System.arraycopy(bytesForSample, 0, realBytes, 0, off);
        return realBytes;
    }
}
