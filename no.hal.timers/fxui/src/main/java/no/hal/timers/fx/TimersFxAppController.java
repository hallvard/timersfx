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
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation;
import no.hal.timers.core.TimeProvider;

public class TimersFxAppController {

  private Competition competition;

  @FXML
  Label timeLabel;

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

  @FXML
  Node timingKeysControls;

  @FXML
  ToggleButton editTimingKeysButton;

  @FXML
  ToggleButton editParticipantNamesButton;

  private final String participantNameFormat = "Løper %s";
  private final String timingKeyFormat = "%s. runde";

  @FXML
  void initialize() {
    if (timeLabel != null) {
      final var timeline = new Timeline();
      timeline.setCycleCount(Timeline.INDEFINITE);
      timeline.getKeyFrames().add(new KeyFrame(javafx.util.Duration.seconds(1), event -> handleTimeChanged()));
      timeline.playFromStart();
    }
    competition = new Competition();
    final String[] timingKeys = new String[4];
    for (var i = 0; i < timingKeys.length - 1; i++) {
      timingKeys[i] = String.format(timingKeyFormat, i + 1);
    }
    timingKeys[timingKeys.length - 1] = "Mål";
    competition.setTimingKeys(timingKeys);
    for (var i = 0; i < 5; i++) {
      competition.addParticipant(new Participant(String.format(participantNameFormat, i + 1)));
    }
    updateGrid();
    clearSelection();
  }

  private final TimeProvider timeProvider = () -> LocalTime.now();

  private String formatTime(final int hour, final int min, final int sec, final boolean ignoreZeroHour) {
    if (ignoreZeroHour && hour == 0) {
      return String.format("%02d:%02d", min, sec);
    } else {
      return String.format("%02d:%02d:%02d", hour, min, sec);
    }
  }

  private String formatTime(final LocalTime time) {
    return (time != null ? formatTime(time.getHour(), time.getMinute(), time.getSecond(), false) : "__:__:__");
  }

  private String formatTime(final Duration time) {
    return (time != null ? formatTime(time.toHoursPart(), time.toMinutesPart(), time.toSecondsPart(), true) : "__:__");
  }

  private final Collection<Callback<LocalTime, Void>> timeCallbacks = new ArrayList<>();

  private void handleTimeChanged() {
    final LocalTime time = timeProvider.get();
    timeLabel.setText(formatTime(time));
    for (final Callback<LocalTime, Void> timeCallback : timeCallbacks) {
      timeCallback.call(time);
    }
  }

  private final Collection<Participation> selection = new ArrayList<>();

  private final Map<Participation, CheckBox> selectParticipationButtons = new HashMap<>();

  private void clearSelection() {
    selectAllButton.setSelected(false);
    selection.clear();
    for (final Participation participation : selectParticipationButtons.keySet() ) {
      setSelected(participation, false);
    }
  }

  private void setSelection(final Collection<Participation> newSelection) {
    selection.clear();
    selection.addAll(newSelection);
    for (final var participation : selectParticipationButtons.keySet()) {
      setSelected(participation, newSelection.contains(participation));
    }
  }

  private void setSelected(final Participation participation, final boolean selected) {
    final var button = selectParticipationButtons.get(participation);
    if (button != null && button.isSelected() != selected) {
      button.setSelected(selected);
    }
    if (selected == selection.contains(participation));
    else if (selected) {
      selection.add(participation);
    } else {
      selection.remove(participation);
    }
    handleParticipationActionButtonsEnablement();
  }

  private boolean isSelected(final Participation participation) {
    return selection.contains(participation);
  }

  private boolean toggleSelected(final Participation participation) {
    final var selected = isSelected(participation);
    setSelected(participation, ! selected);
    return ! selected;
  }

  private final Map<Action<Participation>, Button> participationActions = new HashMap<>();

  private void handleParticipationActionButtonsEnablement() {
    for (final var action : participationActions.keySet()) {
      var disabled = selection.isEmpty();
      for (final var participation : selection) {
        if (! action.isFor(participation)) {
          disabled = true;
          break;
        }
      }
      participationActions.get(action).setDisable(disabled);
    }
  }

  private List<TextField> timingKeyTextFields = null;
  private List<TextField> participantTextFields = null;

  private final Action<Participation> startParticipationAction = new Action<Participation>() {
    @Override
    public boolean isFor(final Participation participation) {
      return participation.getStatus() == Participation.Status.START;
    }
    @Override
    public void doFor(final Participation participation) {
      participation.start(timeProvider.get());
    }
  };

  private void updateGrid() {
    final var children = competitionGrid.getChildren();
    children.clear();
    final var columnConstraints = competitionGrid.getColumnConstraints();
    columnConstraints.clear();

    timeCallbacks.clear();

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

    registerAction(addNode(startButton, rowIndex, columnIndex++, children), startParticipationAction);
    columnConstraints.add(new ColumnConstraints(60, Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE,
        Priority.NEVER, HPos.CENTER, false));

    final var timingKeys = competition.getTimingKeys();
    for (final var timingKey : timingKeys) {
      addTimingKeyColumnHeader(timingKey, rowIndex, columnIndex++, children);
      columnConstraints.add(new ColumnConstraints(60, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE,
          Priority.SOMETIMES, HPos.CENTER, false));
    }
    addNode(timingKeysControls, rowIndex, columnIndex++, children);
    columnConstraints.add(new ColumnConstraints(50, 50, 50, Priority.NEVER, HPos.CENTER, true));
    editTimingKeysButton.setSelected(timingKeyTextFields != null);

    rowIndex++;
    columnIndex = 0;
    final var participations = competition.participations();
    while (participations.hasNext()) {
      final var participation = participations.next();

      addParticipantCell(participation, rowIndex, columnIndex++, children);
      addSelectParticipantCell(participation, rowIndex, columnIndex++, children);
      addParticipantStartCell(participation, rowIndex, columnIndex++, children);

      var last = Duration.ZERO;
      final var timingKeysIt = timingKeys.iterator();
      while (timingKeysIt.hasNext()) {
        final var timingKey = timingKeysIt.next();
        final boolean isLast = timingKeysIt.hasNext();
        addParticipantTimingCell(participation, timingKey, rowIndex, columnIndex++,
            last, isLast, children);
        final var durationOpt = participation.getDuration(timingKey);
        if (durationOpt.isPresent()) {
          last = durationOpt.get();
        }
      }
      rowIndex++;
      columnIndex = 0;
    }
  }

  private int addTimingKeyColumnHeader(final String timingKey,
      final int rowIndex, final int columnIndex,
      final ObservableList<Node> children) {
    if (timingKeyTextFields != null) {
      final var textField = addClearableTextField(timingKey, rowIndex, columnIndex, children);
      timingKeyTextFields.add(textField);
    } else {
      final var timingKeyButton = addNode(new Button(), rowIndex, columnIndex, children);
      copyLookAndFeel(timeButton, timingKeyButton, true);
      timingKeyButton.setText(timingKey);
      registerAction(timingKeyButton, new Action<Participation>() {
        @Override
        public boolean isFor(final Participation participation) {
          return participation.getStatus() == Participation.Status.ACTIVE
              && (! participation.hasTime(timingKey));
        }
        @Override
        public void doFor(final Participation participation) {
          participation.time(timeProvider.get(), timingKey);
        }
      });
    }
    return columnIndex;
  }

  private Optional<Duration> addParticipantTimingCell(final Participation participation,
      final String timingKey, final int rowIndex,
      final int columnIndex, final Duration lastDuration, final boolean isLast,
      final ObservableList<Node> children) {
    final var durationOpt = participation.getDuration(timingKey);
    if (durationOpt.isPresent()) {
      final var duration = durationOpt.get();
      final var labelDuration = (isLast ? duration.minus(lastDuration) : duration);
      addNode(new Label(formatTime(labelDuration)), rowIndex, columnIndex, children);
    } else {
      final var timeParticipationButton = addNode(new Button(), rowIndex, columnIndex, children);
      copyLookAndFeel(timeButton, timeParticipationButton, false);
      timeParticipationButton.setText("");
      timeParticipationButton.setOnAction(event -> {
        if (participation.getStatus() == Participation.Status.ACTIVE
            && (! participation.hasTime(timingKey))) {
          participation.time(timeProvider.get(), timingKey);
          updateGrid();
        }
      });
    }
    return durationOpt;
  }

  private int addParticipantStartCell(final Participation participation,
      final int rowIndex, final int columnIndex,
      final ObservableList<Node> children) {
    if (participation.getStartTime().isPresent()) {
      final var durationLabel = addNode(new Label(formatDuration(participation)),
          rowIndex, columnIndex, children);
      timeCallbacks.add(time -> {
        durationLabel.setText(formatDuration(participation));
        return null;
      });
    } else {
      final var startParticipationButton = addNode(new Button(), rowIndex, columnIndex, children);
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

  private String formatDuration(final Participation participation) {
    final Optional<LocalTime> startTime = participation.getStartTime();
    return formatTime(Duration.between(startTime.get(), participation.getCompetition().getCurrentTime()));
  }

  private int addSelectParticipantCell(final Participation participation,
      final int rowIndex, final int columnIndex,
      final ObservableList<Node> children) {
    final var selectButton = addNode(new CheckBox(), rowIndex, columnIndex, children);
    selectButton.setOnAction(event -> {
      setSelected(participation, selectButton.isSelected());
      selectAllButton.setSelected(false);
    });
    selectParticipationButtons.put(participation, selectButton);
    return columnIndex;
  }

  private int addParticipantCell(final Participation participation,
      final int rowIndex, final int columnIndex,
      final ObservableList<Node> children) {
    final String participantName = participation.getParticipant().getName();
    if (participantTextFields != null) {
      final var textField = addClearableTextField(participantName, rowIndex, columnIndex, children);
      participantTextFields.add(textField);
    } else {
      final Pane pane = addNode(new Pane(), rowIndex, columnIndex, children);
      pane.getStyleClass().add(rowIndex % 2 == 0 ? "even-row" : "odd-row");
      pane.getChildren().add(new Label(participantName));
      //      addNode(new Label(participantName), rowIndex, columnIndex, children);
    }
    return columnIndex;
  }

  @FXML
  void handleEditTimingKeys() {
    if (timingKeyTextFields != null) {
      final var newTimingKeys = getEditedText(timingKeyTextFields, true);
      competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
      timingKeyTextFields = null;
    } else {
      timingKeyTextFields = new ArrayList<>();
    }
    updateGrid();
  }

  private List<String> getEditedText(final Collection<TextField> textFields, final boolean filterEmpty) {
    return textFields.stream()
        .map(TextField::getText)
        .filter(text -> (! filterEmpty) || text.trim().length() > 0)
        .collect(Collectors.toList());
  }

  @FXML
  void handleAddTimingKey() {
    final var newTimingKeys = (timingKeyTextFields != null
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
      final var newParticipantNames = getEditedText(participantTextFields, false);
      int participantNum = 0;
      for (final String name : newParticipantNames) {
        final var participant = competition.getParticipation(participantNum).getParticipant();
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

  private void registerAction(final Button button, final Action<Participation> action) {
    participationActions.put(action, button);
    button.setOnAction(event -> {
      var count = 0;
      for (final var participation : selection) {
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

  private TextField addClearableTextField(final String text, final int rowIndex, final int columnIndex,
      final Collection<Node> children) {
    final var textField = new TextField(text);
    final var clearButton = new Button();
    copyLookAndFeel(clearTextFieldButton, clearButton, true);
    clearButton.setOnAction(event -> textField.clear());
    final HBox box = addNode(new HBox(textField, clearButton), rowIndex, columnIndex, children);
    box.setAlignment(Pos.CENTER);
    return textField;
  }

  private <T extends Node> T addNode(final T node, final int rowIndex, final int columnIndex,
      final Collection<Node> children) {
    //    System.out.println("Adding " + node + " @ " + rowIndex + ", " + columnIndex);
    GridPane.setConstraints(node, columnIndex, rowIndex);
    children.add(node);
    return node;
  }

  private void copyLookAndFeel(final Node source, final Node target) {
    target.setStyle(source.getStyle());
    target.getStyleClass().addAll(source.getStyleClass());
  }

  private void copyLookAndFeel(final Button source, final Button target,
      final boolean includeScaling) {
    copyLookAndFeel(source, target);
    target.setText(source.getText());
    if (source.getGraphic() instanceof ImageView) {
      final ImageView sourceImageView = (ImageView) source.getGraphic();
      final ImageView targeImageView = new ImageView(sourceImageView.getImage());
      target.setGraphic(targeImageView);
      if (includeScaling) {
        targeImageView.setScaleX(sourceImageView.getScaleX());
        targeImageView.setScaleY(sourceImageView.getScaleY());
      }
    }
  }

  @FXML
  void handleSelectAll() {
    setSelection(selectAllButton.isSelected()
        ? selectParticipationButtons.keySet()
        : Collections.emptyList());
  }
}
