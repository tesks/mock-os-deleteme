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
package jpl.gds.monitor.guiapp.gui.views.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.monitor.perspective.view.channel.ChannelDisplayFormat;
import jpl.gds.monitor.perspective.view.channel.ChannelSet;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.ProgressBarShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.SeparatorEntryShell;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * Shell for selecting, removing, adding channels to a monitor display 
 * (channel chart, channel list or alarm)
 */
public class ChannelSetEditorShell implements ChillShell

{
	/**
	 * Shell height in pixels
	 */
	protected static final int MAIN_SHELL_HEIGHT = 350;
	
	/**
	 * Shell width in pixels
	 */
	protected static final int MAIN_SHELL_WIDTH = 900;
	
	/**
	 * Number of columns in the table
	 */
	protected static final int NUM_COLUMNS = 7;
	
	/**
	 * DN format column index
	 */
	protected static final int DN_FORMAT_COLUMN = 5;
	
	/**
	 * EU format column index
	 */
	protected static final int EU_FORMAT_COLUMN = 6;
	
	/**
	 * Window title for the channel set editor
	 */
	protected static final String MAIN_SHELL_TITLE = "Channel Selector";
	
	/**
	 * Represents a separator marker in a channel view
	 */
	protected static final String SEPARATOR_TOKEN = "[separator]";
	
	/**
	 * Represents a line marker in a channel view
	 */
	protected static final String LINE_TOKEN = "[line]";

	/**
	 * Main display
	 */
	protected Display mainDisplay;
	
	/**
	 * Channel set editor shell
	 */
	protected Shell mainShell;
	
	/**
	 * List of channels that can be chosen to be displayed in a monitor view
	 */
	protected List chanList;
	
	/**
	 * Table containing channels that are chosen to be displayed in a monitor view
	 */
	protected Table chanSetTable;
	
	/**
	 * SWT Combo box widget for selecting channel search criteria (i.e. search 
	 * by channel ID, title, module, FSW name)
	 */
	protected Combo searchCombo;
	
	/**
	 * SWT Combo box widget for filtering channel types in the chanList
	 */
	protected Combo typeCombo;
	
	/**
	 * SWT Combo box widget for filtering chanList by category
	 */
	protected Combo subsysCombo;
	
	/**
	 * Parent composite
	 */
	protected Composite parent = null;
	
	/**
	 * SWT Utilities object used for getting a file chooser
	 */
	protected SWTUtilities swtUtil;

	/**
	 * Flag for determining if subsystem filtering should be used
	 */
	protected boolean useSubsys = true;
	
	/**
	 * Flag that determines if the channel set editor window was cancelled
	 */
	protected boolean cancelled = false;

	/**
	 * Channel ID to Channel Definition mapping
	 */
	protected Map<String,IChannelDefinition> idDefMap = new HashMap<String,IChannelDefinition>();
	
	/**
	 * Channel Title to Channel Definition mapping
	 */
	protected Map<String,IChannelDefinition> titleDefMap = new HashMap<String,IChannelDefinition>();
	
	/**
	 * FSW definition to Channel Definition mapping
	 */
	protected Map<String,IChannelDefinition> fswDefMap = new HashMap<String,IChannelDefinition>();
	
	/**
	 * Channel Module to Channel Definition mapping
	 */
	protected Map<String,ArrayList<IChannelDefinition>> moduleDefMap = new HashMap<String,ArrayList<IChannelDefinition>>();

	/**
	 * List of channel IDs to pick from
	 */
	protected java.util.List<String> idList;
	
	/**
	 * List of channel titles to pick from
	 */
	protected java.util.List<String> titleList;
	
	/**
	 * List of FSW names to pick from
	 */
	protected java.util.List<String> fswList;
	
	/**
	 * List of channel modules to pick from
	 */
	protected java.util.List<String> moduleList;

	/**
	 * Listens for changes to the channel set
	 */
	protected ChannelSetUpdateListener updateListener = null;

	/**
	 * List of channels that will be added to a set
	 */
	protected ChannelSet inputSet = null;
	
	/**
	 * Controls maximum number of channels that can be added to the set
	 */
	protected ChannelSetConsumer channelConsumer;
	
	/**
	 * Separate window for formatting DN, EU and Value fields
	 */
	protected ChannelFormatEntryShell formatShell;

	private boolean enableSeparators;

	private final IChannelUtilityDictionaryManager dictUtil;

    private final SprintfFormat formatUtil;

	/**
	 * Constructor: sets the channel set consumer for this object
	 * 
	 * @param consumer channel set consumer that controls the maximum number 
	 * 				   of selected channels
	 */
	public ChannelSetEditorShell(final IChannelUtilityDictionaryManager dictUtil, final SprintfFormat formatter, final ChannelSetConsumer consumer)
	{
		this.dictUtil = dictUtil;
		channelConsumer = consumer;
		this.formatUtil = formatter;
	}
	
	/**
     * Allows for specifying the maximum selected channels.
     * 
     * @param maximumSelections
     *            Maximum number of channels to select.
     * @param dictUtil
     *            Channel Utility dictionary manager
     * @param formatter
     *            Sprintf formatter
     */
    public ChannelSetEditorShell(final long maximumSelections, final IChannelUtilityDictionaryManager dictUtil,
            final SprintfFormat formatter) {
        this(dictUtil, formatter, null);
        channelConsumer = new ChannelSetConsumer() {

            private long maxChannels;

            @Override
            public void setMaxChannelSelectionSize(final long maxChannelSelectionSize) {
                maxChannels = maxChannelSelectionSize;
            }

            @Override
            public long getMaxChannelSelectionSize() {
                return maxChannels;
            }
        };

        channelConsumer.setMaxChannelSelectionSize(maximumSelections);
	}

	/**
	 * Completes set up and opens a channel set editor pop-up window
	 * 
	 * @param parent composite from which this new composite was initialized
	 * @param updateListener listens for channels that are being selected
	 * @param set current channel set which will be populated in this channel set editor
	 * @param separators flag that determines if "separators" are enabled
	 */
	public void popupShell(final Composite parent, final ChannelSetUpdateListener updateListener, final ChannelSet set, 
			final boolean separators)
	{
		this.parent = parent;
		this.updateListener = updateListener;
		inputSet = set.copy();
		enableSeparators = separators;  
		this.setup();
		this.run();
	}

	/**
	 * Initializes the channel dictionary data for this shell.
	 */
	public void setup()
	{
		setupData();
	}

	/**
	 * Launches the channel set editor GUI 
	 */
	public void run()
	{
		createGui();
		if(inputSet != null)
		{
			addChannelsToSet(inputSet.getDisplayCharacteristics());
		}
	}

	/**
	 * Create channel definition data structures used by search and selection widgets.
	 */
	private void setupData()
	{
		final Set<String> ids = dictUtil.getChanIds();

		idList = new ArrayList<String>();
		titleList = new ArrayList<String>();
		fswList = new ArrayList<String>();
		moduleList = new ArrayList<String>();

		for(final String id : ids)
		{
			idList.add(id);
			final IChannelDefinition def = dictUtil.getDefinitionFromChannelId(id);
			final String title = def.getTitle();
			final String fswName = def.getName();
			final String module = def.getCategory(ICategorySupport.MODULE);

			if (title != null) {
				titleList.add(title);
			}

			if(fswName != null)
			{
				fswList.add(fswName);
				fswDefMap.put(fswName, def);
			}

			if(module != null)
			{
				if (!moduleList.contains(module)) {
					moduleList.add(module);
					final ArrayList<IChannelDefinition> list = new ArrayList<IChannelDefinition>();
					list.add(def);
					moduleDefMap.put(module, list);
				} else {
					final ArrayList<IChannelDefinition> list = moduleDefMap.get(module);
					list.add(def);
				}
			}
			idDefMap.put(id, def);
			titleDefMap.put(title, def);
		}

		Collections.sort(idList);
		Collections.sort(titleList);
		Collections.sort(fswList);
		Collections.sort(moduleList);
	}

	/**
	 * Reload the GUI due to changes in search text or other changes.
	 * 
	 * @param searchText
	 * 
	 */
	private void loadGui(final String searchText)
	{
		chanList.removeAll();

		final int selection = searchCombo.getSelectionIndex();

		String[] vals = null;
		final ArrayList<String> valList = new ArrayList<String>();
		switch(selection)
		{
		case 0: //Channel ID

			for(int index = 0; index < idList.size(); index++)
			{
				final String id = idList.get(index);
				if(meetsFilters(id, id, searchText))
				{
					valList.add(id);
				}
			}
			vals = new String[valList.size()];
			for(int index = 0; index < valList.size(); index++)
			{
				vals[index] = valList.get(index);
			}
			chanList.setItems(vals);
			break;

		case 1: //title

			for(int index = 0; index < titleList.size(); index++)
			{
				final String title = titleList.get(index);
				final IChannelDefinition def = titleDefMap.get(title);
				final String id = def.getId().toString();
				if(meetsFilters(id, title, searchText)) {
					valList.add(title);
				}
			}
			vals = new String[valList.size()];
			for(int index = 0; index < valList.size(); index++)
			{
				vals[index] = valList.get(index);
			}
			chanList.setItems(vals);
			break;

		case 2: // FSW name

			for(int index = 0; index < fswList.size(); index++)
			{
				final String fswName = fswList.get(index);
				final IChannelDefinition def = fswDefMap.get(fswName);
				final String id = def.getId();
				if(meetsFilters(id, fswName, searchText)) {
					valList.add(fswName);
				}
			}
			vals = new String[valList.size()];
			for(int index = 0; index < valList.size(); index++)
			{
				vals[index] = valList.get(index);
			}
			chanList.setItems(vals);
			break;

		case 3: // FSW Module

			for(int index = 0; index < moduleList.size(); index++)
			{
				final String module = moduleList.get(index);
				final ArrayList<IChannelDefinition> defs = moduleDefMap.get(module);
				final Iterator<IChannelDefinition> it = defs.iterator();
				while (it.hasNext()) {
					final IChannelDefinition def = it.next();
					final String id = def.getId();
					if (meetsFilters(id, module, searchText) && !valList.contains(module)) {
						valList.add(module);
					}
				}
			}
			vals = new String[valList.size()];
			for(int index = 0; index < valList.size(); index++)
			{
				vals[index] = valList.get(index);
			}
			chanList.setItems(vals);
			break;
		}
	}

	/**
	 * Check against selected filters and indicate whether or not the value passes those filters
	 * 
	 * @param chanId
	 * @param checkText
	 * @param searchText
	 * @return boolean true if meets filters, false otherwise
	 */
	private boolean meetsFilters(final String chanId, final String checkText, final String searchText)
	{
		if(checkText == null)
		{
			System.err.println("Invalid channel entry");
			return false;
		}

		// If the search text is found within the text check string, it
		// passes
		if(checkText.indexOf(searchText) == -1) {
			return false;
		}

		// Check against channel definition
		final IChannelDefinition def = idDefMap.get(chanId);

		// Check against selected channel type
		// Type maps directly to selection index
		final int type = typeCombo.getSelectionIndex();

		final ChannelType ctype = def.getChannelType();
		if((type != 0) && (ctype.ordinal() != type))
		{
			return false;
		}

		// Check against subsystem
		if (useSubsys) {
			final String chanSubsys = def.getSubsystem();
			final int selectedItem = subsysCombo.getSelectionIndex();

			// If not default, check
			if(selectedItem != 0)
			{
				final String selectedSubsys = subsysCombo.getItem(selectedItem);
				if(chanSubsys == null || !chanSubsys.equals(selectedSubsys))
				{
					return false;
				}
			}
		} else {
			// check against ops category
			final String chanCat = def.getOpsCategory();
			final int selectedItem = subsysCombo.getSelectionIndex();

			// If not default, check
			if(selectedItem != 0)
			{
				final String selectedCat= subsysCombo.getItem(selectedItem);
				if(chanCat == null || !chanCat.equals(selectedCat))
				{
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Create and display GUI components
	 */
	public void createGui()
	{
		FormData formData = null;

		if(parent == null)
		{
			mainDisplay = new Display();
		}
		else
		{
			mainDisplay = parent.getDisplay();
		}
		mainShell = new Shell(mainDisplay);
		mainShell.setSize(MAIN_SHELL_WIDTH, MAIN_SHELL_HEIGHT);
		mainShell.setText(MAIN_SHELL_TITLE);
		mainShell.setLayout(new FormLayout());

		final SearchComposite topComposite = new SearchComposite(mainShell);

		formData = new FormData();
		formData.top = new FormAttachment(0, 5);
		formData.left = new FormAttachment(0, 6);
		formData.right = new FormAttachment(100, -5);
		topComposite.setLayoutData(formData);

		final SelectCancelComposite bottomComposite = new SelectCancelComposite(mainShell);
		formData = new FormData();
		formData.bottom = new FormAttachment(95);
		formData.left = new FormAttachment(0, 5);
		formData.right = new FormAttachment(100, -2);
		bottomComposite.setLayoutData(formData);

		final Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.SHADOW_ETCHED_OUT);

		final FormData fd = new FormData();
		fd.bottom = new FormAttachment(bottomComposite, -5);
		fd.left = new FormAttachment(2);
		fd.right = new FormAttachment(98);
		line.setLayoutData(fd);

		final ChannelListComposite listComposite = new ChannelListComposite(mainShell);

		formData = new FormData();
		formData.top = new FormAttachment(topComposite, 2);
		formData.bottom = new FormAttachment(line, -5);
		formData.left = new FormAttachment(0, 5);
		formData.right = new FormAttachment(25);
		listComposite.setLayoutData(formData);

		final ChannelSetComposite setComposite = new ChannelSetComposite(mainShell);

		formData = new FormData();
		formData.top = new FormAttachment(topComposite, 2);
		formData.bottom = new FormAttachment(line, -5);
		formData.left = new FormAttachment(25, 5);
		formData.right = new FormAttachment(98);
		setComposite.setLayoutData(formData);
		mainShell.setLocation(parent.getShell().getLocation().x + 50, parent.getShell().getLocation().y + 50);


		mainShell.open();
		loadGui("");
		if(parent == null)
		{
			while(!mainShell.isDisposed())
			{
				if(!mainDisplay.readAndDispatch()) {
					mainDisplay.sleep();
				}
			}
			if (parent == null) {
				mainDisplay.dispose();
      			mainDisplay = null;
			}
		}
	}

	private void exitGui()
	{
		mainShell.close();
	}

	private String[] getAllSubsysOrCategories()
	{
		useSubsys = false;    
		java.util.List<String> subsys = dictUtil.getOpsCategories();
		if (subsys.isEmpty()) {
			subsys = dictUtil.getSubsystems();
			useSubsys = true;
		}
		return subsys.toArray(new String[] {});
	}

	private String[] getAllTypes()
	{
		final ChannelType[] ctypes = ChannelType.values();
		int i = 0;
		final String[] types = new String[ctypes.length];
        for (final ChannelType c: ctypes) {
        	types[i++] = c.toString();
        }
		return types;
	}

	private String[] getTableArray(final ChannelDisplayFormat def)
	{
		final String[] arr = new String[NUM_COLUMNS];
		arr[0] = def.getChanId();
		arr[1] = def.getChannelDef().getTitle();
		arr[2] = def.getChannelDef().getName();
		arr[3] = def.getChannelDef().getChannelType().toString();
		arr[4] = useSubsys ? def.getChannelDef().getSubsystem() : def.getChannelDef().getOpsCategory();
		arr[5] = def.getRawFormat() == null ? "" : def.getRawFormat();
		arr[6] = def.getValueFormat() == null ? "" : def.getValueFormat();
		return arr;
	}

	private ChannelSet getCurrentSet()
	{
		final ChannelSet set = new ChannelSet();

		// There are as many ids in the table as rows
		final TableItem[] items = chanSetTable.getItems();
		for(int index = 0; index < items.length; index++)
		{
			final String chanId = items[index].getText(0);
			if (chanId.equals(SEPARATOR_TOKEN)) {
				final String sep = items[index].getText(2);
				if (sep.startsWith(LINE_TOKEN)) {
					set.addSeparatorWithLine(sep.substring(6));
				} else {
					set.addSeparator(sep);
				}
			} else {

				final IChannelDefinition def = idDefMap.get(chanId);
				if(def == null)
				{
					continue;
				}
				String raw = items[index].getText(DN_FORMAT_COLUMN).trim();
				if (raw.equals("")) {
					raw = null;
				}
				String val = items[index].getText(EU_FORMAT_COLUMN).trim();
				if (val.equals("")) {
					val = null;
				}
				final ChannelDisplayFormat item = set.addChannel(def);
				item.setRawFormat(raw);
				item.setValueFormat(val);
			}
		}

		return set;
	}

	private ArrayList<IChannelDefinition> getSelectedDefs(final String id)
	{
		final int searchSelected = searchCombo.getSelectionIndex();

		ArrayList<IChannelDefinition>list = new ArrayList<IChannelDefinition>();
		IChannelDefinition def = null;
		switch(searchSelected)
		{
		case 0: // channel
			def = idDefMap.get(id);
			if (def != null) {
				list.add(def);
			}
			break;
		case 1: // Title
			def = titleDefMap.get(id);
			if (def != null) {
				list.add(def);
			}
			break;
		case 2: // FSW Name
			def = fswDefMap.get(id);
			if (def != null) {
				list.add(def);
			}
			break;
		case 3: // FSW Module
			list = moduleDefMap.get(id);
			break;
		}

		return list;
	}

	private int computeNewItemCount(final int[] selected) {
		int count = 0;
		for(int index = 0; index < selected.length; index++)
		{
			final ArrayList<IChannelDefinition> chanDef = getSelectedDefs(chanList.getItem(selected[index]));
			if (chanDef != null) {
				count +=chanDef.size();
			}
		}
		return count;
	}

	/**
	 * Adds channels selected in the chanList to the input set
	 */
	protected void addSelectedChannelsToSet()
	{
		// doesn't filter out repeats currently

		final int[] selectedItems = chanList.getSelectionIndices();

		Arrays.sort(selectedItems);

		final int newItems = computeNewItemCount(selectedItems);

		if((newItems + chanSetTable.getItemCount()) > channelConsumer.getMaxChannelSelectionSize())
		{
			SWTUtilities.showErrorDialog(mainShell,"Too Many Channels","The current channel set contains " +
					chanSetTable.getItemCount() + " channels and you have selected " + newItems + " more " +
					"channels to add.  However, the attached channel view is configured not to allow more than " +
					channelConsumer.getMaxChannelSelectionSize() + " channels per view.");
			return;
		}

		ProgressBarShell shell = new ProgressBarShell(mainShell);
		shell.getShell().setText("Channel Addition Progress");
		shell.getProgressLabel().setText("Adding Channels: ");
		shell.getProgressBar().setMinimum(0);
		shell.getProgressBar().setMaximum(newItems + 1);
		shell.getProgressBar().setSelection(0);

		shell.open();

		for(int index = 0; index < selectedItems.length; index++)
		{
			shell.getProgressBar().setSelection(index);
			final ArrayList<IChannelDefinition> chanDef = getSelectedDefs(chanList.getItem(selectedItems[index]));
			if(chanDef == null)
			{
				continue;
			}
			final Iterator<IChannelDefinition> it = chanDef.iterator();
			while (it.hasNext())
			{
				final IChannelDefinition def = it.next();

				final TableItem item = new TableItem(chanSetTable, SWT.NONE);
				item.setText(getTableArray(new ChannelDisplayFormat(def)));
				inputSet.addChannel(def);
			}
		}

		SWTUtilities.justifyTable(chanSetTable);
		shell.getProgressBar().setSelection(shell.getProgressBar().getMaximum());

		shell.dispose();
		shell = null;
	}

	private void addChannelsToSet(final String channel, final boolean checkIfPresent)
	{
		final ArrayList<IChannelDefinition> defs = getSelectedDefs(channel);

		final TableItem[] currentChannelsSelected = chanSetTable.getItems();
		if ( currentChannelsSelected.length >= channelConsumer.getMaxChannelSelectionSize() )
		{
			SWTUtilities.showErrorDialog ( mainShell, "Too Many Channels", "You are attempting to add more channels, "
					+ "however, the attached channel view is configured not to allow more than "
					+ channelConsumer.getMaxChannelSelectionSize() + " channels per view." );
			return;
		}

		// TODO: Invalid channel popup
		if(defs == null)
		{
			// invalid channel
			return;
		}

		final Iterator<IChannelDefinition>it = defs.iterator();
		while (it.hasNext()) {
			final IChannelDefinition def = it.next();
			if(checkIfPresent)
			{
				// If already present, don't add
				final TableItem[] items = chanSetTable.getItems();
				for(int index = 0; index < items.length; index++)
				{
					final String tabChan = items[index].getText(0);
					final String inChan = def.getId();
					// Already present
					if(tabChan.equals(inChan))
					{
						continue;
					}
				}
			}
			final String[] defVals = getTableArray(new ChannelDisplayFormat(def));
			// System.out.println("Adding table item " + def.getId().toString());
			final TableItem item = new TableItem(chanSetTable, SWT.NONE);
			item.setText(defVals); 
			inputSet.addChannel(def);
		}
		SWTUtilities.justifyTable(chanSetTable);
	}

	private void addChannelsToSet(final ChannelDisplayFormat[] chans)
	{
		final TableItem[] currentChannelsSelected = chanSetTable.getItems();
		if ( currentChannelsSelected.length >= channelConsumer.getMaxChannelSelectionSize() )
		{
			SWTUtilities.showErrorDialog ( mainShell, "Too Many Channels", "You are attempting to add more channels, "
					+ "however, the attached channel view is configured not to allow more than "
					+ channelConsumer.getMaxChannelSelectionSize() + " channels per view." );
			return;
		}

		for(int index = 0; index < chans.length; index++)
		{
			if (chans[index].isSeparator()) {
				final TableItem item = new TableItem(chanSetTable, SWT.NONE);
				item.setText(getSeparatorArray(chans[index]));
			} else if (chans[index].getChannelDef() != null) {
				final TableItem item = new TableItem(chanSetTable, SWT.NONE);
				item.setText(getTableArray(chans[index]));
			}
		}

		SWTUtilities.justifyTable(chanSetTable);
	}

	private String[] getSeparatorArray(final ChannelDisplayFormat sep)
	{
		final String[] arr = new String[NUM_COLUMNS];
		if (sep.isLine()) {
			arr[0] = SEPARATOR_TOKEN;
			arr[1] = "";
			arr[2] = LINE_TOKEN + sep.getSeparatorString();
			arr[3] = "";
			arr[4] = "";
			arr[5] = "";
			arr[6] = "";
		} else {
			arr[0] = SEPARATOR_TOKEN;
			arr[1] = "";
			arr[2] = sep.getSeparatorString();
			arr[3] = "";
			arr[4] = "";
			arr[5] = "";
			arr[6] = "";
		}
		return arr;
	}

	/**
	 * Small composite that contains a list of channels that the user can choose from to add
	 *
	 */
	private class ChannelListComposite extends Composite
	{
		public ChannelListComposite(final Composite parent)
		{
			super(parent, SWT.NO_FOCUS);
			final FormLayout fl = new FormLayout();
			fl.spacing = 0;
			this.setLayout(fl);

			chanList = new List(this, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

			final Button moveButton = new Button(this, SWT.BOLD);

			final FormData cfd = new FormData();
			cfd.top = new FormAttachment(0);
			cfd.left = new FormAttachment(0);
			cfd.right = new FormAttachment(100);
			cfd.bottom = new FormAttachment(moveButton, -2);
			chanList.setLayoutData(cfd);

			chanList.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseDoubleClick(final MouseEvent arg0)
				{
					try {
						final int item = chanList.getSelectionIndex();

						// Apparently -1 means you didn't actually select
						// something. So why register the click....?
						if(item != -1)
						{
							final String text = chanList.getItem(item);
							// addChannelToSet(text, true);
							// BRENT EDIT
							addChannelsToSet(text, false); // QQQQQQ
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			moveButton.setText(">>>");
			final FormData fd = new FormData();
			fd.left = new FormAttachment(20);
			fd.right = new FormAttachment(80);
			fd.bottom = new FormAttachment(100);
			moveButton.setLayoutData(fd);

			moveButton.addSelectionListener(new SelectionListener()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						setEnabled ( false );
						addSelectedChannelsToSet();
						setEnabled ( true );
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0)
				{

				}
			});

		}
	}

	/**
	 * Composite with buttons for manipulating table rows
	 *
	 */
	private class TableButtonComposite extends Composite
	{
		public TableButtonComposite(final Composite parent)
		{
			super(parent, SWT.NO_FOCUS);
			final GridLayout fl = new GridLayout(7,false);
			this.setLayout(fl);

			final Button sepButton = new Button(this, SWT.NONE);
			sepButton.setText("Add Separator");
			sepButton.setEnabled(enableSeparators);

			sepButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final SeparatorEntryShell sepShell = new SeparatorEntryShell(mainShell, "Define Table Separator",
                                "Separator Text", "", true, TraceManager.getDefaultTracer());
						sepShell.open();
						while(!sepShell.getShell().isDisposed()) {
							mainShell.getDisplay().readAndDispatch();
						}

						if (sepShell.wasCanceled()) {
							return;
						}
						int selection = chanSetTable.getSelectionIndex();
						if (selection == -1) {
							selection = 0;
						}
						final TableItem item = new TableItem(chanSetTable, SWT.CENTER, selection);
						final ChannelDisplayFormat c = new ChannelDisplayFormat(null, sepShell.getValue(), true);
						final SeparatorEntryShell.SeparatorType type = sepShell.getSeparatorType();
						c.setLine(type == SeparatorEntryShell.SeparatorType.LINE_ONLY || type == SeparatorEntryShell.SeparatorType.TEXT_AND_LINE);
						item.setText(getSeparatorArray(c));
					} catch (final RuntimeException e1) {
						e1.printStackTrace();
					}
				}
			});

			final Button upButton = new Button(this, SWT.NONE);
			upButton.setText("Up");
			upButton.setEnabled(false);

			final Button downButton = new Button(this, SWT.NONE);
			downButton.setText("Down");
			downButton.setEnabled(false);

			final Button bottomButton = new Button(this, SWT.NONE);
			bottomButton.setText("Bottom");
			bottomButton.setEnabled(false);

			final Button topButton = new Button(this, SWT.NONE);
			topButton.setText("Top");
			topButton.setEnabled(false);

			upButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final int[] selectedInd = chanSetTable.getSelectionIndices();
						if (selectedInd.length == 0) {
							return;
						}
						Arrays.sort(selectedInd);
						if (selectedInd[0] == 0) {
							// cannot move up.
							return;
						}
						for (int i = 0; i < selectedInd.length; i++) {
							final int insertIndex = selectedInd[i] - 1;
							final TableItem oldItem = chanSetTable.getItem(selectedInd[i]);
							final TableItem newItem = new TableItem(chanSetTable, SWT.NONE, insertIndex);
							newItem.setText(0, oldItem.getText(0));
							newItem.setText(1, oldItem.getText(1));
							newItem.setText(2, oldItem.getText(2));
							newItem.setText(3, oldItem.getText(3));
							chanSetTable.remove(selectedInd[i] + 1);
						}
						final int[] newInd = new int[selectedInd.length];
						for (int i = 0; i < selectedInd.length; i++) {
							newInd[i] = selectedInd[i] - 1;
						}
						chanSetTable.setSelection(newInd);
						if (newInd[0] == 0) {
							upButton.setEnabled(false);
							topButton.setEnabled(false);
						}
						downButton.setEnabled(true);
						bottomButton.setEnabled(true);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			topButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final int[] selectedInd = chanSetTable.getSelectionIndices();
						if (selectedInd.length == 0) {
							return;
						}
						Arrays.sort(selectedInd);
						if (selectedInd[0] == 0) {
							// cannot move up.
							return;
						}
						for (int i = 0; i < selectedInd.length; i++) {
							final int insertIndex = 0 + i;
							final TableItem oldItem = chanSetTable.getItem(selectedInd[i]);
							final TableItem newItem = new TableItem(chanSetTable, SWT.NONE, insertIndex);
							newItem.setText(0, oldItem.getText(0));
							newItem.setText(1, oldItem.getText(1));
							newItem.setText(2, oldItem.getText(2));
							newItem.setText(3, oldItem.getText(3));
							chanSetTable.remove(selectedInd[i] + 1);
						}

						chanSetTable.setSelection(0);
						topButton.setEnabled(false);
						upButton.setEnabled(false);
						bottomButton.setEnabled(true);
						downButton.setEnabled(true);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			downButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final int[] selectedInd = chanSetTable.getSelectionIndices();
						if (selectedInd.length == 0) {
							return;
						}
						Arrays.sort(selectedInd);
						if (selectedInd[selectedInd.length - 1] == chanSetTable.getItemCount() - 1) {
							// cannot move down
							return;
						}
						for (int i = selectedInd.length - 1; i >= 0; i--) {
							final int insertIndex = selectedInd[i] + 2;
							final TableItem oldItem = chanSetTable.getItem(selectedInd[i]);
							final TableItem newItem = new TableItem(chanSetTable, SWT.NONE, insertIndex);
							newItem.setText(0, oldItem.getText(0));
							newItem.setText(1, oldItem.getText(1));
							newItem.setText(2, oldItem.getText(2));
							newItem.setText(3, oldItem.getText(3));
							chanSetTable.remove(selectedInd[i]);
						}
						final int[] newInd = new int[selectedInd.length];
						for (int i = 0; i < selectedInd.length; i++) {
							newInd[i] = selectedInd[i] + 1;
						}
						chanSetTable.setSelection(newInd);
						if (newInd[newInd.length - 1] == chanSetTable.getItemCount() - 1) {
							downButton.setEnabled(false);
							bottomButton.setEnabled(false);
						}
						upButton.setEnabled(true);
						topButton.setEnabled(true);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			bottomButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final int[] selectedInd = chanSetTable.getSelectionIndices();
						if (selectedInd.length == 0) {
							return;
						}
						Arrays.sort(selectedInd);
						if (selectedInd[selectedInd.length - 1] == chanSetTable.getItemCount() - 1) {
							// cannot move down
							return;
						}
						final TableItem[] itemsToMove = new TableItem[selectedInd.length];
						for (int i = 0; i < selectedInd.length; i++) {
							itemsToMove[i] = chanSetTable.getItem(selectedInd[i]);
						}
						for (int i = 0; i < itemsToMove.length; i++) {
							final int insertIndex = chanSetTable.getItemCount();
							final TableItem oldItem = itemsToMove[i];
							final TableItem newItem = new TableItem(chanSetTable, SWT.NONE, insertIndex);
							newItem.setText(0, oldItem.getText(0));
							newItem.setText(1, oldItem.getText(1));
							newItem.setText(2, oldItem.getText(2));
							newItem.setText(3, oldItem.getText(3));
							chanSetTable.remove(chanSetTable.indexOf(oldItem));
						}

						chanSetTable.setSelection(chanSetTable.getItemCount() - 1);
						downButton.setEnabled(false);
						bottomButton.setEnabled(false);
						upButton.setEnabled(true);
						topButton.setEnabled(true);
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			chanSetTable.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						if (chanSetTable.getSelectionCount() == 0) {
							upButton.setEnabled(false);
							downButton.setEnabled(false);
						} else {
							final int[] indices = chanSetTable.getSelectionIndices();
							Arrays.sort(indices);
							upButton.setEnabled(indices[0] != 0);
							topButton.setEnabled(indices[0] != 0);

							downButton.setEnabled(chanSetTable.getItemCount() > 1 
									&& indices[indices.length - 1] < chanSetTable.getItemCount() - 1);
							bottomButton.setEnabled(downButton.getEnabled());
						}
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			final Button removeButton = new Button(this, SWT.NONE);
			removeButton.setText("Remove");

			removeButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						final int[] selected = chanSetTable.getSelectionIndices();
						chanSetTable.remove(selected);
						topButton.setEnabled(false);
						bottomButton.setEnabled(false);
						upButton.setEnabled(false);
						downButton.setEnabled(false);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			final Button clearButton = new Button(this, SWT.NONE);
			clearButton.setText("Clear");

			clearButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						chanSetTable.removeAll();
						SWTUtilities.justifyTable(chanSetTable);
						topButton.setEnabled(false);
						bottomButton.setEnabled(false);
						upButton.setEnabled(false);
						downButton.setEnabled(false);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	/**
	 * Small SWT composite with an Ok, Load, Save and Cancel button
	 *
	 */
	private class SelectCancelComposite extends Composite
	{
		public SelectCancelComposite(final Composite parent)
		{
			super(parent, SWT.NO_FOCUS);
			final FormLayout fl = new FormLayout();
			fl.marginWidth = 5;
			fl.spacing = 5;
			this.setLayout(fl);

			final Button cancelButton = new Button(this, SWT.NONE);
			cancelButton.setText("Cancel");
			FormData fd = new FormData();
			fd.top = new FormAttachment(10);
			fd.right = new FormAttachment(98);
			fd.bottom = new FormAttachment(100);
			cancelButton.setLayoutData(fd);

			cancelButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						cancelled = true;
						exitGui();
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			final Button saveButton = new Button(this, SWT.NONE);
			saveButton.setText("Save...");
			fd = new FormData();
			fd.top = new FormAttachment(10);
			fd.right = new FormAttachment(cancelButton);
			fd.bottom = new FormAttachment(100);
			saveButton.setLayoutData(fd);

			saveButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						if(swtUtil == null)
						{
							swtUtil = new SWTUtilities();
						}
						final String filename = swtUtil.displayStickyFileSaver(mainShell, "ChannelSetEditorShell", null, null);

						if(filename == null)
						{
							return;
						}

						final ChannelSet set = getCurrentSet();
						try
						{
							set.saveToFile(filename);
						}
						catch(final IOException e)
						{
							e.printStackTrace();
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			final Button loadButton = new Button(this, SWT.NONE);
			loadButton.setText("Load...");
			fd = new FormData();
			fd.top = new FormAttachment(10);
			fd.right = new FormAttachment(saveButton);
			fd.bottom = new FormAttachment(100);
			loadButton.setLayoutData(fd);

			loadButton.addSelectionListener(new SelectionAdapter()
			{

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						if(swtUtil == null)
						{
							swtUtil = new SWTUtilities();
						}

						final String filename = swtUtil.displayStickyFileChooser(false, mainShell,"ChannelSetEditorShell");
						if(filename != null)
						{
							final ChannelSet set = new ChannelSet();
							try
							{
								set.loadFromFile(dictUtil, filename);
								if(set.size() > channelConsumer.getMaxChannelSelectionSize())
								{
									SWTUtilities.showErrorDialog(mainShell, "Too Many Channels",
											"A channel set for the attached channel view is configured"   
											+ " to only allow " + channelConsumer.getMaxChannelSelectionSize()
											+ " channels per view.  The input file " + filename + " contains "
											+ set.size() + " channels.");

									return;
								}
								addChannelsToSet(set.getDisplayCharacteristics()); 
							}
							catch(final IOException e)
							{
								SWTUtilities.showErrorDialog(mainShell,"File I/O Error",
										"A problem was encountered while reading the file " + filename +
										": " + e.getMessage());
								return;
							}
						}
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			final Button selectButton = new Button(this, SWT.NONE);
			selectButton.setText("Ok");
			fd = new FormData();
			fd.top = new FormAttachment(10);
			fd.right = new FormAttachment(loadButton);
			fd.bottom = new FormAttachment(100);
			selectButton.setLayoutData(fd);

			selectButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					if(updateListener.updateSet(getCurrentSet()) == true)
					{
						try {
							exitGui();
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}

			});
		}
	}

	/**
	 * Main part of the channel set editor shell GUI. Displays channels that 
	 * are currently going to be selected and formatting options
	 *
	 */
	private class ChannelSetComposite extends Composite
	{

		public ChannelSetComposite(final Composite parent)
		{
			super(parent, SWT.NO_FOCUS);
			final FormLayout fl = new FormLayout();
			fl.spacing = 0;
			this.setLayout(fl);

			chanSetTable = new Table(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
			chanSetTable.setHeaderVisible(true);

			final Font f = ChillFontCreator.getFont(new ChillFont(ChillFont.DEFAULT_FACE, ChillFont.FontSize.SMALL, ChillFont.DEFAULT_STYLE));
			chanSetTable.setFont(f);

			chanSetTable.setLinesVisible(true);
			final TableColumn t1 = new TableColumn(chanSetTable, SWT.NULL);
			t1.setText("Channel");
			final TableColumn t2 = new TableColumn(chanSetTable, SWT.NULL);
			t2.setText("Title");
			final TableColumn t3 = new TableColumn(chanSetTable, SWT.NULL);
			t3.setText("FSW Name/Separator");
			final TableColumn t4 = new TableColumn(chanSetTable, SWT.NULL);
			t4.setText("Type");
			final TableColumn t5 = new TableColumn(chanSetTable, SWT.NULL);
			t5.setText(useSubsys ? "Subsystem" : "Category");
			final TableColumn t6 = new TableColumn(chanSetTable, SWT.NULL);
			t6.setText("Raw Format");
			final TableColumn t7 = new TableColumn(chanSetTable, SWT.NULL);
			t7.setText("Value Format");

			final Menu viewMenu = new Menu(this);
			viewMenu.setEnabled(false);
			final MenuItem formatMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			formatMenuItem.setText("Set Raw/DN Format...");
			final MenuItem resetFormatMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			resetFormatMenuItem.setText("Reset Raw/DN Format...");
			chanSetTable.setMenu(viewMenu);
			new MenuItem(viewMenu, SWT.SEPARATOR);
			final MenuItem euFormatMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			euFormatMenuItem.setText("Set Value/EU Format...");
			final MenuItem resetEuFormatMenuItem = new MenuItem(viewMenu, SWT.PUSH);
			resetEuFormatMenuItem.setText("Reset Value/EU Format...");

			chanSetTable.addSelectionListener(new SelectionAdapter() {
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						final int[] i = chanSetTable.getSelectionIndices();
						if (i != null && i.length == 1) {
							final IChannelDefinition def = idDefMap.get(chanSetTable.getItem(i[0]).getText());
							viewMenu.setEnabled(def != null);
						} else {
							viewMenu.setEnabled(false);
						}   
					} catch (final Exception ex) {
						ex.printStackTrace();
						TraceManager.getDefaultTracer().error("Error handling table selection " + ex.toString());

					}
				}
			});

			formatMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						if (formatShell == null) {
							final int index = chanSetTable.getSelectionIndex();
							if (index == -1) {
								return;
							}
							final IChannelDefinition def = idDefMap.get(chanSetTable.getItem(index).getText());
							final ChannelDisplayFormat displayItem = inputSet.getDisplayCharacteristics(def.getId());
							formatShell = new ChannelFormatEntryShell(mainShell, formatUtil);
							formatShell.init(def, true, displayItem.getRawFormat());
							formatShell.open();
							while (!formatShell.getShell().isDisposed()) {
								if (!mainDisplay.readAndDispatch()) {
									mainDisplay.sleep();
								}
							}  
							if (!formatShell.wasCanceled()) {
								String val = formatShell.getFormatString();
								if (val.equals("")) {
									val = null;
								}
								displayItem.setRawFormat(val);
								final TableItem item = chanSetTable.getItem(index);
								item.setText(DN_FORMAT_COLUMN, val);
							}
							formatShell = null;
						}
					} catch (final Exception ex) {
						ex.printStackTrace();
						TraceManager.getDefaultTracer().error("Error handling format menu item " + ex.toString());

					}
				}
			});

			euFormatMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						if (formatShell == null) {
							final int index = chanSetTable.getSelectionIndex();
							if (index == -1) {
								return;
							}
							final IChannelDefinition def = idDefMap.get(chanSetTable.getItem(index).getText());
							final ChannelDisplayFormat displayItem = inputSet.getDisplayCharacteristics(def.getId());
							formatShell = new ChannelFormatEntryShell(mainShell, formatUtil);
							formatShell.init(def, false, displayItem.getValueFormat());
							formatShell.open();
							while (!formatShell.getShell().isDisposed()) {
								if (!mainDisplay.readAndDispatch()) {
									mainDisplay.sleep();
								}
							}  
							if (!formatShell.wasCanceled()) {
								String val = formatShell.getFormatString();
								if (val.equals("")) {
									val = null;
								}
								displayItem.setValueFormat(val);
								final TableItem item = chanSetTable.getItem(index);
								item.setText(EU_FORMAT_COLUMN, val);
							}
							formatShell = null;
						}
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			resetFormatMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						if (formatShell == null) {
							final int index = chanSetTable.getSelectionIndex();
							if (index == -1) {
								return;
							}
							final IChannelDefinition def = idDefMap.get(chanSetTable.getItem(index).getText());
							final ChannelDisplayFormat displayItem = inputSet.getDisplayCharacteristics(def.getId());
							final boolean go = SWTUtilities.showConfirmDialog(mainShell, "Confirm Reset", 
							"This will reset the Raw/DN format to its dictionary value.\nDo you want to continue?");
							if (!go) {
								return;
							}
							final TableItem item = chanSetTable.getItem(index);
							item.setText(DN_FORMAT_COLUMN, def.getDnFormat() == null ? "" : def.getDnFormat());
							displayItem.setRawFormat(def.getDnFormat());
						}
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			resetEuFormatMenuItem.addSelectionListener(new SelectionAdapter() {
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					try {
						if (formatShell == null) {
							final int index = chanSetTable.getSelectionIndex();
							if (index == -1) {
								return;
							}
							final IChannelDefinition def = idDefMap.get(chanSetTable.getItem(index).getText());
							final ChannelDisplayFormat displayItem = inputSet.getDisplayCharacteristics(def.getId());
							final boolean go = SWTUtilities.showConfirmDialog(mainShell, "Confirm Reset", 
							"This will reset the Value/EU format to its dictionary value.\nDo you want to continue?");
							if (!go) {
								return;
							}
							final TableItem item = chanSetTable.getItem(index);
							item.setText(EU_FORMAT_COLUMN, def.getEuFormat() == null ? "" : def.getEuFormat());
							displayItem.setValueFormat(def.getEuFormat());
						}
					} catch (final Exception e1) {
						e1.printStackTrace();
					}
				}
			});

			SWTUtilities.justifyTable(chanSetTable);

			final TableButtonComposite rcc = new TableButtonComposite(this);

			final FormData cfd = new FormData();
			cfd.top = new FormAttachment(0);
			cfd.left = new FormAttachment(0);
			cfd.right = new FormAttachment(100);
			cfd.bottom = new FormAttachment(rcc, -2);
			chanSetTable.setLayoutData(cfd);

			final FormData fd = new FormData();
			fd.right = new FormAttachment(100);
			fd.bottom = new FormAttachment(100);
			rcc.setLayoutData(fd);

		}
	}

	/**
	 * Contains criteria for searching through the channel list. Can be 
	 * searched by channel field, by text search, by subsystem and by channel 
	 * type.
	 *
	 */
	private class SearchComposite extends Composite
	{
		public SearchComposite(final Composite parent)
		{
			super(parent, SWT.NO_FOCUS);
			final FormLayout fl = new FormLayout();
			fl.spacing = 5;
			fl.marginHeight = 2;
			fl.marginWidth = 2;
			this.setLayout(fl);
			final Label searchLabel = new Label(this, SWT.NONE);
			searchLabel.setText("Search: ");
			FormData fd = new FormData();
			fd.top = new FormAttachment(10);
			fd.left = new FormAttachment(0);
			searchLabel.setLayoutData(fd);

			searchCombo = new Combo(this, SWT.READ_ONLY);
			String items[] = { "Channel ID", "Title", "FSW Name", "Module" };
			searchCombo.setItems(items);
			fd = new FormData();
			fd.left = new FormAttachment(searchLabel);
			fd.top = new FormAttachment(searchLabel, 0, SWT.CENTER);
			searchCombo.setLayoutData(fd);
			searchCombo.select(0);

			subsysCombo = new Combo(this, SWT.READ_ONLY);
			items = getAllSubsysOrCategories();
			subsysCombo.setItems(items);
			subsysCombo.add(useSubsys ? "All Subsys" : "All Cats", 0);
			subsysCombo.select(0);

			typeCombo = new Combo(this, SWT.READ_ONLY);
			items = getAllTypes();
			typeCombo.setItems(items);
			// Replace "unknown" with "all types"
			typeCombo.setItem(0, "All Types");
			typeCombo.select(0);
			fd = new FormData();
			fd.top = new FormAttachment(searchLabel, 0, SWT.CENTER);
			fd.right = new FormAttachment(subsysCombo);
			typeCombo.setLayoutData(fd);

			// Now subsys combo data
			fd = new FormData();
			fd.right = new FormAttachment(100);
			fd.top = new FormAttachment(searchLabel, 0, SWT.CENTER);
			subsysCombo.setLayoutData(fd);

			final Text searchText = new Text(this, SWT.SINGLE);
			fd = SWTUtilities.getFormData(searchText, 1, 15);
			fd.left = new FormAttachment(searchCombo);
			fd.top = new FormAttachment(searchCombo, 0, SWT.CENTER);
			fd.bottom = new FormAttachment(searchCombo, 100, SWT.CENTER);
			fd.right = new FormAttachment(typeCombo);
			searchText.setLayoutData(fd);

			subsysCombo.addSelectionListener(new SelectionListener()
			{

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						loadGui(searchText.getText());
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0)
				{
				}

			});

			typeCombo.addSelectionListener(new SelectionListener()
			{

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						loadGui(searchText.getText());
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetDefaultSelected(final SelectionEvent arg0)
				{
				}

			});

			searchText.addTraverseListener(new TraverseListener()
			{
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.TraverseListener#keyTraversed(org.eclipse.swt.events.TraverseEvent)
				 */
				@Override
				public void keyTraversed(final TraverseEvent e)
				{
					if(e.detail == SWT.TRAVERSE_RETURN)
					{
						e.doit = false;
						e.detail = SWT.TRAVERSE_NONE;
					}
					final String text = searchText.getText();

					addChannelsToSet(text, true); 

				}
			});

			searchCombo.addSelectionListener(new SelectionAdapter()
			{
				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				@Override
				public void widgetSelected(final SelectionEvent arg0)
				{
					try {
						searchText.setText("");
						loadGui("");
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

			searchText.addModifyListener(new ModifyListener()
			{
				boolean emptyTextModify = false;

				/**
			     * {@inheritDoc}
				 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
				 */
				@Override
				public void modifyText(final ModifyEvent arg0)
				{
					try {
						if(searchText.getText().length() < 1)
						{
							if(emptyTextModify == false)
							{
								loadGui("");
								emptyTextModify = true;
							} else {
								return;
							}
						}

						loadGui(searchText.getText());
						emptyTextModify = false;
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});

		}
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open()
	{
		mainShell.open();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell()
	{
		return mainShell;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled()
	{
		return cancelled;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle()
	{
		return "ChannelSetEditorShell";
	}

	/**
	 * Close the main shell
	 */
	public void close() {
		mainShell.close();
		
	}
}
