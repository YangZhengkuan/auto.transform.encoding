package plugin

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import inject.EditorInject

class CodePlugin(private var project:Project):ProjectComponent {
    private lateinit var inject:EditorInject
    init {
    }

    override fun getComponentName(): String {
        return "plugin.CodePlugin"
    }

    override fun disposeComponent() {
    }

    override fun projectClosed() {
    }

    override fun initComponent() {

        inject=EditorInject(project)

        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER,inject)
    }

    override fun projectOpened() {
    }
}