package com.jcode.compressor

/**
 * 压缩配置
 */
class ImgCompressExtension {
    int minSize = 0 //单位kb,>minSize(kb)的图片才执行压缩
    ArrayList<String> whiteFiles = new ArrayList<String>()//白名单文件,不进行压缩
    ArrayList<String> imgFileDir = new ArrayList<String>()//哪些目录需要图片压缩
    ArrayList<String> tinyKeys = new ArrayList<String>()//tiny模式下的秘钥
}