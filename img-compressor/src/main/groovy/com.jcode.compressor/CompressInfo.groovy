package com.jcode.compressor

class CompressInfo {

    long preSize
    long compressedSize
    String ratio
    String path
    String outputPath
    String md5

    def update(long preSize, long compressedSize, String md5) {
        this.preSize = preSize
        this.compressedSize = compressedSize
        this.md5 = md5
        ratio = (compressedSize * 1.0F / this.preSize * 100).toInteger() + "%"
    }

    CompressInfo(long preSize, long compressedSize, String ratio, String path,
                 String outputPath, String md5) {
        this.preSize = preSize
        this.compressedSize = compressedSize
        this.ratio = ratio
        this.path = path
        this.outputPath = outputPath
        this.md5 = md5
    }
}