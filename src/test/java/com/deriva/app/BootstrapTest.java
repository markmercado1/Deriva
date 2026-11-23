package com.deriva.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BootstrapTest {
    @TempDir Path workingDirectory;

    @Test
    void bootstrapStartsInSeparateJvmAndExitsWithoutCreatingFiles() throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path java = Path.of(System.getProperty("java.home"), "bin", executable);
        Path classes = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(java.toString(), "-Dfile.encoding=UTF-8",
                "-cp", classes.toString(), Main.class.getName())
                .directory(workingDirectory.toFile()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Bootstrap must exit without a background service");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("Start-Ui.ps1"), output);
            try (var files = Files.list(workingDirectory)) {
                assertEquals(0, files.count(), "Bootstrap must not create recording data in its working directory");
            }
        } finally {
            process.destroyForcibly();
        }
    }
}
