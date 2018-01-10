/*
 * This file is part of BinToBmp (Copyright 2017 Jonas Hülsermann).
 *
 * BinToBmp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BinToBmp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BinToBmp.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.forside.bintobmp


import org.json.JSONArray
import java.awt.Dimension
import java.io.File
import java.util.*

class BitmapFactory {

	// public static values
	companion object {
		val INTERACTION_CONV_START = 0
		val INTERACTION_CONV_PROGRESS = 1
		val INTERACTION_CONV_END = 2
		val INTERACTION_READ = 3
		val INTERACTION_INIT = 4
		val INTERACTION_CANCEL = 5
	}
	// interaction handler
	private var handler: Handler? = null

	//private val jarLocation = File(URLDecoder.decode(BitmapFactory::class.java.protectionDomain.codeSource.location.path, "UTF-8")).parentFile

	private val FILE_SIZE_OFFSET = 2
	private val IMAGE_DATA_OFFSET_OFFSET = 10
	private val IMAGE_WIDTH_OFFSET = 18
	private val IMAGE_HEIGHT_OFFSET = 22
	private val IMAGE_SIZE_OFFSET = 34
	private val COLOR_TABLE_OFFSET = 54

	// array containing all bytes for the bitmap
	private val data = ArrayList<Byte>()
	// array containing the pixel data
	private val imageData = ArrayList<Byte>()

	/**
	 * Initialize the [BitmapFactory]. Adds genereic header informations
	 * into the bitmap.
	 *
	 * @return [BitmapFactory] object to be further used to add pixels and create the bitmap
	 */
	fun initialize(handler: Handler? = null): BitmapFactory {
		this.handler = handler

		// BITMAPFILEHEADER (14 bytes)
		data.addAll(arrayOf<Byte>(
			// 0x00 0 bfType: bitmap header
			0x42, 0x4D,

			// 0x02 2 bfSize: file size (modified at create())
			0, 0, 0, 0,

			// 0x06 6 bfReserved
			0, 0, 0, 0,

			// 0x0A 10 bsOffBits: image data offset
			0x36, 0, 0, 0 // 54 (without color table)
		))

		// BITMAPINFO (40 bytes)
		data.addAll(arrayOf<Byte>(
			// 0x0E 14 biSize: BITMAPINFOHEADER size
			0x28, 0, 0, 0, // 40

			// 0x12 18 biWidth: image width in pixel
			0, 0, 0, 0,

			// 0x16 22 biHeight: image height in pixel
			0, 0, 0, 0,

			// 0x1A 26 biPlanes: unused
			1, 0,

			// 0x1C 28 biBitCount: color depth
			8, 0,

			// 0x1E 30 biCompression
			0, 0, 0, 0, // BI_RGB: no compression

			// 0x22 34 biSizeImage: size of image data
			0, 0, 0, 0, // can be 0 when using BI_RGB

			// 0x26 38 biXPelsPerMeter: horizontal resolution
			0x1F, 0x1C, 0, 0,

			// 0x2A 42 biYPelsPerMeter: vertical resolution
			0x1F, 0x1C, 0, 0,

			// 0x2E 48 biClrUsed: entriey count in color table
			0, 0, 0, 0, // max count

			// 0x32 52 biClrImportant
			0, 0, 0, 0 // all colors
		))

		// Cancel execution if now color palette is found
		if (!writeColorTable()) {
			handler?.sendMessage(INTERACTION_CANCEL)
			throw Exception("unable to load color palette")
		}

		handler?.sendMessage(INTERACTION_INIT)

		return this
	}

	/**
	 * Add a pixel to the bitmap.
	 *
	 * @param   colorIndex  index in the color table to be used for the next pixel
	 * @return  this [BitmapFactory] object for chaining multiple commands
	 */
	fun addPixel(colorIndex: Byte): BitmapFactory {
		imageData.add(colorIndex)
		if (imageData.size % 10240 == 0) {
			val bundle = Handler.Bundle()
			bundle.putInt("kb", imageData.size / 1024)
			handler?.sendMessage(INTERACTION_READ, bundle)
		}
		return this
	}

	/**
	 * Finalizes the bitmap and returns a {@link ByteArray} containing
	 * the header, color table and all pixels.
	 *
	 * @return  [ByteArray] containing all bitmap data
	 */
	fun create(): ByteArray {
		// get best dimensions for a square image with given pixel count
		val dimension = getDimension()
		writeInt(IMAGE_WIDTH_OFFSET, dimension.width, 4)
		// negative height to draw from top to bottom
		writeInt(IMAGE_HEIGHT_OFFSET, -dimension.height, 4)

		val bundle = Handler.Bundle()
		bundle.putInt("size", imageData.size / 1024)
		handler?.sendMessage(INTERACTION_CONV_START, bundle)

		// add line ending paddings (each line has to have a length of a multiple of 4)
		val paddedImageData = padLines(dimension)
		data.addAll(paddedImageData)
		// the whole file length also has to be a multiple of 4 + it apparently always needs extra padding beyond the last pixel
		// thus we don't use getPadding() here because it could give us 0 padding
		val endPadding = 4 - (data.size % 4)
		for (i in 0 until endPadding)
			data.add(0)

		writeInt(FILE_SIZE_OFFSET, data.size, 4)
		writeInt(IMAGE_SIZE_OFFSET, paddedImageData.size+endPadding, 4)

		bundle.putInt("size", imageData.size)
		handler?.sendMessage(INTERACTION_CONV_END, bundle)

		return data.toByteArray()
	}

	/**
	 * Adds padding to whole image and to the pixel line endings.
	 *
	 * @param   dimension   the desired image dimensions to reach
	 * @return  new [ArrayList] containing the padded image data
	 */
	private fun padLines(dimension: Dimension): ArrayList<Byte> {
		// expand the pixel data to fill the last line
		var missing = dimension.width * dimension.height - imageData.size
		while (missing-- > 0)
			imageData.add(0)

		// get the padding needed for every line
		val padding = getPadding(dimension.width, 4)
		// if no padding is needed, return the original image data
		if (padding == 0)
			return imageData

		// create a second array, which will inhabit the padded pixel lines
		val paddedImageData = ArrayList<Byte>()


		// add all pixels to the new array plus the padding for every line
		for (i in 0 until imageData.size) {
			paddedImageData.add(imageData[i])
			if ((i+1) % dimension.width == 0) {
				for (p in 0 until padding)
					paddedImageData.add(0)

				val bundle = Handler.Bundle()
				bundle.putInt("progress", i)
				bundle.putInt("size", imageData.size)
				handler?.sendMessage(INTERACTION_CONV_PROGRESS, bundle)
			}
		}

		return paddedImageData
	}

	/**
	 * Find best dimensions for a square image with given pixel count.
	 *
	 * @return  [Dimension] containing the calculated width and height
	 */
	private fun getDimension(): Dimension {
		val result = Dimension()

		if (imageData.size > 0) {
			// get square root of image size
			val root = Math.sqrt(imageData.size.toDouble())
			// round down. this will be the smallest possible width and height
			val rootFloor = Math.floor(root).toInt()

			// if root is > rounded root, add another column
			val widthI = rootFloor + (if (root > rootFloor) 1 else 0)
			// if root is >= rounded root + 0.5, add another row
			val heightI = rootFloor + (if (root >= rootFloor + 0.5) 1 else 0)

			// we have found the least possible amount of rows and columns for a square image
			result.width = widthI
			result.height = heightI
		}

		return result
	}

	/**
	 * Load a color table from a file 'color.json' either from
	 * outside the application jar or using the embedded file.
	 * The first 256 colors defined in this file will be written
	 * to the bitmap.
	 *
	 * @return  [Boolean] true if a color file could be loaded and has at least 256 colors defined
	 */
	private fun writeColorTable(): Boolean {
		// https://jonasjacek.github.io/colors/data.json

		// load colors data from json file
		val colorsFile = File(/*jarLocation,*/ "colors.json")
		val jsonText = if (colorsFile.exists()) {
			// first look for file outside jar
			colorsFile.readText()
		} else {
			// use embedded file if none outside is found
			this.javaClass.classLoader.getResource("colors.json") ?: return false
			val colorsInput = this.javaClass.classLoader.getResourceAsStream("colors.json").bufferedReader()
			colorsInput.readText()
		}

		val jsonColors = JSONArray(jsonText)
		// stop if there are less than 256 colors
		if (jsonColors.length() < 256)
			return false

		// iterate over the first 256 colors and grab the rgb values
		for (i in 0 until 256) {
			val jsonColor = jsonColors.getJSONObject(i)
			val rgb = jsonColor.getJSONObject("rgb")
			val r = rgb.getInt("r").toByte()
			val g = rgb.getInt("g").toByte()
			val b = rgb.getInt("b").toByte()
			val color = arrayOf(b, g, r, 0)
			data.addAll(color)
		}

		val imageDataOffset = COLOR_TABLE_OFFSET + (256 * 4)
		writeInt(IMAGE_DATA_OFFSET_OFFSET, imageDataOffset, 4)

		return true
	}

	/**
	 * Conveniently writes an integer of any length into the little endian bitmap.
	 *
	 * @param   offset  byte offset in data array where to begin writing
	 * @param   value   int value to write
	 * @param   length  byte length of int value
	 */
	private fun writeInt(offset: Int, value: Int, length: Int) {
		for (i in 0 until length)
			data[offset + i] = value.ushr(i * 8).and(0xFF).toByte()
	}

	/**
	 * Get padding needed to make a number into a multiple of another number.
	 *
	 * @param   size        current size
	 * @param   multiple    number the size should be a multiple of
	 * @return  Int         padding needed
	 */
	private fun getPadding(size: Int, multiple: Int): Int {
		val padding = size % multiple
		return if (padding == 0)
			padding else
			multiple - padding
	}

}
