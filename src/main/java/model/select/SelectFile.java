package model.select;

import com.sun.jna.Pointer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NFDOpenDialogArgs;
import org.lwjgl.util.nfd.NFDWindowHandle;
import org.lwjgl.util.nfd.NativeFileDialog;
import model.utility.OS;
import viewHelp.Alerts;
import viewHelp.WindowsDwmUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static model.utility.PathWorker.*;
import static org.lwjgl.util.nfd.NativeFileDialog.NFD_WINDOW_HANDLE_TYPE_WINDOWS;


public class SelectFile extends AbstractSelectFile {
    private static final Object NFD_LOCK = new Object();

    @Override
    public Optional<File> choiceFile(Stage stage, FileChooser.ExtensionFilter filter) {
        synchronized (NFD_LOCK) {
            boolean initialized = false;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ensureInitialized();
                initialized = true;

                PointerBuffer result = stack.mallocPointer(1);
                NFDFilterItem.Buffer filters = createFilter(stack, filter);
                ByteBuffer defaultPath = stack.UTF8(getSavedInputPath().getAbsolutePath());
                int status = openDialog(result, filters, defaultPath, stage, false, stack);

                if (status == NativeFileDialog.NFD_OKAY) {
                    File selectedFile = new File(MemoryUtil.memUTF8(result.get(0)));
                    NativeFileDialog.NFD_FreePath(result.get(0));
                    saveInputPath(selectedFile);
                    return Optional.of(selectedFile);
                }
                if (status == NativeFileDialog.NFD_ERROR) {
                    throw nativeDialogError("opening a file");
                }
                return Optional.empty();
            } catch (RuntimeException exception) {
                handleDialogError(exception);
                return Optional.empty();
            } finally {
                if (initialized) {
                    NativeFileDialog.NFD_Quit();
                }
            }
        }
    }

    public Optional<List<File>> showOpenMultipleDialog(Stage stage, FileChooser.ExtensionFilter filter) {
        synchronized (NFD_LOCK) {
            boolean initialized = false;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ensureInitialized();
                initialized = true;

                PointerBuffer result = stack.mallocPointer(1);
                NFDFilterItem.Buffer filters = createFilter(stack, filter);
                ByteBuffer defaultPath = stack.UTF8(getSavedInputPath().getAbsolutePath());
                int status = openDialog(result, filters, defaultPath, stage, true, stack);

                if (status == NativeFileDialog.NFD_OKAY) {
                    long pathSet = result.get(0);
                    int[] count = new int[1];
                    NativeFileDialog.NFD_PathSet_GetCount(pathSet, count);
                    List<File> files = new ArrayList<>(count[0]);
                    try {
                        PointerBuffer path = stack.mallocPointer(1);
                        for (int i = 0; i < count[0]; i++) {
                            NativeFileDialog.NFD_PathSet_GetPath(pathSet, i, path);
                            files.add(new File(MemoryUtil.memUTF8(path.get(0))));
                        }
                    } finally {
                        NativeFileDialog.NFD_PathSet_Free(pathSet);
                    }
                    if (!files.isEmpty()) {
                        saveInputPath(files.getFirst());
                    }
                    return Optional.of(files);
                }
                if (status == NativeFileDialog.NFD_ERROR) {
                    throw nativeDialogError("opening multiple files");
                }
                return Optional.empty();
            } catch (RuntimeException exception) {
                handleDialogError(exception);
                return Optional.empty();
            } finally {
                if (initialized) {
                    NativeFileDialog.NFD_Quit();
                }
            }
        }
    }

    private static NFDFilterItem.Buffer createFilter(MemoryStack stack, FileChooser.ExtensionFilter filter) {
        NFDFilterItem.Buffer filters = NFDFilterItem.malloc(1, stack);
        filters.name(stack.UTF8(filter.getDescription()));
        filters.spec(stack.UTF8(toNfdSpec(filter.getExtensions())));
        return filters;
    }

    private static String toNfdSpec(List<String> extensions) {
        return extensions.stream()
                .map(extension -> extension.replace("*.", "").replace("*", ""))
                .filter(extension -> !extension.isBlank())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static int openDialog(
            PointerBuffer result,
            NFDFilterItem.Buffer filters,
            ByteBuffer defaultPath,
            Stage stage,
            boolean multiple,
            MemoryStack stack
    ) {
        NFDWindowHandle parentWindow = createParentWindow(stage, stack);
        if (parentWindow == null) {
            return multiple
                    ? NativeFileDialog.NFD_OpenDialogMultiple(result, filters, defaultPath)
                    : NativeFileDialog.NFD_OpenDialog(result, filters, defaultPath);
        }

        NFDOpenDialogArgs args = NFDOpenDialogArgs.malloc(stack)
                .filterList(filters)
                .defaultPath(defaultPath)
                .parentWindow(parentWindow);
        return multiple
                ? NativeFileDialog.NFD_OpenDialogMultiple_With(result, args)
                : NativeFileDialog.NFD_OpenDialog_With(result, args);
    }

    private static NFDWindowHandle createParentWindow(Stage stage, MemoryStack stack) {
        if (stage == null || OS.getOS() != OS.TypeOS.WINDOWS) {
            return null;
        }

        var hwnd = WindowsDwmUtils.findHwnd(stage);
        if (hwnd == null) {
            return null;
        }

        return NFDWindowHandle.malloc(stack)
                .set(NFD_WINDOW_HANDLE_TYPE_WINDOWS, Pointer.nativeValue(hwnd.getPointer()));
    }

    private static void ensureInitialized() {
        if (NativeFileDialog.NFD_Init() != NativeFileDialog.NFD_OKAY) {
            throw nativeDialogError("initializing the native file dialog");
        }
    }

    private static IllegalStateException nativeDialogError(String operation) {
        String error = NativeFileDialog.NFD_GetError();
        return new IllegalStateException("Failed " + operation + " via NativeFileDialog-Extended"
                + (error == null || error.isBlank() ? "" : ": " + error));
    }

    private static void handleDialogError(RuntimeException exception) {
        String message = exception.getMessage();
        String detail = message == null || message.isBlank()
                ? "The native file dialog could not be opened."
                : message;
        Alerts.alertDialog(
                javafx.scene.control.Alert.AlertType.ERROR,
                "File selection error",
                "Native file dialog failed",
                detail
        );
    }
}
