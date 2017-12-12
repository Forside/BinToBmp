/*
 * This file is part of 2017 (Copyright 2017 Jonas Hülsermann).
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

package de.forside


import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.io.*
import java.util.concurrent.LinkedBlockingQueue

private var running = true
private val handlerQueue = LinkedBlockingQueue<Runnable>()
private var progressBar: CharArray = CharArray(32, {_ -> ' '})
private var progressBarLength = 0

private val handler = object: Handler() {
	override fun handleMessage(what: Int, data: Bundle?) {
		handlerQueue.add(Runnable {
			when (what) {
				BitmapFactory.INTERACTION_INIT ->
					print( ansi().cursorLeft(1000).eraseLine().a("Read: ").saveCursorPosition() )

				BitmapFactory.INTERACTION_READ -> {
					val kb = data!!.getInt("kb")
					print( ansi().restoreCursorPosition().a("$kb kb") )
				}

				BitmapFactory.INTERACTION_CONV_START -> {
					prepareProgressBar()
					val size = data!!.getInt("size")
					print( ansi()
							.restoreCursorPosition().a("$size kb")
							.newline().a("Conversion: ").saveCursorPosition().newline() )
				}

				BitmapFactory.INTERACTION_CONV_PROGRESS -> {
					val progress = data!!.getInt("progress")
					val size = data.getInt("size")
					val relN = progress.toDouble() / size.toDouble()
					val relH = Math.floor(relN * 100).toInt()
					updateProgressBar(relN)
					print( ansi()
							.restoreCursorPosition().a("$progress / $size   ($relH%)")
							.cursorDownLine().a(progressBar) )
				}

				BitmapFactory.INTERACTION_CONV_END -> {
					val size = data!!.getInt("size")
					print( ansi()
							.restoreCursorPosition().a("Conversion: $size / $size   (100%)")
							.cursorDownLine().eraseLine().a("Finished").newline() )
					running = false
				}

				BitmapFactory.INTERACTION_CANCEL ->
					running = false
			}
		})
	}
}

private fun prepareProgressBar() {
	progressBar[0] = '['
	progressBar[progressBar.size-1] = ']'
	progressBar[1] = '>'
}

private fun updateProgressBar(normalizedProgress: Double) {
	val newProgressBarLength = Math.floor(normalizedProgress * progressBar.size-2).toInt()
	if (newProgressBarLength > progressBarLength) {
		for (i in progressBarLength+1 .. newProgressBarLength)
			progressBar[i] = '='
		progressBar[newProgressBarLength+1] = '>'
		progressBarLength = newProgressBarLength
	}
}

private fun convertFileToBitmap(file: File) {
	// create output bitmap file
	val bmp = File(file.nameWithoutExtension + ".bmp")
	bmp.createNewFile()
	val output = BufferedOutputStream(FileOutputStream(bmp))

	// initialize bitmap factory
	val factory = BitmapFactory().initialize(handler)

	// read all bytes from input file and write into factory
	val input = BufferedInputStream(FileInputStream(file))
	var pixel = input.read()
	while (pixel != -1) {
		factory.addPixel(pixel.toByte())
		pixel = input.read()
	}
	input.close()

	// create the bitmap data from the added bytes
	val data = factory.create()

	// write into bitmap file
	output.write(data )
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

	AnsiConsole.systemInstall()

	Thread(Runnable {
		convertFileToBitmap(inFile)
	}).start()

	while (running) {
		handlerQueue.take().run()
	}
}
