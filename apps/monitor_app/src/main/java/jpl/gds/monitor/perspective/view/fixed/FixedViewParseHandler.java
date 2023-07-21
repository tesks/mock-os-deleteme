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
package jpl.gds.monitor.perspective.view.fixed;

import java.text.DateFormat;
import java.text.ParseException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.monitor.perspective.view.fixed.IConditionConfiguration.Comparison;
import jpl.gds.monitor.perspective.view.fixed.IConditionConfiguration.SourceField;
import jpl.gds.monitor.perspective.view.fixed.conditionals.AlarmCondition;
import jpl.gds.monitor.perspective.view.fixed.conditionals.CompoundCondition;
import jpl.gds.monitor.perspective.view.fixed.conditionals.ConditionTable;
import jpl.gds.monitor.perspective.view.fixed.conditionals.DataTypeCondition;
import jpl.gds.monitor.perspective.view.fixed.conditionals.NullCondition;
import jpl.gds.monitor.perspective.view.fixed.conditionals.RelationalCondition;
import jpl.gds.monitor.perspective.view.fixed.conditionals.StaleCondition;
import jpl.gds.monitor.perspective.view.fixed.fields.AbstractTextFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.BoxFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ButtonFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration.ChannelFieldType;
import jpl.gds.monitor.perspective.view.fixed.fields.DualPointFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.HeaderFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ImageFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.LineFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TextFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;
import jpl.gds.perspective.view.IViewConfigParser;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.XmlUtility;

/**
 * A view configuration parser specific to the FixedLayoutViewConfiguration.
 */
public class FixedViewParseHandler implements IViewConfigParser {
    
    private final IFixedLayoutViewConfiguration viewConfig;

	private final ApplicationContext appContext;
    
    private static final String CONDITION_PATTERN = "[A-Za-z0-9\\-_\\.]+";
    

    /**
     * Constructor.
     * 
     * @param config the FixedLayoutViewConfiguration to populate in this parser
     */
    public FixedViewParseHandler(final ApplicationContext appContext, final IFixedLayoutViewConfiguration config) {
        this.viewConfig = config;
        this.appContext = appContext;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.IViewConfigParser#init()
     */
    @Override
    public void init() {
        SystemUtilities.doNothing();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.IViewConfigParser#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public boolean startElement(final String uri, final String localName, final String qname,
            final Attributes attr) throws SAXException {

        boolean elementHandled = true;
        
        if(localName.equalsIgnoreCase(IConditionConfiguration.CONDITION_TAG)) {
            parseConditionField(attr);
        }
        else if (localName.equalsIgnoreCase(BoxFieldConfiguration.BOX_TAG)) {
            parseBoxField(attr);
        }
        else if (localName.equalsIgnoreCase(ButtonFieldConfiguration.BUTTON_TAG)) {
            parseButtonField(attr);
        }
        else if (localName.equalsIgnoreCase(ImageFieldConfiguration.IMAGE_TAG)) {
            parseImageField(attr);
        }
        else if (localName.equalsIgnoreCase(LineFieldConfiguration.LINE_TAG)) {
            parseLineField(attr);
        }
        else if (localName.equalsIgnoreCase(TextFieldConfiguration.TEXT_TAG)) {
            parseTextField(attr);
        }
        else if (localName
                .equalsIgnoreCase(ChannelFieldConfiguration.CHANNEL_FIELD_TAG)) {
            parseChannelField(attr);
        }
        else if (localName.equalsIgnoreCase(TimeFieldConfiguration.TIME_FIELD_TAG)) {
            parseTimeField(attr);
        }
        else if (localName.equalsIgnoreCase(HeaderFieldConfiguration.HEADER_TAG)) {
            parseHeaderField(attr);
            
        } else {
           elementHandled = false;
        }

        return elementHandled;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.IViewConfigParser#endElement(java.lang.String, java.lang.String, java.lang.String, java.lang.StringBuilder)
     */
    @Override
    public boolean endElement(final String uri, final String localName, final String qname,
            final StringBuilder text) throws SAXException {

        if (localName.equalsIgnoreCase(BoxFieldConfiguration.BOX_TAG)
                || localName.equalsIgnoreCase(ButtonFieldConfiguration.BUTTON_TAG)
                || localName.equalsIgnoreCase(ImageFieldConfiguration.IMAGE_TAG)
                || localName.equalsIgnoreCase(LineFieldConfiguration.LINE_TAG)
                || localName.equalsIgnoreCase(TextFieldConfiguration.TEXT_TAG)
                || localName.equalsIgnoreCase(ChannelFieldConfiguration.CHANNEL_FIELD_TAG)
                || localName.equalsIgnoreCase(TimeFieldConfiguration.TIME_FIELD_TAG)
                || localName.equalsIgnoreCase(HeaderFieldConfiguration.HEADER_TAG)
                || localName.equalsIgnoreCase(IConditionConfiguration.CONDITION_TAG)) {
            return true;
        } else {
            return false;
        }

    }
    
    private void parseConditionField(final Attributes attr) throws SAXException {
        IConditionConfiguration condition = null;
        final String comparison = attr.getValue(IConditionConfiguration.COMPARISON_TAG);

        if (comparison != null) {
            Comparison comp;
            String conditionId = null;
            String channelId;
            String value;
            SourceField source;

            try {
                comp = Enum.valueOf(Comparison.class, comparison);
            } catch(final Exception e) {
                throw new SAXException("Condition has invalid comparison attribute");
            }


            // create the condition object based on the comparison type
            switch(comp) {
            case LT:
            case GT:
            case LE:
            case GE:
            case EQ:
            case NE:
                // set the conditionId
                conditionId = attr.getValue(IConditionConfiguration.CONDITION_ID_TAG);
                if(conditionId == null || !conditionId.matches(CONDITION_PATTERN)) {
                    throw new SAXException("RELATIONAL Condition is missing condition ID attribute");
                }

                // set the channelId
                channelId = attr.getValue(IConditionConfiguration.CHANNEL_ID_TAG);
                if(channelId == null || channelId.equals("")) {
                    throw new SAXException("RELATIONAL Condition is missing channel ID attribute");
                }

                // set the source
                final String sourceString = attr.getValue(IConditionConfiguration.SOURCEFIELD_TAG);
                if(sourceString == null || sourceString.equals("")) {
                    throw new SAXException("RELATIONAL Condition is missing source attribute");
                }

                try {
                    source = Enum.valueOf(IConditionConfiguration.SourceField.class,
                            sourceString);
                } catch (final Exception e) {
                    throw new SAXException("RELATIONAL Condition has invalid source attribute");
                }
                final VenueType venue = appContext.getBean(IVenueConfiguration.class).getVenueType();

                if(source != null && source.equals(SourceField.LST) && 
                        venue != null && appContext.getBean(MissionProperties.class).getVenueUsesSol(venue)) {
                    TraceManager.getDefaultTracer().warn(

                            "LST conditional will have unexpected behavior " +
                            "in a non SURFACE venue");
                }    

                // set the value
                value = attr.getValue(IConditionConfiguration.VALUE_TAG);
                if(value == null || value.equals("")) {
                    throw new SAXException("RELATIONAL Condition is missing value attribute");
                }

                DateFormat f;
                try {
                    switch(source) {
                    case EU:
                        Double.valueOf(value);
                        break;
                    case ERT:
                        new AccurateDateTime(value);
                        break;
                    case RCT:
                        f = TimeUtility.getFormatterFromPool();
                        try {
                            f.parse(value);
                        } catch (final ParseException e) {
                            TraceManager.getDefaultTracer ().error

                            ("RCT value caught unhandled and unexpected" +
                                    " exception in ViewConfigParseHandler");
                            e.printStackTrace();
                        } finally {
                            TimeUtility.releaseFormatterToPool(f);
                        }
                        break;
                    case SCET:
                        f = TimeUtility.getFormatterFromPool();
                        try {
                            f.parse(value);
                        } catch (final ParseException e) {
                            TraceManager.getDefaultTracer ().error

                            ("SCET value caught unhandled and unexpected" +
                                    " exception in ViewConfigParseHandler");
                            e.printStackTrace();
                        } finally {
                            TimeUtility.releaseFormatterToPool(f);
                        }
                        break;
                    case SCLK:
                    	final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();
                        sclkFmt.valueOf(value);
                        break;
                    case LST:
                        LocalSolarTimeFactory.getNewLst(value, appContext.getBean(IContextIdentification.class).getSpacecraftId());
                        break;
                    case STATUS:
                        break;
                    case DN:
                    case RAW:
                    case VALUE:
                        break;
                    default:
                        break;
                    }
                }    
                catch(final NumberFormatException nfe) {
                    throw new SAXException("RELATIONAL Condition has invalid value attribute");
                } 
                catch(final ParseException pe) {
                    throw new SAXException("RELATIONAL Condition has invalid value attribute");
                } 
                catch(final IllegalArgumentException ie) {
                    throw new SAXException("RELATIONAL Condition has invalid value attribute");
                } 

                condition = new RelationalCondition(conditionId, channelId, source, comp, value);
                break;
            case SET:
            case NOT_SET:
                // set the conditionId
                conditionId = attr.getValue(IConditionConfiguration.CONDITION_ID_TAG);
                if(conditionId == null || !conditionId.matches(CONDITION_PATTERN)) {
                    throw new SAXException("ALARM Condition is missing condition ID attribute");
                }

                // set the channelId
                channelId = attr.getValue(IConditionConfiguration.CHANNEL_ID_TAG);
                if(channelId == null || channelId.equals("")) {
                    throw new SAXException("ALARM Condition is missing channel ID attribute");
                }

                // set the value
                value = attr.getValue(IConditionConfiguration.VALUE_TAG);
                if(value == null || value.equals("")) {
                    throw new SAXException("ALARM Condition is missing value attribute");
                }

                try {
                    Enum.valueOf(AlarmCondition.Value.class,value);
                } catch (final Exception e) {
                    throw new SAXException("ALARM Condition has invalid value attribute");
                }

                condition = new AlarmCondition(conditionId, channelId, comp, value);
                break;
            case TYPE:
                // set the conditionId
                conditionId = attr.getValue(IConditionConfiguration.CONDITION_ID_TAG);
                if(conditionId == null || !conditionId.matches(CONDITION_PATTERN)) {
                    throw new SAXException("TYPE Condition is missing condition ID attribute");
                }

                // set the channelId
                channelId = attr.getValue(IConditionConfiguration.CHANNEL_ID_TAG);
                if(channelId == null || channelId.equals("")) {
                    throw new SAXException("TYPE Condition is missing channel ID attribute");
                }

                // set the value
                value = attr.getValue(IConditionConfiguration.VALUE_TAG);
                if(value == null || value.equals("")) {
                    throw new SAXException("TYPE Condition is missing value attribute");
                }
                try {
                    Enum.valueOf(ChannelType.class, value);
                } catch (final IllegalArgumentException e) {
                    throw new SAXException("TYPE Condition has invalid value attribute");
                }

                condition = new DataTypeCondition(conditionId, channelId, value);
                break;
            case STALE:
                // set the conditionId
                conditionId = attr.getValue(IConditionConfiguration.CONDITION_ID_TAG);
                if(conditionId == null || !conditionId.matches(CONDITION_PATTERN)) {
                    throw new SAXException("STALE Condition is missing condition ID attribute");
                }

                // set the channelId
                channelId = attr.getValue(IConditionConfiguration.CHANNEL_ID_TAG);
                if(channelId == null || channelId.equals("")) {
                    throw new SAXException("STALE Condition is missing channel ID attribute");
                }

                condition = new StaleCondition(conditionId, channelId);
                break;
            case IS_NULL:
                // set the conditionId
                conditionId = attr.getValue(IConditionConfiguration.CONDITION_ID_TAG);
                if(conditionId == null || !conditionId.matches(CONDITION_PATTERN)) {
                    throw new SAXException("IS_NULL Condition is missing condition ID attribute");
                }

                // set the channelId
                channelId = attr.getValue(IConditionConfiguration.CHANNEL_ID_TAG);
                if(channelId == null || channelId.equals("")) {
                    throw new SAXException("IS_NULL Condition is missing channel ID attribute");
                }

                condition = new NullCondition(conditionId, channelId);
                break;
            default:
                break;
            }

            /* Set view config outside of case
             * statements */
            condition.setViewConfig(viewConfig);


            viewConfig.addConditionConfig(conditionId, condition);
            ConditionTable.getInstance().addCondition(conditionId, condition);
        }
    }

    private void parseHeaderField(final Attributes attr) throws SAXException {
        final HeaderFieldConfiguration field = new HeaderFieldConfiguration(appContext);
        parseCommonFixedFieldAttrs(attr, field);

        final String foreground = attr
                .getValue(HeaderFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }

        final String background = attr
                .getValue(HeaderFieldConfiguration.BACKGROUND_TAG);
        if (background != null && !background.equals("")) {
            field.setBackground(new ChillColor(background));
        }

        final String isTransparent = attr
                .getValue(HeaderFieldConfiguration.TRANSPARENT_TAG);
        if (isTransparent != null  && !isTransparent.equals("")) {
            field.setTransparent(XmlUtility.getBooleanFromAttr(attr,
                    HeaderFieldConfiguration.TRANSPARENT_TAG));
        }

        final String headerType = attr.getValue(HeaderFieldConfiguration.HEADER_TYPE_TAG);
        field.setHeaderType(headerType);

        viewConfig.addField(field);

        field.load();
    }

    private void parseTimeField(final Attributes attr) throws SAXException {
        final TimeFieldConfiguration field = new TimeFieldConfiguration(this.appContext);
        parseCommonFixedFieldAttrs(attr, field);
        final String source = attr.getValue(TimeFieldConfiguration.SOURCE_TAG);
        if (source != null) {
            try {
                field
                .setTimeType(Enum
                        .valueOf(
                                TimeFieldConfiguration.SourceTimeType.class,
                                source));
            } catch (final Exception e) {
                field
                .setTimeType(TimeFieldConfiguration.SourceTimeType.ERT);
            }
        }
        final String foreground = attr
                .getValue(AbstractTextFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null  && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }
        final String background = attr
                .getValue(AbstractTextFieldConfiguration.BACKGROUND_TAG);
        if (background != null  && !background.equals("")) {
            field.setBackground(new ChillColor(background));
        }
        final String font = attr.getValue(AbstractTextFieldConfiguration.FONT_TAG);
        if (font != null  && !font.equals("")) {
            field.setFont(new ChillFont(font));
        }
        final String isTransparent = attr
                .getValue(AbstractTextFieldConfiguration.TRANSPARENT_TAG);
        if (isTransparent != null  && !isTransparent.equals("")) {
            field.setTransparent(XmlUtility.getBooleanFromAttr(attr,
                    AbstractTextFieldConfiguration.TRANSPARENT_TAG));
        }
        final String format = attr.getValue(AbstractTextFieldConfiguration.FORMAT_TAG);
        if (format != null  && !format.equals("")) {
            try {
                field.setFormat(format);
            } catch (final Exception e) {
                e.printStackTrace();
                throw new SAXException("Error processing date/time format string: " + format);
            }
        }
        viewConfig.addField(field);

    }

    private void parseChannelField(final Attributes attr) throws SAXException {
        final ChannelFieldConfiguration field = new ChannelFieldConfiguration(appContext);
        parseCommonFixedFieldAttrs(attr, field);
        final String chanId = attr
                .getValue(ChannelFieldConfiguration.CHANNEL_ID_TAG);
        if (chanId != null && !chanId.equals("")) {
            field.setChannelId(chanId);
        } else {
            throw new SAXException("Channel field is missing channel ID attribute");
        }
        final String source = attr.getValue(ChannelFieldConfiguration.SOURCE_TAG);
        if (source != null) {
            try {
                field.setFieldType(Enum.valueOf(ChannelFieldType.class,
                        source));
            } catch (final Exception e) {
                field
                .setFieldType(ChannelFieldConfiguration.ChannelFieldType.ID);
            }
        }
        final String foreground = attr
                .getValue(AbstractTextFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null   && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }
        final String background = attr
                .getValue(AbstractTextFieldConfiguration.BACKGROUND_TAG);
        if (background != null  && !background.equals("")) {
            field.setBackground(new ChillColor(background));
        }
        final String font = attr.getValue(AbstractTextFieldConfiguration.FONT_TAG);
        if (font != null  && !font.equals("")) {
            field.setFont(new ChillFont(font));
        }
        final String isTransparent = attr
                .getValue(AbstractTextFieldConfiguration.TRANSPARENT_TAG);
        if (isTransparent != null && !isTransparent.equals("")) {
            field.setTransparent(XmlUtility.getBooleanFromAttr(attr,
                    AbstractTextFieldConfiguration.TRANSPARENT_TAG));
        }
        final String format = attr.getValue(AbstractTextFieldConfiguration.FORMAT_TAG);
        if (format != null && !format.equals("")) {
            try {
                field.setFormat(format);
            } catch (final IllegalArgumentException e) {
                e.printStackTrace();
                throw new SAXException("Error processing date/time format string: " + format);
            }
        }
        final String useHighlight = attr
                .getValue(ChannelFieldConfiguration.HIGHLIGHT_TAG);
        if (useHighlight != null  && !useHighlight.equals("")) {
            field.setUseAlarmHighlight(XmlUtility.getBooleanFromAttr(attr,
                    ChannelFieldConfiguration.HIGHLIGHT_TAG));
        }
        viewConfig.addField(field);

    }

    private void parseTextField(final Attributes attr) throws SAXException {
        final TextFieldConfiguration field = new TextFieldConfiguration();
        parseCommonFixedFieldAttrs(attr, field);
        final String text = attr
                .getValue(TextFieldConfiguration.INITIAL_VALUE_TAG);
        if (text == null) {
            throw new SAXException("Text attribute is missing in text field definition");
        }
        field.setText(text);
        final String foreground = attr
                .getValue(AbstractTextFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null  && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }

        final String background = attr
                .getValue(AbstractTextFieldConfiguration.BACKGROUND_TAG);
        if (background != null  && !background.equals("")) {
            field.setBackground(new ChillColor(background));
        }
        final String font = attr.getValue(AbstractTextFieldConfiguration.FONT_TAG);
        if (font != null  && !font.equals("")) {
            field.setFont(new ChillFont(font));
        }
        final String isTransparent = attr
                .getValue(AbstractTextFieldConfiguration.TRANSPARENT_TAG);
        if (isTransparent != null  && !isTransparent.equals("")) {
            field.setTransparent(XmlUtility.getBooleanFromAttr(attr,
                    AbstractTextFieldConfiguration.TRANSPARENT_TAG));
        }
        final String format = attr.getValue(AbstractTextFieldConfiguration.FORMAT_TAG);
        if (format != null   && !format.equals("")) {
            field.setFormat(format);
        }
        viewConfig.addField(field);

    }

    private void parseLineField(final Attributes attr) throws SAXException {
        final LineFieldConfiguration field = new LineFieldConfiguration();
        parseCommonFixedFieldAttrs(attr, field);
        final String xStr = attr.getValue(DualPointFixedFieldConfiguration.X_END_TAG);
        final String yStr = attr.getValue(DualPointFixedFieldConfiguration.Y_END_TAG);
        if (xStr == null || xStr.equals("")) {
            throw new SAXException("End X coordinate attribute is missing in line field definition");
        }
        field.setXEnd(Integer.valueOf(xStr));
        if (yStr == null || yStr.equals("")) {
            throw new SAXException("End Y coordinate attribute is missing in line field definition");
        }
        field.setYEnd(Integer.valueOf(yStr));

        final String foreground = attr
                .getValue(LineFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null  && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }
        final String lineThickness = attr
                .getValue(LineFieldConfiguration.LINE_THICKNESS_TAG);
        if (lineThickness != null  && !lineThickness.equals("")) {
            field.setLineThickness(Integer.valueOf(lineThickness));
        }
        final String lineStyle = attr
                .getValue(LineFieldConfiguration.LINE_STYLE_TAG);
        if (lineStyle != null  && !lineStyle.equals("")) {
            try {
                field
                .setLineStyle(Enum.valueOf(LineStyle.class,
                        lineStyle));
            } catch (final Exception e) {
                field.setLineStyle(LineStyle.SOLID);
            }
        }
        viewConfig.addField(field);

    }

    private void parseImageField(final Attributes attr) throws SAXException {
        final ImageFieldConfiguration field = new ImageFieldConfiguration();
        parseCommonFixedFieldAttrs(attr, field);
        field.setImagePath(attr
                .getValue(ImageFieldConfiguration.IMAGE_PATH_TAG));
        final String xStr = attr.getValue(DualPointFixedFieldConfiguration.X_END_TAG);
        if (xStr != null && !xStr.equals("")) {
            field.setXEnd(Integer.valueOf(xStr));
        }
        final String yStr = attr.getValue(DualPointFixedFieldConfiguration.Y_END_TAG);
        if (yStr != null && !yStr.equals("")) {
            field.setYEnd(Integer.valueOf(yStr));
        }
        viewConfig.addField(field);

    }

    private void parseButtonField(final Attributes attr) throws SAXException {
        final ButtonFieldConfiguration field = new ButtonFieldConfiguration();
        parseCommonFixedFieldAttrs(attr, field);

        final String title = attr.getValue(ButtonFieldConfiguration.TITLE_TAG);
        if (title != null) {
            field.setText(title);
        }

        final String actionString = attr
                .getValue(ButtonFieldConfiguration.ACTION_STRING_TAG);
        if (actionString != null) {
            field.setActionString(actionString);
        }

        final String xStr = attr.getValue(DualPointFixedFieldConfiguration.X_END_TAG);
        if (xStr != null && !xStr.equals("")) {
            field.setXEnd(Integer.valueOf(xStr));
        }

        final String yStr = attr.getValue(DualPointFixedFieldConfiguration.Y_END_TAG);
        if (yStr != null  && !yStr.equals("")) {
            field.setYEnd(Integer.valueOf(yStr));
        }

        final String foreground = attr
                .getValue(ButtonFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }

        final String background = attr
                .getValue(ButtonFieldConfiguration.BACKGROUND_TAG);
        if (background != null && !background.equals("")) {
            field.setBackground(new ChillColor(background));
        }

        final String actionType = attr
                .getValue(ButtonFieldConfiguration.ACTION_TYPE_TAG);
        if (actionType != null) {
            try {
                field.setActionType(Enum.valueOf(
                        ButtonFieldConfiguration.ActionType.class,
                        actionType));
            } catch (final Exception e) {
                field
                .setActionType(ButtonFieldConfiguration.ActionType.LAUNCH_PAGE);
            }
        }

        viewConfig.addField(field);

    }

    private void parseBoxField(final Attributes attr) throws SAXException {
        final BoxFieldConfiguration field = new BoxFieldConfiguration();
        parseCommonFixedFieldAttrs(attr, field);

        final String title = attr.getValue(BoxFieldConfiguration.TITLE_TAG);
        if (title != null) {
            field.setText(title);
        }

        final String xEnd = attr.getValue(DualPointFixedFieldConfiguration.X_END_TAG);
        if (xEnd != null && !xEnd.equals("")) {
            field.setXEnd(Integer.valueOf(xEnd));
        }

        final String yEnd = attr.getValue(DualPointFixedFieldConfiguration.Y_END_TAG);
        if (yEnd != null && !yEnd.equals("")) {
            field.setYEnd(Integer.valueOf(yEnd));
        }

        final String font = attr.getValue(BoxFieldConfiguration.FONT_TAG);
        if (font != null && !font.equals("")) {
            field.setFont(new ChillFont(font));
        }

        final String foreground = attr
                .getValue(BoxFieldConfiguration.FOREGROUND_TAG);
        if (foreground != null && !foreground.equals("")) {
            field.setForeground(new ChillColor(foreground));
        }

        final String fillColor = attr
                .getValue(BoxFieldConfiguration.BACKGROUND_TAG);
        if (fillColor != null && !fillColor.equals("")) {
            field.setBackground(new ChillColor(fillColor));
        }

        final String lineThickness = attr
                .getValue(BoxFieldConfiguration.LINE_THICKNESS_TAG);
        if (lineThickness != null && !lineThickness.equals("")) {
            field.setLineThickness(Integer.valueOf(lineThickness));
        }

        final String isTransparent = attr
                .getValue(BoxFieldConfiguration.TRANSPARENT_TAG);
        if (isTransparent != null && !isTransparent.equals("")) {
            field.setTransparent(XmlUtility.getBooleanFromAttr(attr,
                    BoxFieldConfiguration.TRANSPARENT_TAG));
        }

        final String lineStyle = attr
                .getValue(BoxFieldConfiguration.LINE_STYLE_TAG);
        if (lineStyle != null && !lineStyle.equals("")) {
            try {
                field
                .setLineStyle(Enum.valueOf(LineStyle.class,
                        lineStyle));
            } catch (final Exception e) {
                field.setLineStyle(LineStyle.SOLID);
            }
        }

        viewConfig.addField(field);

    }


    private void parseCommonFixedFieldAttrs(final Attributes attr,
            final IFixedFieldConfiguration field) throws SAXException {
        String str = attr.getValue(IFixedFieldConfiguration.X_START_TAG);
        if (str == null || str.equals("")) {
            throw new SAXException("Missing start X coordinate in fixed view field");
        }
        field.setXStart(Integer.valueOf(str));
        str = attr.getValue(IFixedFieldConfiguration.Y_START_TAG);
        if (str == null || str.equals("")) {
            throw new SAXException("Missing start Y coordinate in fixed view field");
        }
        field.setYStart(Integer.valueOf(str));
        
        final String conditionSet = attr.getValue(IFixedFieldConfiguration.CONDITIONS_TAG);
        if(conditionSet != null && !conditionSet.equals("")) {
            field.setCondition(new CompoundCondition(conditionSet));
        }
    }
}
