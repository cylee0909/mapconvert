package com.zybang.mapconvert.entity

import java.util.*

/**
 * Created by cylee on 2017/4/10.
 */
class MapData {
    var tiles : MutableMap<Int, TileItem> = HashMap()
    var objectGroups : MutableMap<String, ObjectGroup> = HashMap()
    var renderorder : String = ""
    var mapWidth : Int = 0
    var mapHeight : Int = 0
    var filePrefix = ""

    fun reset() {
        tiles.clear()
        objectGroups.clear()
    }

    fun addTile(tile : TileItem) {
        tiles.put(tile.id, tile)
    }

    fun addObjectGroup(objectGroup: ObjectGroup) {
        objectGroups.put(objectGroup.name, objectGroup)
    }
}