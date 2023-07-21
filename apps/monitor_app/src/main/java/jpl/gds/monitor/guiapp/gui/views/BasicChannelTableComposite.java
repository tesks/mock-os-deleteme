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
/**
 * 
 */
package jpl.gds.monitor.guiapp.gui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.perspective.view.ChannelListViewConfiguration;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;


/**
 * The BasicChannelTableComposite class is an extension of the abstract table composite
 * for use by channel list views and channel history windows.
 */
public class BasicChannelTableComposite extends AbstractBasicTableComposite implements ViewConfigurationListener{

    private final MonitorConfigValues globalConfig;

	/**
	 * Mapping of channel IDs to table items
	 */
	protected final Map<String, ArrayList<TableItem>> tableItems = new HashMap<String,ArrayList<TableItem>>();

	private final SclkFormatter sclkFmt;
	
	/** Flag indicating whether to display LST times */
	protected boolean useSolTimes;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 * @param parent parent Composite for this table
	 * @param config ChannelListViewConfiguration object for the parent view
	 */
	public BasicChannelTableComposite(final ApplicationContext appContext, final Composite parent, final ChannelListViewConfiguration config) {
		super (appContext, parent, config);
		
		globalConfig = appContext.getBean(MonitorConfigValues.class);
		sclkFmt = TimeProperties.getInstance().getSclkFormatter();

		tableDef = viewConfig.getTable(ChannelListViewConfiguration.CHANNEL_TABLE_NAME);
		
		useSolTimes = appContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		
		createControls();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#itemHasDefinition(org.eclipse.swt.widgets.TableItem)
	 */
	@Override
	public boolean itemHasDefinition(final TableItem item) {
		final IChannelDefinition def = (IChannelDefinition)item.getData(DEFINITION);
		final ChannelDisplayFormat characteristics = (ChannelDisplayFormat)item.getData(CHARACTERISTICS);
		if (characteristics.isSeparator() || def == null) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the unique list of channel IDs currently in the table.
	 * 
	 * @return list of strings
	 */
	public List<String> getChannelIds() {
		final Set<String> ids = tableItems.keySet();
		return new ArrayList<String>(ids);
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#addChannelValue(jpl.gds.dictionary.impl.impl.api.channel.IChannelDefinition, jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat, jpl.gds.monitor.perspective.view.channel.MonitorChannelSample)
	 */
	@Override
	public void addChannelValue(final IChannelDefinition def, final ChannelDisplayFormat displayStuff, final MonitorChannelSample data) {
		final String time = timeFormat.format(data.getErt());
		String state =  null;

        ISclk sclkDate = data.getSclk();
		if (null == sclkDate) {
			sclkDate = new Sclk(0);
		}

		final String sclk = globalConfig.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL) ?
		        sclkFmt.toDecimalString(sclkDate) : sclkFmt.toTicksString(sclkDate);
		final IAccurateDateTime scetDate = data.getScet();
		final ILocalSolarTime solDate = data.getSol();
		final String recorded = String.valueOf(!data.isRealtime());
		final String dssId = data.getDssId() != StationIdHolder.UNSPECIFIED_VALUE ? String.valueOf(data.getDssId()) :
			DisplayConstants.UNSPECIFIED_STATION;

		final String dn = data.getDnValue().getFormattedValue(formatUtil, displayStuff.getRawFormat());
		String eu = "";
		if (data.getEuValue() != null) {
			eu = data.getEuValue().getFormattedValue(formatUtil, displayStuff.getValueFormat());
		}

		final TableItem item = new TableItem(table, SWT.NONE);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN), dn);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN), eu);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ERT_COLUMN), time);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCLK_COLUMN), sclk);
		item.setData("sclk",  data.getSclk());
		if (scetDate != null && tableDef.isColumnEnabled(tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCET_COLUMN))) {
			final String scetStr = scetDate.getFormattedScet(true);
			setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCET_COLUMN), scetStr);
		}
		if (useSolTimes && tableDef.isColumnEnabled(tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN))) {
		    String solStr;
		    if (solDate != null) {
		        solStr = solDate.getFormattedSol(true);
		    } else {
		        solStr = LocalSolarTimeFactory.getNewLst(0).getFormattedSol(true);
		    }
		    setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN), solStr);				
		}
		
		item.setData(DEFINITION, def);
		item.setData(CHARACTERISTICS, displayStuff);
		final AlarmLevel dnLevel = data.getDnAlarmLevel();
		final AlarmLevel euLevel = data.getEuAlarmLevel();
		if (dnLevel == AlarmLevel.NONE && euLevel == AlarmLevel.NONE) {
			item.setData("AlarmLevel", null);
		} else {
			state = getAlarmState(data);
			final AlarmLevel worstLevel = euLevel.ordinal() > dnLevel.ordinal() ? euLevel : dnLevel;
			setAlarmColors(item, dnLevel, 
					tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN));
			setAlarmColors(item, euLevel, 
					tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN));
			setAlarmColors(item, worstLevel, 
					tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN));
			item.setData("AlarmLevel", worstLevel);
		}

		final String chan = def.getId();
		final String title = def.getTitle();
		final String fswName = def.getName();
		final String module = def.getModule();
		final String dnUnits = def.getDnUnits();
		final String euUnits = def.hasEu() ? def.getEuUnits() : def.getDnUnits();
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ID_COLUMN), chan);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_TITLE_COLUMN), title);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_FSW_COLUMN), fswName == null ? "" : fswName);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_UNITS_COLUMN), dnUnits);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_UNITS_COLUMN), euUnits);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_MODULE_COLUMN), module == null ? "" : module);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN), state);
		setColumn(item, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DSS_COLUMN), dssId);
		setColumn(item, this.tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_RECORDED_COLUMN), recorded);

		ArrayList<TableItem> curList = tableItems.get(chan);		
		if (curList == null) {
			curList = new ArrayList<TableItem>(1);
			tableItems.put(chan, curList);
		}	
		curList.add(item);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#replaceTableItems(org.eclipse.swt.widgets.TableItem[])
	 */
	@Override
	protected void replaceTableItems(final TableItem[] items) {

		final String[] values = new String[tableDef.getActualColumnCount()];
		final Color[] fgColors = new Color[tableDef.getActualColumnCount()];
		final Color[] bgColors = new Color[tableDef.getActualColumnCount()];
		final int dnValIndex = tableDef.getActualIndex(tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN));
		final int euValIndex = tableDef.getActualIndex(tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN));

		ArrayList<TableItem> newSelected = null;

		for (int i = 0; i < items.length; i++) {
			Boolean inAlarm = false;
			final Boolean suspect = false;
			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				values[valIndex] = items[i].getText(valIndex);
				fgColors[valIndex] = items[i].getForeground(valIndex);
				bgColors[valIndex] = items[i].getBackground(valIndex);
			}
			final IChannelDefinition def = (IChannelDefinition)items[i].getData(DEFINITION);
			final ChannelDisplayFormat characteristics = (ChannelDisplayFormat)items[i].getData(CHARACTERISTICS);

			final ISclk sclk = (ISclk)items[i].getData("sclk");
			final AlarmLevel alarmLevel = (AlarmLevel)items[i].getData("alarmLevel");
			inAlarm = alarmLevel != null && !alarmLevel.equals(AlarmLevel.NONE);

			ArrayList<TableItem> curList = null;
			if (def != null) {
				curList = tableItems.get(def.getId());
				curList.remove(items[i]);
			}

			final int oldIndex = table.indexOf(items[i]);
			final boolean selected = table.isSelected(oldIndex);

			items[i].dispose();
			items[i] = null;

			final TableItem item = new TableItem(table, SWT.NONE, i);
			item.setData(DEFINITION, def);
			item.setData(CHARACTERISTICS, characteristics);
			item.setData("alarmLevel", alarmLevel);

			if (selected) {
				if (newSelected == null) {
					newSelected = new ArrayList<TableItem>(1);
				}
				newSelected.add(item);
			}

			if ((def != null) && (curList != null)) {
				curList.add(item);
			}

			if (sclk != null) {
				item.setData("sclk", sclk);
			}
			if(characteristics.isSeparator()) {
				if (characteristics.isLine()) {
					fillWithLine(characteristics.getSeparatorString(), item);
				} else {
					item.setText(0, characteristics.getSeparatorString());
				}
				item.setFont(dataFont);
				item.setForeground(null);
				item.setBackground(null);
			} else {
				for (int valIndex = 0; valIndex < values.length; valIndex++) {
					item.setText(valIndex,values[valIndex]);
					if (inAlarm && (valIndex == dnValIndex || valIndex == euValIndex)) {
						item.setForeground(valIndex,fgColors[valIndex]);
						item.setBackground(valIndex,bgColors[valIndex]);
					} else {
						item.setForeground(valIndex, null);
						item.setBackground(valIndex, null);
					}
					if(suspect != null && suspect.booleanValue())
					{
						item.setFont(italicsFont);
					}
					else
					{
						item.setFont(plainFont);
					}
				} 
			}
		}
		if (newSelected != null) {
			final TableItem[] selectedItems = new TableItem[newSelected.size()];	
			newSelected.toArray(selectedItems);
			table.setSelection(selectedItems);
		}
	}

	/**
	 * Clears all rows of data in the current table.
	 */
	public void clearTable() {
		final TableItem[] items = table.getItems();
		for (int i = 0; i < items.length; i++) {
			final ChannelDisplayFormat characteristics = (ChannelDisplayFormat)items[i].getData(CHARACTERISTICS);
			if (characteristics.isSeparator()) {
				continue;
			}

			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN), "");
			setAlarmColors(items[i], AlarmLevel.NONE, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DN_COLUMN));
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN), "");
			setAlarmColors(items[i], AlarmLevel.NONE, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_EU_COLUMN));
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ERT_COLUMN), "");
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCLK_COLUMN), "");
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN), "");
			setAlarmColors(items[i], AlarmLevel.NONE, tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_ALARM_COLUMN));
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_SCET_COLUMN), "");
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_LST_COLUMN), "");
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_RECORDED_COLUMN), "");
			setColumn(items[i], tableDef.getColumnIndex(ChannelListViewConfiguration.CHAN_DSS_COLUMN), "");
			items[i].setData("alarmLevel", null);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#clearRows()
	 */
	@Override
	public void clearRows() {
		tableItems.clear();
		table.removeAll();
	}

	/**
	 * Updates the fonts on separator rows.
	 */
	public void updateSeparatorFonts() {
		final int count = table.getItemCount();
		for (int i = 0; i < count; i++) {
			final TableItem item = table.getItem(i);
			final ChannelDisplayFormat display = (ChannelDisplayFormat)item.getData(CHARACTERISTICS);
			if (display.isSeparator()) {
				item.setFont(dataFont);
			}
		}      
	}

}
