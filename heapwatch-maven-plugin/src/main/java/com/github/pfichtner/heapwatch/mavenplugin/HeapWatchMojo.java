package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.ValidationResults.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResults.oks;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.pfichtner.heapwatch.library.ValidationResult;
import com.github.pfichtner.heapwatch.library.Validator;
import com.github.pfichtner.heapwatch.library.acl.Stats;

@Mojo(name = HeapWatchMojo.GOAL, defaultPhase = POST_INTEGRATION_TEST)
public class HeapWatchMojo extends AbstractMojo {

	public static class StatsIn {

		public File file;
		public boolean failIfMissing;

		public StatsIn() {
			super();
		}

		public StatsIn(File file, boolean failIfMissing) {
			this.file = file;
			this.failIfMissing = failIfMissing;
		}

	}

	public static class StatsOut {
		public File file;
		public boolean onSuccess = true;
		public boolean onFailure = true;

		public StatsOut() {
			super();
		}

		public StatsOut(File file, boolean onSuccess, boolean onFailure) {
			this.file = file;
			this.onSuccess = onSuccess;
			this.onFailure = onFailure;
		}

	}

	public static final String GOAL = "verify";

	@Parameter(name = "gclog", required = true)
	public File gclog;

	@Parameter(name = "heapOccupancy")
	public Map<String, String> heapOccupancy;
	@Parameter(name = "heapAfterGC")
	public Map<String, String> heapAfterGC;
	@Parameter(name = "heapSpace")
	public Map<String, String> heapSpace;
	@Parameter(name = "metaspaceOccupancy")
	public Map<String, String> metaspaceOccupancy;
	@Parameter(name = "metaspaceAfterGC")
	public Map<String, String> metaspaceAfterGC;
	@Parameter(name = "metaspaceSpace")
	public Map<String, String> metaspaceSpace;

	@Parameter(name = "breakBuildOnValidationError")
	public boolean breakBuildOnValidationError = true;

	@Parameter(name = "readStatsFrom")
	public StatsIn readStatsFrom;
	@Parameter(name = "writeStatsTo")
	public List<StatsOut> writeStatsTo;

	public void execute() throws MojoFailureException, MojoExecutionException {
		if (this.gclog == null) {
			throw new MojoFailureException("gclog must not be null");
		}
		if (!this.gclog.exists()) {
			throw new MojoFailureException(gclog + " does not exist");
		}

		Validator validator = new Validator();
		add(validator, heapOccupancy, "heapOccupancy");
		add(validator, heapAfterGC, "heapAfterGC");
		add(validator, heapSpace, "heapSpace");
		add(validator, metaspaceOccupancy, "metaspaceOccupancy");
		add(validator, metaspaceAfterGC, "metaspaceAfterGC");
		add(validator, metaspaceSpace, "metaspaceSpace");

		if (validator.getValidations() == 0) {
			throw new MojoFailureException("no validation configured");
		}

		validate(validator);
	}

	private void add(Validator validator, Map<String, String> map, String name)
			throws MojoFailureException, MojoExecutionException {
		Set<Entry<String, String>> entrySet = nullSafe(map).entrySet();
		for (Entry<String, String> entry : entrySet) {
			String value = entry.getValue().trim();
			if (value.endsWith("%")) {
				validator.addValidation(name, entry.getKey(), (100.0 + parse(value)) / 100, getPrevious());
			} else {
				validator.addValidation(name, entry.getKey(), memory(value));
			}
		}
	}

	private Stats getPrevious() throws MojoFailureException, MojoExecutionException {
		if (readStatsFrom == null || readStatsFrom.file == null) {
			throw new MojoFailureException("previous stats not configured");
		} else if (readStatsFrom.file.exists()) {
			return readStatsFrom(readStatsFrom.file);
		} else if (readStatsFrom.failIfMissing) {
			throw new MojoFailureException("previous stats " + readStatsFrom.file + " not found");
		} else {
			return new Stats();
		}
	}

	private static Stats readStatsFrom(File file) throws MojoExecutionException {
		try {
			return JsonIO.read(file);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading stats from " + file, e);
		}
	}

	private void writeStatsTo(Stats stats, Predicate<StatsOut> predicate) throws MojoExecutionException {
		writeStatsTo(nullSafe(writeStatsTo).stream().filter(predicate).map(w -> w.file).collect(toList()), stats);
	}

	private void writeStatsTo(List<File> files, Stats stats) throws MojoExecutionException {
		for (File file : files) {
			writeStatsTo(file, stats);
		}
	}

	private static void writeStatsTo(File file, Stats stats) throws MojoExecutionException {
		try {
			JsonIO.write(file, stats);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing stats to " + file, e);
		}
	}

	private static double parse(String value) {
		try {
			return NumberFormat.getNumberInstance().parse(value).doubleValue();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> List<T> nullSafe(List<T> list) {
		return list == null ? Collections.emptyList() : list;
	}

	private static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
		return map == null ? Collections.emptyMap() : map;
	}

	private void validate(Validator validator) throws MojoFailureException, MojoExecutionException {
		Stats stats = stats(gclog);
		Log log = getLog();
		List<ValidationResult> validationResults = validator.validate(stats);
		messagesOf(oks(validationResults)).forEach(log::info);
		List<ValidationResult> errors = errors(validationResults);
		boolean ok = errors.isEmpty();
		// TODO move statfile logic into library (decorator pattern)?
		writeStatsTo(stats, ok ? w -> w.onSuccess : w -> w.onFailure);
		if (!ok) {
			messagesOf(errors).forEach(log::error);
			if (breakBuildOnValidationError) {
				throw new MojoFailureException(messagesOf(errors).collect(joining(", ")));
			}
		}
	}

	private Stream<String> messagesOf(List<ValidationResult> results) {
		return results.stream().map(ValidationResult::getMessage);
	}

}
