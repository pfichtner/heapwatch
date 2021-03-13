package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.functionForAttribute;
import static java.util.stream.Collectors.joining;
import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.pfichtner.heapwatch.library.Comparison;
import com.github.pfichtner.heapwatch.library.Validator;
import com.github.pfichtner.heapwatch.library.Validator.ValidationResult;
import com.github.pfichtner.heapwatch.library.acl.Memory;

@Mojo(name = HeapWatchMojo.GOAL, defaultPhase = POST_INTEGRATION_TEST)
public class HeapWatchMojo extends AbstractMojo {

	public static final String GOAL = "touch";

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

	public void execute() throws MojoExecutionException {
		if (this.gclog == null) {
			throw new NullPointerException("gclog");
		}
		if (!this.gclog.exists()) {
			throw new IllegalStateException(gclog + " does not exist");
		}

		if (nullOrEmpty(heapOccupancy) && //
				nullOrEmpty(heapAfterGC) && //
				nullOrEmpty(heapSpace) && //
				nullOrEmpty(metaspaceOccupancy) && //
				nullOrEmpty(metaspaceAfterGC) && //
				nullOrEmpty(metaspaceSpace)) {
			throw new IllegalStateException("no validation configured");
		}

		Validator validator = new Validator();
		heapOccupancy.entrySet().forEach(entry -> addValidation(validator, "heapOccupancy", entry));
		heapAfterGC.entrySet().forEach(entry -> addValidation(validator, "heapAfterGC", entry));
		heapSpace.entrySet().forEach(entry -> addValidation(validator, "heapSpace", entry));
		metaspaceOccupancy.entrySet().forEach(entry -> addValidation(validator, "metaspaceOccupancy", entry));
		metaspaceAfterGC.entrySet().forEach(entry -> addValidation(validator, "metaspaceAfterGC", entry));
		metaspaceSpace.entrySet().forEach(entry -> addValidation(validator, "metaspaceSpace", entry));
		validate(validator);
	}

	private static boolean nullOrEmpty(Map<String, String> map) {
		return map == null || map.isEmpty();
	}

	private static void addValidation(Validator validator, String name, Entry<String, String> entry) {
		validator.addValidation( //
				functionForAttribute(name), //
				toComparison(entry.getKey()).matcher(toMemory(entry.getValue())), //
				name //
		);
	}

	private void validate(Validator validator) {
		List<ValidationResult> validationResults = validator.validate(stats(gclog));
		if (!validationResults.isEmpty()) {
			throw new RuntimeException(validationResults.stream().map(r -> r.getErrorMessage()).collect(joining(", ")));
		}
	}

	private static Memory toMemory(String value) {
		return memory(value);
	}

	private static Comparison toComparison(String name) {
		return streamOf(Comparison.class).filter(e -> e.name().equalsIgnoreCase(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Invalid comparison " + name));
	}

	private static <T extends Enum<T>> Stream<T> streamOf(Class<T> clazz) {
		return EnumSet.allOf(clazz).stream();
	}

}
