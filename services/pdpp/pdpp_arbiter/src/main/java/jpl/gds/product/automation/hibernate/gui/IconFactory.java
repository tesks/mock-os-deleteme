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
package jpl.gds.product.automation.hibernate.gui;

import java.awt.Image;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;


/**
 * Icon factory for the product automation gui.  Has all of the icon files stored here and will create and 
 * return an icon.  Want to keep all of them in one place.
 * 
 * Setting up a few methods to get some more well used icons. 
 * 
 * MPCS-8182 - 08/11/16 - Added to and updated for AMPCS.
 */
public class IconFactory {
	private static final int ZERO = 0;

	private static final String DOWNLINK_M = "downlink";
	private static final String CORRECT_M = "correct";
	private static final String UNWRAP_M = "unwrap";
	private static final String DECOMP_M = "compress";
	private static final String EXTRACT_M = "extract";
	
	// Used to name merged icons.
	private static final String CORRECT_COMPLETE = "correctcmp";
	private static final String CORRECT_PENDING = "correctpend";
	private static final String CORRECT_FAILED = "correctfail";
	
	private static final String UNWRAP_COMPLETE = "unwrapcomp";
	private static final String UNWRAP_PENDING = "unwrappend";
	private static final String UNWRAP_FAILED = "unwrapfail";
	
	private static final String COMPRESS_COMPLETE = "compresscomp";
	private static final String COMPRESS_PENDING = "compresspend";
	private static final String COMPRESS_FAILED = "compressfail";
	
	private static final String DOWNLINK_COMPLETE = "dlComplete";
	private static final String DOWNLINK_FAILED = "dlFailed";
	
	private static final String APPLICATION_ICON = "icons/application.png";
	
	// Control panel icon files.
	private static final String ARBITER_START_FILE = "icons/play.png";
	private static final String ARBITER_STOP_FILE = "icons/player_stop.png";
	private static final String REFRESH_FILE = "icons/arrow_refresh.png";
	private static final String UPDATE_FILE = "icons/arrow_up.png";
	private static final String UPDATES_PENDING_FILE = "icons/drink.png";
	private static final String NO_UPDATES_PENDING_FILE = "icons/drink_empty.png";
	private static final String ACTION_ENABLED_FILE = "icons/accept.png";
	private static final String ACTION_DISABLED_FILE = "icons/cancel.png";
	
	private static final String DOWNLINK_FILE = "icons/satelite.png";
	
	// Shared control panel and ancestor panel icons.
	private static final String CORRECT_FILE = "icons/correct_2.png";
	private static final String UNWRAP_FILE = "icons/unwraped.png";
	private static final String DECOMPRESS_FILE = "icons/compress.png";
	private static final String EXTRACT_FILE = "icons/plus_large.png";
	
	private static final String EXTRACT_KEY_FILE = "icons/plus.png";
	private static final String PENDING_KEY_FILE = "icons/question-icon.png";
	private static final String FAILED_KEY_FILE = "icons/failed_1.png";
	
	private static final String FAILED_ACTION_FILE = "icons/failed.png";
	private static final String PENDING_ACTION_FILE = "icons/pending.png";
	private static final String RIGHT_TREE_ARROW_FILE = "icons/closed_tree.png";
	private static final String DOWN_TREE_ARROW_FILE = "icons/expanded_tree.png";
	
	// Status Panel
	private static final String CLEAR_FILE = "icons/edit_clear.png";
	private static final String REMOVE_FILE = "icons/table_row_delete.png";
	private static final String RT_START_FILE = "icons/control_play_blue.png";
	private static final String RT_STOP_FILE = "icons/control_stop_blue.png";
	
	private static final String EXECUTE_FILE = "icons/database_go.png";
	
	private static final String INCLUDE_FILE = "icons/tick.png";	
	private static final String EXCLUDE_FILE = "icons/cross.png";
	
	private static final String TOP_DOWN_FILE = "icons/top_down_arrow.png";
	private static final String BOTTOM_UP_FILE = "icons/bottom_up_arrow.png";
	private static final String EXPORT_FILE = "icons/table_save.png";

	private static final String UNKNOWN_FILE = "icons/question.png";
	
	private static final Map<String, Icon> ICON_MAP = Collections.synchronizedMap(new WeakHashMap<String, Icon>());

	private static IconFactory instance;
	
	/**
	 * Gets the current IconFactory instance. If one does not exist it is
	 * created, stored, and returned.
	 * 
	 * @return the stored IconFactory instance.
	 */
	public static IconFactory getInstance() {
		if (instance == null) {
			instance = new IconFactory();
		}
		
		return instance;
	}
	
	/**
	 * Retrieve the icon stored at the given file path
	 * 
	 * @param path
	 *            the file path to an icon
	 * @return the Icon specified by the path
	 */
	public static Icon getIcon(String path) {
		Icon icon;
		
		synchronized (ICON_MAP) {
			icon = ICON_MAP.get(path);
		}
		
		if (icon == null) {
			icon = buildIcon(path);
			
			if (icon != null) {
				synchronized (ICON_MAP) {
					ICON_MAP.put(path, icon);
				}
			} else {
				icon = new ImageIcon();
			}
		}
		
		return icon;
	}
	
	
	private static Icon buildIcon(String path) {
		Icon icon;
		
		try {
			icon = new ImageIcon(getInstance().getClass().getResource(path));
		} catch (Exception e) {
			icon = null;
		}
		
		return icon;
	}	

	/**
	 * Get the applicaction's icon as an image
	 * 
	 * @return the application icon as an Image
	 */
	public static Image applicationImage() {
		try {
			return ImageIO.read(getInstance().getClass().getResource(APPLICATION_ICON));
		} catch (Exception e) {
			System.out.println("WTF? " +e.getMessage());
			return null;
		}
	}
	
	/**
	 * Get the application's icon
	 * 
	 * @return the application icon
	 */
	public static Icon applicationIcon() {
		return getIcon(APPLICATION_ICON);
	}
	
	/**
	 * Get the icon used for indicating that a product completed an action
	 * 
	 * @return the action completion icon
	 */
	public static Icon completeKey() {
		return getIcon(EXTRACT_KEY_FILE);
	}
	
	/**
	 * Get the icon used for indicating a product is pending an action
	 * 
	 * @return the pending processing icon
	 */
	public static Icon pendingKey() {
		return getIcon(PENDING_KEY_FILE);
	}
	
	/**
	 * Get the icon used for indicating a product failed an action
	 * 
	 * @return the failed processing icon
	 */
	public static Icon failedKey() {
		return getIcon(FAILED_KEY_FILE);
	}
	
	// Ancestor panel icons.
	/**
	 * Get the icon used for indicating a product file was received through
	 * downlink, not product processing
	 * 
	 * @return the downlinked product icon
	 */
	public static Icon downlink() {
		return getIcon(DOWNLINK_FILE);
	}
	
	/**
	 * Get the icon used for indicating that a tree has been collapsed and is
	 * not showing descendants
	 * 
	 * @return the tree closed icon
	 */
	public static Icon treeClosed() {
		return getIcon(RIGHT_TREE_ARROW_FILE);
	}
	
	/**
	 * Get the icon used for indicating that a tree is currently displaying its
	 * descendants
	 * 
	 * @return the tree open icon
	 */
	public static Icon treeOpen() {
		return getIcon(DOWN_TREE_ARROW_FILE);
	}

	/**
	 * Get the failed action icon
	 * 
	 * @return the failed action icon
	 */
	public static Icon failedAction() {
		return getIcon(FAILED_ACTION_FILE);
	}
	
	/**
	 * Get the pending action icon
	 * 
	 * @return the pending action icon
	 */
	public static Icon pendingAction() {
		return getIcon(PENDING_ACTION_FILE);
	}

	/**
	 * Gets the Icon for the mnemonic.
	 * @param mnemonic the mnemonic for which an icon is being retrieved
	 * @return the Icon for the supplied mnemonic
	 */
	public static Icon getActionIcon(String mnemonic) {
		return getActionIcon(mnemonic, null);
	}

	/**
	 * Gets the Icon for the mnemonic.  If extracted is true, will merge the extract small icon with the 
	 * action types icon. 
	 * 
	 * @param parentMnemonic the mnemonic for which an icon is being retrieved
	 * @param lastStatus The String value of the most recent status of the product. this works as follows:
	 * <ul><ul>
	 * <li>A null argument will not merge a small icon.</li>
	 * <li>An argument that matches a completed status will merge the completed small icon.</li>
	 * <li>An argument that matches a failed status will merge the failed small icon.</li>
	 * <li>Any other argument value will merge the pending small icon.</li>
	 * </ul></ul>
	 * 
	 * @return The mnemonic Icon.
	 */
	public static Icon getActionIcon(String parentMnemonic, String lastStatus) {
		Icon result;
		// This is going to be long and annoying, but I see no other way...
		
		Integer state;
		
		if (lastStatus == null) {
			state = Integer.MAX_VALUE;
		} else if (ProductAutomationStatusDAO.Status.COMPLETED.toString().equals(lastStatus)) {
			state = ZERO;
		} else if (ProductAutomationStatusDAO.Status.FAILED.toString().equals(lastStatus)) {
			state = -1;
		} else {
			state = 1;
		}
		
		// Only set user the completed if the extracted is true.  Else, just use the normal type icon.
		if (CORRECT_M.equals(parentMnemonic)) {
			if (state == Integer.MAX_VALUE) {
				result = correct();
			} else if (state == ZERO) {
				result = correctComplete();
			} else if (state == 1) {
				result = correctPending();
			} else {
				result = correctFailed();
			}
		} else if (UNWRAP_M.equals(parentMnemonic)) {
			if (state == Integer.MAX_VALUE) {
				result = unwrap();
			} else if (state == ZERO) {
				result = unwrapComplete();
			} else if (state == 1) {
				result = unwrapPending();
			} else {
				result = unwrapFailed();
			}
		} else if (DECOMP_M.equals(parentMnemonic)) {
			if (state == Integer.MAX_VALUE) {
				result = decompress();
			} else if (state == ZERO) {
				result = decompressComplete();
			} else if (state == 1) {
				result = decompressPending();
			} else {
				result = decompressFailed();
			}
		} else if (EXTRACT_M.equals(parentMnemonic)) {
			// This shold not happen.
			// Extract will always return the normal extracted icon.
			result = extract();
		} else if (DOWNLINK_M.equals(parentMnemonic)) {
			result = downlink();
		} else {
			result = null;
		}
		
		return result;
	}
	

	/**
	 * Tries to find the merged icon in the map.  If not found, will merge the extract icon in with the given
	 * background icon file given.  
	 * 
	 * @param mergedName the name of the merged icon
	 * @param background the icon to be placed in the backgorund of the merged icon
	 * @param foreground the icon to be places in the foreground of the merged icon
	 * @return the two icons merged as one
	 */
	private static Icon getMergedIcon(String mergedName, Icon background, Icon forground) {
		Icon merged;
		
		if (ICON_MAP.containsKey(mergedName)) {
			synchronized (ICON_MAP) {
				merged = ICON_MAP.get(mergedName);
			}
		} else {
			merged = new MergedIcon(background, forground);
			
			synchronized (ICON_MAP) {
				ICON_MAP.put(mergedName, merged);
			}
		}
		
		return merged;
		
	}
	
	// Control panel and ancestor shared methods.
	/**
	 * Get the merged icon for indicating a downlinked product has completed
	 * downlink
	 * 
	 * @return the completed product downlink icon
	 */
	public static Icon downlinkComplete() {
		return getMergedIcon(DOWNLINK_COMPLETE, downlink(), completeKey());
	}

	/**
	 * Get the merged icon for indicating a downlinked product has not completed
	 * downlink
	 * 
	 * @return the pending product downlink icon
	 */
	public static Icon downlinkPending() {
		return getMergedIcon(DOWNLINK_COMPLETE, downlink(), pendingKey());
	}

	/**
	 * Get the icon for indicating a downlinked product failed downlink
	 * 
	 * @return the failed product downlink icon
	 */
	public static Icon downlinkFailed() {
		return getMergedIcon(DOWNLINK_FAILED, downlink(), failedKey());
	}

	/**
	 * Get the icon for indicating a product must be corrected
	 * 
	 * @return the correct product icon
	 */
	public static Icon correct() {
		return getIcon(CORRECT_FILE);
	}

	/**
	 * Get the icon for indicating that a product's status is unknown
	 * 
	 * @return the unknown status icon
	 */
	public static Icon unknown() {
		return getIcon(UNKNOWN_FILE);
	}

	/**
	 * Get the merged icon indicating that a product completed the correction action
	 * 
	 * @return the correction completion icon
	 */
	public static Icon correctComplete() {
		return getMergedIcon(CORRECT_COMPLETE, correct(), completeKey());
	}

	/**
	 * Get the merged icon indicating that a product is pending the correction action
	 * 
	 * @return the correction pending icon
	 */
	public static Icon correctPending() {
		return getMergedIcon(CORRECT_PENDING, correct(), pendingKey());
	}

	/**
	 * Get the merged icon indicating that a product has failed the correction action
	 * 
	 * @return the correction failed icon
	 */
	public static Icon correctFailed() {
		return getMergedIcon(CORRECT_FAILED, correct(), failedKey());
	}

	/**
	 * Get the icon for indicating that a product must be unwrapped
	 * 
	 * @return the unwrap product icon
	 */
	public static Icon unwrap() {
		return getIcon(UNWRAP_FILE);
	}

	/**
	 * Get the merged icon indicating that a product has completed the unwrap action
	 * 
	 * @return the unwrap completion icon
	 */
	public static Icon unwrapComplete() {
		return getMergedIcon(UNWRAP_COMPLETE, unwrap(), completeKey());
	}

	/**
	 * Get the merged icon indicating that a product is pending the unwrap action
	 * 
	 * @return the unwrap pending icon
	 */
	public static Icon unwrapPending() {
		return getMergedIcon(UNWRAP_PENDING, unwrap(), pendingKey());
	}

	/**
	 * Get the merged icon indicating that a product has failed the unwrap action
	 * 
	 * @return the unwrap failed icon
	 */
	public static Icon unwrapFailed() {
		return getMergedIcon(UNWRAP_FAILED, unwrap(), failedKey());
	}

	/**
	 * Get the icon indicating that a product must be decompressed
	 * 
	 * @return the decompress product icon
	 */
	public static Icon decompress() {
		return getIcon(DECOMPRESS_FILE);
	}

	/**
	 * Get the merged icon indicating that a product completed the decompress action
	 * 
	 * @return the decompress complete icon
	 */
	public static Icon decompressComplete() {
		return getMergedIcon(COMPRESS_COMPLETE, decompress(), completeKey());
	}

	/**
	 * Get the merged icon indicating that a product is pending the decompress action
	 * 
	 * @return the decompress pending icon
	 */
	public static Icon decompressPending() {
		return getMergedIcon(COMPRESS_PENDING, decompress(), pendingKey());
	}

	/**
	 * Get the merged icon indicating that a product has failed the decompress action
	 * 
	 * @return the decompress failed icon
	 */
	public static Icon decompressFailed() {
		return getMergedIcon(COMPRESS_FAILED, decompress(), failedKey());
	}

	/**
	 * Get the icon indicating that a product must be extracted
	 * 
	 * @return the extract product icon
	 */
	public static Icon extract() {
		return getIcon(EXTRACT_FILE);
	}	
	
	// Control panel Icon conv methods.
	/**
	 * Get the play icon for the arbiter
	 * 
	 * @return the arbiter play icon
	 */
	public static Icon arbiterPlay() {
		return getIcon(ARBITER_START_FILE);
	}

	/**
	 * Get the stop icon for the arbiter
	 * 
	 * @return the arbiter stop icon
	 */
	public static Icon arbiterStop() {
		return getIcon(ARBITER_STOP_FILE);
	}

	/**
	 * Get the refresh icon
	 * 
	 * @return the refresh icon
	 */
	public static Icon refresh() {
		return getIcon(REFRESH_FILE);
	}

	/**
	 * Get the update icon
	 * 
	 * @return the update icon
	 */
	public static Icon controlUpdate() {
		return getIcon(UPDATE_FILE);
	}

	/**
	 * Get the icon indicating updates are pending
	 * 
	 * @return the pending updates icon
	 */
	public static Icon controlUpdatePending() {
		return getIcon(UPDATES_PENDING_FILE);
	}

	/**
	 * Get the icon indicating updates are not pending
	 * 
	 * @return the no pending updates icon
	 */
	public static Icon controlNoUpdatePending() {
		return getIcon(NO_UPDATES_PENDING_FILE);
	}

	/**
	 * Get the icon for indicating that an action can be enabled.
	 * 
	 * @return the enable action icon
	 */
	public static Icon controlActionEnabled() {
		return getIcon(ACTION_ENABLED_FILE);
	}

	/**
	 * Get the icon for indicating that an action can be disabled
	 * 
	 * @return the disable action icon
	 */
	public static Icon controlActionDisabled() {
		return getIcon(ACTION_DISABLED_FILE);
	}

	/**
	 * Get the icon for clearing data from the displayed table
	 * 
	 * @return the clear icon
	 */
	public static Icon clear() {
		return getIcon(CLEAR_FILE);
	}

	/**
	 * Get the icon for removing selected data from the table
	 * 
	 * @return the remove line file
	 */
	public static Icon remove() {
		return getIcon(REMOVE_FILE);
	}	

	/**
	 * Get the icon for allowing the results table to auto-update
	 * @return the results table "play" icon
	 */
	public static Icon rtStart() {
		return getIcon(RT_START_FILE);
	}	

	/**
	 * Get the icon for stopping the results table auto-update
	 * 
	 * @return the results table "stop" icon
	 */
	public static Icon rtStop() {
		return getIcon(RT_STOP_FILE);
	}	

	/**
	 * Get the icon for retrieving results from the database
	 * 
	 * @return the execute query icon
	 */
	public static Icon execute() {
		return getIcon(EXECUTE_FILE);
	}	
	
	/**
	 * Get the icon for including the full file path in the product name
	 * 
	 * @return the file path icon
	 */
	public static Icon includeFilePath() {
		return getIcon(INCLUDE_FILE);
	}	
	
	/**
	 * Get the icon for not including the full file path in the product name
	 * 
	 * @return the no file path icon
	 */
	public static Icon excludeFilePath() {
		return getIcon(EXCLUDE_FILE);
	}	
	/**
	 * Get the icon used for sorting the lineage view "top down", starting from the parent down through its descendants
	 * 
	 * @return the top down sorting icon
	 */
	public static Icon topDown() {
		return getIcon(TOP_DOWN_FILE);
	}	
	
	/**
	 * Get the icon used for sorting the lineage view "bottom up", starting from the child up through its ancestors
	 * 
	 * @return the bottom up sorting icon
	 */
	public static Icon bottomUp() {
		return getIcon(BOTTOM_UP_FILE);
	}		
	
	/**
	 * Get the icon used for exporting the displayed results
	 * 
	 * @return the export data icon
	 */
	public static Icon export() {
		return getIcon(EXPORT_FILE);
	}	
}
