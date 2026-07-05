package com.chatapp.util;
import java.io.*;
import java.nio.file.*;
public class FileUtils {
    private static final String UPLOAD_DIR = "uploads/";
    static { new File(UPLOAD_DIR).mkdirs(); }
    public static void saveFile(String fileName, byte[] data) throws IOException {
        Path path = Paths.get(UPLOAD_DIR + fileName);
        Files.write(path, data);
        System.out.println("[FILE] Saved: " + path.toAbsolutePath());
    }
    public static byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }
    public static String getFileName(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }
}