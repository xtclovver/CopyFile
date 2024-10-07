package org.example;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;

public class Main {
    // Размер файла в килобайтах (например, 50 MB = 50 * 1024 KB)
    private static final int FILE_SIZE_KB = 50 * 1024;
    private static String formatDuration(long nanos) { return String.format("%.5f ms", nanos / 1_000_000.0); }
    public static void main(String[] args) {
        Path projectDir = Paths.get(System.getProperty("user.dir"));
        Path sourceDir = projectDir.resolve("source");
        Path destDir = projectDir.resolve("destination");
        String[] fileNames = {"file1.txt", "file2.txt"};

        try {
            if (!Files.exists(sourceDir)) {
                Files.createDirectory(sourceDir);
                System.out.println("Создана директория: " + sourceDir);
            }

            if (!Files.exists(destDir)) {
                Files.createDirectory(destDir);
                System.out.println("Создана директория: " + destDir);
            }

            for (String fileName : fileNames) {
                Path filePath = sourceDir.resolve(fileName);
                if (!Files.exists(filePath)) {
                    createLargeTextFile(filePath, FILE_SIZE_KB);
                    System.out.println("Создан файл: " + filePath);
                } else { System.out.println("Файл уже существует: " + filePath); }
            }

            // sequence copy
            long startSequential = System.nanoTime();
            for (String fileName : fileNames) {
                Path sourceFile = sourceDir.resolve(fileName);
                Path destFile = destDir.resolve("sequential_" + fileName);
                copyFile(sourceFile, destFile);
                System.out.println("Скопирован файл (последовательно): " + destFile.toString());
            }
            long endSequential = System.nanoTime();
            long durationSequential = endSequential - startSequential;
            System.out.println("Время последовательного копирования: " + formatDuration(durationSequential));
            // parallel copy
            long startParallel = System.nanoTime();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future<?>[] futures = new Future<?>[fileNames.length];

            for (int i = 0; i < fileNames.length; i++) {
                String fileName = fileNames[i];
                futures[i] = executor.submit(() -> {
                    Path sourceFile = sourceDir.resolve(fileName);
                    Path destFile = destDir.resolve("parallel_" + fileName);
                    copyFile(sourceFile, destFile);
                    System.out.println("Скопирован файл (параллельно): " + destFile.toString());
                });
            }
            for (Future<?> future : futures) { future.get(); }
            executor.shutdown();
            long endParallel = System.nanoTime();
            long durationParallel = endParallel - startParallel;
            System.out.println("Время параллельного копирования: " + formatDuration(durationParallel));
            System.out.println("Разница последовательного от паралельного: " + formatDuration(durationSequential - durationParallel));
        } catch (IOException | InterruptedException | ExecutionException e) { e.printStackTrace(); }
    }

    private static void copyFile(Path source, Path destination) {
        try { Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка копирования файла: " + source.toString() + " -> " + destination.toString());
            e.printStackTrace();}
    }

    private static void createLargeTextFile(Path path, int sizeKb) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)) {
            String line = "TOP SECRET DATA TOP SECRET DATA TOP SECRET DATA TOP SECRET DATA TOP SECRET DATA";
            int lines = (sizeKb * 1024) / line.length();
            for (int i = 0; i < lines; i++) { writer.write(line); writer.newLine(); }
        }
    }
}
