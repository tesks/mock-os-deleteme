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
 * File: ViewClassMap.java
 *
 */
package jpl.gds.perspective.view;

import java.util.HashMap;


/**
 * ViewClassMap is used when processing old perspective files that may contain old class
 * names. These old names can be mapped to their current class names using this class.
 *
 *
 */
public class ViewClassMap {

    private static final HashMap<String,String> classMap = new HashMap<>();
    
    static {
        
        /*******************************************************************************
          IMPORTANT: Do not touch this class map except to add new entries. 
          These class names should NOT be changed due to any refactor. They are here to
          map old class names in perspectives to new ones.
         *******************************************************************************/
        classMap.put("jpl.gds.monitor.gui.views.ChannelListPageComposite", "jpl.gds.core.monitor.gui.views.ChannelListPageComposite");
        classMap.put("jpl.gds.monitor.gui.views.ChannelListPageComposite", "jpl.gds.monitor.guiapp.gui.views.ChannelListPageComposite");
        classMap.put("jpl.gds.monitor.gui.views.ChannelChartPageComposite", "jpl.gds.core.monitor.gui.views.ChannelChartPageComposite");
        classMap.put("jpl.gds.monitor.gui.views.EvrComposite", "jpl.gds.core.monitor.gui.views.EvrComposite");
        classMap.put("jpl.gds.monitor.gui.views.ProductStatusComposite", "jpl.gds.core.monitor.gui.views.ProductStatusComposite");
        classMap.put("jpl.gds.monitor.gui.views.CommandMessageComposite", "jpl.gds.core.monitor.gui.views.CommandMessageComposite");
        classMap.put("jpl.gds.monitor.gui.views.StatusMessageComposite", "jpl.gds.core.monitor.gui.views.StatusMessageComposite");
        classMap.put("jpl.gds.monitor.gui.views.MessageListComposite", "jpl.gds.core.monitor.gui.views.MessageListComposite");
        classMap.put("jpl.gds.monitor.gui.views.CustomGridComposite", "jpl.gds.core.monitor.gui.views.CustomGridComposite");
        classMap.put("jpl.gds.monitor.gui.views.ViewTabShell", "jpl.gds.core.monitor.gui.views.TabularViewShell");
        classMap.put("jpl.gds.core.monitor.gui.views.ViewTabShell", "jpl.gds.core.monitor.gui.views.TabularViewShell");
        classMap.put("jpl.gds.down.gui.MessageTable", "jpl.gds.core.down.gui.MessageTable");
        classMap.put("jpl.gds.monitor.gui.ChannelListTabItem", "jpl.gds.core.monitor.gui.ChannelListTabItem");
        classMap.put("jpl.gds.monitor.gui.ChartTabItem", "jpl.gds.core.monitor.gui.ChartTabItem");
        classMap.put("jpl.gds.monitor.gui.EvrTabItem", "jpl.gds.core.monitor.gui.EvrTabItem");
        classMap.put("jpl.gds.monitor.gui.ProductStatusTabItem", "jpl.gds.core.monitor.gui.ProductStatusTabItem");
        classMap.put("jpl.gds.monitor.gui.CommandMessageTabItem", "jpl.gds.core.monitor.gui.CommandMessageTabItem");
        classMap.put("jpl.gds.monitor.gui.StatusMessageTabItem", "jpl.gds.core.monitor.gui.StatusMessageTabItem");
        classMap.put("jpl.gds.monitor.gui.MessageListTabItem", "jpl.gds.core.monitor.gui.MessageListTabItem");
        classMap.put("jpl.gds.monitor.gui.CustomGridTabItem", "jpl.gds.core.monitor.gui.CustomGridTabItem");
        classMap.put("jpl.gds.core.monitor.gui.ChannelListTabItem", "jpl.gds.core.monitor.gui.tab.ChannelListTabItem");
        classMap.put("jpl.gds.core.monitor.gui.ChartTabItem", "jpl.gds.core.monitor.gui.tab.ChartTabItem");
        classMap.put("jpl.gds.core.monitor.gui.EvrTabItem", "jpl.gds.core.monitor.gui.tab.EvrTabItem");
        classMap.put("jpl.gds.core.monitor.gui.ProductStatusTabItem", "jpl.gds.core.monitor.gui.tab.ProductStatusTabItem");
        classMap.put("jpl.gds.core.monitor.gui.CommandMessageTabItem", "jpl.gds.core.monitor.gui.tab.CommandMessageTabItem");
        classMap.put("jpl.gds.core.monitor.gui.StatusMessageTabItem", "jpl.gds.core.monitor.gui.tab.StatusMessageTabItem");
        classMap.put("jpl.gds.core.monitor.gui.MessageListTabItem", "jpl.gds.core.monitor.gui.tab.MessageListTabItem");
        classMap.put("jpl.gds.core.monitor.gui.CustomGridTabItem", "jpl.gds.core.monitor.gui.tab.CustomGridTabItem");
        classMap.put("jpl.gds.core.monitor.gui.EncodingWatchTabItem", "jpl.gds.core.monitor.gui.tab.EncodingWatchTabItem");
        classMap.put("jpl.gds.core.monitor.gui.AlarmTabItem", "jpl.gds.core.monitor.gui.tab.AlarmTabItem");
        classMap.put("jpl.gds.core.monitor.gui.FastAlarmTabItem", "jpl.gds.core.monitor.gui.tab.FastAlarmTabItem");
        classMap.put("jpl.gds.core.monitor.gui.FixedLayoutTabItem", "jpl.gds.core.monitor.gui.tab.FixedLayoutTabItem");
        classMap.put("jpl.gds.core.monitor.gui.FrameAccountabilityTabItem", "jpl.gds.core.monitor.gui.tab.FrameAccountabilityTabItem");
        classMap.put("jpl.gds.core.monitor.gui.FrameWatchTabItem", "jpl.gds.core.monitor.gui.tab.FrameWatchTabItem");
        classMap.put("jpl.gds.core.monitor.gui.PacketWatchTabItem", "jpl.gds.core.monitor.gui.tab.PacketWatchTabItem");

        // R8 entries
        classMap.put("jpl.gds.core.monitor.gui.views.TabularViewShell", "jpl.gds.monitor.guiapp.gui.TabularViewShell");
        classMap.put("jpl.gds.core.monitor.gui.views.SingleViewShell", "jpl.gds.monitor.guiapp.gui.SingleViewShell");
        classMap.put("jpl.gds.core.monitor.gui.tab.ChannelListTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.ChannelListTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.ChartTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.ChartTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.EvrTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.EvrTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.NatEvrTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.NatEvrTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.ProductStatusTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.ProductStatusTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.CommandMessageTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.CommandMessageTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.StatusMessageTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.StatusMessageTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.MessageListTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.MessageListTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.CustomGridTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.CustomGridTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.EncodingWatchTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.EncodingWatchTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.FastAlarmTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.FastAlarmTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.FixedLayoutTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.FixedLayoutTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.FrameAccountabilityTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.FrameAccountabilityTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.FrameWatchTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.FrameWatchTabItem");
        classMap.put("jpl.gds.core.monitor.gui.tab.PacketWatchTabItem", "jpl.gds.monitor.guiapp.gui.views.tab.PacketWatchTabItem");     
        classMap.put("jpl.gds.core.monitor.gui.views.ChannelListPageComposite", "jpl.gds.monitor.guiapp.gui.views.ChannelListPageComposite");
        /* Replaces dastardley comma with period! */
        classMap.put("jpl.gds.core.monitor.gui.views.ChannelChartPageComposite", "jpl.gds.monitor.guiapp.gui.views.ChannelChartPageComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.NatEvrComposite", "jpl.gds.monitor.guiapp.gui.views.NatEvrComposite");
        /* All EVR view components should now be mapped to NatEvr components. */
        classMap.put("jpl.gds.core.monitor.gui.views.EvrComposite", "jpl.gds.monitor.guiapp.gui.views.NatEvrComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.ProductStatusComposite", "jpl.gds.monitor.guiapp.gui.views.ProductStatusComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.CommandMessageComposite", "jpl.gds.monitor.guiapp.gui.views.CommandMessageComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.StatusMessageComposite", "jpl.gds.monitor.guiapp.gui.views.StatusMessageComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.MessageListComposite", "jpl.gds.monitor.guiapp.gui.views.MessageListComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.CustomGridComposite", "jpl.gds.monitor.guiapp.gui.views.CustomGridComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.EncodingWatchComposite", "jpl.gds.monitor.guiapp.gui.views.EncodingWatchComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.FastAlarmComposite", "jpl.gds.monitor.guiapp.gui.views.FastAlarmComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.FixedLayoutComposite", "jpl.gds.monitor.guiapp.gui.views.FixedLayoutComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.FrameAccountabilityComposite", "jpl.gds.monitor.guiapp.gui.views.FrameAccountabilityComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.FrameWatchComposite", "jpl.gds.monitor.guiapp.gui.views.FrameWatchComposite");
        classMap.put("jpl.gds.core.monitor.gui.views.PacketWatchComposite", "jpl.gds.monitor.guiapp.gui.views.PacketWatchComposite");
        classMap.put("jpl.gds.core.down.gui.MessageTable", "jpl.gds.telem.down.gui.MessageTable");
    }
    
    private ViewClassMap() {
        //do nothing
    }
    
    /**
     * Gets the current view class name that an old class name maps to. If there is no new class,
     * the old class name is simply returned.
     * @param oldClass  the old class name to map
     * @return the new class name
     */
    public static String mapClassName(final String oldClass) {
        String newClass = classMap.get(oldClass);
        if (newClass == null) {
            return oldClass;
        }
        /* Second classmap lookup for values that point to another key */
        else if (classMap.containsKey(newClass)) {
            newClass = classMap.get(newClass);
        }
        return newClass;
    }
}
