package zhengkuan.yzk.handler

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import zhengkuan.yzk.TransformEncodingAction

/**
 * 编辑器文件切换事件
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/20
 */
class FileChangedListener : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        TransformEncodingAction.transformFileEncoding(event.newFile)
    }
}