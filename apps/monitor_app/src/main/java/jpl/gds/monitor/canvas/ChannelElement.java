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
package jpl.gds.monitor.canvas;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.monitor.canvas.support.ChannelSupport;
import jpl.gds.monitor.canvas.support.StaleSupport;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.ChannelFieldConfiguration.ChannelFieldType;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.string.ParsedFormatter;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillFont;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;

/**
 * This CanvasElement represents a channel field. Any field in the channel
 * definition may be displayed by one of these elements, depending on the
 * selected source field type. ChannelElements are updated when new telemetry
 * arrives.
 * 
 */
public class ChannelElement extends AbstractTextElement 
implements ChannelSupport, StaleSupport {

	/**
	 * Default selection priority for this CanvasElement.
	 */
	private static final int SELECTION_PRIORITY = 1;

	// Alarm colors
	private static final Color YELLOW_COLOR = ChillColorCreator.getColor(
			new ChillColor(ColorName.YELLOW));
	private static final Color RED_COLOR = ChillColorCreator.getColor(
			new ChillColor(ColorName.RED));
	private static final Color WHITE_COLOR = ChillColorCreator.getColor(
			new ChillColor(ColorName.WHITE));
	private static final Color BLACK_COLOR = ChillColorCreator.getColor(
			new ChillColor(ColorName.BLACK));
	private SclkFmt sclkFmt;

	private String channelId;
	private ChannelFieldConfiguration.ChannelFieldType channelFieldType;
	private IChannelDefinition def;
	private MonitorChannelSample data;
	private final IChannelDefinitionProvider defProv;
	private final MonitorConfigValues configVals;

	/**
	 * Keeps the original background color, in case the alarm highlighting 
	 * changes
	 */
	private Color originalBackground; 

	/**
	 * Keeps the original foreground color, in case the alarm highlighting 
	 * changes
	 */
	private Color originalForeground;

	/**
	 * Optional
	 */
	private boolean useAlarmHighLight;
	private boolean reverse;
	private boolean stale;
	private long lastUpdateTime = 0;

	/**
	 * the suspect font (style is always italicized)
	 */
	private Font suspectFont;
	private boolean isSuspectChannel;
	private boolean inAlarm;
	private final DateFormat defaultTimeFormat = 
			TimeUtility.getIsoFormatter();
	private DateFormat userTimeFormat;

	private final Integer scid;

    private SprintfFormat formatUtil;

	/**
	 * Creates a ChannelElement with the given parent Canvas, associated
	 * ChannelDefinition, and source channel field type.
	 * @param appContext the current application context
	 * 
	 * @param parent the parent Canvas widget
	 * @param def the ChannelDefinition for the channel associated with this 
	 *            field
	 * @param type the ChannelFieldType to be drawn by this ChannelField
	 */
	public ChannelElement(final ApplicationContext appContext, final Canvas parent, final IChannelDefinition def, 
			final ChannelFieldType type) {
		super(parent, FixedFieldType.CHANNEL);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
		formatUtil = appContext.getBean(SprintfFormat.class);
		this.def = def;
		defProv = appContext.getBean(IChannelDefinitionProvider.class);
		MonitorConfigValues temp = null;
        try {
            temp = appContext.getBean(MonitorConfigValues.class);
        } catch (final BeansException e) {
            // ok not to have config values
        }
        configVals = temp;
		setStatic(type.isStaticField());
		setText(getNoDataIndicator());
		setTextFromChannelDefinition();
		setSelectionPriority(SELECTION_PRIORITY);
		scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        printFormatter = new SprintfFormat(scid);
	}

	/**
	 * Creates a ChannelElement with the given parent Canvas and fixed field
	 * configuration object.
	 * @param appContext the current application context
	 * 
	 * @param parent the parent Canvas widget
	 * @param chanConfig the ChannelFieldConfiguration object from the 
	 * perspective
	 */
	public ChannelElement(final ApplicationContext appContext, final Canvas parent, 
			final ChannelFieldConfiguration chanConfig) {
		super(parent, chanConfig);
		defProv = appContext.getBean(IChannelDefinitionProvider.class);
		setSelectionPriority(SELECTION_PRIORITY);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
		formatUtil = appContext.getBean(SprintfFormat.class);
		MonitorConfigValues temp = null;
        try {
            temp = appContext.getBean(MonitorConfigValues.class);
        } catch (final BeansException e) {
            // ok not to have config values
        }
        configVals = temp;
		if (channelId != null) {
			def = defProv.getDefinitionFromChannelId(channelId);
		}
		scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        printFormatter = new SprintfFormat(scid);
		updateFieldsFromConfig();
		setText(getNoDataIndicator());
		setTextFromChannelDefinition();
	}

	/**
	 * Sets the C-printf style or java date/time formatter for the text drawn
	 * by this object.
	 * 
	 * @param format the format string
	 */
	@Override
	public void setFormat(final String format) {
		this.format = format;
		userTimeFormat = null;
		noDataIndicator = null;
	}


	/**
	 * Gets the channel ID associated with this ChannelField.
	 * 
	 * @return the channel ID string
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * Sets the channel ID associated with this ChannelField.
	 * 
	 * @param channelId the channel ID string
	 */
	public void setChannelId(final String channelId) {
		this.channelId = channelId;
	}

	/**
	 * Gets the channel field source type.
	 * 
	 * @return ChannelFieldType
	 */
	public ChannelFieldType getChannelFieldType() {
		return channelFieldType;
	}

	/**
	 * Sets the channel field source type.
	 * 
	 * @param fieldType the ChannelFieldType to set
	 */
	public void setChannelFieldType(final ChannelFieldType fieldType) {
		channelFieldType = fieldType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.StaleSupport#checkStale(int)
	 */
	@Override
	public boolean checkStale(final int staleInterval) {
		if (isStaticField) {
			return false;
		}
		if (lastUpdateTime == 0) {
			return false;
		}
		final long currentTime = System.currentTimeMillis();
		if ((currentTime - lastUpdateTime) > (staleInterval * 1000)) {
			stale = true;
		} else {
			stale = false;
		}
		return stale;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.StaleSupport#clearStale()
	 */
	@Override
	public void clearStale() {
		stale = false;
		lastUpdateTime = System.currentTimeMillis();
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.StaleSupport#isStale()
	 */
	@Override
	public boolean isStale() {
		return stale;
	}

	/**
	 * Sets the flag indicating whether this channel field should be 
	 * highlighted for alarms.
	 * 
	 * @param ah true to enable alarm highlighting, false to disable
	 */
	public void setAlarmHighlight(final boolean ah) {
		useAlarmHighLight = ah;
	}

	/**
	 * Gets the flag indicating whether this channel field should be 
	 * highlighted for alarms.
	 * 
	 * @return true if alarm highlighting is enabled, false if disabled
	 */
	public boolean isAlarmHighlight() {
		return useAlarmHighLight;
	}

	/**
	 * Sets the text value for this field from its associated 
	 * ChannelDefinition. This applies only to static channel fields.
	 */
	public void setTextFromChannelDefinition() {
		if (def == null) {
			setText(getNoDataIndicator());
			return;
		}
		switch (channelFieldType) {
		case ID:
			if (format == null) {
				setText(channelId);
			} else {
				setText(printFormatter.anCsprintf(format, channelId));
			}
			break;
		case TITLE:
			if (def.getTitle() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getTitle());
				} else {
					setText(printFormatter.anCsprintf(format, def.getTitle()));
				}
			}
			break;
		case FSW_NAME:
			if (def.getName() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getName());
				} else {
					setText(printFormatter.anCsprintf(format, def.getName()));
				}
			}
			break;
		case MODULE:
			if (def.getModule() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getModule());
				} else {
					setText(printFormatter
							.anCsprintf(format, def.getModule()));
				}
			}
			break;
		case SUBSYSTEM:
			if (def.getSubsystem() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getSubsystem());
				} else {
					setText(printFormatter
							.anCsprintf(format, def.getSubsystem()));
				}
			}
			break;
		case OPS_CAT:
			if (def.getOpsCategory() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getOpsCategory());
				} else {
					setText(printFormatter.anCsprintf(format, def
							.getOpsCategory()));
				}
			}
			break;
		case DN_UNIT:
			if (def.getDnUnits() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getDnUnits());
				} else {
					setText(printFormatter.anCsprintf(
							format, def.getDnUnits()));
				}
			}
			break;
		case EU_UNIT:
			if (def.getEuUnits() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(def.getEuUnits());
				} else {
					setText(printFormatter.anCsprintf(
							format, def.getEuUnits()));
				}
			}
			break;
		}
	}

	/**
	 * Sets the text for this field based upon a received channel value, and
	 * performs alarm highlighting of both dynamic and static channel fields.
	 * 
	 * @param val the received telemetry point, as a ChannelSample object
	 */
	public void setTextFromChannelValue(final MonitorChannelSample val) {
		if (isStaticField) {
			return;
		}
		if (def == null || val == null) {
			setText(getNoDataIndicator());
			return;
		}
		data = val;
		lastUpdateTime = System.currentTimeMillis();
		stale = false;

		switch (channelFieldType) {
		case DN:
		case RAW:
			if (val.getDnValue() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(val.getDnValue().getFormattedValue(formatUtil,
							def.getDnFormat()));
				} else {
					setText(val.getDnValue().getFormattedValue(formatUtil, format));
				}
			}
			break;
		case EU:
			if (val.getEuValue() == null || !def.hasEu()) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText( val.getEuValue().getFormattedValue(formatUtil,
							def.getEuFormat()));
				} else {
					setText(val.getEuValue().getFormattedValue(formatUtil, format));
				}
			}
			break;
		case STATUS:
			if (def.getChannelType().equals(ChannelType.STATUS) || 
					def.getChannelType().equals(ChannelType.BOOLEAN)) {

				if (val.getEuValue() == null) {
					setText(getNoDataIndicator());
				} else {
					if (format == null) {
						setText(val.getEuValue().getStringValue());
					} else {
						setText(val.getEuValue().getFormattedValue(formatUtil, format));
					}
				}
			}

			break;
		case VALUE:
			if (def.hasEu() || 
					def.getChannelType().equals(ChannelType.STATUS) || 
					def.getChannelType().equals(ChannelType.BOOLEAN)) {
				if (val.getEuValue() == null) {
					setText(getNoDataIndicator());
				} else {
					if (format == null) {
						setText(val.getEuValue()
								.getFormattedValue(formatUtil, def.getEuFormat()));
					} else {
						setText(val.getEuValue().getFormattedValue(formatUtil, format));
					}
				}
			} else {
				if (val.getDnValue() == null) {
					setText(getNoDataIndicator());
				} else {
					if (format == null) {
						setText(val.getDnValue()
								.getFormattedValue(formatUtil, def.getDnFormat()));
					} else {
						setText(val.getDnValue().getFormattedValue(formatUtil, format));
					}
				}
			}
			break;
		case ALARM_STATE:
			final String state = getAlarmState(val);
			setText(format == null ? state : printFormatter.anCsprintf(
					format,state));
			break;
		case ERT:
		case MST:
			if (val.getErt() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(defaultTimeFormat.format(val.getErt()));
				} else {
					setText(val.getErt().formatCustom(format));
				}
			}
			break;
		case SCLK:
			if (val.getSclk() == null) {
				setText(getNoDataIndicator());
			} else {
			    final String str = formatSclk(val);
				setText(format == null ? str
						: printFormatter.anCsprintf(
								format, str));
			}
			break;
		case RCT:

			if (val.getRct() == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(defaultTimeFormat.format(val.getRct()));
				} else {
					if (userTimeFormat == null) {
						userTimeFormat = new SimpleDateFormat(format);
					}
					setText(userTimeFormat.format(val.getRct()));
				}
			}
			break;
		case SCET:
			final IAccurateDateTime scetDate = val.getScet();
			if (scetDate == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(scetDate.getFormattedScet(true));
				} else {
					setText(scetDate.formatCustom(format));
				}
			}
			break;
		case LST:
			final ILocalSolarTime solDate = val.getSol();
			if (solDate == null) {
				setText(getNoDataIndicator());
			} else {
				if (format == null) {
					setText(solDate.getFormattedSol(true));
				} else {
					setText(solDate.formatCustom(format));
				}
			}
			break;
		case DSS_ID:
			final String station = data.getDssId() != 
			StationIdHolder.UNSPECIFIED_VALUE ? 
					String.valueOf(data.getDssId()) : 
						DisplayConstants.UNSPECIFIED_STATION;
					setText(station);

					break;
		case RECORDED:
			final String recorded = String.valueOf(!data.isRealtime());
			setText(recorded);
			break;
		}

		// If using alarm highlight then change the background color to the
		// alarm level color:
		if (useAlarmHighLight) {
			setAlarmColors(val);
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#draw(org.eclipse.swt.graphics.GC)
	 */
	@Override
	public void draw(final GC gc) {

		if(!displayMe  && 
				this.getFieldConfiguration().getCondition() != null) {
			return;
		}

		saveGcSettings(gc);

		final int x = getXCoordinate(startPoint.getX(), gc);
		final int y = getYCoordinate(startPoint.getY(), gc);

		// Set suspect font if channel is suspect
		if(isSuspectChannel) {
			if (suspectFont == null) {
				if (font == null) {
					suspectFont = gc.getFont();
					final FontData[] fd = suspectFont.getFontData();
					fd[0].setStyle(SWT.ITALIC);
					suspectFont = new Font(parent.getDisplay(), fd[0]);				
				} else {
					final FontData[] fd = font.getFontData();
					fd[0].setStyle(SWT.ITALIC);
					suspectFont = new Font(parent.getDisplay(), fd[0]);
				}
			}

			gc.setFont(suspectFont);

		} else if (font != null) {
			gc.setFont(font);
		}

		if (foreground != null) {
			gc.setForeground(foreground);
		}

		if (background != null) {
			gc.setBackground(background);
		}

		// Fields in alarm are always drawn with alarm fg and bg, not reverse 
		// video or transparent, so that alarmed fields will always look the 
		// same
		if(reverse && !inAlarm) {
			final Color save = gc.getForeground();
			gc.setForeground(gc.getBackground());
			gc.setBackground(save);
		} 

		// Set stale marker if channel value is stale
		if (((StaleSupport)this).isStale()) {	
			gc.setAlpha(100);
		}

		//textExtent fixes Linux bug in which background color wouldn't 
		//fill same area as text
		gc.textExtent(text);

		// Draw the channel value
		gc.drawText(text, x, y, (transparent && !inAlarm) ? 
				SWT.DRAW_TRANSPARENT : SWT.NONE);

		final int wx = SWTUtilities.getFontCharacterWidth(gc) * text.length();
		final int wy = SWTUtilities.getFontCharacterHeight(gc);
		setLastBounds(x, y, x + wx, y + wy);

		// If channel is undefined in the dictionary, draw the undefined 
		// channel marker
		if (def == null) {
			gc.setAlpha(150);
			gc.drawLine(x, y, x + wx, y + wy);
			gc.drawLine(x, y + wy, x + wx, y);
		}

		restoreGcSettings(gc);
	}

	/**
	 * Sets the flag indicating the channel value is suspect.
	 * 
	 * @param isSuspect true if channel is suspect, false if not
	 */
	public void setSuspect(final boolean isSuspect) {
		this.isSuspectChannel = isSuspect;
	}

	/**
	 * Returns current suspect status of the channel element.
	 * 
	 * @return true if channel is suspect, false if not
	 */
	public boolean isSuspect() {
		return isSuspectChannel;
	}

	/**
	 * Sets the colors to the alarm level color, as appropriate for
	 * the given telemetry value.
	 * 
	 * @param val a received telemetry point, as a ChannelSample object
	 */
	public void setAlarmColors(final MonitorChannelSample val) {

		final AlarmLevel dnLevel = val.getDnAlarmLevel();
		final AlarmLevel euLevel = val.getEuAlarmLevel();

		if (dnLevel == AlarmLevel.RED || euLevel == AlarmLevel.RED) {
			setBackground(RED_COLOR);
			setForeground(WHITE_COLOR);
			inAlarm = true;
		} else if (dnLevel == AlarmLevel.YELLOW || 
				euLevel == AlarmLevel.YELLOW) {
			setBackground(YELLOW_COLOR);
			setForeground(BLACK_COLOR);
			inAlarm = true;
		} else {
			setBackground(originalBackground);
			setForeground(originalForeground);
			inAlarm = false;
		}

	}

	/**
	 * Removes alarm highlighting from this field.
	 */
	public void resetAlarmState() {
		foreground = originalForeground;
		background = originalBackground;
		inAlarm = false;
	}

	private String getAlarmState(final MonitorChannelSample val) {

		final AlarmLevel dnLevel = val.getDnAlarmLevel();
		final AlarmLevel euLevel = val.getEuAlarmLevel();
		String state = null;
		if (dnLevel != AlarmLevel.NONE) {
			state = "DN-" + val.getDnAlarmState();
		}
		if (euLevel != AlarmLevel.NONE) {
			if (state == null) {
				state = "EU-" + val.getEuAlarmState();
			} else {
				state = state + "," + "EU-" + val.getEuAlarmState();
			}
		}
		if (state == null) {
			state = "";
		}
		return state;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();
		final ChannelFieldConfiguration chanConfig = 
				(ChannelFieldConfiguration) fieldConfig;
		this.setChannelFieldType(chanConfig.getFieldType());
		this.setChannelId(chanConfig.getChannelId());
		transparent = chanConfig.isTransparent();
		if (defProv != null && chanConfig.getChannelId() != null
				&& !chanConfig.getChannelId().equals("null")) {
			def = defProv.getDefinitionFromChannelId(channelId);
		}
		final ChillColor foreground_cc = chanConfig.getForeground();
		final ChillColor background_cc = chanConfig.getBackground();
		if (foreground_cc != null) {
			if (foreground != null && !foreground.isDisposed()) {
				foreground.dispose();
				foreground = null;
			}
			foreground = ChillColorCreator.getColor(foreground_cc);
			originalForeground = foreground;
		}

		if (background_cc != null) {
			if (background != null && !background.isDisposed()) {
				background.dispose();
				background = null;
			}
			background = ChillColorCreator.getColor(background_cc);
			originalBackground = background;
		}

		if (chanConfig.getFont() != null) {
			final ChillFont chillFont = chanConfig.getFont();
			if (font != null && !font.isDisposed()) {
				font.dispose();
				font = null;
			}
			font = ChillFontCreator.getFont(chillFont);
			if (suspectFont != null && !suspectFont.isDisposed()) {
				suspectFont.dispose();
				suspectFont = null;
			}
			suspectFont = ChillFontCreator.getFont(new ChillFont(
					chillFont.getFace(), chillFont.getSize(), SWT.ITALIC));
			reverse = chanConfig.getFont().getReverseFlag();
		}

		format = chanConfig.getFormat();
		useAlarmHighLight = chanConfig.isUseAlarmHighlight();
		if(channelFieldType.isStaticField()) {
			setTextFromChannelDefinition();
		} else {
			setText(getNoDataIndicator());
		}
	}

	/**
	 * Sets the channel definition associated with this channel element.
	 * 
	 * @param def the ChannelDefinition to set
	 */
	public void setChannelDefinition(final IChannelDefinition def) {
		this.def = def;
	}

	/**
	 * Gets the channel definition associated with this channel element.
	 * 
	 * @return the ChannelDefinition
	 */
	public IChannelDefinition getChannelDefinition() {
		return this.def;
	}


	/**
	 * Gets the data item (channel sample) associated with this channel element.
	 * @return  MonitorChannelSample
	 */
	public MonitorChannelSample getData() {
	    return this.data;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#getNoDataIndicator()
	 */
	@Override
	public String getNoDataIndicator() {

		if (channelFieldType.equals(ChannelFieldType.ID)) {
			return channelId;
		}

		String str = NO_DATA;

		switch (channelFieldType) {
		case DN:
		case RAW:
			str = getRawNoDataIndicator();
			break;
		case STATUS:
		case ALARM_STATE:
			if (format != null) {
				final ParsedFormatter formatter = 
						ParsedFormatter.parseStringFormatter(format);
				if (formatter != null) {
					str = formatAndMakeDashes(formatter, str);
				}
			}
			break;
		case EU:
			if (format != null) {
				final ParsedFormatter formatter = 
						ParsedFormatter.parseFloatFormatter(format);
				if (formatter != null) {
					str = formatAndMakeDashes(formatter, Double.valueOf(1.1));
				}
			}
			break;
		case VALUE:
			if (format != null) {
				if (def != null && def.hasEu()) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseFloatFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(
								formatter, Double.valueOf(1.1));
					}
				} else if (def != null && 
						def.getChannelType().equals(ChannelType.STATUS)) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseStringFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(formatter, str);
					}
				} else {
					str = getRawNoDataIndicator();
				}
			}
			break;
		case SCLK:
		    final ISclk sclk = new Sclk(0, 0);
		    str = formatSclk(sclk);
		    
		    if(format == null) {
		        format = AbstractTextElement.DEFAULT_SCLK_FORMAT;
		    }
		    else {
		        final ParsedFormatter formatter = 
						ParsedFormatter.parseStringFormatter(format);
				if (formatter != null) {
					str = formatAndMakeDashes(formatter, str);
				}
			}
		break;
		case ERT:
		case MST:
			if (format == null) {
				format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			}
			else {
				str = new AccurateDateTime().formatCustom(format);
				final int len = str.length();
				final StringBuilder nd = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					final char c = str.charAt(i);
					if (Character.isDigit(c)) {
						nd.append('-');
					} else {
						nd.append(c);
					}
				}
				str = nd.toString();
			}
			break;
		case SCET:
			if (format == null) {
				format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			}
			else {
				str = new AccurateDateTime().formatCustom(format);
				final int len = str.length();
				final StringBuilder nd = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					final char c = str.charAt(i);
					if (Character.isDigit(c)) {
						nd.append('-');
					} else {
						nd.append(c);
					}
				}
				str = nd.toString();
			}
			break;

		case LST:	
			if (format == null) {
				format = AbstractTextElement.DEFAULT_LST_FORMAT;
			}	
			else {
				str = LocalSolarTimeFactory.getNewLst(scid).formatCustom(format);
				final int len = str.length();
				final StringBuilder nd = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					final char c = str.charAt(i);
					if (Character.isDigit(c)) {
						nd.append('-');
					} else {
						nd.append(c);
					}
				}
				str = nd.toString();
			}
			break;

		case RCT:
			if (format == null) {
				format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			} else {
				if (userTimeFormat == null) {
					userTimeFormat = new SimpleDateFormat(format);
				}
				str = userTimeFormat.format(new AccurateDateTime());
				final int len = str.length();
				final StringBuilder nd = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					final char c = str.charAt(i);
					if (Character.isDigit(c)) {
						nd.append('-');
					} else {
						nd.append(c);
					}
				}
				str = nd.toString();
			}
			break;

		default:
			if (format != null) {
				str = printFormatter.anCsprintf(format, str);
			}
		}

		noDataIndicator = str;
		return noDataIndicator;
	}

	private String formatAndMakeDashes(
			final ParsedFormatter formatter, final Object val) {
		final String percentOnly = formatter.getFormatStringOnly();
		final StringBuilder result = new StringBuilder();
		final String prefix = formatter.getPrefix();
		if (prefix != null) {
			result.append(prefix);
		}
		final String formattedVal = printFormatter.anCsprintf(percentOnly, val);
		final int len = formattedVal.length();
		for (int i = 0; i < len; i++) {
			final char c = formattedVal.charAt(i);
			if (Character.isDigit(c) || Character.isWhitespace(c)) {
				result.append('-');
			} else {
				result.append(c);
			}
		}
		final String suffix = formatter.getSuffix();
		if (suffix != null) {
			result.append(formatter.getSuffix());
		}
		return result.toString();
	}

	private String getRawNoDataIndicator() {
		String str = NO_DATA;
		if (def != null) {
			switch (def.getChannelType()) {
			case ASCII:
				if (format != null) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseStringFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(formatter, str);
					}
				}
				break;
			case FLOAT:
				if (format != null) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseFloatFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(
								formatter, Double.valueOf(0.1));
					}
				}
				break;
			case SIGNED_INT:
			case STATUS:
				if (format != null) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseIntFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(formatter, Long.valueOf(1));
					}
				}
				break;
			case UNSIGNED_INT:
			case DIGITAL:
			case BOOLEAN:
			case TIME:
				if (format != null) {
					final ParsedFormatter formatter = 
							ParsedFormatter.parseUnsignedIntFormatter(format);
					if (formatter != null) {
						str = formatAndMakeDashes(formatter, Long.valueOf(1));
					}
				}
				break;				
			}
		}
		return str;
	}
	
	private String formatSclk(final MonitorChannelSample val) {
	    return formatSclk(val.getSclk());
	}
	
    private String formatSclk(final ISclk sclk) {
        if (sclkFmt == null) {
            sclkFmt = TimeProperties.getInstance().getSclkFormatter();
        }
    
        if ((configVals != null && configVals.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL)) ||
                sclkFmt.getUseFractional()) {
            return sclkFmt.toDecimalString(sclk);
        } else {
            return sclk.toTicksString();
        }
        
    }
}
