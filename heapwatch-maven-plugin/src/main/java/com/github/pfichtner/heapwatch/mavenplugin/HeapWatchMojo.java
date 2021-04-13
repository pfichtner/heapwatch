package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.ValidationResults.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResults.oks;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static java.util.stream.Collectors.joining;
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

	@Parameter(name = "previousStats")
	public File previousStats;

	@Parameter(name = "updatePreviousFile")
	public boolean updatePreviousFile;

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

		Stats stats = stats(gclog);
		validate(validator, stats);

		if (this.updatePreviousFile || shouldCreate()) {
			updatePreviousFile(stats);
		}
	}

	private boolean shouldCreate() {
		return previousStats != null && !previousStats.exists();
	}

	private void updatePreviousFile(Stats stats) throws MojoExecutionException {
		try {
			JsonIO.write(previousStats, stats);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing previous stats " + previousStats, e);
		}
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

	public Stats getPrevious() throws MojoFailureException, MojoExecutionException {
		if (previousStats == null) {
			throw new MojoFailureException("previous stats not configured");
		}
		if (!previousStats.exists()) {
			return new Stats();
		}
		try {
			return JsonIO.read(previousStats);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading previous stats " + previousStats, e);
		}
	}

	private static double parse(String value) {
		try {
			return NumberFormat.getNumberInstance().parse(value).doubleValue();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
		return map == null ? Collections.emptyMap() : map;
	}

	private void validate(Validator validator, Stats stats) throws MojoFailureException {
		Log log = getLog();
		List<ValidationResult> validationResults = validator.validate(stats);
		messagesOf(oks(validationResults)).forEach(log::info);
		List<ValidationResult> errors = errors(validationResults);
		if (!errors.isEmpty()) {
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
