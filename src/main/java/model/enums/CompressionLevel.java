package model.enums;

import lombok.Getter;

@Getter
public enum CompressionLevel {
    LOW(0.85f),
    MEDIUM(0.6f),
    HIGH(0.3f);

    private final float imageQuality;

    CompressionLevel(float imageQuality) {
        this.imageQuality = imageQuality;
    }
}