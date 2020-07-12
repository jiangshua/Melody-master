package com.jcode.compressor

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jcode.compressor.util.FileUtils
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class ImgCompressTask extends DefaultTask {
    ImgCompressExtension config
    ResultInfo resultInfo = new ResultInfo()

    ImgCompressTask() {
        description = 'ImgCompressTask'
        group = 'imgCompress'
        config = project.imgCompressOpt
    }


    @TaskAction
    def run() {
        if (!project == project.getProject()) {
            throw new IllegalArgumentException("img-compress-plugin must works on project level gradle")
        }
        def imgDirectories = getSourcesDirs(project)
        def compressedList = getCompressedInfo()
        def unCompressFileList = getUnCompressFileList(imgDirectories, compressedList)

        new TinyCompressor().compress(project, unCompressFileList, config, resultInfo)
        updateCompressInfoList(unCompressFileList, compressedList)
    }

    /**
     * 获取所有的资源目录
     * @param root
     * @return
     */
    List<File> getSourcesDirs(Project root) {
        List<File> dirs = []
        root.allprojects {
            project ->
                //仅对两种module做处理
                if (project.plugins.hasPlugin(AppPlugin)) {
                    dirs.addAll(getSourcesDirsWithVariant((DomainObjectCollection<BaseVariant>)
                            project.android.applicationVariants))
                } else if (project.plugins.hasPlugin(LibraryPlugin)) {
                    dirs.addAll(getSourcesDirsWithVariant((DomainObjectCollection<BaseVariant>)
                            project.android.libraryVariants))
                } else {
                }
        }
        return dirs
    }
    /**
     * 根据当前module的variant获取所有打包方式的资源目录
     * @param collection
     * @return
     */
    List<File> getSourcesDirsWithVariant(DomainObjectCollection<BaseVariant> collection) {
        List<File> imgDirectories = []
        collection.all { variant ->
            variant.sourceSets?.each { sourceSet ->
                if (sourceSet.resDirectories.empty) return
                sourceSet.resDirectories.each { res ->
                    if (res.exists()) {
                        if (res.listFiles() == null) return
                        res.eachDir {
                            if (it.directory) {
                                for (String s : config.imgFileDir) {
                                    if (it.name == s && !imgDirectories.contains(it)) {
                                        imgDirectories << it
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        return imgDirectories
    }

    /**
     * 获取之前压缩文件信息
     * @return
     */
    List<CompressInfo> getCompressedInfo() {
        //读取原先已压缩过的文件,如果压缩过则不再压缩
        def compressedList = new ArrayList<CompressInfo>()
        def compressedListFile = new File("${project.projectDir}/image-compressed-info.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        } else {
            try {
                //将已压缩过的文件json解析-->list
                def json = new FileInputStream(compressedListFile).getText("utf-8")
                def list = new Gson().fromJson(json, new TypeToken<ArrayList<CompressInfo>>() {
                }.getType())
                if (list instanceof ArrayList) {
                    compressedList = list
                } else {
                }
            } catch (Exception ignored) {
            }
        }
        return compressedList


    }

    /**
     * 获取待压缩的文件,过滤白名单目录及文件,过滤文件大小
     * @param imgDirectories
     * @param compressedList
     * @return
     */
    List<CompressInfo> getUnCompressFileList(List<File> imgDirectories,
                                             List<CompressInfo> compressedList) {
        List<CompressInfo> unCompressFileList = new ArrayList<>()

        dirFlag:
        for (File dir : imgDirectories) {
            fileFlag:
            for (File it : dir.listFiles()) {
                String fileName = it.name
                //过滤白名单文件
                if (!config.whiteFiles.empty) {
                    for (String s : config.whiteFiles) {
                        if (fileName == s) {
                            continue fileFlag
                        }
                    }
                }
                def newMd5 = FileUtils.generateMD5(it)
                //过滤已压缩文件
                for (CompressInfo info : compressedList) {
                    //md5校验
                    if (info.md5.equals(newMd5)) {
                        continue fileFlag
                    }
                }
                //过滤非jpg或png图片
                if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                    //.9图剔除
                    if (fileName.contains(".9")) {
                        continue fileFlag
                    }
                    //过滤文件大小
                    if (!(getPicSize(it) >= config.minSize)) {
                        continue fileFlag
                    }
                    unCompressFileList.add(new CompressInfo(-1, -1, "",
                            it.getAbsolutePath(), it.getAbsolutePath(), newMd5))
                }
            }
        }
        return unCompressFileList
    }

    /**
     * 更新已压缩信息
     * @param newCompressedList
     * @param compressedList
     * @return
     */
    def updateCompressInfoList(List<CompressInfo> newCompressedList, List<CompressInfo> compressedList) {
        //脱敏
        String projectDir = project.projectDir.getAbsolutePath()
        for (CompressInfo info : newCompressedList) {
            info.path = info.path.substring(projectDir.length(), info.path.length())
            info.outputPath = info.outputPath.substring(projectDir.length(), info.outputPath.length())
        }
        for (CompressInfo newTinyPng : newCompressedList) {
            def index = compressedList.md5.indexOf(newTinyPng.md5)
            if (index >= 0) {
                compressedList[index] = newTinyPng
            } else {
                compressedList.add(0, newTinyPng)
            }
        }
        def jsonOutput = new JsonOutput()
        def json = jsonOutput.toJson(compressedList)

        def compressedListFile = new File("${project.projectDir}/image-compressed-info.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        }
        compressedListFile.write(jsonOutput.prettyPrint(json), "utf-8")
    }

    /**
     * 获取图片大小,单位kb
     * @param file
     * @return
     */
    int getPicSize(File file) {
        def fis = new FileInputStream(file)
        def beforeSize = file == null ? 0 : fis.available()
        if (fis != null) fis.close()
        return beforeSize / 1024
    }
}
