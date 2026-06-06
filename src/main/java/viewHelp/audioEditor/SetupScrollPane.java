package viewHelp.audioEditor;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import model.utility.DetailsAudioFile;
import model.worker.TableViewWorker;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SetupScrollPane {
    public static void setupHorizontalDragScroll(ScrollPane scrollPane) {
        var dragState = new Object() {
            double startX;
            double startHvalue;
            boolean dragging;
        };

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isShiftDown() || Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY())) {
                double delta = (Math.abs(event.getDeltaX()) > 0 ? event.getDeltaX() : event.getDeltaY()) * -0.002;
                scrollPane.setHvalue(clamp(scrollPane.getHvalue() + delta, 0, 1));
                event.consume();
            }
        });

        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.isMiddleButtonDown() || (event.isPrimaryButtonDown() && event.isAltDown())) {
                dragState.dragging = true;
                dragState.startX = event.getScreenX();
                dragState.startHvalue = scrollPane.getHvalue();
                scrollPane.setCursor(Cursor.CLOSED_HAND);
                event.consume();
            }
        });

        scrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!dragState.dragging) {
                return;
            }
            Node content = scrollPane.getContent();
            if (content == null) {
                return;
            }
            double scrollableWidth = Math.max(content.getLayoutBounds().getWidth() - scrollPane.getViewportBounds().getWidth(), 1);
            double delta = (dragState.startX - event.getScreenX()) / scrollableWidth;
            scrollPane.setHvalue(clamp(dragState.startHvalue + delta, 0, 1));
            event.consume();
        });

        scrollPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!dragState.dragging) {
                return;
            }
            dragState.dragging = false;
            scrollPane.setCursor(Cursor.DEFAULT);
            event.consume();
        });
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean needsVerticalScroll(TableView<?> tableView, double viewportHeight) {
        if (viewportHeight <= 0 || tableView.getItems().isEmpty()) {
            return false;
        }
        double contentHeight = TableViewWorker.getTableHeaderHeight(tableView) + tableView.getItems().size() * tableView.getFixedCellSize();
        return contentHeight > viewportHeight + 0.5;
    }

    public static void updateVerticalScrollBarVisibility(
            TableView<?> tableView,
            ScrollPane scrollPane,
            ScrollBar externalBar) {

        boolean needsScroll = needsVerticalScroll(tableView, scrollPane.getViewportBounds().getHeight());
        externalBar.setVisible(needsScroll);
        externalBar.setManaged(needsScroll);
    }

    public static void configureTableHorizontalScroll(
            ScrollPane scrollPane,
            TableView<DetailsAudioFile> tableView,
            List<TableColumn<DetailsAudioFile, ?>> columns,
            ScrollBar externalBar,
            ScrollBar[] internalBarRef) {

        tableView.setFixedCellSize(TableViewWorker.TABLE_ROW_HEIGHT);

        Runnable updateTableWidth = () -> {
            double totalWidth = columns.stream().mapToDouble(TableColumnBase::getWidth).sum();
            tableView.setPrefWidth(totalWidth);
            tableView.setMinWidth(totalWidth);
            tableView.setMaxWidth(totalWidth);
        };

        updateTableWidth.run();
        columns.forEach(column -> column.widthProperty().addListener((_, _, _) -> updateTableWidth.run()));

        scrollPane.viewportBoundsProperty().addListener((_, _, bounds) -> {
            tableView.setPrefHeight(bounds.getHeight());
            tableView.setMinHeight(bounds.getHeight());
            if (!hasRealTableData(tableView)) {
                fillPlaceholderRows(tableView, bounds.getHeight());
            }
        });

        hideTableViewHorizontalScrollBar(tableView);
        configureTableVerticalScrollBar(tableView, scrollPane, externalBar, internalBarRef);

        Platform.runLater(() -> {
            updateTableWidth.run();
            setupHorizontalDragScroll(scrollPane);
            if (!hasRealTableData(tableView)) {
                fillPlaceholderRows(tableView, scrollPane.getViewportBounds().getHeight());
            }
        });
    }

    public static void configureTableVerticalScrollBar(
            TableView<?> tableView,
            ScrollPane scrollPane,
            ScrollBar externalBar,
            ScrollBar[] internalBarRef) {

        Runnable sync = () -> {
            ScrollBar internalBar = null;
            for (Node node : tableView.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.VERTICAL) {
                    internalBar = scrollBar;
                    break;
                }
            }
            if (internalBar == null) {
                return;
            }

            internalBar.setVisible(false);
            internalBar.setManaged(false);
            internalBar.setPrefWidth(0);
            internalBar.setMaxWidth(0);

            if (internalBarRef[0] != internalBar) {
                if (internalBarRef[0] != null) {
                    externalBar.minProperty().unbind();
                    externalBar.maxProperty().unbind();
                    externalBar.valueProperty().unbindBidirectional(internalBarRef[0].valueProperty());
                }
                internalBarRef[0] = internalBar;
                externalBar.minProperty().bind(internalBar.minProperty());
                externalBar.maxProperty().bind(internalBar.maxProperty());
                externalBar.valueProperty().bindBidirectional(internalBar.valueProperty());
            }

            updateVerticalScrollBarVisibility(tableView, scrollPane, externalBar);
        };

        scrollPane.viewportBoundsProperty().addListener((_, _, bounds) -> {
            externalBar.setPrefHeight(bounds.getHeight());
            externalBar.setMinHeight(bounds.getHeight());
            externalBar.setMaxHeight(bounds.getHeight());
            Platform.runLater(() -> updateVerticalScrollBarVisibility(tableView, scrollPane, externalBar));
        });

        tableView.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(sync);
            }
        });
        InvalidationListener onItemsChange = _ -> Platform.runLater(sync);
        tableView.getItems().addListener(onItemsChange);
        Platform.runLater(sync);
    }

    public static boolean hasRealTableData(TableView<DetailsAudioFile> tableView) {
        return tableView.getItems().stream().anyMatch(Objects::nonNull);
    }

    public static void fillPlaceholderRows(TableView<DetailsAudioFile> tableView, double viewportHeight) {
        int rowCount = Math.max(1, TableViewWorker.getVisibleRowCount(tableView, viewportHeight));
        if (tableView.getItems().size() == rowCount && tableView.getItems().stream().allMatch(Objects::isNull)) {
            return;
        }
        tableView.getItems().setAll(Collections.nCopies(rowCount, null));
    }

    public static void hideTableViewHorizontalScrollBar(TableView<?> tableView) {
        Runnable hide = () -> {
            for (Node node : tableView.lookupAll(".scroll-bar")) {
                if (node instanceof ScrollBar scrollBar && scrollBar.getOrientation() == Orientation.HORIZONTAL) {
                    scrollBar.setVisible(false);
                    scrollBar.setManaged(false);
                    scrollBar.setPrefHeight(0);
                    scrollBar.setMaxHeight(0);
                }
            }
        };

        tableView.skinProperty().addListener((_, _, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(hide);
            }
        });
        InvalidationListener onItemsChange = _ -> Platform.runLater(hide);
        tableView.getItems().addListener(onItemsChange);
        Platform.runLater(hide);
    }
}
