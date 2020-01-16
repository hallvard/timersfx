package no.hal.timers.csv;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation;

public class CompetitionCsvReader {

	private final boolean columnsAreTotals = false;
	private final boolean lastColumnIsTotal = true;
	private final boolean emptyColumnsAreTime = true;

	public Competition readCompetition(final InputStream input) throws Exception {
		final CsvParserSettings settings = new CsvParserSettings();
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		final CsvParser parser = new CsvParser(settings);

		// parses all rows in one go.
		final List<String[]> allRows = parser.parseAll(new InputStreamReader(input, "UTF-8"));
		final List<String> timingKeys = new ArrayList<String>();
		final String[] headers = allRows.get(0);

		// guess which headers are timing keys, by looking at first line with value
		for (int colNum = 1; colNum < headers.length; colNum++) {
			final String header = headers[colNum];
			boolean allEmpty = true;
			for (int rowNum = 1; rowNum < allRows.size(); rowNum++) {
				final String[] row = allRows.get(rowNum);
				if (colNum < row.length) {
					final String value = row[colNum].trim();
					if (value.length() > 0) {
						allEmpty = false;
					}
					if (isTiming(value)) {
						timingKeys.add(header);
						break;
					}
				}
			}
			if (emptyColumnsAreTime && allEmpty) {
				timingKeys.add(header);
			}
		}
		final Competition comp = new Competition();
		comp.setTimingKeys(timingKeys.toArray(new String[timingKeys.size()]));

		for (int rowNum = 1; rowNum < allRows.size(); rowNum++) {
			final String[] row = allRows.get(rowNum);
			final String name = row[0].trim();
			final Participant participant = new Participant(name);
			final Participation participation = comp.addParticipant(participant);
			Duration total = Duration.ZERO;
			for (int colNum = 1; colNum < headers.length; colNum++) {
				final String header = headers[colNum];
				if (timingKeys.contains(header)) {
					if (colNum < row.length) {
						final int nLast = headers.length - colNum - 1;
						final Duration duration = parseDuration(row[colNum].trim());
						if (duration != null) {
							total = total.plus(duration);
							final Duration time = (columnsAreTotals || (nLast == 0 && lastColumnIsTotal) ? duration : total);
							final String timingKey = (nLast == 0 ? Participation.Status.FINISH.name() : header);
							participation.time(time, timingKey);
						}
					}
				}
			}
		}
		return comp;
	}

	private boolean isTiming(final String value) {
		if (parseDuration(value) != null) {
			return true;
		}
		return false;
	}

	Duration parseDuration(final String value) {
		final String[] split = value.split(":");
		final int[] times = new int[split.length];
		for (int i = 0; i < split.length; i++) {
			try {
				times[i] = Integer.valueOf(split[i]);
			} catch (final NumberFormatException e) {
				return null;
			}
		}
		final int[] durations = { 60, 60, 1 };
		long seconds = 0, duration = 1;
		for (int i = 0; i < times.length; i++) {
			duration *= durations[durations.length - 1 - i];
			seconds += times[times.length - 1 - i] * duration;
		}
		return Duration.of(seconds, ChronoUnit.SECONDS);
	}
}
