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

package jpl.gds.db.app.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.db.api.types.IDbCommandProvider;
import jpl.gds.db.api.types.IDbEvrProvider;
import jpl.gds.db.api.types.IDbLog1553Provider;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbProductMetadataProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.app.config.GetEverythingProperties;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.evr.api.util.EvrColorUtility;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * OutputFormatter used by GetEverythingApp to generate Excel output.  
 * Only used for creating a Excel sorted file for now.
 *
 */
public class ExcelOutputFormatter extends AbstractOutputFormatter {
			
	/**
	 * A string representing that there is no data to display to the file. ie Command files
	 * have no ERT time.  
	 */
	private final static String NO_DATA = " ";
	
	/**
	 * Maximum number of rows allowed in one excel sheet/workbook
	 */
	private final static int MAX_NUM_ROWS = 32746;	

	/**
	 * The extension of the excel files
	 */
	private final static String EXCEL_EXT = ".xlsx";
	
	/*
	 * Names of report fields.
	 */
	private static final String ERT = "ERT";
	private static final String SCET = "SCET";
	private static final String SCLK = "SCLK";
	private static final String SOURCE = "SOURCE";
	private static final String TYPE = "TYPE";
	private static final String DATA = "DATA";
	private static final String BACKGROUND_COLOR = "Background_Color";
	private static final String ID = "ID";
	
	/**
	 * Map for the header title to data string corresponding to that title.
	 */
	private Map<String,String> dataMap;	

	/**
	 *  The previous EHA dn value stored in a map (the key is the channel id).  Used for delta calculations.
	 */
	private final Map<String,Object> preValMap = new HashMap<String,Object>();
	
	/**
	 * The current row number of the excel sheet being printed to.
	 */
	private int rowNum;
	
	/**
	 * The excel sheet being printed to.
	 */
	private Sheet sheet;
	
	/**
	 * The excel workbook being printed to
	 */
	private Workbook wb;
	
	/**
	 * Mapping from data type to a map of excel cell style properties
	 */
	private Map<String,Map<String,Object>> dtPropMap;
	
	/**
	 * Mapping of excel cell style properties for the header
	 */
	private Map<String,Object> headerPropMap;
	
	
	/**
	 * The total number of columns in the excel sheet.
	 */
	private int numCols;
	
	/**
	 * The fileName of the excel files not including the workbook number
	 */
	private String fileName;
	
	
	/**
	 * Counter for the number of excel workbooks being printed to.  Starts a new excel workbook 
	 * after maxNumRows have been used.
	 */
	private int wbCnt;
		  
	/*
	 * Excel color indices for various records.
	 */
	private final XSSFColor headerForeColor;
	private final XSSFColor ehaForeColor;
	private final XSSFColor prodForeColor;
	private final XSSFColor evrForeColor;
	private final XSSFColor logForeColor;
	private final XSSFColor cmdForeColor;
	private final XSSFColor log1553ForeColor;
	private final XSSFColor headerBackColor;
	private final XSSFColor ehaBackColor;
	private final XSSFColor prodBackColor;
	private final XSSFColor evrBackColor;
	private final XSSFColor logBackColor;
	private final XSSFColor cmdBackColor;
	private final XSSFColor log1553BackColor;
	
	/**
	 * Indicates whether to color code EVRs by level.
	 */
	private boolean useEvrColorCoding;
	
	/**
	 * Hash tables of EVR level to foregound and background colors
	 */
	private Map<String, XSSFColor> evrForegroundColors;
	private Map<String, XSSFColor> evrBackgroundColors;
	private final Map<String, XSSFFont> evrForegroundFonts = new HashMap<String,XSSFFont>();
	
	private boolean useChannelFormat;
	
	/**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public ExcelOutputFormatter(final ApplicationContext appContext) {
		super(appContext);
		
		this.useChannelFormat = appContext.getBean(DictionaryProperties.class).useChannelFormatters();
		
		final GetEverythingProperties guiProp = new GetEverythingProperties();
		
		this.useEvrColorCoding = guiProp.isGetEverythingEvrColorCodingUsed();

		String [] colorPair = guiProp.getEverythingHeaderColors();
		
	    headerBackColor = loadColor(colorPair[0], ChillColor.ColorName.BLACK);
	    headerForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    colorPair = guiProp.getEverythingChannelColors();
		ehaBackColor = loadColor(colorPair[0], ChillColor.ColorName.ORANGE);
		ehaForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
		colorPair = guiProp.getEverythingEvrColors();
		evrBackColor = loadColor(colorPair[0], ChillColor.ColorName.GREEN);
		evrForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    colorPair = guiProp.getEverythingProductColors();
		prodBackColor = loadColor(colorPair[0], ChillColor.ColorName.BLUE);
	    prodForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    colorPair = guiProp.getEverythingLogColors();
	    logBackColor = loadColor(colorPair[0], ChillColor.ColorName.BLUE_GREY);
	    logForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    colorPair = guiProp.getEverythingCommandColors();
	    cmdBackColor = loadColor(colorPair[0], ChillColor.ColorName.RED);
	    cmdForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    colorPair = guiProp.getEverything1553LogColors();
	    log1553BackColor = loadColor(colorPair[0], ChillColor.ColorName.AQUA);
	    log1553ForeColor = loadColor(colorPair[1], ChillColor.ColorName.WHITE);
	    
	    final EvrColorUtility colorUtility = new EvrColorUtility(appContext.getBean(EvrProperties.class));
	    if (useEvrColorCoding) {
            final Map<String, ChillColor> evrForegrounds = colorUtility.getForegroundColorsForAllLevels();
	    	if (evrForegrounds == null) {
	    		useEvrColorCoding = false;
	    		return;
	    	}
	    	evrForegroundColors = new HashMap<String, XSSFColor>(evrForegrounds.size());
	    	
	    	for (final String level: evrForegrounds.keySet()) {
	    		final String newLevel = "EVR " + level;
	    		final ChillColor chillColor = evrForegrounds.get(level);
	    		final XSSFColor excelColor = new XSSFColor();
	    		final byte[] rgb = new byte[3];
	    		rgb[0] = (byte)chillColor.getRed();
	    		rgb[1] = (byte)chillColor.getGreen();
	    		rgb[2] = (byte)chillColor.getBlue();
	    		excelColor.setRGB(rgb);
	    		evrForegroundColors.put(newLevel, excelColor);	    		
	    	}
	    	
            final Map<String, ChillColor> evrBackgrounds = colorUtility.getBackgroundColorsForAllLevels();
	    	if (evrBackgrounds == null) {
	    		useEvrColorCoding = false;
	    		return;
	    	}
	    	evrBackgroundColors = new HashMap<String, XSSFColor>(evrBackgrounds.size());
	    	
	    	for (final String level: evrBackgrounds.keySet()) {
	    		final String newLevel = "EVR " + level;
	    		final ChillColor chillColor = evrBackgrounds.get(level);
	    		final XSSFColor excelColor = new XSSFColor();
	    		final byte[] rgb = new byte[3];
	    		rgb[0] = (byte)chillColor.getRed();
	    		rgb[1] = (byte)chillColor.getGreen();
	    		rgb[2] = (byte)chillColor.getBlue();
	    		excelColor.setRGB(rgb);
	    		evrBackgroundColors.put(newLevel, excelColor);	    		
	    	}    	
	    }

	}
	
	private XSSFColor loadColor(final String colorStr, final ChillColor.ColorName defaultColor) {
		
        XSSFColor result = null;
				
		try {
			final String[] rgbStr = colorStr.split(",");
			final byte[] rgb = new byte[3];
			rgb[0] = (byte)Integer.parseInt(rgbStr[0]);
			rgb[1] = (byte)Integer.parseInt(rgbStr[1]);
			rgb[2] = (byte)Integer.parseInt(rgbStr[2]);
			result = new XSSFColor();
			result.setRGB(rgb);
			
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
			final ChillColor color = new ChillColor(defaultColor);
			final byte[] rgb = new byte[3];
			rgb[0] = (byte)color.getRed();
			rgb[1] = (byte)color.getGreen();
			rgb[2] = (byte)color.getBlue();
			result = new XSSFColor();
			result.setRGB(rgb);
		}
		
		return result;
	}
    
    /**
     * Prints to the current excel wb/sheet. Also, autosizes the columns and
     * freezes the tile pane.
     * 
     * @throws IOException
     *             if there is an IO error while printing Excel file
     */
    @Override
    public void printFormatterSpecificFile() throws IOException {
    	
		final FileOutputStream fileStrm = new FileOutputStream(fileName + "_" + wbCnt++ + EXCEL_EXT);
		final Sheet sht = wb.getSheetAt(0);
		
		// auto size the columns before printing:
		for (int k = 0; k < numCols; k++) {
			sht.autoSizeColumn(k);
		}
		
		// freeze title row:
		sht.createFreezePane(0,1);
		
		wb.write(fileStrm);	
		
	    fileStrm.close();
    	
    }
	
	/**
     * {@inheritDoc}
	 */
	@Override
	public void setUpSorted(final String filename) throws FileNotFoundException {

		wb = new XSSFWorkbook();
        sheet = wb.createSheet("SORTED"); 
        rowNum = 0;
        //dtPropMap = new HashMap<String,Map<String,Object>>();
        //headerPropMap = new HashMap<String,Object>();
        fileName = filename;
        wbCnt = 1;
        //setFont();
        
        setupStyleProperties(); 
	}

	private void setupStyleProperties() {
		dtPropMap = new HashMap<String,Map<String,Object>>();
	    headerPropMap = new HashMap<String,Object>();
	    // Header style properties:
        headerPropMap.put(BACKGROUND_COLOR, headerBackColor);
        XSSFFont font = (XSSFFont)wb.createFont();
		font.setColor(headerForeColor);
		headerPropMap.put("Font", font);
		
        // EHA style properties:
		final Map<String,Object> propMap1 = new HashMap<String,Object>();
		propMap1.put(BACKGROUND_COLOR, ehaBackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(ehaForeColor);
		propMap1.put("Font", font);
		dtPropMap.put("Eha", propMap1);
		
		// EVR style properties:
		final Map<String,Object> propMap2 = new HashMap<String,Object>();
		propMap2.put(BACKGROUND_COLOR, evrBackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(evrForeColor);
		propMap2.put("Font", font);
		dtPropMap.put("Evr", propMap2);
		
		// CMD style properties:
		final Map<String,Object> propMap3 = new HashMap<String,Object>();
		propMap3.put(BACKGROUND_COLOR, cmdBackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(cmdForeColor);
		propMap3.put("Font", font);
		dtPropMap.put("Cmd", propMap3);
		
		// PROD style properties:
		final Map<String,Object> propMap4= new HashMap<String,Object>();
		propMap4.put(BACKGROUND_COLOR, prodBackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(prodForeColor);
		propMap4.put("Font", font);
		dtPropMap.put("Prod", propMap4);
		
		// LOG style properties:
		final Map<String,Object> propMap5= new HashMap<String,Object>();
		propMap5.put(BACKGROUND_COLOR, logBackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(logForeColor);
		propMap5.put("Font", font);
		dtPropMap.put("Log", propMap5);
		
		// LOG 1553 style properties:
		final Map<String,Object> propMap6= new HashMap<String,Object>();
		propMap6.put(BACKGROUND_COLOR, log1553BackColor);
		font = (XSSFFont)wb.createFont();
		font.setColor(log1553ForeColor);
		propMap6.put("Font", font);
		dtPropMap.put("1553 Log", propMap6);
		
	}
	
	/**
	 * Sets up the data map utilizing data from IDbQueryable object.
	 * 
	 * @param dq The IDbQueryable object to create the map from
	 */
	public void setUpDataMap(final IDbQueryable dq) {
				
		dataMap = new LinkedHashMap<String,String>();
		dqMap = dq.getFileData(NO_DATA);   
		
        if (dq instanceof IDbChannelSampleUpdater) {
			dataMap.put(ERT,dqMap.get(ERT.toLowerCase()));
			dataMap.put(SCET,dqMap.get(SCET.toLowerCase()));
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));
			dataMap.put(SOURCE,dqMap.get(SOURCE.toLowerCase()));
			dataMap.put(TYPE,dqMap.get("type"));
			dataMap.put(ID,dqMap.get("csv_id"));
			
    		// Delta Calculations:
            final IDbChannelSampleUpdater dcs = (IDbChannelSampleUpdater) dq;
            final String chanId = dcs.getChannelId();
			if (!preValMap.containsKey(chanId)) {
				preValMap.put(chanId, null);	// Store null for the previous value if this is the first time the channel has been processed
			}
    		dcs.calculateDelta(preValMap.get(chanId), dqMap, NO_DATA);
    		Object dn = dqMap.get("dn");
			Object eu = dqMap.get("eu");
			// TODO - This does not work. The problem is the that DN and EU formatters are not in the CSV file
			// output by chill_get_chanvals, and the dictionary is not loaded, so the format specifiers are 
			// not known.
			if (useChannelFormat) {
				if (dcs.getDnFormat() != null) {
					dn = formatUtil.anCsprintf(dcs.getDnFormat(), dn).trim();
				}
				if (eu != null && dcs.getEuFormat() != null) {
					eu = formatUtil.anCsprintf(dcs.getEuFormat(), eu).trim();
				}
			}
    		if (!dqMap.get("status").equals(NO_DATA)) {
    			dataMap.put(DATA,"DN=" + dn + " DELTA=" + dqMap.get("delta") + 
    					" STATUS=" + dqMap.get("status") + " DN_ALARM=" + dqMap.get("dn_alarm_state") + 
    					" EU_ALARM=" + dqMap.get("eu_alarm_state"));
    		} else {
    			dataMap.put(DATA,"DN=" + dn + " DELTA=" + dqMap.get("delta") + 
    					" EU=" + eu + " DN_ALARM=" + dqMap.get("dn_alarm_state") + 
    					" EU_ALARM=" + dqMap.get("eu_alarm_state"));
    		}
    		preValMap.put(chanId,dcs.getValue());	// Store the previous value
		}
        else if (dq instanceof IDbEvrProvider) {
			dataMap = new LinkedHashMap<String,String>();
			dataMap.put(ERT,dqMap.get(ERT.toLowerCase()));
			dataMap.put(SCET,dqMap.get(SCET.toLowerCase()));
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));
			dataMap.put(SOURCE,dqMap.get(SOURCE.toLowerCase()));	
			dataMap.put(TYPE,dqMap.get("type"));
			dataMap.put(ID,dqMap.get("evr_name"));
			dataMap.put(DATA,dqMap.get(DATA.toLowerCase()));
    		dataMap.put("SEQUENCE_ID",dqMap.get("id"));
		}
        else if (dq instanceof IDbCommandProvider) {
			dataMap = new LinkedHashMap<String,String>();
			dataMap.put(ERT,dqMap.get(ERT.toLowerCase()));
			dataMap.put(SCET,dqMap.get(SCET.toLowerCase()));
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));
			dataMap.put(SOURCE,"MPCS");
			dataMap.put(TYPE,dqMap.get(SOURCE.toLowerCase()));
   			dataMap.put(ID,dqMap.get("id"));
   			dataMap.put(DATA,dqMap.get(DATA.toLowerCase()));
		}
		else if (dq instanceof IProductMetadataProvider) {
			dataMap = new LinkedHashMap<String,String>();
			dataMap.put(ERT,dqMap.get(ERT.toLowerCase()));
			dataMap.put(SCET,dqMap.get(SCET.toLowerCase()));
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));	
			dataMap.put(SOURCE,dqMap.get(SOURCE.toLowerCase()));
			dataMap.put(TYPE,dqMap.get("type"));
   			dataMap.put(ID,dqMap.get("csv_id"));
   			dataMap.put(DATA,dqMap.get(DATA.toLowerCase()));
		}
        else if (dq instanceof IDbLogProvider) {
			dataMap = new LinkedHashMap<String,String>();
			dataMap.put(ERT,dqMap.get(ERT.toLowerCase()));
			dataMap.put(SCET,dqMap.get(SCET.toLowerCase()));
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));	
    		dataMap.put(SOURCE,dqMap.get(SOURCE.toLowerCase()));	
			dataMap.put(TYPE,dqMap.get("type"));
			dataMap.put(ID,dqMap.get("id"));
			dataMap.put(DATA,dqMap.get(DATA.toLowerCase()));
		}
        else if (dq instanceof IDbLog1553Provider) {
			dataMap = new LinkedHashMap<String,String>();
			dataMap.put(ERT,dqMap.get("sysTime"));
			dataMap.put(SCET,NO_DATA);
			dataMap.put(SCLK,dqMap.get(SCLK.toLowerCase()));	
    		dataMap.put(SOURCE,dqMap.get("bus"));	
			dataMap.put(TYPE,"1553 LOG");
			dataMap.put(ID,NO_DATA);
			dataMap.put(DATA,"RT=" + dqMap.get("remoteTerminal") + " SA=" + dqMap.get("subAddress") + 
					    " TR=" + dqMap.get("transmitReceiveStatus") + " Data=" + dqMap.get(DATA.toLowerCase()));
		}
		
	}
	
	/**
     * {@inheritDoc}
	 */
	@Override
	public void setUpDataMapSorted() {
				
		dataMap = new LinkedHashMap<String,String>();
		dataMap.put(ERT,null);
		dataMap.put(SCET,null);
		dataMap.put(SCLK,null);
		dataMap.put(SOURCE,null);
		dataMap.put(TYPE,null);
		dataMap.put(ID,null);
		dataMap.put(DATA,null);
  		dataMap.put("SEQUENCE_ID",null);
  		
  		numCols = dataMap.size();	// The number of columns in the excel sheet
	}
	
	/**
	 * Writes to the PrintWriter associated with the IDbQueryable object passed.
	 * 
   	 * @param dq the IDbQueryable object to write
	 * @param isHeader should be true if printing a table header, otherwise false
	 * @throws IOException I/O error
	 */
    @Override
	public void writeObjectCustom(final IDbQueryable dq, final boolean isHeader) throws IOException {
		
		Map<String,Object> propMap = null;
		boolean isEvr = false;
		
        if (dq instanceof IDbChannelSampleProvider) {
			propMap = dtPropMap.get("Eha");
		}
        else if (dq instanceof IDbEvrProvider) {
			propMap = dtPropMap.get("Evr");
			isEvr= true;
		}
        else if (dq instanceof IDbCommandProvider) {
			propMap = dtPropMap.get("Cmd");
		}
        else if (dq instanceof IDbProductMetadataProvider) {
			propMap = dtPropMap.get("Prod");
		}
        else if (dq instanceof IDbLogProvider) {
			propMap = dtPropMap.get("Log");
		}
        else if (dq instanceof IDbLog1553Provider) {
			propMap = dtPropMap.get("1553 Log");
		}
		
		// If rowNum is over the maximum allowed in one workbook:
		if (rowNum > MAX_NUM_ROWS) {
            printFormatterSpecificFile(); // Prints the wb before creating a new
                                          // one
			wb = new XSSFWorkbook();
	        sheet = wb.createSheet("SORTED"); 
	        //setFont();
	        setupStyleProperties();
			setUpDataMapSorted();
			evrForegroundFonts.clear();
			setExcelLine(dataMap,propMap,headerPropMap,sheet,wb,0, false);	// Sets up the header
			setUpDataMap(dq);
			setExcelLine(dataMap,propMap,headerPropMap,sheet,wb,1, isEvr);	// Sets up a line for the data
			rowNum=2;
		}
		else if (isHeader) {
			setUpDataMapSorted();
			setExcelLine(dataMap,propMap,headerPropMap,sheet,wb,rowNum++, false);	
		}
		else {
			setUpDataMap(dq);
			setExcelLine(dataMap,propMap,headerPropMap,sheet,wb,rowNum++, isEvr);	
		}
		
	}
	
	/**
     * {@inheritDoc}
	 */
    @Override
	public void writeObjectEL(final IDbQueryable dq, final String level, final boolean isHeader) {
		// Not used.  No EVR level Excel files.
	}
	
	/**
     * {@inheritDoc}
     *
	 * @see jpl.gds.db.app.util.OutputFormatter#writeObjectSorted(java.lang.String, boolean)
	 */
    @Override
	public void writeObjectSorted(final String data, final boolean isHeader) {
		// Not used.  Only need writeObjectExcel().
	}

	/**
     * {@inheritDoc}
     */
    @Override
	public void writeObject(final IDbQueryable dq, final boolean isHeader) {
		// Not used.  See writeObjectExcel()
	}
	
	/**
     * Sets up a line for a excel sheet for printing.
     * 
     * @param dataMap is a map consisting of header column name and the corresponding value string
     * @param propMap is a map consisting of excel style properties
     * @param headerPropMap is map of excel style properties for the header
     * @param sheet Sheet object for the excel writer
     * @param wb Workbook object for the excel writer
     * @param rowNum the row number that is being printed to on the excel sheet
     * @param isEvr true if the incoming data record is for an EVR
     */
    protected void setExcelLine(final Map<String,String> dataMap, final Map<String,Object> propMap, final Map<String,Object> headerPropMap, 
    		final Sheet sheet, final Workbook wb, final int rowNum, final boolean isEvr) {
	
   		if (dataMap != null) {
   		   
			int k = 0;
			final Row row = sheet.createRow(rowNum);
   			final XSSFCellStyle style = (XSSFCellStyle)wb.createCellStyle();
   			
   			// Header:
			if (rowNum == 0) {
				if (headerPropMap.containsKey(BACKGROUND_COLOR)) {
	    			style.setFillForegroundColor((XSSFColor) headerPropMap.get(BACKGROUND_COLOR));
	    			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				}
				if (headerPropMap.containsKey("Font")) {
					style.setFont((Font) headerPropMap.get("Font"));	
				} 
			}
			// Data:
			else if (rowNum > 0) {
				if (isEvr && useEvrColorCoding) {
					final String level = dataMap.get(TYPE);
					XSSFColor foreColor = evrForegroundColors.get(level);
					XSSFColor backColor = evrBackgroundColors.get(level);
					if (foreColor == null) {
						foreColor = evrForeColor;
					}
					if (backColor == null) {
						backColor = evrBackColor;
					}
					XSSFFont font = evrForegroundFonts.get(level);
					if (font == null) {
						font = (XSSFFont)wb.createFont();
						font.setColor(foreColor);
						evrForegroundFonts.put(level, font);
					}
					style.setFillForegroundColor(backColor);
					style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					style.setFont(font);	
				} else {
					if (propMap.containsKey(BACKGROUND_COLOR)) {
						style.setFillForegroundColor((XSSFColor)propMap.get(BACKGROUND_COLOR));
						style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
					}
					if (propMap.containsKey("Font")) {
						style.setFont((Font) propMap.get("Font"));	
					} 
				}
			}

    		for (final Entry<String,String> entry : dataMap.entrySet()) {
    			final Cell cell = row.createCell(k++);
				cell.setCellStyle(style);
				
    			if (rowNum == 0) {
        			cell.setCellValue(entry.getKey());
    			}
    			else if (rowNum > 0) {
        			cell.setCellValue(entry.getValue());
    			}    			
    		} 
    		
		}
    }
}
