/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;

class LogServletTest {

    @TempDir
    Path folder;

    private final LogServlet logServlet = new LogServlet();

    @Test
    void shouldKeepTheFileNameWhenThereIsNoCollision() throws Exception {
        assertEquals(List.of("kura.log", "messages"), new ArrayList<>(whenFilesAreZipped(List.of(
                givenFile("one", "kura.log", "kura"), givenFile("two", "messages", "system"))).keySet()));
    }

    @Test
    void shouldSuffixCollidingEntryNamesPreservingTheExtension() throws Exception {
        assertEquals(Map.of("kura.log", "one", "kura_1.log", "two", "kura_2.log", "three"),
                whenFilesAreZipped(List.of(givenFile("one", "kura.log", "one"),
                        givenFile("two", "kura.log", "two"), givenFile("three", "kura.log", "three"))));
    }

    @Test
    void shouldSuffixCollidingEntryNamesWithoutExtension() throws Exception {
        assertEquals(Map.of("messages", "one", "messages_1", "two"), whenFilesAreZipped(List.of(
                givenFile("one", "messages", "one"), givenFile("two", "messages", "two"))));
    }

    @Test
    void shouldNotMistakeALeadingDotForAnExtension() throws Exception {
        assertEquals(Map.of(".hidden", "one", ".hidden_1", "two"), whenFilesAreZipped(List.of(
                givenFile("one", ".hidden", "one"), givenFile("two", ".hidden", "two"))));
    }

    @Test
    void shouldZipAllProvidedFiles() throws Exception {
        String content = "日志 bytes\n".repeat(600);
        assertEquals(Map.of("kura.log", content, "messages", "system content"), whenFilesAreZipped(List.of(
                givenFile("one", "kura.log", content), givenFile("one", "messages", "system content"))));
    }

    @Test
    void shouldSkipUnreadableFilesAndStillProduceAValidArchive() throws Exception {
        File missing = this.folder.resolve("rotated.log").toFile();
        assertFalse(missing.exists());
        assertEquals(Map.of("kura.log", "kura content", "messages", "system content"), whenFilesAreZipped(List.of(
                givenFile("one", "kura.log", "kura content"), missing,
                givenFile("one", "messages", "system content"))));
    }

    @Test
    void shouldDeduplicateEntryNamesComingFromDifferentDirectories() throws Exception {
        assertEquals(Map.of("kura.log", "first content", "kura_1.log", "second content"),
                whenFilesAreZipped(List.of(givenFile("var-log", "kura.log", "first content"),
                        givenFile("opt-log", "kura.log", "second content"))));
    }

    @Test
    void shouldProduceAValidArchiveWhenThereIsNothingToZip() throws Exception {
        byte[] archive = zip(List.of());
        assertTrue(archive.length >= 22, "ZIP must include its end-of-central-directory record");
        assertTrue(readEntries(archive).isEmpty());
    }

    @Test
    void generatedSuffixMustNotOverwriteAnExistingEntry() throws Exception {
        assertEquals(Map.of("kura.log", "one", "kura_1.log", "existing", "kura_2.log", "two"),
                whenFilesAreZipped(List.of(givenFile("one", "kura.log", "one"),
                        givenFile("one", "kura_1.log", "existing"), givenFile("two", "kura.log", "two"))));
    }

    @Test
    void replyPreservesLocalArchiveNameAndCompleteBytes() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int value) {
                bytes.write(value);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                throw new UnsupportedOperationException();
            }
        });
        Method method = LogServlet.class.getDeclaredMethod("createReply", HttpServletResponse.class, List.class,
                String.class);
        method.setAccessible(true);
        method.invoke(this.logServlet, response, List.of(givenFile("one", "kura.log", "complete bytes")), "test");
        verify(response).setContentType("application/zip");
        verify(response).setHeader("Content-Disposition", "attachment; filename=\"Kura_Logs.zip\"");
        assertEquals(Map.of("kura.log", "complete bytes"), readEntries(bytes.toByteArray()));
    }

    private File givenFile(String directory, String name, String content) throws Exception {
        Path parent = Files.createDirectories(this.folder.resolve(directory));
        return Files.writeString(parent.resolve(name), content, StandardCharsets.UTF_8).toFile();
    }

    private Map<String, String> whenFilesAreZipped(List<File> files) throws Exception {
        return readEntries(zip(files));
    }

    private byte[] zip(List<File> files) throws Exception {
        // The local implementation returns bytes and keeps this method private.
        Method method = LogServlet.class.getDeclaredMethod("zipFiles", List.class);
        method.setAccessible(true);
        return (byte[]) method.invoke(this.logServlet, files);
    }

    private Map<String, String> readEntries(byte[] bytes) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                assertFalse(entries.containsKey(entry.getName()));
                entries.put(entry.getName(), new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }
}
