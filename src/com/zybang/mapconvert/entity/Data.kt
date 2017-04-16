package com.zybang.mapconvert.entity

import com.zybang.mapconvert.util.Util
import java.util.*

/**
 * Created by cylee on 2017/4/10.
 */
data class TileItem(val id:Int, val img : ImageItem)
data class ImageItem(val width:Int, val height:Int, val source: String) {
    companion object {
        const val TYPE_CHESTS = 1
        const val TYPE_BOSS = 2
        const val TYPE_NORMAL = 3;
        const val TYPE_VIDEO = 4
        const val TYPE_FRIEND = 5
    }
}
data class ObjectGroup(val name: String, val obj : ObjectItem) {
    var props : MutableMap<String, String> = HashMap()
    var androidTmpCode:StringBuilder = StringBuilder()
    var clickObj : ObjectItem? = null
    var subGroup : MutableSet<ObjectGroup> = HashSet()
    var iosParam : IOSParam? = null
}

data class IOSParam(var id:Int) {
    var tipText = ""
    var currentImg = ""
    var completeImg = ""
    var lockImage = ""
    var animFrameCount = 0
    var x = 0
    var y = 0
    var width = 0
    var height = 0

    // 点击区域
    var ht = 0
    var hl = 0
    var hr = 0
    var hb = 0

    var labelW = 194
    var modelType = "HLMAPLEVETYPEBOSSRIGHT"

    var subLevels:MutableList<String> = ArrayList<String>()
}

data class ObjectItem(val id : Int, val gid:Int, val x: Int, val y : Int, val width:Int, val height:Int) {
    var tile : TileItem? = null
    /**和地图中关卡id对应*/
    var objId = -1
    var imgState : ImageState? = null
}

data class ImageState(var name:String) {
    companion object {
        fun getTypeByName(rawName: String): Int {
            if (rawName.contains("底座")) return ImageItem.TYPE_NORMAL
            if (rawName.contains("boss")) return ImageItem.TYPE_BOSS
            if (rawName.contains("友情助力关")) return ImageItem.TYPE_FRIEND
            if (rawName.contains("视频")) return ImageItem.TYPE_VIDEO
            if (rawName.contains("宝箱")) return ImageItem.TYPE_CHESTS
            return 0
        }

        /**
         * 检查状态
         * 1 当前, 2 完成 3, 未完成（锁定状态）
         */
        fun getStateByName(rawName: String): Int {
            if (rawName.contains("当前")) return 1
            if (rawName.contains("已过") || rawName.contains("已完成") || rawName.contains("已领取")) return 2
            if (rawName.contains("未完成") || rawName.contains("未解锁")) return 3
            return 1
        }
    }

    /**
     * 关卡的类型
     */
    var objType = getTypeByName(name)
    /**
     * 关卡的进度状态
     */
    var objState = getStateByName(name)
    /**同类型关卡的索引，例如boss1中的的1*/
    var objIndex = Util.matchNum(name)

    var imgType = -1

    constructor(objType:Int, objState:Int, objIndex : Int) : this("") {
        this.objType = objType
        this.objState = objState
        this.objIndex = objIndex
    }

    data class IdDef(var name:String, var value:Int) : Comparable<IdDef>{
        init {
            ID_KEY_VALUE.put(name, value)
            ID_VALUE_KEY.put(value, name)
        }

        companion object{
            var ID_KEY_VALUE:MutableMap<String, Int> = HashMap()
            var ID_VALUE_KEY :MutableMap<Int, String> = HashMap()
        }
        override fun compareTo(other: IdDef): Int {
            return value - other.value
        }
    }
}