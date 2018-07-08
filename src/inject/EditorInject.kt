package inject

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import zhengkuan.yzk.AutoTransformEncodingAction


class EditorInject(private var project: Project) : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {

        var source = event.manager
        var file = event.newFile
        print("\n" + file!!.name + " ")

        AutoTransformEncodingAction.transformFileEncoding(file)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
    }
}