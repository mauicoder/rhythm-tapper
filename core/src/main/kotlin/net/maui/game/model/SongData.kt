package net.maui.game.model

import com.badlogic.gdx.files.FileHandle
import kotlin.properties.Delegates


class SongData {
    var songName: String by Delegates.notNull<String>()
    var songDuration by Delegates.notNull<Float>()
    var keyTimeList = ArrayList<Pair<String, Float>>()
    var keyTimeIndex = 0

    fun addKeyTime(k: String, t: Float) {
        keyTimeList.add(Pair(k, t))
    }
    fun resetIndex() {
        keyTimeIndex = 0
    }

    fun advanceIndex() {
        keyTimeIndex++
    }

    fun getCurrentKeyTime(): Pair<String, Float> {
        return keyTimeList[keyTimeIndex]
    }
    fun keyTimeCount(): Int {
        return keyTimeList.size
    }

    fun isFinished(): Boolean {
        return keyTimeIndex >= keyTimeCount()
    }
    fun writeToFile(file: FileHandle) {
        file.writeString("$songName \n", false)
        file.writeString("$songDuration \n", true)
        for (ktp in keyTimeList) {
            val data: String = (ktp.first + "," + ktp.second) + "\n"
            file.writeString(data, true)
        }
    }
    fun readFromFile(file: FileHandle) {
        val rawData = file.readString()
        val dataArray = rawData.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        songName = dataArray[0]
        songDuration = dataArray[1].toFloat()
        keyTimeList.clear()
        for (i in 2 until dataArray.size) {
            val keyTimeData =
                dataArray[i].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val key = keyTimeData[0]
            val time = keyTimeData[1].toFloat()
            keyTimeList.add(Pair(key, time))
        }
    }
}
