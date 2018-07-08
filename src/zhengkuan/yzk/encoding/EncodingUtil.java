// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be
// found in the LICENSE file.
package zhengkuan.yzk.encoding;

import com.intellij.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.encoding.EncodingManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 文件编码工具类
 *
 * @author zhengkuan.yzk@alibaba-inc.com
 * @date 2018/6/26
 */
public class EncodingUtil {

    /**
     * the result of wild guess
     */
    public enum Magic8 {
        /**
         * 可以
         */
        ABSOLUTELY,
        /**
         * 不建议
         */
        WELL_IF_YOU_INSIST,
        /**
         * 不行
         */
        NO_WAY
    }

    /**
     * check if file can be loaded in the encoding correctly:
     * <p/>
     * returns ABSOLUTELY if bytes on disk, converted to text with the charset, converted back to bytes matched
     * <p/>
     * returns NO_WAY if the new encoding is incompatible (bytes on disk will differ)
     * <p/>
     * returns WELL_IF_YOU_INSIST if the bytes on disk remain the same but the text will change
     *
     * @param virtualFile
     * @param text
     * @param bytes
     * @param charset
     * @return
     */
    @NotNull
    private static Magic8 isSafeToReloadIn(@NotNull VirtualFile virtualFile, @NotNull CharSequence text,
                                           @NotNull byte[] bytes,
                                           @NotNull Charset charset) {
        // file has BOM but the charset hasn't
        byte[] bom = virtualFile.getBOM();
        if (bom != null && !CharsetToolkit.canHaveBom(charset, bom)) {return Magic8.NO_WAY;}

        // the charset has mandatory BOM (e.g. UTF-xx) but the file hasn't or has wrong
        byte[] mandatoryBom = CharsetToolkit.getMandatoryBom(charset);
        if (mandatoryBom != null && !ArrayUtil.startsWith(bytes, mandatoryBom)) { return Magic8.NO_WAY;}

        String loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, charset).toString();

        String separator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null);
        String toSave = StringUtil.convertLineSeparators(loaded, separator);

        LoadTextUtil.AutoDetectionReason failReason = LoadTextUtil.getCharsetAutoDetectionReason(virtualFile);
        if (failReason != null && CharsetToolkit.UTF8_CHARSET.equals(virtualFile.getCharset())
            && !CharsetToolkit.UTF8_CHARSET.equals(charset)) {
            // can't reload utf8-autodetected file in another charset
            return Magic8.NO_WAY;
        }

        byte[] bytesToSave;
        try {
            bytesToSave = toSave.getBytes(charset);
        }
        // turned out some crazy charsets have incorrectly implemented .newEncoder() returning null
        catch (UnsupportedOperationException | NullPointerException e) {
            return Magic8.NO_WAY;
        }
        if (bom != null && !ArrayUtil.startsWith(bytesToSave, bom)) {
            // for 2-byte encodings String.getBytes(Charset) adds BOM automatically
            bytesToSave = ArrayUtil.mergeArrays(bom, bytesToSave);
        }

        return !Arrays.equals(bytesToSave, bytes) ? Magic8.NO_WAY : StringUtil
            .equals(loaded, text) ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
    }

    @NotNull
    private static Magic8 isSafeToConvertTo(@NotNull VirtualFile virtualFile, @NotNull CharSequence text,
                                            @NotNull byte[] bytesOnDisk, @NotNull Charset charset) {
        try {
            String lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null);
            CharSequence textToSave = lineSeparator.equals("\n") ? text : StringUtilRt.convertLineSeparators(text,
                lineSeparator);

            Pair<Charset, byte[]> chosen = LoadTextUtil.chooseMostlyHarmlessCharset(virtualFile.getCharset(), charset,
                textToSave.toString());

            byte[] saved = chosen.second;

            CharSequence textLoadedBack = LoadTextUtil.getTextByBinaryPresentation(saved, charset);

            return !StringUtil.equals(text, textLoadedBack) ? Magic8.NO_WAY : Arrays.equals(saved, bytesOnDisk)
                ? Magic8.ABSOLUTELY : Magic8.WELL_IF_YOU_INSIST;
        } catch (UnsupportedOperationException e) { // unsupported encoding
            return Magic8.NO_WAY;
        }
    }

    private static void reloadIn(@NotNull final VirtualFile virtualFile, @NotNull final Charset charset) {
        final FileDocumentManager documentManager = FileDocumentManager.getInstance();

        if (documentManager.getCachedDocument(virtualFile) == null) {
            // no need to reload document
            EncodingManager.getInstance().setEncoding(virtualFile, charset);
            return;
        }

        final Disposable disposable = Disposer.newDisposable();
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(disposable);
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {

            @Override
            public void beforeAllDocumentsSaving() {

            }

            @Override
            public void beforeDocumentSaving(@NotNull Document document) {

            }

            @Override
            public void beforeFileContentReload(VirtualFile file, @NotNull Document document) {
                if (!file.equals(virtualFile)) {
                    return;
                }
                // disconnect
                Disposer.dispose(disposable);

                EncodingManager.getInstance().setEncoding(file, charset);

                LoadTextUtil.clearCharsetAutoDetectionReason(file);
            }

            @Override
            public void fileWithNoDocumentChanged(@NotNull VirtualFile virtualFile) {

            }

            @Override
            public void fileContentReloaded(@NotNull VirtualFile virtualFile, @NotNull Document document) {

            }

            @Override
            public void fileContentLoaded(@NotNull VirtualFile virtualFile, @NotNull Document document) {

            }

            @Override
            public void unsavedDocumentsDropped() {

            }
        });

        // if file was modified, the user will be asked here
        try {
            EncodingProjectManagerImpl.suppressReloadDuring(
                () -> ((VirtualFileListener)documentManager).contentsChanged(
                    new VirtualFileEvent(null, virtualFile, virtualFile.getName(), virtualFile.getParent())));
        } finally {
            Disposer.dispose(disposable);
        }
    }

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

        String text = document.getText();
        EncodingUtil.Magic8 isSafeToConvert = EncodingUtil.isSafeToConvertTo(virtualFile, text, bytes, charset);
        EncodingUtil.Magic8 isSafeToReload = EncodingUtil.isSafeToReloadIn(virtualFile, text, bytes, charset);

        final Runnable todo;

        if (isSafeToConvert == EncodingUtil.Magic8.ABSOLUTELY && isSafeToReload == EncodingUtil.Magic8.ABSOLUTELY) {
            todo = () -> EncodingManager.getInstance().setEncoding(virtualFile, charset);
        } else {
            todo = () -> EncodingUtil.reloadIn(virtualFile, charset);
        }

        todo.run();

        return true;
    }
}
