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
package ammos.datagen.dictionary.app;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.cmdline.DatagenOptions;
import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.DictionaryClassContainer.ClassType;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.IAlarmDefinition;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmDictionaryFactory;
import jpl.gds.dictionary.api.alarm.IAlarmOffControl;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.alarm.ICombinationGroupMember;
import jpl.gds.dictionary.api.alarm.ICombinationSource;
import jpl.gds.dictionary.api.alarm.ICombinationTarget;
import jpl.gds.dictionary.api.alarm.ICompoundAlarmDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * This is an application that reads an alarm dictionary for the current AMPCS
 * mission and writes an alarm dictionary in the multimission XML format. This
 * converter app differs from other dictionary converters, in that it requires
 * the input of an already-converted channel XML in multi-mission format. This
 * channel XML is identified with the -C option.
 * 
 *
 * MPCS-6524 - 1/28/15. Added class.
 * MPCS-6996 - 4/13/15 Added alarm description and categories.
 * MPCS-7279 - 8/3/15 Added key/value attributes.
 * MPCS-7750 - 10/23/15. Changed to use new command line option
 *          strategy throughput.
 */
public class AlarmDictionaryConverterApp extends AbstractDictionaryConverterApp
        implements StaxSerializable {

    /**
     * Short option name for the channel dictionary command line option.
     */
    public static final String CHAN_DICT_SHORT_OPT = "C";
    /**
     * Long option name for the channel dictionary command line option.
     */
    public static final String CHAN_DICT_LONG_OPT = "channelDictionary";

    private IAlarmDictionary missionDictionary;
    private FileOption channelOpt;
    private String channelDictPath;
    private Map<String, IChannelDefinition> chanMap;
    

    /**
     * {@inheritDoc}
     * @see ammos.datagen.dictionary.app.AbstractDictionaryConverterApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        final DatagenOptions options = super.createOptions();
        this.channelOpt = new FileOption(CHAN_DICT_SHORT_OPT,
                CHAN_DICT_LONG_OPT, "file path",
                "path to multi-mission-formatted channel dictionary file",
                true, true);
        options.addOption(this.channelOpt);
        return options;

    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.dictionary.app.AbstractDictionaryConverterApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        this.channelDictPath = this.channelOpt.parse(commandLine, true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.dictionary.app.AbstractDictionaryConverterApp#validateSourceSchema(java.lang.String)
     */
    @Override
    protected void validateSourceSchema(final String schemaName)
            throws ParseException {
        super.validateSourceSchema(schemaName);
        if (schemaName.equalsIgnoreCase("monitor")) {
            throw new ParseException("There is no monitor Alarm schema");
        }
    }

    /**
     * Parses both the multimission channel dictionary and the mission-specific
     * alarm dictionary.
     * 
     * @throws DictionaryException
     *             if there is a problem reading the dictionary
     */
    public void readMissionDictionary() throws DictionaryException {

        /*
         * Read the multi-mission channel dictionary to set up required data
         * structures. We use a separate DictionaryConfiguration object to force
         * the parser to be the MM parser.
         */
        final DictionaryProperties localDictConfig = new DictionaryProperties(
                true);
        localDictConfig.setDictionaryClass(DictionaryType.CHANNEL,
                new DictionaryClassContainer(ClassType.MM_CHANNEL));
        final IChannelDictionary channelDictionary = appContext.getBean(IChannelDictionaryFactory.class)
                .getNewInstance(localDictConfig, this.channelDictPath);

        /*
         * Get the mapping of String to IChannelDefinition and pass it as a
         * second argument to the factory method. In this case, use the
         * dictionary configuration for the mission as established by the
         * superclass from command line arguments, which should reflect the
         * desired alarm schema.
         */
        this.chanMap = channelDictionary.getChannelDefinitionMap();
        this.missionDictionary = appContext.getBean(IAlarmDictionaryFactory.class).getNewInstance(dictConfig,
                this.dictionaryPath, this.chanMap);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        super.showHelp();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println();
        pw.println("This utility requires a channel dictionary as input, because alarm");
        pw.println("definitions cannot be parsed without channel definitions.  This");
        pw.println("channel dictionary must be in multimission format.");

        pw.flush();

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {

        writer.writeStartDocument();
        writer.writeStartElement("alarm_dictionary");

        /* MPCS-7434 - 1/29/16. Get schema version from dictionary properties, mission and scid
         * from the parsed dictionary */
        writeHeaderElement(writer,
                DictionaryProperties.getMultimissionDictionaryVersion(DictionaryType.ALARM),
                this.missionDictionary.getGdsVersionId(),
                this.missionDictionary.getMission(),
                this.missionDictionary.getSpacecraftIds());

        writer.writeStartElement("alarms");

        /*
         * First write the single channel alarm definitions
         */
        final List<IAlarmDefinition> alarms = this.missionDictionary
                .getSingleChannelAlarmDefinitions();
        for (final IAlarmDefinition alarm : alarms) {

            writer.writeStartElement("alarm");
            writer.writeAttribute("channel_id", alarm.getChannelId());
            writer.writeAttribute("test_type", alarm.isCheckOnDn() ? "DN"
                    : "EU");

            switch (alarm.getAlarmType()) {

            case HIGH_VALUE_COMPARE:
            case LOW_VALUE_COMPARE:
            case VALUE_CHANGE:
            case MASK_COMPARE:
            case DIGITAL_COMPARE:
            case EXCLUSIVE_COMPARE:
            case INCLUSIVE_COMPARE:
            case VALUE_DELTA:
            case STATE_COMPARE:
                generateSingleAlarmDef(writer, alarm);
                break;

            case COMPOUND:
                generateCompoundAlarmDef(writer,
                        (ICompoundAlarmDefinition) alarm);

                break;

            default:
                throw new XMLStreamException("Unrecognized alarm type: "
                        + alarm.getAlarmType());

            }

            writer.writeEndElement(); // alarm

        }

        /*
         * Write the combination alarms. Warn the user this requires an
         * alternate MM schema.
         */
        final Map<String, ICombinationAlarmDefinition> comboAlarms = this.missionDictionary
                .getCombinationAlarmMap();
        if (comboAlarms != null && !comboAlarms.isEmpty()) {
            TraceManager

                    .getDefaultTracer()
                    .warn("Alarm input file contains combination alarms, which are not officially supported by the multimission alarm schema");
            TraceManager

                    .getDefaultTracer()
                    .warn("These alarms will be generated according to an unofficial, alternate schema");

            for (final ICombinationAlarmDefinition comboDef : comboAlarms
                    .values()) {
                generateCombinationAlarmDef(writer, comboDef);
            }
        }

        /* Write alarm off controls. */
        final List<IAlarmOffControl> offControls = this.missionDictionary
                .getOffControls();
        if (offControls != null && !offControls.isEmpty()) {
            for (final IAlarmOffControl off : offControls) {
                generateOffControlDef(writer, off);
            }

        }

        writer.writeEndElement(); // alarms
        writer.writeEndElement(); // alarm_dictionary
        writer.writeEndDocument();

    }

    /**
     * Writes a simple, single channel alarm definition element (e.g, a high
     * alarm)
     * 
     * @param writer
     *            the XML stream writer to write to
     * @param alarm
     *            the definition of the alarm to write
     * @throws XMLStreamException
     *             if there is a problem generating the XML stream
     */
    private void generateSingleAlarmDef(final XMLStreamWriter writer,
            final IAlarmDefinition alarm) throws XMLStreamException {

        String elementname = "";

        switch (alarm.getAlarmType()) {

        case HIGH_VALUE_COMPARE:
            elementname = "high_alarm";
            break;
        case LOW_VALUE_COMPARE:
            elementname = "low_alarm";
            break;
        case VALUE_CHANGE:
            elementname = "change_alarm";
            break;
        case MASK_COMPARE:
            elementname = "mask_alarm";
            break;
        case DIGITAL_COMPARE:
            elementname = "digital_alarm";
            break;
        case EXCLUSIVE_COMPARE:
            elementname = "exclusive_alarm";
            break;
        case INCLUSIVE_COMPARE:
            elementname = "inclusive_alarm";
            break;
        case VALUE_DELTA:
            elementname = "delta_alarm";
            break;
        case STATE_COMPARE:
            elementname = "state_alarm";
            break;
        default:
            throw new XMLStreamException("Unrecognized alarm type");

        }

        writer.writeStartElement(elementname);

        writer.writeAttribute("id", alarm.getAlarmId());

        if (alarm.getAlarmLevel() != AlarmLevel.NONE) {
            writer.writeAttribute("level", alarm.getAlarmLevel().toString());
        }

        if (alarm.hasHysteresis()) {
            writer.writeAttribute("hysteresis_in",
                    Integer.toString(alarm.getHysteresisInValue()));
            writer.writeAttribute("hysteresis_out",
                    Integer.toString(alarm.getHysteresisOutValue()));
        }

        switch (alarm.getAlarmType()) {

        case HIGH_VALUE_COMPARE:
            writer.writeAttribute("high_limit",
                    String.format("%g", alarm.getUpperLimit()));
            break;

        case LOW_VALUE_COMPARE:
            writer.writeAttribute("low_limit",
                    String.format("%g", alarm.getLowerLimit()));
            break;

        case INCLUSIVE_COMPARE:
            writer.writeAttribute("low_limit",
                    String.format("%g", alarm.getLowerLimit()));
            writer.writeAttribute("high_limit",
                    String.format("%g", alarm.getUpperLimit()));
            break;

        case EXCLUSIVE_COMPARE:
            writer.writeAttribute("low_limit",
                    String.format("%g", alarm.getLowerLimit()));
            writer.writeAttribute("high_limit",
                    String.format("%g", alarm.getUpperLimit()));
            break;

        case STATE_COMPARE:
            final List<Long> states = alarm.getAlarmStates();
            final IChannelDefinition currentChannel = this.chanMap.get(alarm
                    .getChannelId());
            final EnumerationDefinition enumdef = currentChannel
                    .getLookupTable();
            for (final long state : states) {
                if (enumdef.getValue(state) != null) {
                    XmlUtility.writeSimpleElement(writer, "state",
                            enumdef.getValue(state));
                } else {
                    XmlUtility.writeSimpleElement(writer, "state", state);
                }
            }
            break;

        case VALUE_DELTA:
            writer.writeAttribute("delta_limit",
                    String.format("%g", alarm.getDeltaLimit()));
            break;

        case MASK_COMPARE:
            writer.writeAttribute("value_mask",
                    String.format("0x%X", alarm.getValueMask()));
            break;

        case DIGITAL_COMPARE:
            writer.writeAttribute("value_mask",
                    String.format("0x%X", alarm.getValueMask()));
            writer.writeAttribute("valid_mask",
                    String.format("0x%X", alarm.getDigitalValidMask()));
            break;

        case VALUE_CHANGE:
            break;
        default:
            break;

        } // MHT - MPCS-6996 - 4/13/15 Added alarm description and categories
        final Categories cat = alarm.getCategories();
        writeCategory(writer, cat);
        final KeyValueAttributes kvaMap = alarm.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        final String desc = alarm.getAlarmDescription();
        if (desc != null) {
            XmlUtility.writeSimpleElement(writer, "description", desc);
        }
        writer.writeEndElement(); // elementname - alarmType
    }

    /**
     * Writes a compound channel alarm definition element.
     * 
     * @param writer
     *            the XML stream writer to write to
     * @param alarm
     *            the definition of the compound alarm to write
     * @throws XMLStreamException
     *             if there is a problem generating the XML stream
     */
    private void generateCompoundAlarmDef(final XMLStreamWriter writer,
            final ICompoundAlarmDefinition alarm) throws XMLStreamException {

        writer.writeStartElement("compound_alarm");
        writer.writeAttribute("id", alarm.getAlarmId());
        writer.writeAttribute("combination_type", alarm
                .getCombinationOperator().toString());

        /*
         * Each child alarm of the compound alarm is just another simple alarm
         * and can be written using the same method.
         */
        final List<IAlarmDefinition> innerDefs = alarm.getChildAlarms();
        if (innerDefs != null) {
            for (final IAlarmDefinition childDef : innerDefs) {
                generateSingleAlarmDef(writer, childDef);
            }
        }

        writer.writeEndElement(); // compound_alarm

    }

    /**
     * Writes a combination (multi-channel) alarm definition element.
     * 
     * @param writer
     *            the XML stream writer to write to
     * @param alarm
     *            the definition of the combination alarm to write
     * @throws XMLStreamException
     *             if there is a problem generating the XML stream
     */
    private void generateCombinationAlarmDef(final XMLStreamWriter writer,
            final ICombinationAlarmDefinition comboDef)
            throws XMLStreamException {

        writer.writeStartElement("combination_alarm");
        writer.writeAttribute("id", comboDef.getAlarmId());
        writer.writeAttribute("level", comboDef.getAlarmLevel().toString());

        /*
         * At the top level of the combination, there is one combination group.
         * The call here will then recurse to write nested groups.
         */
        final ICombinationGroup sourceGroup = comboDef.getSourceGroup();
        if (sourceGroup != null) {
            writeComboSourceGroupDef(writer, sourceGroup);
        }

        /*
         * Write the combination target definitions.
         */
        final List<ICombinationTarget> targets = comboDef.getTargets();
        if (targets != null && !targets.isEmpty()) {
            writer.writeStartElement("target_channels");
            for (final ICombinationTarget target : targets) {
                writer.writeStartElement("target_channel");
                writer.writeAttribute("channel_id", target.getChannelId());
                writer.writeAttribute("test_type", target.isCheckOnDn() ? "DN"
                        : "EU");
                writer.writeEndElement(); // target channel
            }
            writer.writeEndElement(); // target_channels
        }
        final Categories cat = comboDef.getCategories();
        writeCategory(writer, cat);
        final KeyValueAttributes kvaMap = comboDef.getKeyValueAttributes();
        writeKeyValue(writer, kvaMap);
        final String desc = comboDef.getAlarmDescription();
        if (desc != null) {
            XmlUtility.writeSimpleElement(writer, "description", desc);
        }
        writer.writeEndElement(); // combination_alarm

    }

    /**
     * Writes a combination (multi-channel) alarm group element. Recurses to
     * write nested groups.
     * 
     * @param writer
     *            the XML stream writer to write to
     * @param alarm
     *            the definition of the combination group to write
     * @throws XMLStreamException
     *             if there is a problem generating the XML stream
     */
    private void writeComboSourceGroupDef(final XMLStreamWriter writer,
            final ICombinationGroup groupDef) throws XMLStreamException {

        writer.writeStartElement("combination");
        writer.writeAttribute("combination_type", groupDef.getOperator()
                .toString());

        final List<ICombinationGroupMember> childDefs = groupDef.getOperands();
        if (childDefs != null && !childDefs.isEmpty()) {

            for (final ICombinationGroupMember child : childDefs) {

                /*
                 * Each member of the group is either another group (in which
                 * case make a recursive call) or a single source alarm.
                 */
                if (child instanceof ICombinationGroup) {
                    writeComboSourceGroupDef(writer, (ICombinationGroup) child);
                } else {

                    /*
                     * Single source alarm is just another simple, single
                     * channel alarm, and can be written using the same method
                     * those alarms use.
                     */
                    final ICombinationSource sourceChild = (ICombinationSource) child;
                    writer.writeStartElement("combo_source_alarm");
                    writer.writeAttribute("channel_id",
                            sourceChild.getChannelId());
                    writer.writeAttribute("test_type",
                            sourceChild.isCheckOnDn() ? "DN" : "EU");
                    generateSingleAlarmDef(writer,
                            sourceChild.getActualAlarmDefinition());
                    writer.writeEndElement(); // combo_source_alarm
                }
            }
        }
        writer.writeEndElement(); // combination

    }

    /**
     * Writes an off control.
     * 
     * @param writer
     *            the XML stream writer to write to
     * @param control
     *            the definition of the alarm off control to write
     * @throws XMLStreamException
     *             if there is a problem generating the XML stream
     */
    private void generateOffControlDef(final XMLStreamWriter writer,
            final IAlarmOffControl control) throws XMLStreamException {

        switch (control.getScope()) {
        case ALARM:
            writer.writeStartElement("off_control_by_id");
            writer.writeAttribute("id", control.getAlarmId());
            break;
        case CHANNEL:
            writer.writeStartElement("off_control_by_channel");
            writer.writeAttribute("channel_id", control.getChannelId());
            break;
        case CHANNEL_AND_LEVEL:
            writer.writeStartElement("off_control_by_channel");
            writer.writeAttribute("channel_id", control.getChannelId());
            writer.writeAttribute("level", control.getLevel().toString());
            break;
        default:
            throw new IllegalStateException("unrecognized off control scope "
                    + control.getScope());
        }
        writer.writeEndElement(); // off_control_by_id or off_control_by_channel
    }

    /**
     * Main method for the application.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {

        final AlarmDictionaryConverterApp theApp = new AlarmDictionaryConverterApp();

        try {
            /*
             * MPCS-7750 - 10/23/15. Use createOptions() rather than
             * creating a new reserved/base options object.
             */
            final ICommandLine commandLine = theApp.createOptions()
                    .parseCommandLine(args, true);

            theApp.configure(commandLine);

            theApp.readMissionDictionary();
            theApp.writeMultimissionDictionary();

        } catch (final DictionaryException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(1);

        } catch (final ParseException e) {
            if (e.getMessage() == null) {
                TraceManager.getDefaultTracer().error(e.toString());

            } else {
                TraceManager.getDefaultTracer().error(e.getMessage());

            }
            System.exit(1);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

}
