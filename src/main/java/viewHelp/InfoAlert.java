package viewHelp;

import javafx.scene.control.Alert;

public class InfoAlert {
    private InfoAlert() {
        /* This utility class should not be instantiated */
    }

    public static void showToolInfo(String toolName, String toolSpecificInstructions, String toolDescription) {
        String text = String.format("""
                        How to use:
                        1. Select an image file using "Select image" or drag and drop it into the dash-bordered zone;
                        
                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);
                        
                        %s
                        
                        (Optional) Click "To Clipboard" to copy the image to the clipboard.
                        Certain copied images may not show a preview in the Windows clipboard menu (Win + V).
                        However, the image is still in the clipboard and can be pasted as usual.
                        
                        %s
                        
                        If you have any questions or problems, please go to Info and write to me on Discord.""",

                toolSpecificInstructions,
                toolDescription
        );

        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", toolName, text);
    }

    public static void showToolInfo(String toolName, String toolSpecificInstructions) {
        String text = String.format("""
                        How to use:
                        1. Select an image file using "Select image" or drag and drop it into the dash-bordered zone;
                        
                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);
                        
                        %s
                        
                        (Optional) Click "To Clipboard" to copy the image to the clipboard.
                        Certain copied images may not show a preview in the Windows clipboard menu (Win + V).
                        However, the image is still in the clipboard and can be pasted as usual.
                        
                        If you have any questions or problems, please go to Info and write to me on Discord.""",

                toolSpecificInstructions
        );

        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", toolName, text);
    }

    public static void showToolInfoSimple(String toolName, String toolSpecificInstructions) {
        String text = String.format("""
                        How to use:
                        1. Select an image file using "Select image" or drag and drop it into the dash-bordered zone;
                        
                        %s
                        
                        If you have any questions or problems, please go to Info and write to me on Discord.""",

                toolSpecificInstructions
        );

        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", toolName, text);
    }

    public static void showToolInfoWithoutClipboard(String toolName, String firstInstructions, String secondInstructions) {
        String text = String.format("""
                        How to use:
                        %s;
                        
                        2. (Optional) Select where you want to save the result by clicking on "Directory for save".
                            (Default directory: Desktop);
                        
                        %s
                        
                        If you have any questions or problems, please go to Info and write to me on Discord.""",

                firstInstructions, secondInstructions
        );

        Alerts.alertDialog(Alert.AlertType.INFORMATION, "Information", toolName, text);
    }
}
