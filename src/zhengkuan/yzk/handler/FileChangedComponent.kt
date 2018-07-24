package zhengkuan.yzk.handler

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

/**
 * 编辑器文件切换组件，将 “编辑器文件切换事件监听” 注册到应用中
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/20
 */
class FileChangedComponent(private var project: Project) : ProjectComponent {

    override fun initComponent() {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, FileChangedListener())
    }
}