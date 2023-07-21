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
import java.util.Date;

import org.eclipse.swt.widgets.Canvas;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.monitor.canvas.support.StaleSupport;
import jpl.gds.monitor.canvas.support.TimeSupport;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.perspective.view.channel.LatestChannelTimes;
import jpl.gds.monitor.perspective.view.fixed.FixedFieldType;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.fields.TimeFieldConfiguration.SourceTimeType;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.time.TimeUtility;

/**
 * The TimeElement represents a current time field. The type of time it 
 * displays is configurable. The fields are updated periodically based upon 
 * last received telemetry.
 * 
 */
public class TimeElement extends AbstractTextElement implements 
TimeSupport, StaleSupport
{
    
	private static final int SELECTION_PRIORITY = 1;

	private final DateFormat defaultTimeFormat = TimeUtility.getIsoFormatter();
	private DateFormat userTimeFormat;

	private TimeFieldConfiguration.SourceTimeType timeType;
	private LatestChannelTimes times;
	private boolean stale;
	private long lastUpdateTime = 0;
	private final MonitorConfigValues configVals;
	private final Integer scid;
    private final SclkFormatter sclkFmt;
	

	/**
	 * Creates a new TimeElement with the given canvas as parent.
	 * @param appContext the current application context
	 * 
	 * @param parent Canvas widget
	 */
	public TimeElement(final ApplicationContext appContext, final Canvas parent)
	{
		super(parent, FixedFieldType.TIME);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();
		MonitorConfigValues temp = null;
		try {
		    temp = appContext.getBean(MonitorConfigValues.class);
		} catch (final BeansException e) {
		    // ok not to have config values
		}
		configVals = temp;
		this.setSelectionPriority(SELECTION_PRIORITY);
		setText(getNoDataIndicator());
		scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        printFormatter = new SprintfFormat(scid);
	}

	/**
	 * Creates a new TimeElement with the given canvas as parent and the given
	 * fixed field configuration.
	 * @param appContext the current application context
	 * @param parent Canvas widget
	 * @param timeConfig the TimeFieldConfiguration object that configures this
	 *            element
	 */
	public TimeElement(final ApplicationContext appContext, final Canvas parent, final TimeFieldConfiguration timeConfig)
	{
		super(parent, timeConfig);
	    sclkFmt = TimeProperties.getInstance().getSclkFormatter();
		MonitorConfigValues temp = null;
		try {
            temp = appContext.getBean(MonitorConfigValues.class);
        } catch (final BeansException e) {
            // ok not to have config values
        }
        configVals = temp;
        scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
		updateFieldsFromConfig();
		setSelectionPriority(SELECTION_PRIORITY);
		setText(getNoDataIndicator());
	}

	/**
	 * Sets the C-printf style or java date/time formatter for the text drawn by
	 * this object.
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
	 * Retrieves the time type (SCLK, SCET, etc) displayed for this element.
	 * 
	 * @return SourceTimeType
	 */
	public TimeFieldConfiguration.SourceTimeType getTimeType()
	{
		return timeType;
	}

	/**
	 * Sets the time type (SCLK, SCET, etc) displayed for this element.
	 * 
	 * @param timeType SourceTimeType
	 */
	public void setTimeType(final TimeFieldConfiguration.SourceTimeType timeType)
	{
		this.timeType = timeType;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.support.StaleSupport#checkStale(int)
	 */
	@Override
    public boolean checkStale(final int staleInterval) {
		if (isStaticField || lastUpdateTime == 0 || 
		        timeType.equals(SourceTimeType.UTC)) {
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
	 * Sets the text value of this time field from the given latest times.
	 * 
	 * @param times the LatestTimes objects to get data from
	 */
	public void setTextFromLatestTimes(final LatestChannelTimes times)
	{
		this.times = times;

		if (isStaticField) {
			return;
		}

		if (times == null) {
			setText(getNoDataIndicator());
			return;
		}

		if (!timeType.equals(SourceTimeType.UTC)) {
			lastUpdateTime = System.currentTimeMillis();
			stale = false;
		}

		switch(timeType)
		{
		case ERT:
			if(times.getLatestErt() != null)
			{
				setFormattedDate(times.getLatestErt());
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case MST:
			if(times.getLatestMst() != null)
			{
				setFormattedDate(times.getLatestMst());
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case SCLK:
			if(times.getLatestSclk() != null)
			{	                         
				if (format == null) {
					setText(times.getLatestSclk().toString());
				} else {
					setText(printFormatter.anCsprintf(format, 
					        times.getLatestSclk().toString()));
				}
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case SCET:
			if(times.getLatestScet() != null)
			{
				setFormattedDate(times.getLatestScet());
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case LST:
			if(times.getLatestSol() != null)
			{
			    if(format == null) {
			        setText(times.getLatestSol().getFormattedSol(true));
			    }
			    else {
			        setText(times.getLatestSol().formatCustom(format));
			    }
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case RCT:
			if (times.getLatestRct() != null)
			{
				setFormattedDate(times.getLatestRct());
			}
			else
			{
				setText(getNoDataIndicator());
			}
			break;
		case  UTC:
			setFormattedDate(times.getLatestUtcAsDate());
			break;
		}
	}

	private void setFormattedDate(final Date d) {
		if (d != null) {
			if (format == null) {
				setText(defaultTimeFormat.format(d));
			} else {
				if (userTimeFormat == null) {
					userTimeFormat = new SimpleDateFormat(format);
				}
				setText(userTimeFormat.format(d));
			}
		} else {
			setText(getNoDataIndicator());
		}
	}
	
	private void setFormattedDate(final IAccurateDateTime d) {
		if (d != null) {
			if (format == null) {
				setText(defaultTimeFormat.format(d));
			} else {
				setText(d.formatCustom(format));
			}
		} else {
			setText(getNoDataIndicator());
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#getFormattedText()
	 */
	@Override
	public String getFormattedText() {
		if (isEditMode) {
			return getNoDataIndicator();
		} else {
			return super.getText();
		}
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#updateFieldsFromConfig()
	 */
	@Override
	protected void updateFieldsFromConfig() {
		super.updateFieldsFromConfig();

		final TimeFieldConfiguration timeConfig = (TimeFieldConfiguration)fieldConfig;
		this.setTimeType(timeConfig.getTimeType());
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.AbstractTextElement#getNoDataIndicator()
	 */
	@Override
	public String getNoDataIndicator() {
		
		String str = "---";

		switch (timeType) {
		case SCLK:

		    final ISclk sclk = new Sclk(0, 0);
		    str = formatSclk(sclk);

			if (format == null) {
                format = AbstractTextElement.DEFAULT_SCLK_FORMAT;
            }
			str = printFormatter.anCsprintf(format, str);
			break;
		case ERT:			
		case MST:
			if (format == null) {
			    format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			}
			str = new AccurateDateTime().formatCustom(format);
			break;
		case SCET:
			if (format == null) {
			    format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			}
			str = new AccurateDateTime().formatCustom(format);
			break;
		case LST:
			if (format == null) {
			    format = AbstractTextElement.DEFAULT_LST_FORMAT;
			}
			str = LocalSolarTimeFactory.getNewLst(scid).formatCustom(format);
			break;
		case RCT:
		case UTC:
			if (format == null) {
			    format = AbstractTextElement.DEFAULT_TIME_FORMAT;
			}
			if (userTimeFormat == null) {
				userTimeFormat = new SimpleDateFormat(format);
			}
			str = userTimeFormat.format(new AccurateDateTime());
			break;
		}

		final int len = str.length();
		final StringBuilder nd = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			final char c = str.charAt(i);
			if (Character.isDigit(c) || Character.isWhitespace(c)) {
				nd.append('-');
			} else {
				nd.append(c);
			}
		}
		noDataIndicator = nd.toString();

		return noDataIndicator;
	}

	/**
	 * Returns a formatted String containing the time
	 * 
	 * @return String time
	 */
	public String getTimeText()
	{
		if (times == null) {
			return "";
		}
		switch(timeType)
		{
		case ERT:
			if(times.getLatestErt() != null)
			{
				return "ERT: " + times.getLatestErt().getFormattedErt(true);
			}
			else
			{
				return "ERT: " + getNoDataIndicator();
			}
		case MST:
			if(times.getLatestMst() != null)
			{
				return "MST: " + times.getLatestMst().getFormattedErt(true);
			}
			else
			{
				return "MST: " + getNoDataIndicator();
			}
		case SCLK:
			if(times.getLatestSclk() != null)
			{
			    final String str = formatSclk(times.getLatestSclk());
			    return "SCLK: " + str;
			}
			else
			{
				return "SCLK: " + getNoDataIndicator();
			}
		case SCET:
			if(times.getLatestScet() != null)
			{
				return "SCET: " + times.getLatestScet().getFormattedScet(true);
			}
			else
			{
				return "SCET: " + getNoDataIndicator();
			}
		case LST:
			if(times.getLatestSol() != null)
			{
				return "LST: " + times.getLatestSol().getFormattedSol(true);
			}
			else
			{
				return "LST: " + getNoDataIndicator();
			}
		case RCT:
			if (times.getLatestRct() != null)
			{
				return "RCT: " + defaultTimeFormat.format(
				        times.getLatestRct());
			}
			else
			{
				return "RCT: " + getNoDataIndicator();
			}
		case  UTC:
			return "UTC: " + defaultTimeFormat.format(
			        times.getLatestUtcAsDate());

		default:
			return "";
		}
	}

    private String formatSclk(final ISclk sclk) {
	    if ((configVals != null && configVals.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL)) ||
	            sclkFmt.getUseFractional()) {
	        return sclkFmt.toDecimalString(sclk);
	    } else {
	        return sclk.toTicksString();
	    }

	}
}
