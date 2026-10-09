package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaType {
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio");

    final String typeCategory;
}
