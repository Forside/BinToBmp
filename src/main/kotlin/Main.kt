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


import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private fun convertFileToBitmap(file: File) {
	// create output bitmap file
	val bmp = File("out.bmp")
	bmp.createNewFile()
	val output = BufferedOutputStream(FileOutputStream(bmp))

	// initialize bitmap factory
	val factory = BitmapFactory().initialize()

	// read all bytes from input file and write into factory
	val input = FileInputStream(file)
	var pixel = input.read()
	while (pixel != -1) {
		factory.addPixel(pixel.toByte())
		pixel = input.read()
	}
	input.close()

	// create the bitmap data from the added bytes
	val data = factory.create()

	// write into bitmap file
	output.write(data)
	output.close()
}

fun main(args: Array<String>) {
	if (args.isEmpty()) {
		println("Error: No file supplied!")
		return
	}

	val inFile = File(args[0])
	if (!inFile.exists()) {
		println("Error: File '${args[0]}' does not exist!")
		return
	}

	convertFileToBitmap(inFile)
}
