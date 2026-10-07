package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AspectRatio {
    TO_SQUARE(1, 1),
    TO_9X16(9, 16),
    TO_16X9(16, 9),
    TO_4X5(4, 5),
    TO_5X4(5, 4),
    TO_3X4(3, 4),
    TO_4X3(4, 3),
    TO_2X3(2, 3),
    TO_3X2(3, 2),
    TO_5X7(5, 7),
    TO_7X5(7, 5),
    TO_1X2(1, 2),
    TO_2X1(2, 1);

    private final double ratioWidth;
    private final double ratioHeight;
}
