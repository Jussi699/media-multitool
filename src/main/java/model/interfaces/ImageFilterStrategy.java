package model.interfaces;

import java.awt.image.BufferedImage;

@FunctionalInterface
public interface ImageFilterStrategy {
    BufferedImage process(BufferedImage source) throws Exception;
}
