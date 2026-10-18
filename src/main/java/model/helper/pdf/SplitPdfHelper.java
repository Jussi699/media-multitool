package model.helper.pdf;

import model.exceptions.ImageProcessingException;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static model.utility.PathWorker.generateUniquePdfOutputFile;
import static model.utility.Utility.cleanupFile;

public class SplitPdfHelper {
    private SplitPdfHelper() {
        /* This utility class should not be instantiated */
    }

    public static File splitMultiplePagesToZip(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                               String baseName, String shortId, List<Integer> sortedIndices,
                                               BiConsumer<Long, Long> progress) throws IOException {
        File zipFile = new File(outputDir, baseName + "_split_" + shortId + ".zip");
        int totalSteps = sortedIndices.size();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (int j = 0; j < sortedIndices.size(); j++) {
                checkCancellation();
                int idx = sortedIndices.get(j);
                try (PDDocument targetDoc = new PDDocument()) {
                    try (PDDocument tempDoc = new PDDocument()) {
                        tempDoc.addPage(sourceDoc.getPage(idx));
                        merger.appendDocument(targetDoc, tempDoc);
                    }
                    ZipEntry ze = new ZipEntry(baseName + "_page_" + (idx + 1) + "_" + shortId + ".pdf");
                    zos.putNextEntry(ze);
                    targetDoc.save(zos);
                    zos.closeEntry();
                }
                progress.accept((long) (j + 1), (long) totalSteps);
            }
            checkCancellation();
            return zipFile;
        } catch (IOException | RuntimeException exception) {
            cleanupFile(zipFile);
            throw exception;
        }
    }

    public static File splitSinglePage(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                        String baseName, int idx, BiConsumer<Long, Long> progress) throws IOException {
        checkPageIndex(sourceDoc, idx);
        File outputFile = generateUniquePdfOutputFile(outputDir.getAbsolutePath(), baseName + "_page_" + (idx + 1));
        try {
            try (PDDocument targetDoc = new PDDocument()) {
                try (PDDocument tempDoc = new PDDocument()) {
                    tempDoc.addPage(sourceDoc.getPage(idx));
                    merger.appendDocument(targetDoc, tempDoc);
                }
                checkCancellation();
                targetDoc.save(outputFile);
            }
            checkCancellation();
            progress.accept(100L, 100L);
            return outputFile;
        } catch (IOException | RuntimeException exception) {
            cleanupFile(outputFile);
            throw exception;
        }
    }

    public static File splitByPages(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                    BiConsumer<Long, Long> progress, Set<Integer> selectedPageIndices,
                                    File image) throws ImageProcessingException, IOException {
        if (selectedPageIndices.isEmpty()) {
            throw new ImageProcessingException("No pages selected");
        }

        List<Integer> sortedIndices = new ArrayList<>(selectedPageIndices);
        Collections.sort(sortedIndices);
        for (int index : sortedIndices) {
            checkPageIndex(sourceDoc, index);
        }

        String baseName = image.getName().replaceFirst("[.][^.]+$", "");
        String shortId = UUID.randomUUID().toString().substring(0, 8);

        if (sortedIndices.size() == 1) {
            return splitSinglePage(sourceDoc, merger, outputDir, baseName, sortedIndices.getFirst(), progress);
        }
        return splitMultiplePagesToZip(sourceDoc, merger, outputDir, baseName, shortId, sortedIndices, progress);
    }

    public static File splitByRange(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                    BiConsumer<Long, Long> progress, int from, int to,
                                    File image) throws IOException {
        int totalPages = sourceDoc.getNumberOfPages();
        if (from < 1 || to < from || to > totalPages) {
            throw new IllegalArgumentException("Page range must be within 1.." + totalPages
                    + " and the start page must not exceed the end page.");
        }

        String baseName = image.getName().replaceFirst("[.][^.]+$", "");
        int totalSteps = to - from + 1;
        File outputFile = generateUniquePdfOutputFile(outputDir.getAbsolutePath(), baseName + "_range");
        try {
            try (PDDocument targetDoc = new PDDocument()) {
                for (int i = from - 1; i < to; i++) {
                    checkCancellation();
                    try (PDDocument tempDoc = new PDDocument()) {
                        tempDoc.addPage(sourceDoc.getPage(i));
                        merger.appendDocument(targetDoc, tempDoc);
                    }
                    progress.accept((long) (i - from + 2), (long) totalSteps);
                }
                checkCancellation();
                targetDoc.save(outputFile);
            }
            checkCancellation();
            return outputFile;
        } catch (IOException | RuntimeException exception) {
            cleanupFile(outputFile);
            throw exception;
        }
    }

    private static void checkPageIndex(PDDocument document, int index) {
        if (index < 0 || index >= document.getNumberOfPages()) {
            throw new IllegalArgumentException("Page index is outside the PDF: " + index);
        }
    }

    private static void checkCancellation() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Split cancelled");
        }
    }
}
