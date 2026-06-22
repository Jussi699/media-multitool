package model.helper.pdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import model.utility.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SplitPdfHelper {
    public static void splitMultiplePagesToZip(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                         String baseName, String shortId, List<Integer> sortedIndices,
                                         BiConsumer<Long, Long> progress) throws Exception {
        File zipFile = new File(outputDir, baseName + "_split_" + shortId + ".zip");
        int totalSteps = sortedIndices.size();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (int j = 0; j < sortedIndices.size(); j++) {
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
        }
    }

    public static void splitSinglePage(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                                        String baseName, int idx, BiConsumer<Long, Long> progress) throws Exception {
        try (PDDocument targetDoc = new PDDocument()) {
            try (PDDocument tempDoc = new PDDocument()) {
                tempDoc.addPage(sourceDoc.getPage(idx));
                merger.appendDocument(targetDoc, tempDoc);
            }
            File outputFile = Util.generateUniquePdfOutputFile(outputDir.getAbsolutePath(), baseName + "_page_" + (idx + 1));
            targetDoc.save(outputFile);
        }
        progress.accept(100L, 100L);
    }

    public static void splitByPages(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                              BiConsumer<Long, Long> progress, Set<Integer> selectedPageIndices, File image) throws Exception {
        if (selectedPageIndices.isEmpty()) {
            throw new RuntimeException("No pages selected");
        }

        List<Integer> sortedIndices = new ArrayList<>(selectedPageIndices);
        Collections.sort(sortedIndices);

        String baseName = image.getName().replaceFirst("[.][^.]+$", "");
        String shortId = UUID.randomUUID().toString().substring(0, 8);

        if (sortedIndices.size() == 1) {
            splitSinglePage(sourceDoc, merger, outputDir, baseName, sortedIndices.getFirst(), progress);
        } else {
            splitMultiplePagesToZip(sourceDoc, merger, outputDir, baseName, shortId, sortedIndices, progress);
        }
    }

    public static void splitByRange(PDDocument sourceDoc, PDFMergerUtility merger, File outputDir,
                              BiConsumer<Long, Long> progress, int from, int to, File image) throws Exception {
        int totalPages = sourceDoc.getNumberOfPages();

        from = Math.max(1, from);
        to = Math.min(totalPages, to);

        if (from > totalPages) {
            from = 1;
            to = totalPages;
        }

        if (from > to) return;

        String baseName = image.getName().replaceFirst("[.][^.]+$", "");
        int totalSteps = to - from + 1;
        try (PDDocument targetDoc = new PDDocument()) {
            for (int i = from - 1; i < to; i++) {
                try (PDDocument tempDoc = new PDDocument()) {
                    tempDoc.addPage(sourceDoc.getPage(i));
                    merger.appendDocument(targetDoc, tempDoc);
                }
                progress.accept((long) (i - from + 2), (long) totalSteps);
            }
            File outputFile = Util.generateUniquePdfOutputFile(outputDir.getAbsolutePath(), baseName + "_range");
            targetDoc.save(outputFile);
        }
    }
}
