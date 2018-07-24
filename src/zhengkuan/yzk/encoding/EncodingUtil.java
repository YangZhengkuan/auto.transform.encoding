// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package zhengkuan.yzk.encoding;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingManager;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 文件编码工具类
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/26
 */
public class EncodingUtil {

    /**
     * 对文件进行转码，成功时返回true
     *
     * @param virtualFile 当前打开文件的VirtualFile
     * @param charset     当前文件的正确编码
     * @return 转码成功与否
     */
    public static boolean changeTo(final VirtualFile virtualFile, final Charset charset) {

        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        Document document = documentManager.getDocument(virtualFile);

        final byte[] bytes;
        try {
            bytes = virtualFile.isDirectory() ? null : VfsUtilCore.loadBytes(virtualFile);
        } catch (IOException exception) {
            exception.printStackTrace();
            return false;
        }

        if (document == null || bytes == null) {
            return false;
        }

        final Runnable todo = () -> EncodingManager.getInstance().setEncoding(virtualFile, charset);
        todo.run();
        return true;
    }
}
