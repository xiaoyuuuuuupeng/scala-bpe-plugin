package com.shengqugames.bpe.highlight

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile

/**
 * When the Scala plugin is installed, treat .flow files as Scala
 * so they get full syntax highlighting. If Scala plugin is absent,
 * returns null and .flow stays as plain text.
 */
class BpeFlowFileTypeOverrider : FileTypeOverrider {

    override fun getOverriddenFileType(file: VirtualFile): FileType? {
        if (file.extension?.lowercase() != "flow") return null
        val scalaType = FileTypeManager.getInstance().getFileTypeByExtension("scala")
        return if (scalaType is UnknownFileType) null else scalaType
    }
}
