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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
import jpl.gds.monitor.perspective.view.FastAlarmViewConfiguration;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;
import jpl.gds.perspective.ChillTableColumn;
import jpl.gds.perspective.ChillTableColumn.SortType;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.swt.TableItemQuickSort;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;


/**
 * The BasicFastAlarmTableComposite class represents the table of channels used in Fast Alarm views
 * and alarm history windows.
 */
public class BasicFastAlarmTableComposite extends AbstractBasicTableComposite implements ViewConfigurationListener {
    
    private final MonitorConfigValues globalConfig;

	/**
	 * Mapping of channel IDs to table items
	 */
	protected final Map<String, TableItem> tableItems = new HashMap<String,TableItem>();

	private final boolean useSolTimes;

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 * @param parent parent Composite for this table
	 * @param config FastAlarmViewConfiguration object for the parent view
	 */
	public BasicFastAlarmTableComposite(final ApplicationContext appContext, final Composite parent, final FastAlarmViewConfiguration config) {
		super (appContext, parent, config);

		globalConfig = appContext.getBean(MonitorConfigValues.class);
		
		tableDef = viewConfig.getTable(FastAlarmViewConfiguration.ALARM_TABLE_NAME);
		
		useSolTimes = appContext.getBean(EnableLstContextFlag.class).isLstEnabled();
		
		createControls();
	}	

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#addChannelValue(jpl.gds.dictionary.impl.impl.api.channel.IChannelDefinition, jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat, jpl.gds.monitor.perspective.view.channel.MonitorChannelSample)
	 */
	@Override
	public void addChannelValue(final IChannelDefinition def, final ChannelDisplayFormat displayStuff, final MonitorChannelSample data) {
		final String time = timeFormat.format(data.getErt());
		final String sclk = globalConfig.getValue(GlobalPerspectiveParameter.SCLK_FORMAT).equals(SclkFormat.DECIMAL) ?
                data.getSclk().toDecimalString() : data.getSclk().toTicksString();
		final IAccurateDateTime scetDate = data.getScet();
		final ILocalSolarTime solDate = data.getSol();

		final String dn = data.getDnValue().getFormattedValue(formatUtil, displayStuff.getRawFormat());
		String eu = "";
		if (data.getEuValue() != null) {
			eu = data.getEuValue().getFormattedValue(formatUtil, displayStuff.getValueFormat());
		}

		final TableItem item = new TableItem(table, SWT.NONE);
		setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_COLUMN), dn);
		setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_COLUMN), eu);
		setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ERT_COLUMN), time);
		setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_SCLK_COLUMN), sclk);
		item.setData("sclk",  data.getSclk());
		if (scetDate != null && tableDef.isColumnEnabled(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_SCET_COLUMN))) {
			final String scetStr = scetDate.getFormattedScet(true);
			setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_SCET_COLUMN), scetStr);
		}
		if (useSolTimes && tableDef.isColumnEnabled(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_LST_COLUMN))) {
		    if (solDate != null) {
		        final String solStr = solDate.getFormattedSol(true);
		        setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_LST_COLUMN), solStr);
		    } else {
		        setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_LST_COLUMN), LocalSolarTimeFactory.getNewLst(0).getFormattedSol(true));
		    }
		}


		item.setData(DEFINITION, def);
		item.setData(CHARACTERISTICS, displayStuff);

		final String chan = def.getId();
		final String title = def.getTitle();
		final String fswName = def.getName();
        final String module = def.getCategory(IChannelDefinition.MODULE);
		final String dnUnits = def.getDnUnits();
		final String euUnits = def.hasEu() ? def.getEuUnits() : def.getDnUnits();

		final String fullState = getAlarmState(data);

		final String station = data.getDssId() != 
				StationIdHolder.UNSPECIFIED_VALUE ? 
						String.valueOf(data.getDssId()) : 
							DisplayConstants.UNSPECIFIED_STATION;
						final String recorded = String.valueOf(!data.isRealtime());

						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ID_COLUMN), chan);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_TITLE_COLUMN), title);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_FSW_COLUMN), fswName == null ? "" : fswName);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_UNITS_COLUMN), dnUnits);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_UNITS_COLUMN), euUnits);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_MODULE_COLUMN), module == null ? "" : module);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_IN_ERT_COLUMN), "---");
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_OUT_ERT_COLUMN), "---");
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ALARM_COLUMN), fullState);

						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DSS_COLUMN), station);
						setColumn(item, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_RECORDED_COLUMN), recorded);

						//Set alarm colors
						highlightAlarmColumns(item, data);
	}

	/**
	 * Colors the data columns and alarm state columns approriately in the given table item to match
	 * the alarm state of the given channel sample.
	 * @param item TableItem to highlight
	 * @param data channel sample to look at for current alarm state
	 */
	protected void highlightAlarmColumns(final TableItem item, final MonitorChannelSample data) {
		setAlarmColors(item, data.getDnAlarmLevel(), tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_COLUMN));
		setAlarmColors(item, data.getEuAlarmLevel(), tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_COLUMN));

		AlarmLevel masterLevel = data.getDnAlarmLevel();
		if (data.getEuAlarmLevel().ordinal() > masterLevel.ordinal()) {
			masterLevel = data.getEuAlarmLevel();
		}

		setAlarmColors(item, masterLevel, tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ALARM_COLUMN));
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
		final int dnValIndex = tableDef.getActualIndex(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_DN_COLUMN));
		final int euValIndex = tableDef.getActualIndex(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_EU_COLUMN));
		final int stateIndex = tableDef.getActualIndex(tableDef.getColumnIndex(FastAlarmViewConfiguration.ALARM_ALARM_COLUMN));

		ArrayList<TableItem> newSelected = null;

		for (int i = 0; i < items.length; i++) {
			Boolean inAlarm = false;
			final Boolean suspect = false;
			Long clearTime = null;

			//Store current values for a record (text, fg/bg color, inAlarm, suspect)
			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				values[valIndex] = items[i].getText(valIndex);
				clearTime = ((Long)items[i].getData("clearTime"));
				fgColors[valIndex] = items[i].getForeground(valIndex);
				bgColors[valIndex] = items[i].getBackground(valIndex);   
			}
			final IChannelDefinition def = (IChannelDefinition)items[i].getData(DEFINITION);
			final ChannelDisplayFormat characteristics = (ChannelDisplayFormat)items[i].getData(CHARACTERISTICS);

			final ISclk sclk = (ISclk)items[i].getData("sclk");
			final AlarmLevel alarmLevel = (AlarmLevel)items[i].getData("alarmLevel");
			inAlarm = alarmLevel != null && !alarmLevel.equals(AlarmLevel.NONE);

			if (def != null) {
				tableItems.remove(def.getId());
			}

			final int oldIndex = table.indexOf(items[i]);
			final boolean selected = table.isSelected(oldIndex);

			items[i].dispose();
			items[i] = null;

			final TableItem item = new TableItem(table, SWT.NONE, i);
			item.setData(DEFINITION, def);
			item.setData(CHARACTERISTICS, characteristics);
			item.setData("alarmLevel", alarmLevel);
			item.setData("clearTime", clearTime);

			if (selected) {
				if (newSelected == null) {
					newSelected = new ArrayList<TableItem>(1);
				}
				newSelected.add(item);
			}

			if (def != null) {
				tableItems.put(def.getId(), item);
			}

			if (sclk != null) {
				item.setData("sclk", sclk);
			}
			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				item.setText(valIndex,values[valIndex]);
				if (inAlarm && (valIndex == dnValIndex || valIndex == euValIndex || valIndex == stateIndex)) {
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
		if (newSelected != null) {
			final TableItem[] selectedItems = new TableItem[newSelected.size()];	
			newSelected.toArray(selectedItems);
			table.setSelection(selectedItems);
		}
	}

	/**
	 * Creates a new TableItem at the appropriate index for the curent sort settings.
	 * @param args an ordered list of table column values, as strings
	 * @return new TableItem
	 */
	protected TableItem createTableItem(final String[] args) {
		final boolean ascending = table.getSortDirection() != SWT.DOWN;
		int itemIndex = 0;
		if (ascending || table.getSortDirection() == SWT.NONE) {
			itemIndex = table.getItemCount();
		}
		if (tableDef.isSortAllowed() && sortColumn != null) {
			final int colPos = sortColumn.getConfigurationNumber();
			itemIndex = this.findInsertIndex(args[colPos]);
		}

		final TableItem item = new TableItem(table, SWT.NONE, itemIndex);
		final ChillTableColumn[] cols = tableDef.getAvailableColumns();
		if (cols.length != args.length) {
			throw new IllegalArgumentException(
					"Number of arguments does not match number of columns");
		}
		for (final ChillTableColumn col : cols) {
			final int index = col.getConfigurationNumber();
			setColumn(item, index, args[index]);
		}
		return item;
	}

	/**
	 * Find the correct index at which to insert a new table row.
	 * @param valueToInsert value of the sort column for the item to be inserted
	 * @return index to insert into table at
	 */
	@SuppressWarnings("unchecked")
	protected int findInsertIndex(final String valueToInsert) {
		// No sort column defined. All new items added at the end of the table
		if (!tableDef.isSortAllowed() || sortColumn == null) {
			return table.getItemCount();
		}
		if (table.getItemCount() == 0) {
			return 0;
		}

		TableItemQuickSort.CollatorType collatorType = null;
		if (sortColumn.getSortType() == SortType.CHARACTER) {
			collatorType = TableItemQuickSort.CollatorType.CHARACTER;
		} else {
			collatorType = TableItemQuickSort.CollatorType.NUMERIC;
		}

		final Comparator<String> collator = (Comparator<String>)TableItemQuickSort.getCollator(collatorType);

		final int actualIndex = sortColumn.getCurrentPosition();
		final TableItem[] items = table.getItems();
		final boolean ascending = table.getSortDirection() != SWT.DOWN;

		if (valueToInsert == null) {
			if (ascending) {
				return 0;
			} else {
				return table.getItemCount();
			}
		}

		if (ascending) {
			for (int i = items.length - 1; i >= 0; i--) {
				final String currentRowVal = items[i].getText(actualIndex);
				if (currentRowVal == null || valueToInsert == null ) {
					System.out.println("here");
				}
				if (collator.compare(currentRowVal, valueToInsert) <= 0) {
					return i + 1;
				}
			}
			return 0;
		} else {
			for (int i = 0; i < items.length; i++) {
				final String currentRowVal = items[i].getText(actualIndex);
				if (currentRowVal == null || valueToInsert == null) {
					System.out.println("here");
				}
				if (collator.compare(currentRowVal, valueToInsert) <= 0) {
					return i;
				}
			}
			return table.getItemCount();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.AbstractBasicTableComposite#clearRows()
	 */
	@Override
	public void clearRows() {
		table.removeAll();
		tableItems.clear();
	}

}
