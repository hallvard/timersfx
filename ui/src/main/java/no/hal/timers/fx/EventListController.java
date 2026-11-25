package no.hal.timers.fx;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.hal.timers.io.EventStore;
import no.hal.timers.io.EventStore.EventInfo;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for the list of events, which is the main screen. It makes, copies, renames and deletes events, and opens them.
 */
public class EventListController {

  /** What is done when the location has been written. */
  private interface PromptAction {
    void accept(String location) throws IOException;
  }

  @FXML
  ResourceBundle resources;

  @FXML
  VBox eventList;

  @FXML
  Label noEventsLabel;

  @FXML
  Label localeLabel;

  @FXML
  Node promptBox;

  @FXML
  Label promptLabel;

  @FXML
  Label datePrefixLabel;

  @FXML
  TextField locationField;

  @FXML
  Label promptErrorLabel;

  private Navigator navigator;
  private EventStore store;
  private PromptAction promptAction;
  /** The event that is asked whether to delete, if any. */
  private String deleting;

  @FXML
  void initialize() {
    // temporary diagnostic: the default locale and the locale the bundle was found for
    localeLabel.setText("locale " + java.util.Locale.getDefault() + ", bundle '" + resources.getLocale() + "'");
    // only what a location can have can be written, so the name of an event is always a valid file name
    locationField.setTextFormatter(new TextFormatter<String>(
        change -> EventStore.isValidLocation(change.getControlNewText()) ? change : null));
    locationField.textProperty().addListener((prop, oldText, newText) -> showError(null));
  }

  void setNavigator(Navigator navigator, EventStore store) {
    this.navigator = navigator;
    this.store = store;
    refresh();
  }

  @FXML
  void handleSettings() {
    navigator.showSettings();
  }

  private static void setShown(Node node, boolean shown) {
    node.setVisible(shown);
    node.setManaged(shown);
  }

  private void showError(String text) {
    promptErrorLabel.setText(text);
    setShown(promptErrorLabel, text != null);
  }

  private void refresh() {
    eventList.getChildren().clear();
    try {
      var events = store.list();
      setShown(noEventsLabel, events.isEmpty());
      for (var event : events) {
        eventList.getChildren().add(event.baseName().equals(deleting) ? createDeleteRow(event) : createRow(event));
      }
    } catch (IOException e) {
      System.err.println("The events could not be listed: " + e);
      setShown(noEventsLabel, true);
    }
  }

  private Button createIconButton(String icon, Runnable action) {
    var button = new Button();
    button.setGraphic(new FontIcon(icon));
    button.setOnAction(event -> action.run());
    return button;
  }

  private Node createRow(EventInfo event) {
    var open = new Button(event.baseName());
    open.setMaxWidth(Double.MAX_VALUE);
    open.setAlignment(Pos.CENTER_LEFT);
    open.setOnAction(e -> navigator.openEvent(event.baseName()));
    HBox.setHgrow(open, Priority.ALWAYS);
    var count = new Label(String.valueOf(event.participantCount()), new FontIcon("mdi2a-account:18"));
    var row = new HBox(open, count,
        createIconButton("mdi2c-content-copy:20", () -> promptDuplicate(event)),
        createIconButton("mdi2p-pencil:20", () -> promptRename(event)),
        createIconButton("mdi2d-delete-outline:20", () -> {
          deleting = event.baseName();
          refresh();
        }));
    row.getStyleClass().addAll("spacer", "event-row");
    return row;
  }

  /** The row of an event that is asked whether to delete, which replaces the ordinary row. */
  private Node createDeleteRow(EventInfo event) {
    var label = new Label(String.format(resources.getString("deleteConfirm.format"), event.baseName()));
    label.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(label, Priority.ALWAYS);
    var confirm = createIconButton("mdi2c-check:24:green", () -> {
      try {
        store.delete(event.baseName());
      } catch (IOException e) {
        System.err.println("The event could not be deleted: " + e);
      }
      deleting = null;
      refresh();
    });
    var cancel = createIconButton("mdi2c-close:24:red", () -> {
      deleting = null;
      refresh();
    });
    var row = new HBox(label, confirm, cancel);
    row.getStyleClass().addAll("spacer", "event-row");
    return row;
  }

  private void prompt(String promptKey, LocalDate date, String location, PromptAction action) {
    promptLabel.setText(resources.getString(promptKey));
    datePrefixLabel.setText(date + "-");
    locationField.setText(location);
    showError(null);
    promptAction = action;
    setShown(promptBox, true);
    locationField.requestFocus();
    locationField.positionCaret(location.length());
  }

  private void closePrompt() {
    promptAction = null;
    setShown(promptBox, false);
  }

  @FXML
  void handleNew() {
    prompt("newEventPrompt.text", LocalDate.now(), "", location -> {
      var baseName = store.create(LocalDate.now(), location, navigator.createDefaultCompetition());
      closePrompt();
      navigator.openEvent(baseName);
    });
  }

  private void promptDuplicate(EventInfo event) {
    prompt("duplicatePrompt.text", LocalDate.now(), event.location(), location -> {
      var baseName = store.duplicate(event.baseName(), LocalDate.now(), location);
      closePrompt();
      navigator.openEvent(baseName);
    });
  }

  private void promptRename(EventInfo event) {
    prompt("renamePrompt.text", event.date(), event.location(), location -> {
      store.rename(event.baseName(), location);
      closePrompt();
      refresh();
    });
  }

  @FXML
  void handleConfirmPrompt() {
    if (promptAction == null) {
      return;
    }
    try {
      promptAction.accept(locationField.getText());
    } catch (FileAlreadyExistsException e) {
      showError(resources.getString("nameTaken.text"));
    } catch (IOException e) {
      System.err.println("The event could not be saved: " + e);
      showError(e.toString());
    }
  }

  @FXML
  void handleCancelPrompt() {
    closePrompt();
  }
}
