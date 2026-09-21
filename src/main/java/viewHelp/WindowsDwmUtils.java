package viewHelp;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.logger.ErrorLogger;
import model.utility.OS;

import java.util.UUID;

public final class WindowsDwmUtils {
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;

    private static final int DEFAULT_CAPTION_COLOR_REF = toColorRef(59, 59, 59);
    private static final int DEFAULT_TEXT_COLOR_REF = toColorRef(0, 255, 0);

    public interface Dwmapi extends StdCallLibrary {
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        int DwmSetWindowAttribute(HWND hwnd, int dwAttribute, IntByReference pvAttribute, int cbAttribute);
    }

    private WindowsDwmUtils() {}

    public static void enableDarkMode(Window window) {
        if (window == null || OS.getOS() != OS.TypeOS.WINDOWS) {
            return;
        }

        if (window.isShowing()) {
            Platform.runLater(() -> applyDarkMode(window));
        }

        window.showingProperty().addListener((_, _, isShowing) -> {
            if (Boolean.TRUE.equals(isShowing)) {
                Platform.runLater(() -> applyDarkMode(window));
            }
        });
    }

    public static boolean applyDarkMode(Window window) {
        if (window == null || OS.getOS() != OS.TypeOS.WINDOWS) {
            return false;
        }

        try {
            HWND hwnd = findHwnd(window);
            if (hwnd != null) {
                return applyCustomDarkTitleBar(hwnd, DEFAULT_CAPTION_COLOR_REF, DEFAULT_TEXT_COLOR_REF);
            }
        } catch (Exception t) {
            ErrorLogger.warn("Failed to apply DWM dark mode: " + t.getMessage());
        }
        return false;
    }

    /**
     * Applies dark mode and attempts to set custom caption/text/border colors.
     * Compatible across all Windows versions:
     * - Windows 11 (22000+): custom background (#3b3b3b), text color (#2ba82b), and border.
     * - Windows 10 (1809+): immersive dark mode title bar fallback.
     * - Older Windows: fails gracefully without error.
     */
    public static boolean applyCustomDarkTitleBar(HWND hwnd, int captionColorRef, int textColorRef) {
        if (hwnd == null) {
            return false;
        }
        boolean darkModeApplied = setWindowDarkMode(hwnd, true);

        // Try setting custom caption color (Windows 11+)
        setDwmAttributeColorRef(hwnd, DWMWA_CAPTION_COLOR, captionColorRef);
        // Try setting custom text color (Windows 11+)
        setDwmAttributeColorRef(hwnd, DWMWA_TEXT_COLOR, textColorRef);
        // Try setting matching border color (Windows 11+)
        setDwmAttributeColorRef(hwnd, DWMWA_BORDER_COLOR, captionColorRef);

        return darkModeApplied;
    }

    public static boolean setWindowDarkMode(HWND hwnd, boolean enable) {
        if (hwnd == null) {
            return false;
        }
        try {
            IntByReference darkMode = new IntByReference(enable ? 1 : 0);
            // DWMWA_USE_IMMERSIVE_DARK_MODE = 20 (Windows 10 20H1+ and Windows 11)
            int hr = Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkMode, 4);
            if (hr != 0) {
                // Fallback for older Windows 10 builds (1809 - 1909): attribute 19
                hr = Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, darkMode, 4);
            }
            return hr == 0;
        } catch (Exception t) {
            ErrorLogger.warn("Error calling DwmSetWindowAttribute: " + t.getMessage());
            return false;
        }
    }

    public static boolean setCaptionColor(HWND hwnd, int red, int green, int blue) {
        return setDwmAttributeColorRef(hwnd, DWMWA_CAPTION_COLOR, toColorRef(red, green, blue));
    }

    public static boolean setTextColor(HWND hwnd, int red, int green, int blue) {
        return setDwmAttributeColorRef(hwnd, DWMWA_TEXT_COLOR, toColorRef(red, green, blue));
    }

    public static boolean setBorderColor(HWND hwnd, int red, int green, int blue) {
        return setDwmAttributeColorRef(hwnd, DWMWA_BORDER_COLOR, toColorRef(red, green, blue));
    }

    private static boolean setDwmAttributeColorRef(HWND hwnd, int attribute, int colorRef) {
        if (hwnd == null) {
            return false;
        }
        try {
            IntByReference color = new IntByReference(colorRef);
            return Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, color, 4) == 0;
        } catch (Exception _) {
            return false;
        }
    }

    private static int toColorRef(int red, int green, int blue) {
        return ((blue & 0xFF) << 16) | ((green & 0xFF) << 8) | (red & 0xFF);
    }

    public static HWND findHwnd(Window window) {
        int currentPid = Kernel32.INSTANCE.GetCurrentProcessId();

        if (window instanceof Stage stage) {
            String title = stage.getTitle();
            if (title != null && !title.isBlank()) {
                HWND hwnd = User32.INSTANCE.FindWindow(null, title);
                if (hwnd != null && isWindowOfProcess(hwnd, currentPid)) {
                    return hwnd;
                }
            }

            // Fallback if title is not unique or blank: use a unique temporary title
            String originalTitle = stage.getTitle();
            String tempTitle = "__MMT_HWND_LOOKUP_" + UUID.randomUUID() + "__";
            stage.setTitle(tempTitle);
            HWND hwnd = User32.INSTANCE.FindWindow(null, tempTitle);
            stage.setTitle(originalTitle);
            if (hwnd != null && isWindowOfProcess(hwnd, currentPid)) {
                return hwnd;
            }
        }

        // Fallback: search among visible top-level windows of current process
        final HWND[] found = new HWND[1];
        User32.INSTANCE.EnumWindows((hwnd, pointer) -> {
            if (isWindowOfProcess(hwnd, currentPid) && User32.INSTANCE.IsWindowVisible(hwnd)) {
                found[0] = hwnd;
                return false;
            }
            return true;
        }, null);

        return found[0];
    }

    private static boolean isWindowOfProcess(HWND hwnd, int targetPid) {
        IntByReference windowPid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, windowPid);
        return windowPid.getValue() == targetPid;
    }
}
