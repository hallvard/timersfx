package no.hal.timers.fxui;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation;
import no.hal.timers.core.TimeProvider;
import no.hal.timers.csv.CompetitionCsvReader;

public class AppController {

	private Competition competition;

	@FXML
	private Label timeLabel;

	@FXML
	private GridPane competitionGrid;

	@FXML
	private CheckBox selectAllButton;

	@FXML
	private Label participantLabel;

	@FXML
	private Button startButton;

	@FXML
	private Button timeButton;

	@FXML
	private ToggleButton editTimingKeysButton;

	@FXML
	private Button addTimingKeyButton;

	@FXML
	private ToggleButton editParticipantNamesButton;

	@FXML
	private Button addParticipantButton;

	@FXML
	public void initialize() {
		if (timeLabel != null) {
			final var timeline = new Timeline();
			timeline.setCycleCount(Timeline.INDEFINITE);
			timeline.getKeyFrames().add(new KeyFrame(javafx.util.Duration.seconds(1), event -> handleTimeChanged()));
			timeline.playFromStart();
		}
		final CompetitionCsvReader reader = new CompetitionCsvReader();
		try {
			competition = reader.readCompetition(getClass().getResourceAsStream("sample2.csv"));
			updateGrid();
			clearSelection();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
		}
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
		timeCallbacks.clear();

		// add headers
		var rowIndex = 0;
		var columnIndex = 0;

		addNode(selectAllButton, rowIndex, columnIndex++, children);
		addNode(participantLabel, rowIndex, columnIndex++, children);
		registerAction(addNode(startButton, rowIndex, columnIndex++, children), startParticipationAction);

		final var timingKeys = competition.getTimingKeys();
		for (final var timingKey : timingKeys) {
			if (timingKeyTextFields != null) {
				final var textField = addClearableTextField(timingKey, rowIndex, columnIndex, children);
				timingKeyTextFields.add(textField);
			} else {
				final var timingKeyButton = addNode(new Button(), rowIndex, columnIndex++, children);
				copyLookNFeel(timeButton, timingKeyButton);
				timingKeyButton.setText(timingKey);
				registerAction(timingKeyButton, new Action<Participation>() {
					@Override
					public boolean isFor(final Participation participation) {
						return participation.getStatus() == Participation.Status.ACTIVE && (! participation.hasTime(timingKey));
					}
					@Override
					public void doFor(final Participation participation) {
						participation.time(timeProvider.get(), timingKey);
					}
				});
			}
		}
		addNode(editTimingKeysButton, rowIndex, columnIndex++, children)
		.setSelected(timingKeyTextFields != null);
		addNode(addTimingKeyButton, rowIndex, columnIndex++, children);
		rowIndex++;
		columnIndex = 0;
		final var participations = competition.participations();
		while (participations.hasNext()) {
			final var participation = participations.next();

			final var selectButton = addNode(new CheckBox(), rowIndex, columnIndex++, children);
			selectButton.setOnAction(event -> {
				setSelected(participation, selectButton.isSelected());
				selectAllButton.setSelected(false);
			});
			selectParticipationButtons.put(participation, selectButton);

			final String participantName = participation.getParticipant().getName();
			if (participantTextFields != null) {
				final var textField = addClearableTextField(participantName, rowIndex, columnIndex++, children);
				participantTextFields.add(textField);
			} else {
				addNode(new Label(participantName), rowIndex, columnIndex++, children);
			}

			if (participation.getStartTime().isPresent()) {
				final var durationLabel = addNode(new Label(formatTime((Duration) null)), rowIndex, columnIndex++, children);
				timeCallbacks.add(time -> {
					final var startTime = participation.getStartTime();
					final var durationString = formatTime(Duration.between(startTime.get(), participation.getCompetition().getCurrentTime()));
					durationLabel.setText(durationString);
					return null;
				});
			} else {
				final var startParticipationButton = addNode(new Button(), rowIndex, columnIndex++, children);
				copyLookNFeel(startButton, startParticipationButton);
				startParticipationButton.setOnAction(event -> {
					if (startParticipationAction.isFor(participation)) {
						startParticipationAction.doFor(participation);
						updateGrid();
					}
				});
			}
			var last = Duration.ZERO;
			final var timingKeysIt = timingKeys.iterator();
			while (timingKeysIt.hasNext()) {
				final var timingKey = timingKeysIt.next();
				final var durationOpt = participation.getDuration(timingKey);
				if (durationOpt.isPresent()) {
					final var duration = durationOpt.get();
					final var labelDuration = (timingKeysIt.hasNext() ? duration.minus(last) : duration);
					last = duration;
					addNode(new Label(formatTime(labelDuration)), rowIndex, columnIndex++, children);
				} else {
					final var timeParticipationButton = addNode(new Button(), rowIndex, columnIndex++, children);
					copyLookNFeel(timeButton, timeParticipationButton);
					timeParticipationButton.setText("");
					timeParticipationButton.setOnAction(event -> {
						if (participation.getStatus() == Participation.Status.ACTIVE && (! participation.hasTime(timingKey))) {
							participation.time(timeProvider.get(), timingKey);
							updateGrid();
						}
					});
				}
			}
			rowIndex++;
			columnIndex = 0;
		}
		addNode(editParticipantNamesButton, rowIndex++, 1, children)
		.setSelected(participantTextFields != null);
		addNode(addParticipantButton, rowIndex++, 1, children);
		rowIndex++;
		columnIndex = 0;
	}

	@FXML
	private void handleEditTimingKeys() {
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
	private void handleAddTimingKey() {
		final var newTimingKeys = (timingKeyTextFields != null ? getEditedText(timingKeyTextFields, false) :	new ArrayList<>(competition.getTimingKeys()));
		newTimingKeys.add("");
		competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
		timingKeyTextFields = new ArrayList<>();
		updateGrid();
	}

	@FXML
	private void handleEditParticipantNames() {
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
	private void handleAddParticipant() {
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
	private Button clearTextFieldButton;

	private TextField addClearableTextField(final String text, final int rowIndex, final int columnIndex, final Collection<Node> children) {
		final var textField = new TextField(text);
		final var clearButton = new Button();
		copyLookNFeel(clearTextFieldButton, clearButton);
		clearButton.setOnAction(event -> textField.clear());
		addNode(new HBox(textField, clearButton), rowIndex, columnIndex, children);
		return textField;
	}

	private <T extends Node> T addNode(final T node, final int rowIndex, final int columnIndex, final Collection<Node> children) {
		System.out.println("Adding " + node + " @ " + rowIndex + ", " + columnIndex);
		GridPane.setConstraints(node, columnIndex, rowIndex);
		children.add(node);
		return node;
	}

	private void copyLookNFeel(final Node source, final Node target) {
		target.setStyle(source.getStyle());
		target.getStyleClass().addAll(source.getStyleClass());
	}

	private void copyLookNFeel(final Button source, final Button target) {
		copyLookNFeel((Node) source, (Node) target);
		target.setText(source.getText());
		if (source.getGraphic() instanceof ImageView) {
			target.setGraphic(new ImageView(((ImageView) source.getGraphic()).getImage()));
		}
	}

	@FXML
	private void handleSelectAll() {
		setSelection(selectAllButton.isSelected() ? selectParticipationButtons.keySet() : Collections.emptyList());
	}
}
