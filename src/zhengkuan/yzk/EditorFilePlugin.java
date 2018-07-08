//package zhengkuan.yzk;
//
//import com.intellij.openapi.components.ProjectComponent;
//import com.intellij.openapi.fileEditor.FileEditorManagerListener;
//import com.intellij.openapi.project.Project;
//
///**
// * @author zhengkuan.yzk@alibaba-inc.com
// * @date 2018/7/8
// */
//public class EditorFilePlugin implements ProjectComponent {
//
//    private Project project;
//
//    @Override
//    public void initComponent() {
//        EditorInject inject = new EditorInject(project);
//        project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, inject);
//    }
//}
