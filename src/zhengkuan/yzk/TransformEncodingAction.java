package zhengkuan.yzk;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import info.monitorenter.cpdetector.io.*;
import zhengkuan.yzk.encoding.EncodingUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 文件转码Action
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/20
 */
public class TransformEncodingAction extends AnAction {

    private static final String GB2312 = "GB2312";
    private static final String GBK = "GBK";
    private static final String UTF8 = "UTF-8";

    /**
     * 当编辑框被打开时显示自定义的Action菜单项，否则，将Action菜单项设置为灰色
     *
     * @param e AnActionEvent
     */
    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        if (editor != null) {
            e.getPresentation().setEnabled(true);
        } else {
            e.getPresentation().setEnabled(false);
        }
    }

    /**
     * 自动转码 Action 动作执行
     *
     * @param e ActionEvent
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        transformFileEncoding(virtualFile);
    }

    /**
     * 修改文件编码
     *
     * @param virtualFile IDEA中为每个文件创建的虚拟文件
     */
    public static void transformFileEncoding(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return;
        }

        try {
            Charset charset = getInputStreamEncode(virtualFile.getInputStream());
            EncodingUtil.changeTo(virtualFile, charset);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 获取文件输入流的编码
     *
     * @param inputStream 文件输入流
     * @return 文件输入流的正确编码
     */
    private static Charset getInputStreamEncode(InputStream inputStream) {
        // 使用UTF-8作为默认编码
        String charsetName = UTF8;
        try {
            CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
            detector.add(new ParsingDetector(false));
            detector.add(JChardetFacade.getInstance());
            detector.add(ASCIIDetector.getInstance());
            detector.add(UnicodeDetector.getInstance());
            Charset charset = detector.detectCodepage(inputStream, Integer.MAX_VALUE);
            if (charset != null) {
                charsetName = charset.name();
                // 将GB2312升级为GBK
                if (GB2312.equals(charsetName)) {
                    charsetName = GBK;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Charset.forName(charsetName);
    }

}
