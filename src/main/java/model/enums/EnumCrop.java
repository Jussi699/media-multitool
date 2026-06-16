package model.enums;

import javafx.scene.Cursor;

public class EnumCrop {
    public enum HandlePosition {
        TOP_LEFT(Cursor.NW_RESIZE, true, false, true, false),
        TOP(Cursor.N_RESIZE, false, false, true, false),
        TOP_RIGHT(Cursor.NE_RESIZE, false, true, true, false),
        RIGHT(Cursor.E_RESIZE, false, true, false, false),
        BOTTOM_RIGHT(Cursor.SE_RESIZE, false, true, false, true),
        BOTTOM(Cursor.S_RESIZE, false, false, false, true),
        BOTTOM_LEFT(Cursor.SW_RESIZE, true, false, false, true),
        LEFT(Cursor.W_RESIZE, true, false, false, false);

        public final Cursor cursor;
        public final boolean changesLeft;
        public final boolean changesRight;
        public final boolean changesTop;
        public final boolean changesBottom;

        HandlePosition(Cursor cursor, boolean changesLeft, boolean changesRight, boolean changesTop, boolean changesBottom) {
            this.cursor = cursor;
            this.changesLeft = changesLeft;
            this.changesRight = changesRight;
            this.changesTop = changesTop;
            this.changesBottom = changesBottom;
        }
    }

    public enum DragMode {
        NONE,
        CREATE,
        MOVE,
        RESIZE
    }
}
