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
package jpl.gds.dictionary.api.channel;

import jpl.gds.dictionary.api.eu.IImmutableEUSupport;
import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IImmutableChannelDefinition interface is to be implemented by all channel
 * definition classes. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IImmutableChannelDefinition defines methods needed to interact with Channel Definition
 * objects as required by the IChannelDictionary interface. This is the read-only
 * interface. It is extended by a mutable interface for use in actual dictionary classes.
 *
 *
 * @see IChannelDictionary
 * @see ChannelDefinitionFactory
 */
@CustomerAccessible(immutable = true)
public interface IImmutableChannelDefinition extends IImmutableEUSupport {

    /** 
     * Used to indicate no index, or measurement ID, is defined.
     */
    int NO_INDEX = -1;

    /**
     * Gets the ID for this channel. This is an upper case string that starts
     * with a letter, followed by 0 to 3 letters or digits, followed by a dash
     * (-), followed by 4 digits. Channel ID is required for all channel definitions.
     * <p>
     * The ID attribute of a channel definition corresponds to the
     * "abbreviation"; attribute on the &lt;telemetry&gt; element in the
     * multimission channel dictionary schema.
     * 
     * @return the Channel Id string; may not be null
     */
    String getId();

    /**
     * Gets the description of this channel. There is no length limit on this
     * description and it may contain new lines. Description is optional for
     * channel definitions.
     * 
     * @return the description text, or null if none defined
     * <p>
     * The description attribute of a channel definition corresponds to
     * the &lt;description&gt; element within the &lt;telemetry&gt;
     * element in the multimission channel dictionary schema.
     */
    String getDescription();

    /**
     * Gets the numeric index, or measurement ID, of this channel. The index of
     * a channel is a 16-bit value used to identify the channel in a
     * pre-channelized packet. Measurement ID is only required for channels that
     * may be received in such packets, which means it is not generally defined
     * for (will be NO_INDEX) derived channels.   
     * <p>
     * The index attribute of a channel definition corresponds to the
     * &lt;measurement_id&gt; element within the &lt;telemetry&gt; element in
     * the multimission channel dictionary schema.
     * 
     * @return the channel index; a value of NO_INDEX indicates "undefined"
     */
    int getIndex();

    /**
     * Gets the operational category of the channel. Operational category can be
     * used by the author of the dictionary as a means for categorizing channels.
     * Values are mission-specific. May be null.
     * 
     * The operational category attribute of a channel is specified using
     * the &lt;ops_category&gt; element in the multimission channel dictionary schema.
     * 
     * @return operational category text, or null if none set
     * @deprecated use the ICategorySupport interface method
     * 
     */
    @Deprecated
    String getOpsCategory();

    /**
     * Gets the descriptive title of this channel. There is no length limit, and
     * the character set is all UTF-8 characters, except that the string may not
     * contain new lines. Title is optional, but because it may be used in
     * reports and displays, that value will be the empty string, rather than
     * null, when it is not defined.
     * <p>
     * The title attribute of a channel definition corresponds to the
     * &lt;title&gt; element within the &lt;telemetry&gt; element in the
     * multimission channel dictionary schema.
     * 
     * @return the title text, or the empty string if none defined
     */
    String getTitle();

    /**
     * Gets the software module name associated with this channel. Module is used by
     * the author of the dictionary as a means for categorizing channels. May be null.
     * <p>
     * The module attribute of a channel is specified using the
     * &lt;module&gt; element in the multimission channel dictionary schema.
     * 
     * @return the module name text, or null if none defined for this channel
     * 
     * @deprecated use the ICategorySupport interface method
     */
    @Deprecated
    String getModule();

    /**
     * Gets the name of this channel. Channel name is required on all channel
     * definitions. Must start with a letter, consist of only letters, digits,
     * dashes, and underscores, and is limited to 64 characters in length. This
     * value is used on displays and in reports. It is highly advised that it be
     * kept unique within the first 16 characters for this purpose.
     * <p>
     * The name attribute of a channel definition corresponds to the "name"
     * attribute on the &lt;telemetry&gt; element in the multimission channel
     * dictionary schema.
     * 
     * @return the name text; may not be null
     * 
     */
    String getName();

    /**
     * Gets the group ID of this channel. Group ID is optional on all channel
     * definitions. It allows channels to be placed into groups for displays or other 
     * common purpose.  The group ID can be letters, numbers, dashes, and underscores, 
     * and is limited to 16 characters in length. This value can be used on displays, 
     * databases and in reports. It is highly advised that it be kept unique.
     * <p>
     * The group ID attribute of a channel definition corresponds to the "group_id"
     * attribute on the &lt;telemetry&gt; element in the multimission channel
     * dictionary schema.
     * 
     * @return the group id text; may not be null
     * 
     *
     */
    String getGroupId();

    /**
     * Gets the software or hardware subsystem name associated with this
     * channel. Subsystem is used by the author of the dictionary as a means for
     * categorizing channels. May be null.
     * <p>
     * The subsystem attribute of a channel is specified using the
     * &lt;subsystem&gt; element in the multimission channel dictionary schema.
     * 
     * @return the subsystem text, or null if none defined
     * @deprecated use the ICategorySupport interface method
     */
    @Deprecated
    String getSubsystem();

    /**
     * Gets the data type of this channel. Data type is mandatory for all
     * channel definitions. Will be ChannelType.UNKNOWN if never set.
     * <p>
     * The type attribute of a channel definition corresponds to the "type"
     * attribute on the &lt;telemetry&gt; element in the multimission channel
     * dictionary schema.
     * 
     * @return ChannelType enumeration value; never null; will be
     *         ChannelType.UNKNOWN if undefined
     */
    ChannelType getChannelType();

    /**
     * Gets the definition type of this channel. The definition type of a
     * channel distinguishes basic types of AMPCS channels from one another:
     * flight channels from station monitor channels, for instance.
     * 
     * The definition type attribute of a channel definition partially
     * corresponds to the "source" attribute on the &lt;telemetry&gt; element in
     * the multimission channel dictionary schema, in that a source value of
     * "flight" will correspond to a ChannelDefinitionType.FSW, and a source
     * value of "simulation" will correspond to a ChannelDefintionType.SSE.
     * However, AMPCS also supports other channel definition types such as 
     * ChannelDefintionType.M for monitor and ChannelDefintionType.H for header.
     * 
     * @return ChannelDefinitionType enumeration value; will default to
     *         ChannelDefinitionType.FSW
     */
    ChannelDefinitionType getDefinitionType();

    /**
     * Gets the format specifier for formatting the data number/raw output of
     * this channel. The format specifier should adhere to the C "printf"
     * standard. The format specifier is optional and may be null; however, the
     * AMPCS channel definition factory will attempt to default the value based
     * upon the channel data type.
     * <p>
     * The DN format attribute of a channel definition corresponds to the
     * &lt;format&gt; element within the &lt;telemetry&gt; element in the
     * multimission channel dictionary schema.
     * 
     * @return the format string; may be null
     */
    String getDnFormat();

    /**
     * Gets the units associated with the data number/raw output of this channel.
     * Units are optional. AMPCS will set this value to the empty string if not 
     * specified.
     * <p>
     * The DN units attribute of a channel definition corresponds to the
     * &lt;raw_units&gt; element within the &lt;telemetry&gt; element in the
     * multimission channel dictionary schema.
     * 
     * @return the unit string, or the empty string if none specified
     */
    String getDnUnits();

    /**
     * Gets the size of the channel value, in bits. Supported sizes are 8, 16,
     * 24, 32, and 64, and all positive multiples of 8 for string channels. For
     * string channels, this number must be divided by 8 to get the number of
     * characters in the channel.
     * 
     * <p>
     * The bit size attribute of a channel definition corresponds to the
     * &lt;byte_length&gt; element within the &lt;telemetry&gt; element in the
     * multimission channel dictionary schema, where that value is multiplied by
     * 8 to get this bit size.
     * 
     * @return the channel data size in bits
     */
    int getSize();

    /**
     * Gets the flag indicating whether this channel is derived from other
     * channels.
     * <p>
     * The isDerived flag on a channel definition will be set if a
     * &lt;source_derivation_id&gt; or &lt;bit_extract&gt; element is defined
     * within the &lt;telemetry&gt; element in the multimission channel
     * dictionary schema,
     * 
     * @return true if the channel is derived, false if it is an original
     *         channel
     */
    boolean isDerived();

    /**
     * Gets the DerivationType of this channel. The derivation type is defined
     * only if the channel is derived, and indicates what type of derivation
     * produces it.
     * 
     * @return the DerivationType of the channel; will be DerivationType.NONE if
     *         the channel is not derived
     */
    DerivationType getDerivationType();

    /**
     * Gets the ID of the algorithmic derivation that produces this channel.
     * Will be null if the channel is not derived. May be defined for bit
     * extracted derived channels, but the value is not predictable or reliable
     * for other than algorithmically-derived channels.
     * 
     * <p>
     * The source derivation ID attribute of a channel definition corresponds to
     * the &lt;source_derivation_id&gt; element within the &lt;telemetry&gt;
     * element in the multimission channel dictionary schema.
     * 
     * @return the source derivation ID; may be null
     */
    String getSourceDerivationId();

}