package com.jcode.compressor

import com.jcode.compressor.util.FileUtils
import com.tinify.AccountException
import com.tinify.Exception
import com.tinify.Tinify
import org.gradle.api.Project

class TinyCompressor {
    int keyIndex = 0
    def rootProject
    def compressInfoList = new ArrayList<CompressInfo>()
    boolean accountError = false
    ImgCompressExtension config
    long beforeTotalSize = 0
    long afterTotalSize = 0

    void compress(Project rootProject, List<CompressInfo> unCompressFileList,
                  ImgCompressExtension config, ResultInfo resultInfo) {
        this.rootProject = rootProject
        this.compressInfoList = compressInfoList
        this.config = config
        checkKey()
        unCompressFileList.each {
            tryCompressSingleFile(it)
        }
        resultInfo.compressedSize = unCompressFileList.size()
        resultInfo.beforeSize = beforeTotalSize
        resultInfo.afterSize = afterTotalSize
        resultInfo.skipCount = 0
    }

    def tryCompressSingleFile(CompressInfo info) {
        println("find target pic >>>>>>>>>>>>> ${info.path}")
        try {
            def fis = new FileInputStream(new File(info.path))
            //available在读取之前知道数据流有多少个字节,即原始文件大小
            def beforeSize = fis.available()
            // Use the Tinify API client
            def tSource = Tinify.fromFile(info.path)
            tSource.toFile(info.outputPath)
            fis = new FileInputStream(new File(info.outputPath))
            //这里没对压缩后如果文件变大做处理
            def afterSize = fis.available()
            beforeTotalSize += beforeSize
            afterTotalSize += afterSize
            info.update(beforeSize, afterSize, FileUtils.generateMD5(new File(info.outputPath)))
        } catch (AccountException e) {
            println("AccountException: ${e.getMessage()}")
            if (config.tinyKeys.size() <= ++keyIndex) {
                accountError = true
                return
            } else {
                //失败重试
                Tinify.setKey(config.tinyKeys[keyIndex])
                tryCompressSingleFile(file)
            }
            // Verify your API key and account limit.
        } catch (Exception e) {
            // Check your source image and request options.
            println("ClientException: ${e.getMessage()}")
        }
    }

    def checkKey() {
        if (config.tinyKeys.empty) {
            println("Tiny tinyKeys is empty.")
            return
        }
        try {
            //测试key值的正确性
            Tinify.setKey("${config.tinyKeys[keyIndex]}")
            Tinify.validate()
        } catch (AccountException ex) {
            println("TinyCompressor" + ex.printStackTrace())
        }
    }
}