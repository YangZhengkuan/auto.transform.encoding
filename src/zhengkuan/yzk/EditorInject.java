//package zhengkuan.yzk;
//
//import com.intellij.openapi.fileEditor.FileEditorManager;
//import com.intellij.openapi.fileEditor.FileEditorManagerListener;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.vfs.VirtualFile;
//import org.jetbrains.annotations.NotNull;
//
///**
// * @author zhengkuan.yzk@alibaba-inc.com
// * @date 2018/7/8
// */
//public class EditorInject implements FileEditorManagerListener {
//
//    private Project project;
//
//    public EditorInject(Project project) {
//        this.project = project;
//    }
//
//    @Override
//    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//        System.out.println(file.getName());
//    }
//}
