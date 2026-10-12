package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MediaType {
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    ALL_MEDIA("ALL_MEDIA"),
    NONE("");

    final String typeCategory;
}
