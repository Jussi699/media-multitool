package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BatchTargetType {
    ALL("All media"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    AUDIO("Audio");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }
}