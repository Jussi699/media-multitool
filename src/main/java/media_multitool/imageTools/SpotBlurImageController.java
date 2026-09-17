package media_multitool.imageTools;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import media_multitool.AbstractMediaController;
import model.helper.images.BlurShapeHelper;
import model.helper.images.BlurShapeHelper.BlurShapeVisual;
import model.helper.images.BlurShapeHelper.ResizeHandle;
import model.helper.images.BlurShapeHelper.ShapeType;
import model.checks.Checking;
import model.preprocessing.ImagePreprocessing;
import model.logger.ErrorLogger;
import model.properties.ImageProperties;
import model.properties.MediaProperties;
import model.select.SelectFile;
import model.utility.*;
import viewHelp.Alerts;
import viewHelp.SliderSetup;
import viewHelp.ZoomControlHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;

import static model.utility.PathWorker.createOutputFile;
import static model.utility.PathWorker.getSavedPath;
import static viewHelp.Message.*;

public class SpotBlurImageController extends AbstractMediaController {
    private final ImageProperties imageProperties = new ImageProperties();
    private BufferedImage originalBufferedImage, currentBufferedImage;

    @FXML private Slider sliderBlurIntensity, imageScaleSlider;
    @FXML private ScrollPane scrollPaneImage;
    @FXML private StackPane dropZone, previewContainer;
    @FXML private Pane blurOverlay;
    @FXML private Button btnSelectFile, btnChoiceFolderForSaveFile, btnSubmit, btnCancel;
    @FXML private Button btnAddRect, btnAddOval, btnUndo;
    @FXML private Label labelSelectImageName, textDragZone, labelPreviewPlaceholder;
    @FXML private Label labelBlurIntensity;
    @FXML private ImageView imageViewPreview;

    private Task<?> currentTask;
    private List<Control> listControls;
    private ZoomControlHelper zoomControlHelper;

    private final List<BlurShapeVisual> blurShapes = new ArrayList<>();
    private BlurShapeVisual currentShape = null;
    private BlurShapeVisual selectedShape = null;
    private boolean isDrawing = false;
    private ResizeHandle resizeHandle = null;
    private ShapeType selectedShapeType = ShapeType.RECTANGLE;
    private double dragOffsetX, dragOffsetY;
    private double originalStartX, originalStartY, originalEndX, originalEndY;

    private static final int HANDLE_SIZE = 8;

    @Override
    protected MediaProperties getProperties() {
        return imageProperties;
    }

    @FXML
    public void initialize() {
        listControls = List.of(sliderBlurIntensity, btnSubmit, btnAddRect, btnAddOval, btnUndo, btnReset, imageScaleSlider);
        imageProperties.setOutput(getSavedPath());

        sliderBlurIntensity.setMin(0);
        sliderBlurIntensity.setValue(5);
        sliderBlurIntensity.setMax(100);

        setupClearMessageTimer(labelSuccess, progressBar, imageProperties.getHideSuccessMessageTimer(), true);

        zoomControlHelper = new ZoomControlHelper(scrollPaneImage, imageViewPreview, imageScaleSlider, previewContainer, 1.0, 3.0);

        SliderSetup.setupListenerInSliderForUpdateNewValueInLabelInTheFromNumbers(sliderBlurIntensity, labelBlurIntensity);

        sliderBlurIntensity.setOnMouseReleased(_ -> {
            if (!blurShapes.isEmpty()) {
                updateBlurPreview();
            }
        });

        sliderBlurIntensity.setOnTouchReleased(_ -> {
            if (!blurShapes.isEmpty()) {
                updateBlurPreview();
            }
        });

        previewContainer.widthProperty().addListener((_, _, _) -> {
            if (!blurShapes.isEmpty()) {
                updateShapeVisuals();
            }
        });
        previewContainer.heightProperty().addListener((_, _, _) -> {
            if (!blurShapes.isEmpty()) {
                updateShapeVisuals();
            }
        });
        
        previewContainer.layoutBoundsProperty().addListener((_, oldVal, newVal) -> {
            if (!blurShapes.isEmpty() && oldVal != null && !oldVal.equals(newVal)) {
                Platform.runLater(this::updateShapeVisuals);
            }
        });

        setupImageViewInteraction();

        isPressedReset();
        setupDragAndDrop(dropZone, Global.getAllSupportedImageFormats(), this::loadFile);
    }

    private void setupImageViewInteraction() {
        if (imageViewPreview == null) {
            return;
        }

        blurOverlay.setOnMousePressed(this::handleMousePressed);
        blurOverlay.setOnMouseDragged(this::handleMouseDragged);
        blurOverlay.setOnMouseReleased(this::handleMouseReleased);
        blurOverlay.setOnMouseMoved(this::handleMouseMoved);
    }

     private void handleMouseMoved(MouseEvent event) {
         ResizeHandle handle = getHandleAtPosition(event.getX(), event.getY());
         if (handle != null) {
             blurOverlay.setCursor(handle.cursor);
         } else if (getShapeAtPosition(event.getX(), event.getY()) != null) {
             blurOverlay.setCursor(Cursor.HAND);
         } else {
             blurOverlay.setCursor(Cursor.DEFAULT);
         }
     }

     private boolean isPointInsideImage(double x, double y, Bounds imageBounds) {
         return x >= imageBounds.getMinX() && x <= imageBounds.getMaxX() &&
                y >= imageBounds.getMinY() && y <= imageBounds.getMaxY();
     }

     private void setupResizeHandle(ResizeHandle handle) {
         if (handle != null) {
             resizeHandle = handle;
             selectedShape = handle.shape;
             originalStartX = selectedShape.startX;
             originalStartY = selectedShape.startY;
             originalEndX = selectedShape.endX;
             originalEndY = selectedShape.endY;
         }
     }

      private void setupShapeSelection(double displayX, double displayY, double normalizedX, double normalizedY) {
          selectedShape = getShapeAtPosition(displayX, displayY);
          if (selectedShape != null) {
              dragOffsetX = normalizedX - selectedShape.startX;
              dragOffsetY = normalizedY - selectedShape.startY;
              selectedShape.isSelected = true;
              updateShapeVisuals();
          }
      }

     private void startNewShape(double normalizedX, double normalizedY) {
         if (selectedShapeType != null) {
             isDrawing = true;
             currentShape = new BlurShapeVisual(selectedShapeType, normalizedX, normalizedY, normalizedX, normalizedY);
             updateShapeVisuals();
         }
     }

         private void handleMousePressed(MouseEvent event) {
             if (originalBufferedImage == null || event.getButton() != MouseButton.PRIMARY) {
                 return;
             }

             event.consume();

             Bounds imageBounds = imageViewPreview.localToParent(imageViewPreview.getBoundsInLocal());
             
             if (!isPointInsideImage(event.getX(), event.getY(), imageBounds)) {
                 return;
             }
             
             double normalizedX = (event.getX() - imageBounds.getMinX()) / imageBounds.getWidth();
             double normalizedY = (event.getY() - imageBounds.getMinY()) / imageBounds.getHeight();

             ResizeHandle handle = getHandleAtPosition(event.getX(), event.getY());
             if (handle != null) {
                 setupResizeHandle(handle);
                 return;
             }

             if (getShapeAtPosition(event.getX(), event.getY()) != null) {
                 setupShapeSelection(event.getX(), event.getY(), normalizedX, normalizedY);
                 return;
             }

             startNewShape(normalizedX, normalizedY);
         }

        private void handleMouseDragged(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            event.consume();

             Bounds imageBounds = imageViewPreview.localToParent(imageViewPreview.getBoundsInLocal());

             double clampedX = Math.clamp(event.getX(), imageBounds.getMinX(), imageBounds.getMaxX());
             double clampedY = Math.clamp(event.getY(), imageBounds.getMinY(), imageBounds.getMaxY());
            
             double normalizedX = (clampedX - imageBounds.getMinX()) / imageBounds.getWidth();
             double normalizedY = (clampedY - imageBounds.getMinY()) / imageBounds.getHeight();

             if (resizeSelectedShape(normalizedX, normalizedY)) { return; }
             if (moveSelectedShape(normalizedX, normalizedY))    { return; }

           if (isDrawing && currentShape != null) {
               currentShape.endX = normalizedX;
               currentShape.endY = normalizedY;
               updateShapeVisuals();
           }
       }

    private boolean moveSelectedShape(double normalizedX, double normalizedY) {
        if (selectedShape == null || isDrawing) return false;

        double newStartX = normalizedX - dragOffsetX;
        double newStartY = normalizedY - dragOffsetY;

        double currentWidth = selectedShape.endX - selectedShape.startX;
        double currentHeight = selectedShape.endY - selectedShape.startY;

        selectedShape.startX = newStartX;
        selectedShape.startY = newStartY;
        selectedShape.endX = newStartX + currentWidth;
        selectedShape.endY = newStartY + currentHeight;

        constrainShapeToImageBounds(selectedShape);
        updateShapeVisuals();
        return true;
    }

    private boolean resizeSelectedShape(double normalizedX, double normalizedY) {
        if (resizeHandle == null || selectedShape == null) return false;

        double left = originalStartX;
        double top = originalStartY;
        double right = originalEndX;
        double bottom = originalEndY;

        if (resizeHandle.position == ResizeHandle.Position.TOP_LEFT ||
                resizeHandle.position == ResizeHandle.Position.LEFT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_LEFT) {
            left = normalizedX;
        }
        if (resizeHandle.position == ResizeHandle.Position.TOP_LEFT ||
                resizeHandle.position == ResizeHandle.Position.TOP ||
                resizeHandle.position == ResizeHandle.Position.TOP_RIGHT) {
            top = normalizedY;
        }
        if (resizeHandle.position == ResizeHandle.Position.TOP_RIGHT ||
                resizeHandle.position == ResizeHandle.Position.RIGHT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_RIGHT) {
            right = normalizedX;
        }
        if (resizeHandle.position == ResizeHandle.Position.BOTTOM_LEFT ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM ||
                resizeHandle.position == ResizeHandle.Position.BOTTOM_RIGHT) {
            bottom = normalizedY;
        }

        selectedShape.startX = Math.min(left, right);
        selectedShape.startY = Math.min(top, bottom);
        selectedShape.endX   = Math.max(left, right);
        selectedShape.endY   = Math.max(top, bottom);

        updateShapeVisuals();
        return true;
    }

       private void constrainShapeToImageBounds(BlurShapeVisual shape) {
           double minX = Math.min(shape.startX, shape.endX);
           double maxX = Math.max(shape.startX, shape.endX);
           double minY = Math.min(shape.startY, shape.endY);
           double maxY = Math.max(shape.startY, shape.endY);

           boolean fitsHorizontally = (minX >= 0.0 && maxX <= 1.0);
           boolean fitsVertically = (minY >= 0.0 && maxY <= 1.0);

           if (!fitsHorizontally) {
               if (minX < 0.0) {
                   double diff = -minX;
                   shape.startX += diff;
                   shape.endX += diff;
               } else if (maxX > 1.0) {
                   double diff = maxX - 1.0;
                   shape.startX -= diff;
                   shape.endX -= diff;
               }
           }

           if (!fitsVertically) {
               if (minY < 0.0) {
                   double diff = -minY;
                   shape.startY += diff;
                   shape.endY += diff;
               } else if (maxY > 1.0) {
                   double diff = maxY - 1.0;
                   shape.startY -= diff;
                   shape.endY -= diff;
               }
           }

           shape.startX = Math.clamp(shape.startX, 0.0, 1.0);
           shape.startY = Math.clamp(shape.startY, 0.0, 1.0);
           shape.endX   = Math.clamp(shape.endX, 0.0, 1.0);
           shape.endY   = Math.clamp(shape.endY, 0.0, 1.0);
       }

        private void handleMouseReleased(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            event.consume();

            if (isDrawing && currentShape != null) {
                isDrawing = false;

                if (Math.abs(currentShape.endX - currentShape.startX) > 0.01 && 
                    Math.abs(currentShape.endY - currentShape.startY) > 0.01) {
                    blurShapes.add(currentShape);
                    updateBlurPreview();
                }

                currentShape = null;
                updateShapeVisuals();
            } else if (selectedShape != null) {
                constrainShapeToImageBounds(selectedShape);
                updateBlurPreview();
                selectedShape.isSelected = false;
                selectedShape = null;
                updateShapeVisuals();
            } else {
                blurShapes.forEach(shape -> shape.isSelected = false);
                updateShapeVisuals();
            }
            
            resizeHandle = null;
        }

    private BlurShapeVisual getShapeAtPosition(double x, double y) {
         Bounds imageBounds = imageViewPreview.localToParent(imageViewPreview.getBoundsInLocal());
         for (BlurShapeVisual shape : blurShapes) {
             if (BlurShapeHelper.isPointInShape(x, y, shape, imageBounds)) {
                 return shape;
             }
         }
         return null;
     }

    private ResizeHandle getHandleAtPosition(double x, double y) {
        Bounds imageBounds = imageViewPreview.localToParent(imageViewPreview.getBoundsInLocal());
        for (BlurShapeVisual shape : blurShapes) {
            ResizeHandle handle = shape.getHandleAt(x, y, imageBounds);
            if (handle != null) { return handle; }
        }
        return null;
    }

       private void updateShapeVisuals() {
          blurOverlay.getChildren().clear();
          Bounds imageBounds = imageViewPreview.localToParent(imageViewPreview.getBoundsInLocal());

          for (BlurShapeVisual shape : blurShapes) {
              Shape visual = BlurShapeHelper.createShapeVisual(shape, false, imageBounds);

              if (visual != null) { blurOverlay.getChildren().add(visual); }

              List<Shape> guides = new ArrayList<>();
              BlurShapeHelper.drawDashedGuides(shape, imageBounds, guides);
              blurOverlay.getChildren().addAll(guides);
              for (ResizeHandle handle : shape.getHandles(imageBounds)) {
                  Rectangle handleRect = new Rectangle(handle.x - (double) HANDLE_SIZE / 2, handle.y - (double) HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
                  handleRect.setFill(Color.WHITE);
                  handleRect.setStroke(Color.CYAN);
                  handleRect.setStrokeWidth(1);
                  blurOverlay.getChildren().add(handleRect);
              }
          }

          if (currentShape != null) {
              Shape visual = BlurShapeHelper.createShapeVisual(currentShape, true, imageBounds);
              if (visual != null) { blurOverlay.getChildren().add(visual); }
          }
      }

     private void updateBlurPreview() {
          if (currentBufferedImage == null || originalBufferedImage == null) {
              return;
          }

          currentTask = new Task<BufferedImage>() {
              @Override
              protected BufferedImage call() {
                  updateProgress(0, 1.0);
                  updateMessage("Applying blur effect...");
                  
                  int blurIntensity = (int) sliderBlurIntensity.getValue();
                  BufferedImage result = new BufferedImage(
                          originalBufferedImage.getWidth(),
                          originalBufferedImage.getHeight(),
                          BufferedImage.TYPE_INT_ARGB
                  );

                  Graphics2D g2d = result.createGraphics();
                  g2d.drawImage(originalBufferedImage, 0, 0, null);
                  g2d.dispose();

                  int width = originalBufferedImage.getWidth();
                  int height = originalBufferedImage.getHeight();
                  long totalPixels = (long) width * height;
                  long processedPixels = 0;

                  for (int y = 0; y < height; y++) {
                      if (isCancelled()) {
                          return null;
                      }
                      for (int x = 0; x < width; x++) {
                          for (BlurShapeVisual shape : blurShapes) {
                              if (BlurShapeHelper.isPointInShapeImage(x, y, shape, imageViewPreview)) {
                                  int blurredRGB = BlurShapeHelper.getBlurredPixel(originalBufferedImage, x, y, blurIntensity);
                                  result.setRGB(x, y, blurredRGB);
                                  break;
                              }
                          }
                          processedPixels++;

                          if (processedPixels % 1000 == 0 || x == width - 1) {
                              double progress = (double) processedPixels / totalPixels;
                              updateProgress(progress, 1.0);
                          }
                      }
                  }

                  updateProgress(1.0, 1.0);
                  return result;
              }
          };

          executeMediaTask(currentTask);
      }

    @FXML
    public void onActionAddRectBlur() {
        selectedShapeType = ShapeType.RECTANGLE;
        btnAddRect.setStyle("-fx-background-color: #32CD32;");
        btnAddOval.setStyle("");
    }

    @FXML
    public void onActionAddOvalBlur() {
        selectedShapeType = ShapeType.ELLIPSE;
        btnAddOval.setStyle("-fx-background-color: #32CD32;");
        btnAddRect.setStyle("");
    }

    @FXML
    public void onActionUndo() {
        if (!blurShapes.isEmpty()) {
            blurShapes.removeLast();
            updateShapeVisuals();
            updateBlurPreview();
        }
    }

     @FXML
     public void handleBlurIntensityChange() {
         labelBlurIntensity.setText(String.valueOf((int) sliderBlurIntensity.getValue()));
     }

    @FXML
    private void showInfo() {
        Alerts.alertDialog(
                Alert.AlertType.INFORMATION,
                "Information",
                "Spot Blur",
                """
                        How to use:
                        1. Select an image file using 'Select image' or drag and drop.
                        2. (Optional) Choose a directory for saving the output.
                        3. Adjust the blur intensity (0-20) using the slider.
                        4. Click 'Rectangle' or 'Ellipse' to select shape type.
                        5. Click and drag on the image to add blur areas.
                        6. After adding a shape:
                           - Drag it to move
                           - Use white squares on corners/edges to resize
                        7. Use 'Undo' to remove the last shape.
                        8. Use the zoom slider for precision.
                        9. Blur preview updates in real-time.
                        10. Click 'Download' to save the final image.
                        
                        Tips:
                        - RED outline shows the shape being drawn
                        - Cyan outline shows already added shapes
                        - White squares are resize handles
                        
                        If you have any questions or problems, please go to Info and write to me on Discord."""
        );
    }

    @Override
    protected void lockUI() {
        disableControls();
        btnSelectFile.setDisable(true);
        btnChoiceFolderForSaveFile.setDisable(true);
        btnReset.setDisable(true);
        btnCancel.setVisible(true);
        btnCancel.setManaged(true);
    }

    @Override
    protected void unlockUI() {
        enableControls();
        btnSelectFile.setDisable(false);
        btnChoiceFolderForSaveFile.setDisable(false);
        btnReset.setDisable(false);
        btnCancel.setVisible(false);
        btnCancel.setManaged(false);
    }

    @Override
    protected void disableControls() {
        listControls.forEach(c -> c.setDisable(true));
    }

    @Override
    protected void enableControls() {
        listControls.forEach(c -> c.setDisable(false));
    }

    @FXML
    public void onActionBtnSelectFile() {
        SelectFile selectImageFile = new SelectFile();
        Stage stage = (Stage) btnSelectFile.getScene().getWindow();
        selectImageFile.choiceFile(stage,
                new FileChooser.ExtensionFilter("Images", Global.getSupportedImageFormatsForFileChooser()),
                "Select image"
        ).ifPresent(this::loadFile);
    }

    @FXML
    public void onChoiceFolderForSaveFile() {
        selectOutputDirectory(btnChoiceFolderForSaveFile, imageProperties.getOutput(), imageProperties::setOutput, "Select directory for save image");
    }

     @FXML
     public void submitAndDownload() {
         if (Checking.checkImageAndOutputOnNull(imageProperties) || originalBufferedImage == null) {
             return;
         }

         if (blurShapes.isEmpty()) {
             Alerts.alertDialog(Alert.AlertType.WARNING, "No shapes", "No blur shapes", "Please add at least one blur shape before downloading.");
             return;
         }

         currentTask = new Task<File>() {
             @Override
             protected File call() throws Exception {
                 updateProgress(0, 1.0);
                 updateMessage("Applying final blur...");

                 BufferedImage finalBlur = applyBlurShapes(originalBufferedImage);
                 if (finalBlur == null) {
                     return null;
                 }

                 updateProgress(0.9, 1.0);
                 updateMessage("Saving image...");

                 File outputFile = createOutputFile(
                         imageProperties.getImage(),
                         imageProperties.getOutput(),
                         imageProperties.getTypeImage()
                 );

                 ImagePreprocessing.downloadImage(finalBlur, imageProperties.getTypeImage(), outputFile);
                 updateProgress(1.0, 1.0);
                 updateMessage("Download complete!");

                 return outputFile;
             }
         };

         executeMediaTask(currentTask);
         if (labelSuccess != null) {
             labelSuccess.setManaged(true);
         }
     }

     private BufferedImage applyBlurShapes(BufferedImage sourceImage) {
           BufferedImage result = new BufferedImage(
                   sourceImage.getWidth(),
                   sourceImage.getHeight(),
                   BufferedImage.TYPE_INT_ARGB
           );

           Graphics2D g2d = result.createGraphics();
           g2d.drawImage(sourceImage, 0, 0, null);
           g2d.dispose();

           int blurIntensity = (int) sliderBlurIntensity.getValue();
           int width = sourceImage.getWidth();
           int height = sourceImage.getHeight();

           for (int y = 0; y < height; y++) {
               if (Thread.interrupted()) {
                   return null;
               }
               for (int x = 0; x < width; x++) {
                   for (BlurShapeVisual shape : blurShapes) {
                       if (BlurShapeHelper.isPointInShapeImage(x, y, shape, imageViewPreview)) {
                           int blurredRGB = BlurShapeHelper.getBlurredPixel(sourceImage, x, y, blurIntensity);
                           result.setRGB(x, y, blurredRGB);
                           break;
                       }
                   }
               }
           }

           return result;
       }

    @FXML
    private void cancelTask() {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }
    }

    @Override
    protected void handleTaskSuccess(Object result) {
        if (result instanceof BufferedImage bi) {
            currentBufferedImage = bi;
            setPreview(currentBufferedImage);
            Platform.runLater(() -> {
                if (progressBar != null) {
                    progressBar.setVisible(true);
                    progressBar.setManaged(true);
                }
                if (labelSuccess != null) {
                    labelSuccess.setVisible(true);
                    labelSuccess.setManaged(true);
                }
            });
            return;
        }

        super.handleTaskSuccess(result);

        if (Boolean.FALSE.equals(result)) {
            return;
        }
        File outputFile = (File) result;
        ErrorLogger.info("Spot blur successful! Saved to: " + outputFile.getAbsolutePath());

        Platform.runLater(() -> {
            showSuccessText(labelSuccess, "Spot blur image saved!", imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @Override
    protected void handleTaskFailure(Throwable exception) {
        super.handleTaskFailure(exception);
        Platform.runLater(() -> {
            showErrorMessage(labelSuccess, "Error: " + exception.getMessage(), imageProperties.getHideSuccessMessageTimer());
            labelSuccess.setManaged(true);
        });
    }

    @FXML
    public void isPressedReset() {
        ResetContext ctx = new ResetContext(
                labelSelectImageName, labelSuccess, textDragZone, labelPreviewPlaceholder,
                dropZone, imageViewPreview, progressBar, true
        );
        reset(imageProperties, ctx, "Selected image file: none");

        currentBufferedImage = null;
        originalBufferedImage = null;
        blurShapes.clear();
        currentShape = null;
        selectedShape = null;
        blurOverlay.getChildren().clear();

        if (sliderBlurIntensity != null) {
            sliderBlurIntensity.setValue(5);
            labelBlurIntensity.setText("5%");
        }

        selectedShapeType = ShapeType.RECTANGLE;
        btnAddRect.setStyle("-fx-background-color: #32CD32;");
        btnAddOval.setStyle("");

        zoomControlHelper.resetZoom();
        disableControls();
    }

    private void loadFile(File selectedFile) {
        enableControls();

        blurShapes.clear();
        currentShape = null;
        selectedShape = null;
        resizeHandle = null;
        blurOverlay.getChildren().clear();
        currentBufferedImage = null;
        originalBufferedImage = null;

        imageProperties.setImage(selectedFile);
        imageProperties.setTypeImage(DetermineType.determineFormat(selectedFile).orElse(null));
        labelSelectImageName.setText("Select image: " + selectedFile.getName());

        supportLoadFile(selectedFile);

        if (textDragZone != null) {
            textDragZone.setText("Selected: " + selectedFile.getName());
        }
        if (dropZone != null && !dropZone.getStyleClass().contains("drop-zone-filled")) {
            dropZone.getStyleClass().add("drop-zone-filled");
        }
    }

    private void supportLoadFile(File selectedFile) {
        if (imageViewPreview != null) {
            try {
                originalBufferedImage = ImageIO.read(selectedFile);
                if (originalBufferedImage != null) {
                    if (labelPreviewPlaceholder != null) {
                        labelPreviewPlaceholder.setVisible(false);
                    }
                    currentBufferedImage = new BufferedImage(
                            originalBufferedImage.getWidth(),
                            originalBufferedImage.getHeight(),
                            BufferedImage.TYPE_INT_ARGB
                    );
                    Graphics2D g2d = currentBufferedImage.createGraphics();
                    g2d.drawImage(originalBufferedImage, 0, 0, null);
                    g2d.dispose();

                    setPreview(currentBufferedImage);
                    zoomControlHelper.resetZoom();
                    zoomControlHelper.updateImageSize();
                }
            } catch (Exception e) {
                ErrorLogger.error("Failed to load preview: " + e.getMessage());
            }
        }
    }

    private void setPreview(BufferedImage bi) {
        if (bi != null && imageViewPreview != null) {
            Image image = SwingFXUtils.toFXImage(bi, null);
            imageViewPreview.setImage(image);
        }
    }

}


