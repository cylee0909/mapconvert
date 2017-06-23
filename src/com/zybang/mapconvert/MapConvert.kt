package com.zybang.mapconvert

import com.zybang.mapconvert.android.AnimXmlCreator
import com.zybang.mapconvert.entity.*
import com.zybang.mapconvert.util.Util
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.File
import java.io.FileFilter
import java.nio.charset.Charset
import java.util.*

/**
 * Created by cylee on 2017/4/10.
 */
object MapConvert {
    var androidOutFolder:File? = null;
    var iosOutFolder:File? = null

    val gamePrefix = "g_"
    var mapData = MapData()
    var ID_DEFIN:MutableSet<ImageState.IdDef> = TreeSet()
    var tipTexts:MutableList<String> = ArrayList()
    var mapId = 0;

    @JvmStatic
    fun readMap(args: Array<String>) {
        if (args.size < 1) {
            println("file not exits, please input the tmx file path or folder contains txm file!")
            return
        }
        var pathName = args[0]
        var file = File(pathName)
        if (!file.exists()) {
            println("${file.absolutePath} not exits, please input the correct file path!")
            return
        }

        if (file.isFile && file.name.endsWith("tmx")) {
            processTmxFile(file)
        } else{
            file.walkTopDown().maxDepth(2).forEach {
                if (it.isFile && it.name.endsWith("tmx")) {
                    processTmxFile(it)
                }
            }
        }
    }

    fun convert(folder: File) {
        var outputFile = File(folder, "out")
        if (outputFile.exists()) {
            outputFile.deleteRecursively()
        }
        outputFile.mkdirs()

        androidOutFolder = File(outputFile, "android")
        iosOutFolder = File(outputFile, "ios")
        androidOutFolder?.mkdirs()
        iosOutFolder?.mkdirs()

        processAnim(folder)
        renameImgFile(folder)
        processGroup(folder)
    }

    private fun processTmxFile(file : File) {
        mapId = Util.matchNum(file.nameWithoutExtension)

        ID_DEFIN.clear()
        tipTexts.clear()
        mapData.reset()
        mapData.filePrefix = file.nameWithoutExtension

        var saxReader = SAXReader()
        var doc = saxReader.read(file)

        var tileset = doc.rootElement.element("tileset")
        mapData.renderorder = doc.rootElement.attributeValue("renderorder")
        mapData.mapWidth = doc.rootElement.attributeValue("width", "0").toInt()
        mapData.mapHeight = doc.rootElement.attributeValue("height", "0").toInt()
        if (tileset != null) {
            var tiles = tileset.elements()
            tiles.forEach {
                var tileElement: Element = it as Element
                var imgElement = tileElement.element("image")
                if (imgElement != null) {
                    var img = ImageItem(imgElement.attributeValue("width").toInt(),
                            imgElement.attributeValue("height").toInt(),
                            imgElement.attributeValue("source"))
                    var tile = TileItem(tileElement.attributeValue("id").toInt(), img)
                    mapData.addTile(tile)
                }
            }
        }
        var objectGroups = doc.rootElement.elements("objectgroup")
        if (objectGroups != null) {
            objectGroups.forEach {
                var objGroupEnement: Element = it as Element
                var objElement = objGroupEnement.element("object")
                if (objElement != null) {
                    var obj = ObjectItem(objElement.attributeValue("id").toInt(),
                            objElement.attributeValue("gid", "-1").toInt(),
                            objElement.attributeValue("x").toInt(),
                            objElement.attributeValue("y").toInt(),
                            objElement.attributeValue("width").toInt(),
                            objElement.attributeValue("height").toInt())
                    obj.tile = mapData.tiles.get(obj.gid - 1)
                    if (obj.tile != null && obj.tile!!.img != null) {
                        var source = obj.tile!!.img.source
                        obj.imgState = ImageState(source)
                    }
                    var objGroup = ObjectGroup(objGroupEnement.attributeValue("name"), obj)

                    var idIndex = Util.matchNum(objGroup.name)
                    if (objGroup.name.contains("宝箱")) {
                        idIndex += 100
                    } else if (objGroup.name.contains("点击区域")) {
                        idIndex = -1
                    }
                    obj.objId = idIndex
                    var propertiesElement = objGroupEnement.element("properties")
                    if (propertiesElement != null) {
                        var props = propertiesElement.elements("property")
                        if (props != null) {
                            props.forEach {
                                var prop = it as Element
                                objGroup.props.put(prop.attributeValue("name"), prop.attributeValue("value"))
                            }
                        }
                    }
                    mapData.addObjectGroup(objGroup)
                }
            }
        }

        mapData.objectGroups.forEach {
            var objGrp = it.value
            if (objGrp.props != null) {
                objGrp.props.forEach {
                    if ("p".equals(it.key)) {
                        var parentGrp = mapData.objectGroups.get(it.value)
                        if (parentGrp != null) {
                            parentGrp.subGroup.add(objGrp)
                        }
                    }
                }
            }

            if (objGrp.obj.gid == -1) {
                // xxx点击区域
                var rootObjKey:String = Util.frontNum(objGrp.name).toString()
                var parentGrp = mapData.objectGroups.get(rootObjKey)
                if (parentGrp != null) {
                    parentGrp.clickObj = objGrp.obj
                }
            }
        }

        var tipsFile = File(file.parent, "tips.txt")
        if (!tipsFile.exists()) {
            throw RuntimeException("tips file not found")
        }
        tipsFile.readLines(Charset.forName("GBK")).forEach {
            tipTexts.add(it)
        }

        convert(file.parentFile)
    }

    private fun processAnim(folder: File) {
        if (!folder.isFile && folder.exists()) {
            folder.listFiles().forEach {
                if (!it.isFile && it.name.contains("动效")) {
                    var name = it.name
                    var animFileName = gamePrefix + mapData.filePrefix
                    var durationStr = name.substringAfterLast('_')
                    var nameStr = name.substringBefore('_')
                    var animIndex = Util.matchNum(nameStr)
                    var type = ImageState.getTypeByName(nameStr)
                    var typeName = ""
                    when(type) {
                        ImageItem.TYPE_CHESTS -> typeName = "chests"
                        ImageItem.TYPE_FRIEND -> typeName = "friend"
                        ImageItem.TYPE_VIDEO -> typeName = "video"
                        ImageItem.TYPE_BOSS -> typeName = "boss"
                    }
                    animFileName += "_anim"
                    animFileName += ("_"+typeName)
                    animFileName += ("_" + animIndex)
                    AnimXmlCreator.create(it.absolutePath, animFileName, durationStr.toInt())
                }
            }
        }
    }

    private fun processGroup(folder: File) {
        var iosOutFile = File(iosOutFolder, "result.txt")
        if (iosOutFile.exists()) {
            iosOutFile.delete()
        }
        iosOutFile.createNewFile()


        var androidOutFile = File(androidOutFolder, "Calc${mapId}MapData.java")
        if (androidOutFile.exists()) {
            androidOutFile.delete()
        }
        androidOutFile.createNewFile()
        androidOutFile.appendText("""
package com.baidu.homework.game.pve.calc.map;
import com.baidu.homework.R;
import com.baidu.homework.game.pve.calc.GameCalcMapActivity;

/**
 * Created by cylee.
 */

public class Calc${mapId}MapData extends BaseMapData {""")

        var androidLevelData = StringBuilder()
        if (mapData.objectGroups != null) {
            var typeIndexs = IntArray(5)
            mapData.objectGroups.values.sortedWith(Comparator { t1, t2 ->
                t1.obj.objId - t2.obj.objId
            }).forEach {
                if (it.obj != null) {
                    var gid = it.obj.gid
                    if (gid > 0) { // 普通贴图
                        var tile = it.obj.tile
                        if (tile != null) {
                            generateItemData(folder, typeIndexs, it)
                        }
                    }
                }
            }
            mapData.objectGroups.values.forEach {
                var objGrp = it
                // 处理subLevels
                if (!objGrp.subGroup.isEmpty()) {
                    objGrp.subGroup.forEach {
                        objGrp.iosParam!!.subLevels.add("\""+it.obj.objId.toString()+"\"")
                        objGrp.androidTmpCode.append("\n.subLevel(").append(ImageState.IdDef.ID_VALUE_KEY.get(it.obj.objId)).append(")")
                    }
                }

                // 处理点击区域
                // 注意，点击区域的坐标是左上角
                if (objGrp.clickObj != null) {
                    var clickObj = objGrp.clickObj
                    objGrp.androidTmpCode.append("\n.hitRect(").append(clickObj!!.x).append(",").append(clickObj!!.y).append(",")
                            .append(clickObj!!.width).append(",").append(clickObj!!.height).append(")")

                    objGrp.iosParam!!.hl =  clickObj!!.x - objGrp.obj.x
                    objGrp.iosParam!!.ht = (clickObj!!.y) - (objGrp.obj.y - objGrp.obj.height)

                    objGrp.iosParam!!.hr = (objGrp.obj.x + objGrp.obj.width) - (clickObj!!.x + clickObj!!.width)
                    objGrp.iosParam!!.hb = (objGrp.obj.y) - (clickObj!!.y + clickObj!!.height)
                }

                if (objGrp.androidTmpCode.isNotEmpty()) {
                    objGrp.androidTmpCode.append(");")
                    var levelData = it.androidTmpCode.toString()
                    androidLevelData.append(levelData)
                    println(levelData)
                }

                if (objGrp.iosParam != null) {
                    var ios:IOSParam = objGrp.iosParam!!
                    iosOutFile.appendText("@\"${ios.id}\":  [[HLMapLeveModel alloc]initWithDataID:${ios.id} currentLeve:0 misionName:@\"${ios.tipText}\" misionImageName:@\"${ios.currentImg}\" completeImage:@\"${ios.completeImg}\" lockImage:@\"${ios.lockImage}\" animation:${ios.animFrameCount} x:${ios.x} y:${ios.y} w:${ios.width} h:${ios.height} edgeT:${ios.ht} edgeL:${ios.hl} edgeB:${ios.hb} edgeR:${ios.hr} labelW:194 modelType:HLMAPLEVETYPEBOSSRIGHT correlationArr:@${ios.subLevels}],\n")
                }
            }

            var normalIds = StringBuilder()
            ID_DEFIN.forEach {
                if (it.name.contains("ID_NORMAL")) {
                    normalIds.append(it.name).append(",\n")
                }
                androidOutFile.appendText("private static final int "+it.name+" = "+it.value+";")
                println("private static final int "+it.name+" = "+it.value+";")
            }


            androidOutFile.appendText("""
            @Override
            public int[] getNormalIds() {
                return new int[]{
                ${normalIds.toString()}
                };
            }""")

            androidOutFile.appendText("""
            @Override
            public int getMapResId() {
                return R.drawable.g_calc_${mapId}_map;
            }
            """);

            androidOutFile.appendText("""
               public int getTipTextBg() {
                    return R.drawable.g_calc_${mapId}_text_bg;
               }
            """)

            androidOutFile.appendText("""
            @Override
            public int getMapHeight() {
                return ${mapData.mapHeight};
            }
            """)

            var mapBgColor = "0xffecab5d"
            var prefix = gamePrefix + mapData.filePrefix
            var bgFile = File(androidOutFolder, "drawable-xhdpi/${prefix}_map.png")
            if (bgFile.exists()) {
                mapBgColor = Util.bgColor(bgFile)
            }
            androidOutFile.appendText("""
            @Override
            public int getMapBgColor() {
                return ${mapBgColor};
            }
            """)

            androidOutFile.appendText("""
            public Calc${mapId}MapData(GameCalcMapActivity.CalcGameConfig config) {
                super(config);
            }
            """)

            androidOutFile.appendText("""
            @Override
            protected void fillMapLevels() {
            ${androidLevelData.toString()}
                        }
            }
            """)
        }
    }

    private fun generateItemData(folder: File, typeIndexs : IntArray, objGrp : ObjectGroup) {
        var obj = objGrp.obj
        var type = obj.imgState?.objType ?: 0
        var data:StringBuilder = StringBuilder("mapLevels.add(LevelItem.")
        var typeDefName = ""
        var typeName = ""

        when(type) {
            ImageItem.TYPE_BOSS -> {typeName = "bossItem"; typeDefName = "BOSS"}
            ImageItem.TYPE_CHESTS -> {typeName = "chestsItem"; typeDefName = "CHESTS"}
            ImageItem.TYPE_FRIEND -> {typeName = "friendItem"; typeDefName = "FRIEND"}
            ImageItem.TYPE_NORMAL -> {typeName = "normalItem"; typeDefName = "NORMAL"}
            ImageItem.TYPE_VIDEO -> {typeName = "videoItem"; typeDefName = "VIDEO"}
        }
        if (type > 0) {
            typeIndexs[type-1]++
            data.append(typeName+"(")
            var idDefName = "ID_"+typeDefName+"_"+typeIndexs[type-1];
            ID_DEFIN.add(ImageState.IdDef(idDefName, obj.objId))

            objGrp.iosParam = IOSParam(obj.objId)
            data.append(idDefName)
            data.append(", ").append(obj.x).append(", ").append(obj.y - obj.height).append(", ").append(obj.width).append(", ").append(obj.height).append(")\n.playImg(")
            objGrp.iosParam!!.x = obj.x
            objGrp.iosParam!!.y = obj.y - obj.height
            objGrp.iosParam!!.width = obj.width
            objGrp.iosParam!!.height = obj.height

            obj.imgState?.objState = 1 // 当前
            var currentName = preFixImgName(obj.imgState)
            objGrp.iosParam!!.currentImg = currentName
            data.append("R.drawable.").append(currentName).append(")")

            obj.imgState?.objState = 2 // 已完成
            var completeImg = preFixImgName(obj.imgState)
            data.append("\n.completeImg(")
            objGrp.iosParam!!.completeImg = completeImg
            data.append("R.drawable.").append(completeImg).append(")")

            obj.imgState?.objState = 3 // 锁定状态
            var lockImg = preFixImgName(obj.imgState)
            objGrp.iosParam!!.lockImage = lockImg
            data.append("\n.lockImg(")
            data.append("R.drawable.").append(lockImg).append(")")

            // 处理动画
            if (type == ImageItem.TYPE_BOSS || type == ImageItem.TYPE_CHESTS ||
                    type == ImageItem.TYPE_VIDEO || type == ImageItem.TYPE_FRIEND) {
                var objIndex = obj.imgState?.objIndex ?: 0
                if (objIndex > 0) { // 正常关卡
                   var animFolderName = ""
                    when(type) {
                        ImageItem.TYPE_FRIEND -> animFolderName = "友情助力关"
                        ImageItem.TYPE_VIDEO -> animFolderName = "视频"
                        ImageItem.TYPE_CHESTS -> animFolderName = "宝箱"
                        ImageItem.TYPE_BOSS -> animFolderName = "boss"
                    }
                    var folders = folder.listFiles { file, s ->  s.contains(animFolderName+objIndex)}
                    if (folders != null && folders.size == 1) {
                            if (!folders[0].isFile) {
                                var animPngs = folders[0].listFiles { file, s ->
                                    s.endsWith("png") && !s.startsWith(".")
                                }

                                var androidXdpDrawable = File(androidOutFolder, "drawable-xhdpi")
                                if (!androidXdpDrawable.exists()) {
                                    androidXdpDrawable.mkdirs()
                                }

                                var iosAnimDrawable = File(iosOutFolder, "anim")
                                if (!iosAnimDrawable.exists()) {
                                    iosAnimDrawable.mkdirs()
                                }


                                var androidDrawable = File(androidOutFolder, "drawable")
                                if (!androidDrawable.exists()) {
                                    androidDrawable.mkdirs()
                                }

                                animPngs.forEach {
                                    var destFile = File(androidXdpDrawable, it.name)
                                    if (!destFile.exists()) {
                                        it.copyTo(destFile)
                                    }

                                    var iosDestFile = File(iosAnimDrawable, it.name)
                                    if (!iosDestFile.exists()) {
                                        it.copyTo(iosDestFile)
                                    }
                                }

                                objGrp.iosParam!!.animFrameCount = animPngs.size

                                var xmlFiles = folders[0].listFiles { file, s ->
                                    s.endsWith(".xml")
                                }
                                if (xmlFiles != null && xmlFiles.size == 1) {
                                    var xmlFile = xmlFiles[0]
                                    var destFile = File(androidDrawable, xmlFile.name)
                                    if (!destFile.exists()) {
                                        xmlFile.copyTo(destFile)
                                    }
                                    data.append("\n.anim(")
                                    data.append("R.drawable.").append(xmlFiles[0].nameWithoutExtension).append(")")
                                }
                            }
                    }
                }
            }

            // 处理tipstext
            if (type == ImageItem.TYPE_NORMAL) {
                var objIndex = obj.imgState?.objIndex ?: 0
                if (objIndex > 0) {
                    data.append("\n.tipText(")
                    var tipText = tipTexts[objIndex - 1].trim()
                    objGrp.iosParam!!.tipText = tipText
                    data.append("\"").append(tipText).append("\")")
                } else{
                    throw RuntimeException("objIndex error = "+objIndex +" "+obj.imgState)
                }
            }

            // 处理starOffset
            if (type == ImageItem.TYPE_BOSS) {
                data.append("\n.starOffset(34)")
            }
            objGrp.androidTmpCode.append(data.toString())
        }
    }

    private fun preFixImgName(imgState: ImageState?):String{
        var result = ""
        var filePrefix = gamePrefix + mapData.filePrefix
        if (!filePrefix.isNullOrEmpty()) {
            result += filePrefix
            result += "_"
        }
        result += imageName(imgState)
        return result
    }

    private fun imageName(imgState:ImageState?):String {
        if (imgState == null) return ""
        var result = ""
        var typeName = "";
        when (imgState.objType) { // 关卡类型
            ImageItem.TYPE_CHESTS -> typeName = "chests"
            ImageItem.TYPE_VIDEO -> typeName = "video"
            ImageItem.TYPE_FRIEND -> typeName = "friend"
            ImageItem.TYPE_BOSS -> typeName = "boss"
            ImageItem.TYPE_NORMAL -> typeName = "pedestal"
            else -> throw RuntimeException("objType error "+imgState.objType+" "+imgState)
        }

        result += typeName

        var stateName = ""
        when (imgState.objState) {
            1 -> stateName = "_p"
            2 -> stateName = "_c"
            3 -> stateName = "_l"
            else -> throw RuntimeException("objState error "+imgState.objState+" "+imgState)
        }

        if (imgState.objType == ImageItem.TYPE_FRIEND) {
            stateName = "_l" // 友情关只有一种状态
        } else if (imgState.objType == ImageItem.TYPE_CHESTS) { // 宝箱只有两种状态
            if (imgState.objState == 1) { // 宝箱正在玩的状态和锁定状态一致
                stateName = "_l"
            }
        }

        result += stateName

        var imgIndex = imgState.objIndex
        if (imgState.objState == 3 && imgState.objType != ImageItem.TYPE_BOSS) { //如果是锁定状态，index为1
            imgIndex = 1
        }

        if (imgIndex > 0) {
            result += ("_" + imgIndex)
        } else{
            throw RuntimeException("objIndex must > 0, current is "+imgState.objIndex+" "+imgState)
        }

        return result
    }

    private fun renameImgFile(folder: File) {
        var folders = ArrayList<File>()
        var androidFolder = File(folder, "android")

        var androidXdpDrawable = File(androidOutFolder, "drawable-xhdpi")
        if (!androidXdpDrawable.exists()) {
            androidXdpDrawable.mkdirs()
        }

        if (androidFolder.exists()) {
            androidFolder.listFiles().forEach {
                if (!it.isFile && it.name.contains("dpi")) {
                    folders.add(it)
                }
            }
            var filePrefix = gamePrefix + mapData.filePrefix
            folders.forEach {
                it.listFiles(FileFilter {
                    if (it.name.startsWith(".") && !it.name.contains("name_record")) it.delete()
                    it.name.endsWith("png") && !it.name.startsWith(".")
                }).forEach out@ {
                    var itname = it.nameWithoutExtension
                    if (itname.equals("未解锁底座")) {
                        itname+="1"
                    }
                    if (itname.contains("文字背景")){
                        var destFile = File(androidXdpDrawable, filePrefix+"_text_bg.9.png")
                        if (!destFile.exists()) {
                            it.copyTo(destFile)
                        }
                    } else if (itname.equals("地图大背景") || itname.contains("map")) {
                        var destFile = File(androidXdpDrawable, filePrefix+"_map.png")
                        if (!destFile.exists()) {
                            it.copyTo(destFile)
                        }
                    } else {
                        var destName = preFixImgName(ImageState(itname))+".png"
                        var destFile = File(androidXdpDrawable, destName)
                        if (!destFile.exists()) {
                            it.copyTo(destFile)
                        }
                        println("rename : "+itname +" ------> "+destName)
                    }
                }
            }
        }


        var iosFolder = File(folder, "ios")
        if (iosFolder.exists()) {
            var filePrefix = gamePrefix + mapData.filePrefix
            iosFolder.listFiles().forEach {
                if (!it.isFile && it.name.contains("x")) {

                    var iosFolder = File(iosOutFolder, it.name)
                    if (!iosFolder.exists()) {
                        iosFolder.mkdirs()
                    }

                    var solutionName = it.name
                    var recordFile =File(it, "name_record.tmp")
                    if (recordFile.exists()) recordFile.delete()
                    recordFile.createNewFile()
                    it.listFiles(FileFilter {
                        if (it.name.startsWith(".") && !it.name.contains("name_record")) it.delete()
                        it.name.endsWith("png") && !it.name.startsWith(".")
                    }).forEach out@ {
                        var itname = it.nameWithoutExtension
                        if (itname.equals("未解锁底座")) {
                            itname+="1"
                        }
                        if (itname.equals("地图大背景") || itname.contains("map")) {
                            Util.bgColor(it)
                            it.renameTo(File(it.parent, filePrefix+"_map.png"))
                        } else {
                            var destName = preFixImgName(ImageState(itname))+"@"+solutionName+".png"
                            var destFile = File(iosFolder, destName)
                            if (!destFile.exists()) {
                                it.copyTo(destFile)
                            }
                            recordFile.appendText(itname +" --> "+destName+"\n")
                            println("rename : "+itname +" ------> "+destName)
                        }
                    }
                }
            }
        }
    }
}