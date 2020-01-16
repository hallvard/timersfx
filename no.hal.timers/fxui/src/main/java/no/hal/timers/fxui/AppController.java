package no.hal.timers.fxui;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
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
import javafx.scene.layout.GridPane;
import no.hal.timers.core.Competition;
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
	private TextField timingKeyTextField;

	@FXML
	private ToggleButton editTimingKeysButton;

	@FXML
	private Button addTimingKeyButton;

	@FXML
	public void initialize() {
		if (timeLabel != null) {
			final Timeline timeline = new Timeline();
			timeline.setCycleCount(Timeline.INDEFINITE);
			timeline.getKeyFrames().add(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateTimeLabel()));
			timeline.playFromStart();
		}
		final CompetitionCsvReader reader = new CompetitionCsvReader();
		try {
			competition = reader.readCompetition(getClass().getResourceAsStream("sample2.csv"));
			updateGrid();
			clearSelection();
		} catch (final Exception e) {
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
		return (time != null ? formatTime(time.toHoursPart(), time.toMinutesPart(), time.toSecondsPart(), false) : "__:__");
	}

	private void updateTimeLabel() {
		timeLabel.setText(formatTime(timeProvider.get()));
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
		for (final Participation participation : selectParticipationButtons.keySet()) {
			setSelected(participation, newSelection.contains(participation));
		}
	}

	private void setSelected(final Participation participation, final boolean selected) {
		final CheckBox button = selectParticipationButtons.get(participation);
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
		final boolean selected = isSelected(participation);
		setSelected(participation, ! selected);
		System.out.println("Toggling selected for " + participation.getParticipant().getName());
		return ! selected;
	}

	private final Map<Action<Participation>, Button> participationActions = new HashMap<>();

	private void handleParticipationActionButtonsEnablement() {
		for (final Action<Participation> action : participationActions.keySet()) {
			boolean disabled = selection.isEmpty();
			for (final Participation participation : selection) {
				if (! action.isFor(participation)) {
					disabled = true;
					break;
				}
			}
			participationActions.get(action).setDisable(disabled);
		}
	}

	private List<TextField> timingKeyTextFields = null;

	private void updateGrid() {
		final List<Node> children = competitionGrid.getChildren();
		children.clear();
		// add headers
		int rowIndex = 0;
		int columnIndex = 0;

		addNode(() -> selectAllButton, rowIndex, columnIndex++, children);
		addNode(() -> participantLabel, rowIndex, columnIndex++, children);
		registerAction(addNode(() -> startButton, rowIndex, columnIndex++, children), new Action<Participation>() {
			@Override
			public boolean isFor(final Participation participation) {
				return participation.getStatus() == Participation.Status.START;
			}
			@Override
			public void doFor(final Participation participation) {
				participation.start(timeProvider.get());
			}
		});

		final List<String> timingKeys = competition.getTimingKeys();
		for (final String timingKey : timingKeys) {
			if (timingKeyTextFields != null) {
				final TextField textField = addNode(() -> new TextField(timingKey), rowIndex, columnIndex++, children);
				timingKeyTextFields.add(textField);
			} else {
				final Button timingKeyButton = addNode(() ->  new Button(timingKey), rowIndex, columnIndex++, children);
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
		addNode(() -> editTimingKeysButton, rowIndex, columnIndex++, children)

		.setSelected(timingKeyTextFields != null);
		addNode(() -> addTimingKeyButton, rowIndex, columnIndex++, children);
		rowIndex++;
		columnIndex = 0;
		final Iterator<Participation> participations = competition.participations();
		while (participations.hasNext()) {
			final Participation participation = participations.next();

			final CheckBox selectButton = addNode(() -> new CheckBox(), rowIndex, columnIndex++, children);
			selectButton.setOnAction(event -> {
				setSelected(participation, selectButton.isSelected());
				selectAllButton.setSelected(false);
			});
			selectParticipationButtons.put(participation, selectButton);

			addNode(() -> new Label(participation.getParticipant().getName()), rowIndex, columnIndex++, children)
			.setOnMouseClicked(event -> toggleSelected(participation));

			final LocalTime startTime = participation.getStartTime();
			addNode(() -> new Label(formatTime(startTime)), rowIndex, columnIndex++, children);

			Duration last = Duration.ZERO;
			final Iterator<String> timingKeysIt = timingKeys.iterator();
			while (timingKeysIt.hasNext()) {
				final String timingKey = timingKeysIt.next();
				final Optional<Duration> durationOpt = participation.getDuration(timingKey);
				String durationString = "__:__";
				if (durationOpt.isPresent()) {
					final Duration duration = durationOpt.get();
					final Duration labelDuration = (timingKeysIt.hasNext() ? duration.minus(last) : duration);
					durationString = formatTime(labelDuration);
					last = duration;
				}
				final String label = durationString;
				addNode(() -> new Label(label), rowIndex, columnIndex++, children);
			}
			rowIndex++;
			columnIndex = 0;
		}
	}

	@FXML
	public void handleEditTimingKeys() {
		if (timingKeyTextFields != null) {
			final List<String> newTimingKeys = getEditedTimingKeys(true);
			competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
			timingKeyTextFields = null;
		} else {
			timingKeyTextFields = new ArrayList<>();
		}
		updateGrid();
	}

	private List<String> getEditedTimingKeys(final boolean filterEmpty) {
		return timingKeyTextFields.stream()
				.map(TextField::getText)
				.filter(text -> (! filterEmpty) || text.trim().length() > 0)
				.collect(Collectors.toList());
	}

	@FXML
	public void handleAddTimingKey() {
		final List<String> newTimingKeys = (timingKeyTextFields != null ? getEditedTimingKeys(false) :	new ArrayList<>(competition.getTimingKeys()));
		newTimingKeys.add("");
		competition.setTimingKeys(newTimingKeys.toArray(new String[newTimingKeys.size()]));
		timingKeyTextFields = new ArrayList<>();
		updateGrid();
	}

	private void registerAction(final Button button, final Action<Participation> action) {
		participationActions.put(action, button);
		button.setOnAction(event -> {
			int count = 0;
			for (final Participation participation : selection) {
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

	private <T extends Node> T addNode(final Supplier<T> supplier, final int rowIndex, final int columnIndex, final Collection<Node> children) {
		final T node = supplier.get();
		GridPane.setConstraints(node, columnIndex, rowIndex);
		children.add(node);
		return node;
	}

	@FXML
	private void handleSelectAll() {
		setSelection(selectAllButton.isSelected() ? selectParticipationButtons.keySet() : Collections.emptyList());
	}
}
