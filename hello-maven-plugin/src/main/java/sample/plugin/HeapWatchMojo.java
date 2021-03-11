package sample.plugin;

import static foo.StatsReader.stats;
import static foo.acl.Stats.functionForAttribute;
import static java.util.stream.Collectors.joining;
import static org.apache.maven.plugins.annotations.LifecyclePhase.POST_INTEGRATION_TEST;
import static org.eclipselabs.garbagecat.util.Memory.fromOptionSize;

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
import org.eclipselabs.garbagecat.util.Memory;

import foo.Comparison;
import foo.Validator;
import foo.Validator.ValidationResult;

@Mojo(name = HeapWatchMojo.GOAL, defaultPhase = POST_INTEGRATION_TEST)
public class HeapWatchMojo extends AbstractMojo {

	public static final String GOAL = "touch";

	@Parameter(name = "gclog", required = true)
	public File gclog;

	@Parameter(name = "heapSpace")
	public Map<String, String> heapSpace;

	public void execute() throws MojoExecutionException {
		if (this.gclog == null) {
			throw new NullPointerException("gclog");
		}
		if (!this.gclog.exists()) {
			throw new IllegalStateException(gclog + " does not exist");
		}
		if (heapSpace == null || heapSpace.isEmpty()) {
			throw new IllegalStateException("no validation configured");
		}

		Validator validator = new Validator();
		heapSpace.entrySet().forEach(entry -> addValidation(validator, "heapSpace", entry));
		validate(validator);
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
		return fromOptionSize(value);
	}

	private static Comparison toComparison(String name) {
		return streamOf(Comparison.class).filter(e -> e.name().equalsIgnoreCase(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Invalid comparison " + name));
	}

	private static <T extends Enum<T>> Stream<T> streamOf(Class<T> clazz) {
		return EnumSet.allOf(clazz).stream();
	}

}
