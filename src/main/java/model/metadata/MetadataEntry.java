package model.metadata;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetadataEntry {
    private final StringProperty category = new SimpleStringProperty();
    private final StringProperty key = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();
    private final BooleanProperty canDelete = new SimpleBooleanProperty();
    private final Object rawTagKey;

    public MetadataEntry(String category, String key, String value, boolean canDelete, Object rawTagKey) {
        this.category.set(category != null ? category : "");
        this.key.set(key != null ? key : "");
        this.value.set(value != null ? value : "");
        this.canDelete.set(canDelete);
        this.rawTagKey = rawTagKey;
    }

    public String getCategory() {
        return category.get();
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public String getKey() {
        return key.get();
    }

    public StringProperty keyProperty() {
        return key;
    }

    public String getValue() {
        return value.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public boolean isCanDelete() {
        return canDelete.get();
    }

    public BooleanProperty canDeleteProperty() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete.set(canDelete);
    }

    public Object getRawTagKey() {
        return rawTagKey;
    }
}
