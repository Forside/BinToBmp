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

package de.forside.bintobmp


open class Handler {

	private var callback: Callback? = null

	class Bundle {
		private var intData: HashMap<String, Int> ?= null
		private var stringData: HashMap<String, String> ?= null
		private var boolData: HashMap<String, Boolean> ?= null

		fun putInt(name: String, value: Int) {
			if (intData == null)
				intData = HashMap()

			intData?.put(name, value)
		}
		fun getInt(name: String): Int {
			if (intData == null || !intData!!.contains(name)) {
				throw Exception("Key $name does not exist!")
			} else {
				return intData?.get(name)!!
			}
		}

		fun putString(name: String, value: String) {
			if (stringData == null)
				stringData = HashMap()

			stringData?.put(name, value)
		}
		fun getString(name: String): String {
			if (intData == null || !intData!!.contains(name)) {
				throw Exception("Key $name does not exist!")
			} else {
				return stringData?.get(name)!!
			}
		}

		fun putBoolean(name: String, value: Boolean) {
			if (boolData == null)
				boolData = HashMap()

			boolData?.put(name, value)
		}
		fun getBooealn(name: String): Boolean {
			if (intData == null || !intData!!.contains(name)) {
				throw Exception("Key $name does not exist!")
			} else {
				return boolData?.get(name)!!
			}
		}
	}

	constructor()
	constructor(callback: Callback) {
		this.callback = callback
	}

	fun sendMessage(what: Int, data: Bundle? = null)  {
		if (callback == null)
			handleMessage(what, data)
		else
			callback?.handleMessage(what, data)
	}

	open fun handleMessage(what: Int, data: Bundle?) {}

	interface Callback {
		fun handleMessage(what: Int, data: Bundle?)
	}

}