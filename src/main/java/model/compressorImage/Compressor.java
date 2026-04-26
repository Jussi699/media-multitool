package model.compressorImage;

import model.logger.ErrorLogger;
import model.utility.DetermineType;
import model.utility.Util;
import net.coobird.thumbnailator.Thumbnails;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Compressor {
    public static CompressionResult compressorStandardImage(File file, File pathToSave, float scale, float outputQuality)
            throws IOException {
        String format = normalizeFormat(DetermineType.determineFormat(file));
        File outputFile = Util.createOutputFile(file, pathToSave, format);
        long originalSize = file.length();

        try {
            switch (format) {
                case "jpg", "jpeg" -> Thumbnails.of(file)
                        .scale(scale)
                        .outputFormat("jpg")
                        .outputQuality(outputQuality)
                        .toFile(outputFile);
                case "webp" -> Thumbnails.of(file)
                        .scale(scale)
                        .outputFormat("webp")
                        .outputQuality(outputQuality)
                        .toFile(outputFile);
                case "png" -> Thumbnails.of(file)
                        .scale(scale)
                        .outputFormat("png")
                        .toFile(outputFile);
                case "tif", "tiff" -> {
                    BufferedImage bi = Thumbnails.of(file)
                            .scale(scale)
                            .asBufferedImage();
                    if (!ImageIO.write(bi, "tiff", outputFile)) {
                        throw new IOException("No appropriate writer found for TIFF");
                    }
                }
                default -> Thumbnails.of(file)
                        .scale(scale)
                        .outputFormat(format)
                        .toFile(outputFile);
            }

            long compressedSize = outputFile.length();
            if (compressedSize <= 0) {
                return null;
            }

            if (compressedSize >= originalSize && !outputFile.delete()) {
                ErrorLogger.warn("Compressed file is larger than source and could not be deleted: "
                        + outputFile.getAbsolutePath());
            }

            boolean sizeReduced = compressedSize < originalSize;
            return new CompressionResult(outputFile, format, originalSize, compressedSize, sizeReduced);
        } catch (IOException e) {
            ErrorLogger.log(115, ErrorLogger.Level.ERROR, "Failed to compress image", e);
            return null;
        }
    }

    public static CompressionResult removeSvgMetadata(File file, File pathToSave) throws IOException {
        File outputFile = Util.createOutputFile(file, pathToSave, "svg");
        long originalSize = file.length();

        try {
            String normalizedSvg = normalizeSvg(file);
            Files.writeString(outputFile.toPath(), normalizedSvg);

            long compressedSize = outputFile.length();
            if (compressedSize <= 0) {
                return null;
            }

            if (compressedSize >= originalSize && !outputFile.delete()) {
                ErrorLogger.warn("SVG file without metadata is larger than source and could not be deleted: "
                        + outputFile.getAbsolutePath());
            }

            boolean sizeReduced = compressedSize < originalSize;
            return new CompressionResult(outputFile, "svg", originalSize, compressedSize, sizeReduced);
        } catch (IOException e) {
            ErrorLogger.log(117, ErrorLogger.Level.ERROR, "SVG metadata removal error", e);
            throw e;
        }
    }

    private static String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new IllegalArgumentException("Unable to determine image format");
        }

        String normalized = format.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jpg", "jpeg" -> "jpeg";
            case "svg+xml", "svg" -> "svg";
            case "tif", "tiff" -> "tiff";
            case "x-icon", "vnd.microsoft.icon" -> "ico";
            case "x-portable-pixmap" -> "ppm";
            case "x-portable-graymap" -> "pgm";
            default -> normalized;
        };
    }

    private static String normalizeSvg(File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            removeMetadataNodes(document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString().trim();
        } catch (Exception e) {
            ErrorLogger.log(119, ErrorLogger.Level.ERROR, "Failed to normalize SVG before compression", e);
            throw new IOException("Failed to process SVG", e);
        }
    }

    private static void removeMetadataNodes(Document document) {
        NodeList metadataNodes = document.getElementsByTagNameNS("*", "metadata");
        for (int i = metadataNodes.getLength() - 1; i >= 0; i--) {
            Node metadataNode = metadataNodes.item(i);
            Node parent = metadataNode.getParentNode();
            if (parent != null) {
                parent.removeChild(metadataNode);
            }
        }
    }
}
