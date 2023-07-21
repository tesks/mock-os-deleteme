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
package jpl.gds.product.utilities.file;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpl.gds.product.api.file.IProductMetadata;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilder;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeProperties;

/**
 *
 * 9/26/2016 - Abstract builder implementation.  Most of the functionality was moved from the old AbstractProductFilename class.
 * The mission specific methods were moved to the MSL builder.
 * 
 * 05/18/17 - Updated constructor to include the appContext as argument.
 *
 */
public abstract class AbstractProductFilenameBuilder implements IProductFilenameBuilder {
	private static final Pattern PRODUCT_PATTERN = Pattern.compile("(?<productPath>.+)/(?<productName>.+?)(?:_Partial)?(-\\d+)(\\.\\d+)?((.dat)|(.pdat)|(.emd)|(.pemd))$");
	private static final Pattern PRODUCT_PATH_AND_NAME_NO_EXT = Pattern.compile("(?<productNameWithPath>.+?)((.dat)|(.pdat)|(.emd)|(.pemd))$");

	private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^.+((.dat)|(.pdat)|(.emd)|(.pemd))$");
	private static final Pattern PARTIAL_FILENAME_PATTERN = Pattern.compile("^.+((.pdat)|(.pemd))$");

	/** doy pattern (regex matching) */
	protected static final Pattern				DOY_PATTERN						= Pattern.compile("/\\d\\d\\d\\d/\\d\\d\\d");
	/** ymd pattern (regex matching) */
	protected static final Pattern				YMD_PATTERN						= Pattern.compile("/\\d\\d\\d\\d/\\d\\d/\\d\\d");

	/** Regular expression for product path */
	protected static final String PRODUCT_PATH_REGEX_NAME = "productPath";
	/** Regular expression for product name */
	protected static final String PRODUCT_NAME_REGEX_NAME = "productName";
	/** Regular expression for product path and name */
	protected static final String PRODUCT_FULL_NO_EXT_NAME = "productNameWithPath";

	/** Indicates if the current path is valid */
	protected boolean isValid;
	/** Reasons path is not valid */
	protected StringBuilder invalidReasons;

	/** Original path to product file */
	protected String originalFilePath;
	/** Directory portion of path */
	protected String productPath;
	/** Product name portion of path */
	protected String productName;
	/** Indicates if the product is partial */
	protected boolean isPartial;
	
	/** product directory override config */
	private final String productDirOverrideConfig;

	protected boolean isCompressed;

	/**
	 * Creates a new builder.  If constructing a new file name object with a full product
	 * path for a dat or emd, use the addFullProductPath.  This will set all internally required fields
	 * and build can be called immediately.  Use the other add methods to set up the builder without the full
	 * path.
	 * @param appContext the current application context
	 */
	public AbstractProductFilenameBuilder(final ApplicationContext appContext) {
		invalidReasons = new StringBuilder();
		isValid = false;
		productDirOverrideConfig = appContext.getBean(IProductPropertiesProvider.class).getOverrideProductDir();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addFullProductPath(final String fullProductPath) {
		originalFilePath = fullProductPath;

		isValid = checkIsValid(originalFilePath);

		if (isValid) {
			productPath = checkAndGetProductPathOrName(originalFilePath, PRODUCT_PATH_REGEX_NAME);
			productName = checkAndGetProductPathOrName(originalFilePath, PRODUCT_NAME_REGEX_NAME);

			isPartial = checkIsPartial(originalFilePath);
		}

		return this;
	}

	/**
	 * Gets the compiled product pattern.  This expects a REGEX with named groups "productPath" and "productName".
	 * The value for the reference product builder is:
	 * </p>
	 * "(?<productPath>.+)/(?<productName>.+?)(?:_Partial)?(-\\d+)(\\.\\d+)?((.dat)|(.pdat)|(.emd)|(.pemd))$
	 * </p>
	 * @return pattern
	 */
	protected Pattern getProductPattern() {
		return PRODUCT_PATTERN;
	}

	/**
	 * Gets the compiled product path no extention pattern.  This expects a REGEX with named group "productNameWithPath".
	 * The value for the reference product builder is:
	 * </p>
	 * (?<productNameWithPath>.+)((.dat)|(.pdat)|(.emd)|(.pemd))$");
	 * </p>
	 * @return pattern
	 */
	protected Pattern getProductPathNoExtensionPattern() {
		return PRODUCT_PATH_AND_NAME_NO_EXT;
	}

	/**
	 * @return pattern used to check if a file path is a valid product path.
	 */
	protected Pattern getValidFilenamePattern() {
		return VALID_FILENAME_PATTERN;
	}

	/**
	 * Determines if the given file path is valid.
	 * 
	 * @param originalFilePath path to check
	 * @return true if the path matches to pattern returned by getValidFilenamePatter.
	 */
	protected boolean checkIsValid(final String originalFilePath) {
		return getValidFilenamePattern().matcher(originalFilePath).matches();
	}

	/**
	 * @return pattern used to indicate if a path is partial.
	 */
	protected Pattern getPartialFilenamePattern() {
		return PARTIAL_FILENAME_PATTERN;
	}

	/**
	 * Determines if the given path is to a partial product file.
	 * 
	 * @param originalFilePath path to check
	 * @return true if the path matches to pattern returned by getPartialFilenamePatter.
	 */
	protected boolean checkIsPartial(final String originalFilePath) {
		return getPartialFilenamePattern().matcher(originalFilePath).matches();
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addProductPath(final String productPath) {
		if (originalFilePath == null) {
			this.productPath = productPath;
		}

		return this;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addProductName(final String productName) {
		if (originalFilePath == null) {
			this.productName = productName;
		}

		return this;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addIsPartial(final boolean isPartial) {
		if (originalFilePath == null) {
			this.isPartial = isPartial;
		}

		return this;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addProductMetadata(final IProductMetadataProvider metadata) {
		if (originalFilePath == null) {
			this.isPartial = metadata.isPartial();
			this.productPath = metadata.getAbsoluteDataFile();
			this.productName = metadata.getFilename();
		}

		return this;
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public IProductFilenameBuilder addProductFilename(final IProductFilename pfn) {
		if (originalFilePath == null) {
			this.isPartial = pfn.isPartial();
			this.productPath = pfn.getProductPath();
			this.productName = pfn.getProductName();
		}

		return this;
	}


	/**
	 * Sanity check to be used before doing a build. Checks that the product path and name are set.
	 *
	 * @return true if all is good to build.
	 */
	protected boolean isValid() {
		boolean valid = true;
		if (isValid && originalFilePath != null && this.invalidReasons.length() > 0) {
			valid = false;
		} else if(productPath == null) {
			addInvalidReason("productPath is null");
			valid = false;
		} else if(productName == null) {
			addInvalidReason("productName is null");
			valid = false;
		}

		return valid;
	}

	/**
	 * @return originalFilePath with extension stripped, null if not defined.
	 */
	protected String getPathWithNoExtension() {
		if (originalFilePath == null) {
			return null;
		} else {
			final Matcher matcher = getProductPathNoExtensionPattern().matcher(originalFilePath);
			return matcher.find() ? matcher.group(PRODUCT_FULL_NO_EXT_NAME) : null;
		}
	}

	/**
	 * Checks if the product matches the PRODUCT_PATTERN and will extract the group name
	 * given, either productPath or productName.
	 *
	 * @param originalFilePath the path
	 * @param groupName the group name of what is requested, either productPath or productName.
	 * @return the product path or name
	 */
	protected String checkAndGetProductPathOrName(final String originalFilePath, final String groupName) {
		final Matcher matcher = getProductPattern().matcher(originalFilePath);

		final String field = matcher.find() ? matcher.group(groupName) : null;

		if (field == null) {
			addInvalidReason("Unable to find " + groupName + " from input product file.");
		}

		return field;
	}


	/**
	 * Adds to the internal invalid reasons buffers so that better error messages can be created when attempting to build.
	 * @param message the error message to include when attempting to build this.
	 */
	protected void addInvalidReason(final String message) {
		invalidReasons
			.append("(")
			.append(message)
			.append("()");
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public String getContextOutputDirectory(final IContextConfiguration xsQuerySession) {
		return getRootContextOutputDirectoryPathPart(xsQuerySession) + getContextSpecificOutputDirectoryPathPart(xsQuerySession);
	}
	/**
	 *
	 * This method returns the root Context output directory for the given Contxt Configuration.
	 *
	 * It does this by calculating the YYYY/DOY/<PRODUCT_LINE>/products directory string from the provided ContextConfiguration,
	 * and searching through the ContextConfigurations output directory field for the first occurrence of this string. It then
	 * returns a string composed of the DataFilename string up to and NOT including the calculated (and matched) string.
	 *
	 * @param sc
	 *            the context configuration after it has been modified to the specification of the XS Payload
	 * @return the root path (up to and NOT including the SCET) of the session output directory path.
	 */
	protected String getRootContextOutputDirectoryPathPart(final IContextConfiguration sc) {
		return truncatePathBeforeDateTimeElement(sc.getGeneralInfo().getOutputDir());
	}

	/**
	 * It does this by calculating the YYYY/DOY products directory string from the provided ContextConfiguration, and searching
	 * through the ContextConfiguration's output directory field for the first occurrence of this string. It then returns a string
	 * composed of the DataFilename string up to and NOT including the calculated (and matched) string.
	 *
	 * @param rootDirectory
	 *            the path to truncate.
	 * @return truncated path
	 */
	protected String truncatePathBeforeDateTimeElement(final String rootDirectory) {
		Matcher m = null;
		int idx = -1;

		for (final Pattern pattern: new Pattern[] { DOY_PATTERN, YMD_PATTERN }) {
			m = pattern.matcher(rootDirectory);
			if (m.find()) {
				idx = m.start();
				break;
			}
		}
		return (idx < 0) ? rootDirectory : rootDirectory.substring(0, idx);
	}

	/**
	 *
	 * This method returns the root Product output directory for the given Context Configuration.
	 *
	 * It does this by calculating the YYYY/DOY/<PRODUCT_LINE>/products directory string from the provided ContextConfiguration,
	 * and searching through the ContextConfiguration's output directory field for the first occurrence of this string. It then
	 * returns a string composed of the DataFilename string up to and NOT including the calculated (and matched) string.
	 *
	 * @param sc
	 *            the context configuration after it has been modified to the specification of the XS Payload
	 * @return the root path (up to and NOT including the SCET) of the product output directory path.
	 */
	protected String getRootProductOutputDirectoryPathPart(final IContextConfiguration sc) {
		/*
		 * 02/16/2012 Set root product directory based on venuType and whether it is in OPS or TEST. Use Context
		 * Output Directory for TEST Use GDS Configuration's Product Builder's Product Output Directory for OPS unless it is null, in
		 * which case, fall-back to Context Output Directory.
		 */
		String rootProductDirectory = sc.getVenueConfiguration().getVenueType().isOpsVenue() ? productDirOverrideConfig : sc.getGeneralInfo().getOutputDir();
		if (null == rootProductDirectory) {
			rootProductDirectory = sc.getGeneralInfo().getOutputDir();
		}
		return truncatePathBeforeDateTimeElement(rootProductDirectory);
	}

	/**
	 *
	 * Create the part of the context output directory path that is unique with respect to the start date/time of the specified
	 * ContextConfiguration.
	 *
	 * @param sc
	 *            the ContextConfiguration whose start date/time will be used to create the path part.
	 * @return the path part
	 */
	protected String getDateComponentOfContextPath(final IContextConfiguration sc) {
		return getDateComponentOfPath(sc.getContextId().getStartTime());
	}

	/**
	 * Create the part of the product filename path that is unique with respect to the SCET of the specified product metadata.
	 * @param md the IProductMetadataProvider whose SCET will be used to create the path part.
	 * @return the path part
	 */
	protected String getDateComponentOfProductPath(final IProductMetadataProvider md) {
		return getDateComponentOfPath(md.getScet());
	}

	/**
	 * Given a Date object, create a string suitable for inclusion in a Context Output Directory path or a product storage path.
	 * Choose format based upon whether the TimeProperties specifies usig YEAR/DOY or YEAR/MONTH/DAY formats.
	 *
	 * @param date
	 *            the date from which to create the path element.
	 * @return the date component
	 */
    protected String getDateComponentOfPath(final IAccurateDateTime date) {
		final StringBuilder s = new StringBuilder();
		final boolean useDoyDir = TimeProperties.getInstance().useDoyOutputDirectory();
		
		final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTimeInMillis(date.getTime());

		if (useDoyDir) {
			// add the year, month, day (GMT)
			s.append(File.separator);
			s.append(GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4, '0'));
			s.append(File.separator);
			s.append(GDR.fillStr(String.valueOf(c.get(Calendar.DAY_OF_YEAR)), 3, '0'));
		}
		else {
			// add the year, month, day (GMT)
			s.append(File.separator);
			s.append(GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4, '0'));
			// we have to add 1 to the month because Calendar.JANUARY equals 0 and Calendar.DECEMBER equals 11
			s.append(File.separator);
			s.append(GDR.fillStr(String.valueOf(c.get(Calendar.MONTH) + 1), 2, '0'));
			s.append(File.separator);
			s.append(GDR.fillStr(String.valueOf(c.get(Calendar.DAY_OF_MONTH)), 2, '0'));
		}
		return s.toString();
	}

	/**
	 * Calculate the correct context data output path for the specified context configuration. The value returned from
	 * this method is intended to be used in conjunction with the base context output directory.
	 *
	 * @param sc
	 *            the specified ContextConfiguration

	 * @return the correct context data output path for the specified ContextConfiguration. The value returned from
	 *         this method is intended to be used in conjunction with the base context output directory.
	 */
	protected String getContextSpecificOutputDirectoryPathPart(final IContextConfiguration sc) {
		final StringBuilder s = new StringBuilder(getDateComponentOfContextPath(sc));
		s.append(File.separator);
		s.append(ReleaseProperties.getProductLine().toLowerCase());
		s.append(File.separator);
		s.append(sc.getContextId().getHost());
		s.append(File.separator);
		s.append(sc.getContextId().getUser() + "_" + sc.getContextId().getName().replaceAll("\\W", "_"));
		s.append("_" + sc.getContextId().getStartTimeStr().replaceAll("\\W", "_"));
		return s.toString();
	}

	/**
	 * Calculates the product-specific part of the product output directory as specified context configuration and metadata. The
	 * value returned from this method is intended to be used in conjunction with the base context output directory.
	 *
	 * @see #getContextSpecificOutputDirectoryPathPart
	 *
	 * @param md
	 *            the specified metadata
	 * @return the correct context data output path for the specified metadata. The value returned from
	 *         this method is intended to be used in conjunction with the base context output directory.
	 */
	protected String getProductFilenamePathPart(final IProductMetadataProvider md) {
		final StringBuilder s = new StringBuilder();
		s.append(File.separator);
		s.append("products");
		s.append(File.separator);
		s.append(md.getProductType());
		s.append(File.separator);
		s.append(md.getProductType());
		s.append('_');
		s.append(md.getDvtString());
		return s.toString();
	}

	/**
	 * Create the part of a product filename path or context output directory path that is unique with respect to the Venue type,
	 * specifically whether or not it is an OPS venue.
	 *
	 * @param sc the ContextConfiguration for the path part to be created
	 * @param md the product metadata
	 * @return the path part
	 */
	protected String getVenueSpecificProductPathPart(final IContextConfiguration sc, final IProductMetadataProvider md) {
		final StringBuilder s = new StringBuilder();
		if (sc.getVenueConfiguration().getVenueType().isOpsVenue()) {
			s.append(getDateComponentOfProductPath(md));
			s.append(File.separator);
			s.append(ReleaseProperties.getProductLine().toLowerCase());
		}
		else {
			s.append(getContextSpecificOutputDirectoryPathPart(sc));
		}
		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductFilenameBuilder addIsCompressed(final boolean isCompressed) {
		if (originalFilePath == null) {
			this.isCompressed = isCompressed;
		}

		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductFilenameBuilder addVenueAppropriateFullyQualifiedDataFilename(final IContextConfiguration sc, final IProductMetadata md) {
		final StringBuilder s = new StringBuilder(getRootProductOutputDirectoryPathPart(sc));
		s.append(getVenueSpecificProductPathPart(sc, md));
		s.append(File.separator);

		s.append("products" + File.separator + md.getProductType());

		addIsCompressed(md.getIsCompressed());
		addIsPartial(md.isPartial());
		addProductPath(s.toString());
		addProductName(md.getFilenameNoVersionOrExtension());

		return this;
	}
}
