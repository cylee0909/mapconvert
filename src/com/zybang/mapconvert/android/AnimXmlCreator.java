package com.zybang.mapconvert.android;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by cylee on 2017/4/9.
 */
public class AnimXmlCreator {

    /**
     * 创建android对应的动画xml文件，并自动重命名dirName文件夹下的png文件
     *
     * @param dirName  存放png文件的文件夹路径
     * @param preName  重命名文件的前缀
     * @param duration 动画的总时长，会根据此时间计算每一帧的时长
     */
    public static void create(String dirName, String preName, int duration) {
        String animFileName = preName + ".xml";
        File dir = new File(dirName);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.getName().endsWith(".xml")) {
                        pathname.delete();
                    }
                    return pathname.isFile() && pathname.getName().endsWith(".png");
                }
            });
            List<File> fileList = Arrays.asList(files);
            fileList.sort(new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            File animFile = new File(dir, animFileName);
            StringBuilder animXmlConten = new StringBuilder();
            animXmlConten.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<animation-list xmlns:android=\"http://schemas.android.com/apk/res/android\" android:oneshot=\"false\">\n");
            int frameDuration = (int) (duration / (float) fileList.size());
            int i = 0;
            for (File f : fileList) {
                System.out.println("process file " + f.getName());
                String newFileName = preName + "_" + String.format("%02d", i++) + ".png";
                f.renameTo(new File(f.getParentFile(), newFileName));
                animXmlConten.append("<item android:drawable=\"@drawable/").append(getFileNameNoSubfix(newFileName))
                        .append("\" android:duration=\"").append(frameDuration).append("\"/>\n");
            }
            animXmlConten.append("</animation-list>");


            if (animFile.exists()) {
                animFile.delete();
            }
            try {
                animFile.createNewFile();
                OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(animFile));
                os.write(animXmlConten.toString());
                os.flush();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getFileNameNoSubfix(String fileName) {
        String rawName = fileName;
        int dotIndex = rawName.lastIndexOf(".");
        if (dotIndex > -1) {
            return rawName.substring(0, dotIndex);
        }
        return "";
    }

    private void test() {
        String dirname = "C:\\Users\\cylee\\Desktop\\动效\\地图上的小动效\\马(时长4s)";
        String prename = "g_calc_1_anim_friend_1";
        int duration = 4000; // default total time
        create(dirname, prename, duration);
    }
}
