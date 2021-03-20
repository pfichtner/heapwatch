package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.Comparison.valueOfIgnoreCase;
import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.ValidationResults.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResults.oks;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.functionForAttribute;
import static java.util.stream.Collectors.joining;
import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.pfichtner.heapwatch.library.ValidationResult;
import com.github.pfichtner.heapwatch.library.Validator;
import com.github.pfichtner.heapwatch.library.acl.Memory;

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

	public void execute() throws MojoExecutionException {
		if (gclog == null) {
			throw new NullPointerException("gclog");
		}
		if (!gclog.exists()) {
			throw new IllegalStateException(gclog + " does not exist");
		}

		Validator validator = new Validator();
		nullSafe(heapOccupancy).entrySet().forEach(entry -> addValidation(validator, "heapOccupancy", entry));
		nullSafe(heapAfterGC).entrySet().forEach(entry -> addValidation(validator, "heapAfterGC", entry));
		nullSafe(heapSpace).entrySet().forEach(entry -> addValidation(validator, "heapSpace", entry));
		nullSafe(metaspaceOccupancy).entrySet().forEach(entry -> addValidation(validator, "metaspaceOccupancy", entry));
		nullSafe(metaspaceAfterGC).entrySet().forEach(entry -> addValidation(validator, "metaspaceAfterGC", entry));
		nullSafe(metaspaceSpace).entrySet().forEach(entry -> addValidation(validator, "metaspaceSpace", entry));

		if (validator.getValidations() == 0) {
			throw new IllegalStateException("no validation configured");
		}

		validate(validator);
	}

	private static <K, V> Map<K, V> nullSafe(Map<K, V> map) {
		return map == null ? Collections.emptyMap() : map;
	}

	private static void addValidation(Validator validator, String name, Entry<String, String> entry) {
		validator.addValidation( //
				functionForAttribute(name), //
				valueOfIgnoreCase(entry.getKey()).matcher(toMemory(entry.getValue())), //
				name //
		);
	}

	private void validate(Validator validator) {
		List<ValidationResult> validationResults = validator.validate(stats(gclog));
		messagesOf(oks(validationResults)).forEach(getLog()::info);
		List<ValidationResult> errors = errors(validationResults);
		if (!errors.isEmpty()) {
			messagesOf(errors).forEach(getLog()::error);
			if (breakBuildOnValidationError) {
				throw new RuntimeException(messagesOf(errors).collect(joining(", ")));
			}
		}
	}

	private Stream<String> messagesOf(List<ValidationResult> results) {
		return results.stream().map(ValidationResult::getMessage);
	}

	private static Memory toMemory(String value) {
		return memory(value);
	}

}
