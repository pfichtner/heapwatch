package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Comparison.LE;
import static com.github.pfichtner.heapwatch.library.Comparison.LT;
import static com.github.pfichtner.heapwatch.library.ValidationResult.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResult.oks;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_SPACE;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_SPACE;
import static com.github.pfichtner.heapwatch.library.acl.Stats.functionForAttribute;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public class ValidatorTest {

	public static class Parameter {

		private final String name;
		private final Object value;
		private final Comparison comparison;

		public Parameter(String name, Comparison comparison, Object value) {
			this.name = name;
			this.value = value;
			this.comparison = comparison;
		}

		public Matcher<Memory> matcher() {
			return comparison.matcher(memory(String.valueOf(value)));
		}
	}

	static final boolean OK = true;
	static final boolean NOT_OK = false;

	List<Parameter> parameters = new ArrayList<>();

	Memory maximumHeap;
	Stats stats = new Stats();
	Validator validator = new Validator();

	List<ValidationResult> validations;

	@Test
	void raisedIfMoreHeapSpaceIsUsedThanAllowed() {
		String name = "heapSpace";
		givenValidation(new Parameter(name, LE, "32K"));
		givenAnalyseHasHeapSpaceUsed("33K");
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withErrorMessage("Expected \"" + name + "\", not a value greater than <32K> but was <33K>");
	}

	@Test
	void doNotRaiseIfNotMoreHeapSpaceIsUsedThanAllowed() {
		givenValidation(new Parameter("heapSpace", LE, "32K"));
		givenAnalyseHasHeapSpaceUsed("32K");
		whenCheckIsDone();
		thenTheResultIs(OK);
		withNoErrorMessages();
		withOkMessage("\"heapSpace\", not a value greater than <32K> matched for value <32K>");
	}

	@Test
	void multipleViolations() {
		String[] messages = new String[] { "A", "B", "C" };
		givenAlwaysFalseValidationWithMessage(messages);
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withErrorMessages(e("A"), e("B"), e("C"));
	}

	@Test
	void allAttributes() {
		String anyMemSize = "32K";
		givenValidations( //
				new Parameter(HEAP_AFTER_GC, LT, anyMemSize), //
				new Parameter(HEAP_OCCUPANCY, LT, anyMemSize), //
				new Parameter(HEAP_SPACE, LT, anyMemSize), //
				new Parameter(METASPACE_AFTER_GC, LT, anyMemSize), //
				new Parameter(METASPACE_OCCUPANCY, LT, anyMemSize), //
				new Parameter(METASPACE_SPACE, LT, anyMemSize) //
		);

		stats.maxHeapAfterGC = memory(anyMemSize);
		stats.maxHeapOccupancy = memory(anyMemSize);
		stats.maxHeapSpace = memory(anyMemSize);
		stats.maxMetaspaceAfterGC = memory(anyMemSize);
		stats.maxMetaspaceOccupancy = memory(anyMemSize);
		stats.maxMetaspaceSpace = memory(anyMemSize);
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		assertThat(validations.size(), is(validator.getValidations()));
	}

	private String e(String attribute) {
		return "Expected \"" + attribute + "\", not ANYTHING but was null";
	}

	private void givenValidation(Parameter parameter) {
		givenValidations(parameter);
	}

	private void givenValidations(Parameter... parameters) {
		for (Parameter parameter : parameters) {
			Function<Stats, Memory> function = functionForAttribute(parameter.name);
			if (function != null) {
				validator.addValidation(function, parameter.matcher(), parameter.name);
			}
		}
	}

	private void givenAnalyseHasHeapSpaceUsed(String memory) {
		stats.maxHeapSpace = memory(memory);
	}

	public void givenAlwaysFalseValidationWithMessage(String[] messages) {
		for (String message : messages) {
			validator.addValidation(a -> null, not(anything()), message);
		}
	}

	private void whenCheckIsDone() {
		validations = validator.validate(stats);
	}

	private void thenTheResultIs(boolean expected) {
		assertThat(isOk(), is(expected));
	}

	private boolean isOk() {
		return errors(validations).isEmpty();
	}

	private void withNoErrorMessages() {
		withErrorMessages();
	}

	private void withErrorMessage(String expected) {
		withErrorMessages(expected);
	}

	private void withErrorMessages(String... expectedMessages) {
		assertThat(errorMessages(), is(asList(expectedMessages)));
	}

	private List<String> errorMessages() {
		return messages(errors(validations));
	}

	private void withOkMessage(String... expectedMessages) {
		assertThat(messages(oks(validations)), is(asList(expectedMessages)));
	}

	private List<String> messages(List<ValidationResult> results) {
		return results.stream().map(ValidationResult::getMessage).collect(toList());
	}

}
