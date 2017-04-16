package com.zybang.mapconvert.util

import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

/**
 * Created by cylee on 2017/4/13.
 */
object Util {
    fun matchNum(rawName: String): Int {
        var m = Regex(".*?(\\d+).*?").matchEntire(rawName)
        if (m != null && m.groups != null && m.groups.size > 1) {
            var s =  m.groups.get(1)
            if (!s?.value.isNullOrEmpty()) {
                try {
                    return s!!.value.toInt()
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
        return 0
    }

    fun frontNum(rawName: String): Int {
        var m = Regex("(\\d+).*?").matchEntire(rawName)
        if (m != null && m.groups != null && m.groups.size > 1) {
            var s =  m.groups.get(1)
            if (!s?.value.isNullOrEmpty()) {
                try {
                    return s!!.value.toInt()
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
        return 0
    }

    fun bgColor(imgFile : File): String {
        var resultColor = Integer.toHexString(Color.WHITE.rgb);
        if (imgFile.exists()) {
            var img = ImageIO.read(imgFile);
            var result = MMCQ.computeMap(img, 2);
            if (result != null && result.boxes != null && result.boxes.size > 0) {
                var color = result.boxes[0].color
                resultColor = "0x"+Integer.toHexString(Color(color[0], color[1],color[2]).rgb)
            }
        }
        return resultColor
    }
}