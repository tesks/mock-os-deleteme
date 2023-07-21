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
package jpl.gds.product.impl.decom;

import java.io.IOException;
import java.io.PrintStream;

import jpl.gds.product.api.decom.DecomDataType;
import jpl.gds.product.api.decom.IProductDecomField;
import jpl.gds.product.api.decom.ProductDecomFieldType;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.types.ByteArraySlice;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.shared.types.MemoryByteStream;
import jpl.gds.shared.util.ByteOffset;

/**
 * 
 * DefinitionElement is the parent class for all decom field definitions. The
 * DefinitionElement not only stores the detailed metadata about the data field
 * it represents, but it capable of reading the field value directly from a data
 * stream.
 * 
 */
public abstract class AbstractDecomField implements IProductDecomField {

	protected static final Tracer tracer = TraceManager.getTracer(Loggers.TLM_PRODUCT);


	/**
	 * Name of this decom field.
	 */
	protected String name;
	/**
	 * Print format string for this decom field.
	 */
    protected String printFormat;

    private String title;
	private boolean suppressName = false;
	private String fswName;
	private String fswDescription;
	private String sysDescription;
	private final ProductDecomFieldType fieldType;
	   
    /**
     * Constructs an AbstractDecomField with the given decom field type.
     * 
     * @param type the ProductDecomFieldType being created
     */
	protected AbstractDecomField(final ProductDecomFieldType type) {
	    fieldType = type;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ProductDecomFieldType getDecomFieldType() {
	    return fieldType;
	}

    /**
     * {@inheritDoc}
     */
    @Override
	public String getPrintFormat() {
        return printFormat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void setPrintFormat(final String printFormat) {
        this.printFormat = printFormat;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFswDescription() {
		return fswDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFswDescription(final String fswDescription) {
		this.fswDescription = fswDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSysDescription() {
		return sysDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSysDescription(final String sysDescription) {
		this.sysDescription = sysDescription;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int printValue(final ByteArraySlice data, final IDecomOutputFormatter out)
	throws IOException {
		final MemoryByteStream stream = new MemoryByteStream(data);
		return printValue(stream, out, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int printValue(final ByteStream stream, final IDecomOutputFormatter out)
	throws IOException {
		return printValue(stream, out, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void printType(final PrintStream out) throws IOException {
		printType(out, 0);
	}


	/**
	 * Prints a four character indentation (uses spacing) depth number of times to the
	 * current output position in the given print stream.
	 * @param out print stream to write to
	 * @param depth level of indentation
	 * @throws IOException if the re is a problem writing to the output stream
	 */
	protected void printIndent(final PrintStream out, final int depth) throws IOException {
		out.print("    ");
		for (int i = 0; i < depth; ++i) {
			out.print("    ");

		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSuppressName(final boolean suppressName) {
		this.suppressName = suppressName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean getSuppressName() {
		return suppressName;
	}

	/**
	 * Gets the value of a bit field in the given data slice.
	 * 
	 * @param bitoffset the offset of the value within the data
	 * @param bitlength the length of the value, in bits
	 * @param data the ByteArraySlice to read the value from
	 * @return the value of the BitField as an Integer
	 */
	protected Number getBitValue(final int bitoffset, final int bitlength,
			final ByteArraySlice data) {
		return GDR.get_u16(data.array, data.offset, bitoffset, bitlength);
	}

	/**
	 * Gets the value of a numeric field in the given data slice.
	 * 
	 * @param dataType the dictionary definition type of the data
	 * @param data the ByteArraySlice to read the value from
	 * @param newOffset a ByteOffset object used to track position in the
	 *            ByteArraySlice; will be updated to reflect the number of bytes
	 *            read
	 * @return the field value as a Number object; the type will vary based upon
	 *         the field type
	 */
	protected Object getValue(final DecomDataType dataType, final ByteArraySlice data,
			final ByteOffset newOffset) {

		switch (dataType.getBaseType()) {
		case UNSIGNED_INT:
		case DIGITAL: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 8:
				return GDR.get_u8(data.array, data.offset);
			case 16:
				return GDR.get_u16(data.array, data.offset);
			case 24:
				return (long) GDR.get_u24(data.array, data.offset);
			case 32:
				return GDR.get_u32(data.array, data.offset);
			case 64:
				tracer.debug("Field definitions contain 64 bit unsigned field; values greater than 2**63 may not display correctly");
				return GDR.get_u64(data.array, data.offset);
			default:
				throw new IllegalStateException(
						"Unsupported unsigned integer, status, or digital bit length in field definitions: "
						+ dataType.getBitLength());
			}
		}

		case SIGNED_INT: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 8:
				return GDR.get_i8(data.array, data.offset);
			case 16:
				return GDR.get_i16(data.array, data.offset);
			case 24:
				return (long) GDR.get_i24(data.array, data.offset);
			case 32:
				return (long) GDR.get_i32(data.array, data.offset);
			case 64:
				return GDR.get_i64(data.array, data.offset);
			default:
				throw new IllegalStateException(
						"Unsupported integer bit length in field definition: "
						+ dataType.getBitLength());
			}
		}

		case FLOAT: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 32:
				return GDR.get_float(data.array, data.offset);
			case 64:
				return GDR.get_double(data.array, data.offset);
			default:
				throw new IllegalStateException(
						"Unsupported float bit length in field definition: "
						+ dataType.getBitLength());
			}
		}

		case ENUMERATION: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 8:
				return GDR.get_u8(data.array, data.offset);
			case 16:
				return GDR.get_u16(data.array, data.offset);
			case 24:
				return (0xffffff00 & GDR.get_u32(data.array, data.offset)) >> 8;
			case 32:
				return GDR.get_u32(data.array, data.offset);
			default:
				throw new IllegalStateException(
						"Unsupported enumeration bit length in field definition: "
						+ dataType.getBitLength());
			}
		}

		case STRING: {
			newOffset.inc(dataType.getByteLength());
			return GDR.get_string(data.array, data.offset, dataType
					.getByteLength());
		}

		case BOOLEAN: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 8:
				return GDR.get_u8(data.array, data.offset);
			case 16:
				return GDR.get_u16(data.array, data.offset);
			case 24:
				return (long) GDR.get_u24(data.array, data.offset);
			case 32:
				return GDR.get_u32(data.array, data.offset);
			default:
				throw new IllegalStateException(
						"Unsupported boolean bit length in field definition: "
						+ dataType.getBitLength());
			}
		}

		case TIME: {
			newOffset.inc(dataType.getByteLength());
			switch (dataType.getBitLength()) {
			case 32:
				return new Sclk(GDR.get_u32(data.array, data.offset), 0);
			case 64:
				long fine = GDR.get_u32(data.array, data.offset + 4);
				fine = (fine >> 16) & 0x000000000000FFFF;
				return new Sclk(GDR.get_u32(data.array, data.offset),fine);
			default:
				throw new IllegalStateException(
						"Unsupported time bit length in field definition: "
						+ dataType.getBitLength());
			}
		}
		
        case FILL:
            break;
            
        case UNKNOWN:
            break;

        default:
            break;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getValue(final DecomDataType dataType, final ByteStream stream) {
		ByteArraySlice slice = null;
		slice = stream.read(dataType.getByteLength());
		final ByteOffset offset = new ByteOffset();
		return getValue(dataType, slice, offset);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFswName() {
		return fswName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFswName(final String fswName) {
		this.fswName = fswName;
	}
}
