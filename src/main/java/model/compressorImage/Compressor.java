package model.compressorImage;

import model.logger.ErrorLogger;
import model.properties.ImageProperties;
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
    public static CompressionResult compressorStandardImage(ImageProperties imageProperties) {
        String format = normalizeFormat(DetermineType.determineFormat(imageProperties.getImage()));
        File outputFile = Util.createOutputFile(imageProperties.getImage(), imageProperties.getOutput(), format);
        long originalSize = imageProperties.getImage().length();

        try {
            switch (format) {
                case "jpg", "jpeg" -> Thumbnails.of(imageProperties.getImage())
                        .scale(imageProperties.getScale())
                        .outputFormat("jpg")
                        .outputQuality(imageProperties.getQuality())
                        .toFile(outputFile);
                case "webp" -> Thumbnails.of(imageProperties.getImage())
                        .scale(imageProperties.getScale())
                        .outputFormat("webp")
                        .outputQuality(imageProperties.getQuality())
                        .toFile(outputFile);
                case "png" -> Thumbnails.of(imageProperties.getImage())
                        .scale(imageProperties.getScale())
                        .outputFormat("png")
                        .toFile(outputFile);
                case "tif", "tiff" -> {
                    BufferedImage bi = Thumbnails.of(imageProperties.getImage())
                            .scale(imageProperties.getScale())
                            .asBufferedImage();
                    if (!ImageIO.write(bi, "tiff", outputFile)) {
                        throw new IOException("No appropriate writer found for TIFF");
                    }
                }
                default -> Thumbnails.of(imageProperties.getImage())
                        .scale(imageProperties.getScale())
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

            return new CompressionResult(outputFile, format, originalSize, compressedSize, compressedSize < originalSize);
        } catch (IOException e) {
            ErrorLogger.log(115, ErrorLogger.Level.ERROR, "Failed to compress image", e);
            return null;
        }
    }

    public static CompressionResult removeSvgMetadata(ImageProperties imageProperties) throws IOException {
        File outputFile = Util.createOutputFile(imageProperties.getImage(), imageProperties.getOutput(), "svg");
        long originalSize = imageProperties.getImage().length();

        try {
            String normalizedSvg = normalizeSvg(imageProperties.getImage());
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
            
            try {
                factory.setFeature("https://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception e) {
                ErrorLogger.info("XML feature 'disallow-doctype-decl' not supported in this JDK version");
            }
            
            try {
                factory.setFeature("https://xml.org/sax/features/external-general-entities", false);
            } catch (Exception e) {
                ErrorLogger.info("XML feature 'external-general-entities' not supported in this JDK version");
            }
            
            try {
                factory.setFeature("https://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception e) {
                ErrorLogger.info("XML feature 'external-parameter-entities' not supported in this JDK version");
            }

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
        cleanNode(document.getDocumentElement());
    }

    private static void cleanNode(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.COMMENT_NODE ||
                    (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty())) {
                node.removeChild(child);
                continue;
            }

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = child.getNodeName().toLowerCase(Locale.ROOT);
                if (name.contains("metadata") || name.contains("sodipodi") ||
                        name.contains("inkscape") || name.contains("foreignobject") ||
                        name.equals("desc") || name.equals("title")) {
                    node.removeChild(child);
                    continue;
                }

                org.w3c.dom.NamedNodeMap attributes = child.getAttributes();
                for (int j = attributes.getLength() - 1; j >= 0; j--) {
                    Node attr = attributes.item(j);
                    String attrName = attr.getNodeName().toLowerCase(Locale.ROOT);
                    if (attrName.contains(":") && !attrName.startsWith("xmlns") && !attrName.startsWith("xlink")) {
                        attributes.removeNamedItem(attr.getNodeName());
                    }
                }

                cleanNode(child);

                if (name.equals("g") && !child.hasChildNodes()) {
                    node.removeChild(child);
                }
            }
        }
    }
}
