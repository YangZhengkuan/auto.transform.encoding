package zhengkuan.yzk;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnicodeDetector;
import zhengkuan.yzk.encoding.EncodingUtil;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件转码Action
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/20
 */
public class TransformEncodingAction extends AnAction {

    private static final Set<String> GBK_SET = new HashSet<String>() {{
        add("Big5");
        add("GB18030");
        add("GB2312");
    }};

    private static final String GBK = "GBK";
    private static final String UTF8 = "UTF-8";

    private static final String VOID_CHARSET_NAME = "void";

    /**
     * 文件编码缓存
     */
    private static Map<VirtualFile, Charset> charsetCache = new HashMap<>();

    /**
     * 当编辑框被打开时显示自定义的Action菜单项，否则，将Action菜单项设置为灰色
     *
     * @param e AnActionEvent
     */
    @Override
    public void update(AnActionEvent e) {
        try {
            Editor editor = e.getData(PlatformDataKeys.EDITOR);

            if (editor != null) {
                e.getPresentation().setEnabled(true);
            } else {
                e.getPresentation().setEnabled(false);
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * 自动转码 Action 动作执行：GBK与UTF-8编码切换
     *
     * @param e ActionEvent
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);

            if (null == virtualFile) {
                return;
            }

            // 当前的文件编码
            Charset charset = virtualFile.getCharset();

            // 执行GBK与UTF-8编码的切换动作
            boolean success;
            if (UTF8.equals(charset.name())) {
                success = EncodingUtil.changeTo(virtualFile, Charset.forName(GBK));
            } else {
                success = EncodingUtil.changeTo(virtualFile, Charset.forName(UTF8));
            }
            if (success) {
                charsetCache.put(virtualFile, virtualFile.getCharset());
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * 修改文件编码
     *
     * @param virtualFile IDEA中为每个文件创建的虚拟文件
     */
    public static void transformFileEncoding(VirtualFile virtualFile) {
        try {
            if (virtualFile == null) {
                return;
            }

            // 缓存的文件编码，如果已经识别过，则直接返回
            // 因此，若识别编码错误，即可手动更改文件的编码，不会出现始终强制转为错误编码的情况
            Charset charset = charsetCache.get(virtualFile);
            if (null != charset) {
                return;
            }

            // 当前的文件编码
            Charset currentCharset = virtualFile.getCharset();

            charset = getInputStreamEncode(virtualFile.getInputStream());
            if (charset.equals(currentCharset)) {
                charsetCache.put(virtualFile, charset);
                return;
            }

            boolean success = EncodingUtil.changeTo(virtualFile, charset);
            if (success) {
                charsetCache.put(virtualFile, charset);
            }
        } catch (Exception ignored) {

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
            detector.add(UnicodeDetector.getInstance());
            Charset charset = detector.detectCodepage(inputStream, Integer.MAX_VALUE);
            if (charset != null) {
                charsetName = charset.name();
                // GBK子集编码
                if (GBK_SET.contains(charsetName)) {
                    charsetName = GBK;
                }
            }
        } catch (Exception ignored) {

        }
        // 如果获取到的编码名称为void，则取默认UTF-8编码
        if (VOID_CHARSET_NAME.equals(charsetName)) {
            charsetName = UTF8;
        }
        return Charset.forName(charsetName);
    }

}
