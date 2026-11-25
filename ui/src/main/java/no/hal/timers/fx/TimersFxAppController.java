package no.hal.timers.fx;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation;
import no.hal.timers.core.TimeProvider;
import no.hal.timers.io.CsvTarget;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for the app as a whole.
 */
public class TimersFxAppController {

  private Competition competition;

  @FXML
  Label timeLabel;
  
  @FXML
  String timeLabelFormat;

  @FXML
  String durationLabelFormat;

  @FXML
  String longDurationLabelFormat;

  @FXML
  ScrollPane scrollPane;

  @FXML
  GridPane competitionGrid;

  @FXML
  CheckBox selectAllButton;

  @FXML
  Node participantColumnHeader;

  @FXML
  Button startButton;

  @FXML
  Button timeButton;

  /** Not shown, but used as the look of the buttons for the last timing key, which finishes. */
  @FXML
  Button finishButton;

  @FXML
  Node timingKeysControls;

  @FXML
  ToggleButton editTimingKeysButton;

  @FXML
  ToggleButton editParticipantNamesButton;

  /** The texts, from the resource bundle of the FXML: the default names, and the empty time. */
  @FXML
  ResourceBundle resources;

  private String emptyTimeLabel;
  private Timeline timeline;

  @FXML
  void initialize() {
    emptyTimeLabel = resources.getString("emptyTimeLabel");
    if (timeLabel != null) {
      timeline = new Timeline();
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.getKeyFrames()
          .add(new KeyFrame(javafx.util.Duration.seconds(1), event -> handleTimeChanged()));
      timeline.playFromStart();
    }
    if (shareResultsButton != null && TimersFxApp.getResultsSharer() != null) {
      shareResultsButton.setVisible(true);
      shareResultsButton.setManaged(true);
    }
  }

  private Runnable onChanged = () -> { };
  private Runnable onBack = () -> { };

  /**
   * Shows an event, instead of the empty table the screen starts with.
   *
   * @param competition the table of the event
   * @param onChanged called when the table has changed, to save it
   * @param onBack called to go back to the list of events
   */
  void setEvent(Competition competition, Runnable onChanged, Runnable onBack) {
    this.competition = competition;
    this.onBack = onBack;
    updateGrid();
    clearSelection();
    // set last, so showing the event isn't saved as a change
    this.onChanged = onChanged;
  }

  /** Goes back to the list of events. The names that are being edited are kept first. */
  @FXML
  void handleBack() {
    if (participantTextFields != null) {
      updateEditedParticipants();
      participantTextFields = null;
    }
    if (timingKeyTextFields != null) {
      var newTimingKeys = getEditedText(timingKeyTextFields, true);
      competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
      timingKeyTextFields = null;
    }
    onChanged.run();
    // the clock must not go on when the screen is gone
    if (timeline != null) {
      timeline.stop();
    }
    onBack.run();
  }

  private final TimeProvider timeProvider = () -> LocalTime.now();

  private String formatTime(Duration time) {
    if (time == null) {
      return emptyTimeLabel;
    } else {
      int hour = time.toHoursPart();
      int min = time.toMinutesPart();
      int sec = time.toSecondsPart();
      return (hour > 0
          ? String.format(longDurationLabelFormat, hour, min, sec)
          : String.format(durationLabelFormat, min, sec)
      );
    }
  }

  private final Collection<Consumer<LocalTime>> timeCallbacks = new ArrayList<>();

  private void handleTimeChanged() {
    LocalTime time = timeProvider.get();
    timeLabel.setText(String.format(timeLabelFormat, time.getHour(), time.getMinute(), time.getSecond()));
    for (Consumer<LocalTime> timeCallback : timeCallbacks) {
      timeCallback.accept(time);
    }
  }

  private final Collection<Participation> selection = new ArrayList<>();

  private final Map<Participation, CheckBox> selectParticipationButtons = new HashMap<>();

  private void clearSelection() {
    selectAllButton.setSelected(false);
    selection.clear();
    for (Participation participation : selectParticipationButtons.keySet()) {
      setSelected(participation, false);
    }
  }

  private void setSelection(Collection<Participation> newSelection) {
    selection.clear();
    selection.addAll(newSelection);
    for (var participation : selectParticipationButtons.keySet()) {
      setSelected(participation, newSelection.contains(participation));
    }
  }

  private void setSelected(Participation participation, boolean selected) {
    var button = selectParticipationButtons.get(participation);
    if (button != null && button.isSelected() != selected) {
      button.setSelected(selected);
    }
    if (selected == selection.contains(participation)) {
      // do nothing
    } else if (selected) {
      selection.add(participation);
    } else {
      selection.remove(participation);
    }
    handleParticipationActionButtonsEnablement();
  }

  private boolean isSelected(Participation participation) {
    return selection.contains(participation);
  }

  private boolean toggleSelected(Participation participation) {
    var selected = isSelected(participation);
    setSelected(participation, ! selected);
    return ! selected;
  }

  private final Map<Action<Participation>, Button> participationActions = new HashMap<>();

  private void handleParticipationActionButtonsEnablement() {
    for (var entry : participationActions.entrySet()) {
      var action = entry.getKey();
      // enabled when the action applies to at least one of the selected, and is then done for those it applies to
      var disabled = selection.stream().noneMatch(action::isFor);
      entry.getValue().setDisable(disabled);
    }
  }

  private List<TextField> timingKeyTextFields = null;
  private List<TextField> participantTextFields = null;

  private final Action<Participation> startParticipationAction = new Action<Participation>() {
    
    @Override
    public boolean isFor(Participation participation) {
      return participation.getStatus() == Participation.Status.START;
    }

    @Override
    public void doFor(Participation participation) {
      participation.start(timeProvider.get());
    }
  };

  private void updateGrid() {
    var children = competitionGrid.getChildren();
    children.clear();
    var columnConstraints = competitionGrid.getColumnConstraints();
    columnConstraints.clear();

    timeCallbacks.clear();
    participationActions.clear();
    selectParticipationButtons.clear();

    // add headers
    var rowIndex = 0;
    var columnIndex = 0;

    addNode(participantColumnHeader, rowIndex, columnIndex++, children);
    columnConstraints.add(new ColumnConstraints(110.0, Region.USE_COMPUTED_SIZE,
        Region.USE_PREF_SIZE, Priority.SOMETIMES, HPos.LEFT, true));
    editParticipantNamesButton.setSelected(participantTextFields != null);

    addNode(selectAllButton, rowIndex, columnIndex++, children);
    columnConstraints.add(new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE,
        Region.USE_PREF_SIZE, Priority.NEVER, HPos.CENTER, false));

    registerAction(addNode(startButton, rowIndex, columnIndex++, children),
        startParticipationAction);
    columnConstraints.add(new ColumnConstraints(60, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE,
        Priority.NEVER, HPos.CENTER, false));

    var timingKeys = competition.getTimingKeys();
    for (var i = 0; i < timingKeys.size(); i++) {
      addTimingKeyColumnHeader(i, rowIndex, columnIndex++, children);
      columnConstraints.add(new ColumnConstraints(60,
          Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE,
          Priority.SOMETIMES, HPos.CENTER, false));
    }
    addNode(timingKeysControls, rowIndex, columnIndex++, children);
    columnConstraints.add(new ColumnConstraints(50, 50, 50, Priority.NEVER, HPos.CENTER, true));
    editTimingKeysButton.setSelected(timingKeyTextFields != null);

    rowIndex++;
    columnIndex = 0;
    for (var participation : competition) {
      addParticipantCell(participation, rowIndex, columnIndex++, children);
      addSelectParticipantCell(participation, rowIndex, columnIndex++, children);
      addParticipantStartCell(participation, rowIndex, columnIndex++, children);

      var last = Duration.ZERO;
      for (var i = 0; i < timingKeys.size(); i++) {
        addParticipantTimingCell(participation, i, rowIndex, columnIndex++, last, children);
        var durationOpt = participation.getDuration(competition.getTimeKey(i));
        if (durationOpt.isPresent()) {
          last = durationOpt.get();
        }
      }
      rowIndex++;
      columnIndex = 0;
    }
    addEditingSpace();
    handleParticipationActionButtonsEnablement();
    updateStopButton();
    // every change makes the grid again, so this is where the table is saved
    onChanged.run();
  }

  private boolean isFinishKey(int timingKeyIndex) {
    return timingKeyIndex == competition.getTimingKeys().size() - 1;
  }

  /**
   * The action of a timing key. For the last one it is finishing, whatever the key is named, and it is recorded as FINISH.
   * For the others it is timing the key. Both are for participants that are active, so not for those that finished or stopped.
   */
  private Action<Participation> createTimingAction(int timingKeyIndex) {
    var timeKey = competition.getTimeKey(timingKeyIndex);
    if (isFinishKey(timingKeyIndex)) {
      return new Action<Participation>() {
        @Override
        public boolean isFor(Participation participation) {
          return participation.getStatus() == Participation.Status.ACTIVE;
        }

        @Override
        public void doFor(Participation participation) {
          participation.finish(timeProvider.get());
        }
      };
    }
    return new Action<Participation>() {
      @Override
      public boolean isFor(Participation participation) {
        return participation.getStatus() == Participation.Status.ACTIVE
            && (! participation.hasTime(timeKey));
      }

      @Override
      public void doFor(Participation participation) {
        participation.time(timeProvider.get(), timeKey);
      }
    };
  }

  private int addTimingKeyColumnHeader(int timingKeyIndex,
      int rowIndex, int columnIndex,
      ObservableList<Node> children) {
    var timingKey = competition.getTimingKeys().get(timingKeyIndex);
    if (timingKeyTextFields != null) {
      var textField = addClearableTextField(timingKey, rowIndex, columnIndex, children);
      timingKeyTextFields.add(textField);
    } else {
      // tapping the name of the timing key times (or finishes) all the selected participants that are active and don't have the time yet
      var button = addNode(new Button(), rowIndex, columnIndex, children);
      copyLookAndFeel(timeButton, button, false);
      button.setText(timingKey);
      if (isFinishKey(timingKeyIndex)) {
        copyLookAndFeel(finishButton, button, true);
        button.setText(timingKey);
      }
      registerAction(button, createTimingAction(timingKeyIndex));
    }
    return columnIndex;
  }

  private Optional<Duration> addParticipantTimingCell(Participation participation,
      int timingKeyIndex, int rowIndex,
      int columnIndex, Duration lastDuration,
      ObservableList<Node> children) {
    var timeKey = competition.getTimeKey(timingKeyIndex);
    var durationOpt = participation.getDuration(timeKey);
    if (durationOpt.isPresent()) {
      var duration = durationOpt.get();
      // the laps show the time since the previous one, the finish shows the total time
      var labelDuration = (isFinishKey(timingKeyIndex) ? duration : duration.minus(lastDuration));
      addNode(new Label(formatTime(labelDuration)), rowIndex, columnIndex, children);
    } else if (participation.getStatus() == Participation.Status.STOP
        || participation.getStatus() == Participation.Status.FINISH) {
      // stopped, or finished without this lap (which is up to the participant), so there is nothing more to time
    } else {
      var timeParticipationButton = addNode(new Button(), rowIndex, columnIndex, children);
      copyLookAndFeel(timeButton, timeParticipationButton, false);
      timeParticipationButton.setText("");
      if (isFinishKey(timingKeyIndex)) {
        copyLookAndFeel(finishButton, timeParticipationButton, false);
        timeParticipationButton.setText("");
      }
      var action = createTimingAction(timingKeyIndex);
      timeParticipationButton.setOnAction(event -> {
        if (action.isFor(participation)) {
          action.doFor(participation);
          updateGrid();
        }
      });
    }
    return durationOpt;
  }

  private int addParticipantStartCell(Participation participation,
      int rowIndex, int columnIndex,
      ObservableList<Node> children) {
    if (participation.getStartTime().isPresent()) {
      // the time only runs while the participant is active, and the total is with the finish
      if (participation.getEndDuration().isEmpty()) {
        var durationLabel = addNode(new Label(formatDuration(participation)),
            rowIndex, columnIndex, children);
        timeCallbacks.add(time -> durationLabel.setText(formatDuration(participation)));
      }
    } else {
      var startParticipationButton = addNode(new Button(), rowIndex, columnIndex, children);
      copyLookAndFeel(startButton, startParticipationButton, false);
      startParticipationButton.setOnAction(event -> {
        if (startParticipationAction.isFor(participation)) {
          startParticipationAction.doFor(participation);
          updateGrid();
        }
      });
    }
    return columnIndex;
  }

  private String formatDuration(Participation participation) {
    var duration = Duration.between(participation.getStartTime().get(),
        competition.getCurrentTime());
    return formatTime(duration);
  }

  private int addSelectParticipantCell(Participation participation,
      int rowIndex, int columnIndex,
      ObservableList<Node> children) {
    var selectButton = addNode(new CheckBox(), rowIndex, columnIndex, children);
    selectButton.setSelected(isSelected(participation));
    selectButton.setOnAction(event -> {
      setSelected(participation, selectButton.isSelected());
      selectAllButton.setSelected(false);
    });
    selectParticipationButtons.put(participation, selectButton);
    return columnIndex;
  }

  private int addParticipantCell(Participation participation,
      int rowIndex, int columnIndex,
      ObservableList<Node> children) {
    String participantName = participation.getParticipant().getName();
    if (participantTextFields != null) {
      var textField = addClearableTextField(participantName, rowIndex, columnIndex, children);
      participantTextFields.add(textField);
    } else {
      Pane pane = addNode(new Pane(), rowIndex, columnIndex, children);
      pane.getStyleClass().add(rowIndex % 2 == 0 ? "even-row" : "odd-row");
      pane.getChildren().add(new Label(participantName));
      //      addNode(new Label(participantName), rowIndex, columnIndex, children);
    }
    return columnIndex;
  }

  @FXML
  void handleEditTimingKeys() {
    if (timingKeyTextFields != null) {
      var newTimingKeys = getEditedText(timingKeyTextFields, true);
      competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
      timingKeyTextFields = null;
    } else {
      timingKeyTextFields = new ArrayList<>();
    }
    updateGrid();
  }

  private List<String> getEditedText(Collection<TextField> textFields, boolean filterEmpty) {
    return textFields.stream()
        .map(TextField::getText)
        .filter(text -> (! filterEmpty) || text.trim().length() > 0)
        .collect(Collectors.toList());
  }

  @FXML
  void handleAddTimingKey() {
    var newTimingKeys = (timingKeyTextFields != null
        ? getEditedText(timingKeyTextFields, false)
        : new ArrayList<>(competition.getTimingKeys()));
    newTimingKeys.add("");
    competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
    timingKeyTextFields = new ArrayList<>();
    updateGrid();
  }

  @FXML
  void handleEditParticipantNames() {
    if (participantTextFields != null) {
      updateEditedParticipants();
      participantTextFields = null;
    } else {
      participantTextFields = new ArrayList<>();
    }
    updateGrid();
  }

  private void updateEditedParticipants() {
    if (participantTextFields != null) {
      var newParticipantNames = getEditedText(participantTextFields, false);
      int participantNum = 0;
      for (String name : newParticipantNames) {
        var participant = competition.getParticipation(participantNum).getParticipant();
        if (name.isBlank()) {
          competition.removeParticipant(participant);
        } else {
          participant.setName(name);
          participantNum++;
        }
      }
    }
  }

  @FXML
  void handleAddParticipant() {
    updateEditedParticipants();
    competition.addParticipant(new Participant(""));
    participantTextFields = new ArrayList<>();
    updateGrid();
  }

  private void registerAction(Button button, Action<Participation> action) {
    participationActions.put(action, button);
    button.setOnAction(event -> {
      var count = 0;
      for (var participation : selection) {
        if (action.isFor(participation)) {
          action.doFor(participation);
          count++;
        }
      }
      if (count > 0) {
        updateGrid();
        clearSelection();
      }
    });
  }

  @FXML
  Button clearTextFieldButton;

  private TextField addClearableTextField(String text, int rowIndex, int columnIndex,
      Collection<Node> children) {
    var textField = new TextField(text);
    var clearButton = new Button();
    copyLookAndFeel(clearTextFieldButton, clearButton, true);
    clearButton.setOnAction(event -> textField.clear());
    HBox box = addNode(new HBox(textField, clearButton), rowIndex, columnIndex, children);
    box.setAlignment(Pos.CENTER);
    // the on-screen keyboard may cover the line, so it is scrolled to the top of the view
    textField.focusedProperty().addListener((prop, wasFocused, isFocused) -> {
      if (isFocused) {
        Platform.runLater(() -> scrollToTop(box));
      }
    });
    return textField;
  }

  private void scrollToTop(Node node) {
    var content = scrollPane.getContent();
    content.applyCss();
    scrollPane.layout();
    double range = content.getLayoutBounds().getHeight() - scrollPane.getViewportBounds().getHeight();
    if (range > 0) {
      double y = node.localToScene(node.getBoundsInLocal()).getMinY()
          - content.localToScene(content.getBoundsInLocal()).getMinY();
      scrollPane.setVvalue(Math.clamp(y / range, 0.0, 1.0));
    }
  }

  /**
   * While editing, space as high as the view is added below the grid, so that a line near the end can still be scrolled to the top.
   */
  private void addEditingSpace() {
    if (participantTextFields != null || timingKeyTextFields != null) {
      var lastRow = competitionGrid.getChildren().stream()
          .mapToInt(node -> GridPane.getRowIndex(node) != null ? GridPane.getRowIndex(node) : 0)
          .max().orElse(0);
      var space = new Region();
      space.setPrefHeight(Math.max(scrollPane.getViewportBounds().getHeight(), 300.0));
      GridPane.setConstraints(space, 0, lastRow + 1, 2, 1);
      competitionGrid.getChildren().add(space);
    }
  }

  private <T extends Node> T addNode(T node, int rowIndex, int columnIndex,
      Collection<Node> children) {
    //    System.out.println("Adding " + node + " @ " + rowIndex + ", " + columnIndex);
    GridPane.setConstraints(node, columnIndex, rowIndex);
    children.add(node);
    return node;
  }

  private void copyLookAndFeel(Button source, Button target,
      boolean includeScaling) {
    target.setStyle(source.getStyle());
    target.getStyleClass().addAll(source.getStyleClass());
    target.setText(source.getText());
    if (source.getGraphic() instanceof ImageView) {
      ImageView sourceImageView = (ImageView) source.getGraphic();
      ImageView targetImageView = new ImageView(sourceImageView.getImage());
      if (includeScaling) {
        targetImageView.setScaleX(sourceImageView.getScaleX());
        targetImageView.setScaleY(sourceImageView.getScaleY());
      }
      target.setGraphic(targetImageView);
    } else if (source.getGraphic() instanceof FontIcon) {
      FontIcon sourceFontIcon = (FontIcon) source.getGraphic();
      FontIcon targetFontIcon = new FontIcon(sourceFontIcon.getIconLiteral());
      targetFontIcon.setRotate(sourceFontIcon.getRotate());
      targetFontIcon.setIconSize(sourceFontIcon.getIconSize());
      targetFontIcon.setIconColor(sourceFontIcon.getIconColor());
      if (includeScaling) {
        targetFontIcon.setScaleX(sourceFontIcon.getScaleX());
        targetFontIcon.setScaleY(sourceFontIcon.getScaleY());
      }
      target.setGraphic(targetFontIcon);
    }
  }

  @FXML
  void handleSelectAll() {
    setSelection(selectAllButton.isSelected()
        ? selectParticipationButtons.keySet()
        : Collections.emptyList());
  }

  @FXML
  Button shareResultsButton;

  /** Copies the results (CSV) to the system clipboard. */
  @FXML
  void handleCopyCsvToClipboard() {
    Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, CsvTarget.toString(competition)));
  }

  /**
   * Shares the results (CSV) with other apps. Only on the phone, where the mobile module sets how, see
   * {@link TimersFxApp#setResultsSharer}.
   */
  @FXML
  void handleShareResults() {
    var sharer = TimersFxApp.getResultsSharer();
    if (sharer != null) {
      sharer.accept(CsvTarget.toString(competition));
    }
  }

  @FXML
  Button stopButton;

  /** Stop is enabled as long as a participant is active. */
  private void updateStopButton() {
    if (stopButton != null) {
      stopButton.setDisable(competition.stream().noneMatch(p -> p.getStatus() == Participation.Status.ACTIVE));
    }
  }

  /** Stops all that are active. */
  @FXML
  void handleStop() {
    var time = timeProvider.get();
    for (var participation : competition) {
      if (participation.getStatus() == Participation.Status.ACTIVE) {
        participation.stop(time);
      }
    }
    updateGrid();
    clearSelection();
  }
}
