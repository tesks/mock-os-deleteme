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
package jpl.gds.compression.lzo;


/**
 1/2/2003        
 - removed #ifdef __cplusplus conditional code
 - removed #if 0 unconditional non-code
 - removed #if 1 from around unconditional code
 - 2/3/2003 removed compression software
 - 08/29/2012 ported to Java for MPCS for MSL
 */

/**
 minilzo.c -- mini subset of the LZO real-time data compression library

 This file is part of the LZO real-time data compression library.

 Copyright (C) 2002 Markus Franz Xaver Johannes Oberhumer

 All Rights Reserved.

 The LZO library is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation; either version 2 of
 the License, or (at your option) any later version.

 The LZO library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the LZO library; see the file COPYING.
 If not, write to the Free Software Foundation, Inc.,
 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

 Markus F.X.J. Oberhumer
 <markus@oberhumer.com>
 http://www.oberhumer.com/opensource/lzo/

 * NOTE:
 *   the full LZO package can be found at
 *   http://www.oberhumer.com/opensource/lzo/
 */

/**
 * Ported to Java from FSW Source Code for MPCS 7.0.0
 *
 */
public final class LzoDecompress {
	/**
	 * 
	 */
	static final Lzo1xDecompressor	deCompressor	= new Lzo1xDecompressor();

	/**
	 * @param src
	 * @param srcLength
	 * @param dst
	 * @param dstLength
	 * @return
	 */
	public static final LzoConstant lzo1x_decompress(final byte[] src, final int srcLength, final byte[] dst, final Int dstLength) {
		return LzoConstant.getValue(deCompressor.decompress(src, 0, srcLength, dst, 0, dstLength));
	}

	/**
	 * lzo1x_decompress
	 *
	 * @param src
	 * @param srcOffset
	 * @param srcLength
	 * @param dst
	 * @param dstOffset
	 * @param dstLength
	 * @return LzoConstant
	 * @throws DecompressionException 
	 */
	public static final LzoConstant lzo1x_decompress(final byte[] src, final int srcOffset, final int srcLength, final byte[] dst, final int dstOffset, final Int dstLength) throws DecompressionException {
		try {
			return LzoConstant.getValue(deCompressor.decompress(src, srcOffset, srcLength, dst, dstOffset, dstLength));
		}
		catch (Throwable t) {
			throw new DecompressionException("LZO Decompression failed: " + t, t);
		}
	}
}
