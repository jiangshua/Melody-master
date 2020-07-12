package com.jcode.compressor

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.jcode.compressor.util.Constants

class ImgCompressPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("ImgCompressPlugin  call " + project.name + "  gradle:"
                + project.gradle.toString() + " " + (project == project.getRootProject()))
        if (!project == project.getRootProject()) {
            throw new IllegalArgumentException("img-compress-plugin must works on project level gradle")
        }
        project.extensions.create(Constants.EXT_OPT, ImgCompressExtension)
        project.task(type: ImgCompressTask, overwrite: true, "imgCompressTask") {
            it.group = "image"
            it.description = "Compress  images"
        }
    }
}