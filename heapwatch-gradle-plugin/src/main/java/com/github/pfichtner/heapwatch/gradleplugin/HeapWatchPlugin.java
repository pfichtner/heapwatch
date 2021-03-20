package com.github.pfichtner.heapwatch.gradleplugin;

import static com.github.pfichtner.heapwatch.library.Comparison.valueOfIgnoreCase;
import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.ValidationResults.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResults.oks;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.functionForAttribute;
import static java.util.stream.Collectors.joining;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import com.github.pfichtner.heapwatch.library.ValidationResult;
import com.github.pfichtner.heapwatch.library.Validator;
import com.github.pfichtner.heapwatch.library.acl.Memory;

public class HeapWatchPlugin implements Plugin<Project> {

	private Logger logger;
	private HeapWatchPluginExtension extension;

	@Override
	public void apply(Project project) {
		project.task("verify").doLast(task -> exec(project));
	}

	private void exec(Project project) {
		logger = project.getLogger();
		extension = project.getExtensions().create("heapwatch",
				HeapWatchPluginExtension.class);

		if (extension.gclog == null) {
			throw new NullPointerException("gclog");
		}
		if (!extension.gclog.exists()) {
			throw new IllegalStateException(extension.gclog + " does not exist");
		}

		Validator validator = new Validator();
		nullSafe(extension.heapOccupancy).entrySet().forEach(entry -> addValidation(validator, "heapOccupancy", entry));
		nullSafe(extension.heapAfterGC).entrySet().forEach(entry -> addValidation(validator, "heapAfterGC", entry));
		nullSafe(extension.heapSpace).entrySet().forEach(entry -> addValidation(validator, "heapSpace", entry));
		nullSafe(extension.metaspaceOccupancy).entrySet()
				.forEach(entry -> addValidation(validator, "metaspaceOccupancy", entry));
		nullSafe(extension.metaspaceAfterGC).entrySet()
				.forEach(entry -> addValidation(validator, "metaspaceAfterGC", entry));
		nullSafe(extension.metaspaceSpace).entrySet()
				.forEach(entry -> addValidation(validator, "metaspaceSpace", entry));

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
		List<ValidationResult> validationResults = validator.validate(stats(extension.gclog));
		messagesOf(oks(validationResults)).forEach(logger::info);
		List<ValidationResult> errors = errors(validationResults);
		if (!errors.isEmpty()) {
			messagesOf(errors).forEach(logger::error);
			if (extension.breakBuildOnValidationError) {
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