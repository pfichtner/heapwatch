package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Comparison.LE;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
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

import com.github.pfichtner.heapwatch.library.Validator.ValidationResult;
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
		withMessage("Expected \"" + name + "\", not a value greater than <32K> but was <33K>");
	}

	@Test
	void doNotRaiseIfNotMoreHeapSpaceIsUsedThanAllowed() {
		givenValidation(new Parameter("heapSpace", LE, "32K"));
		givenAnalyseHasHeapSpaceUsed("32K");
		whenCheckIsDone();
		thenTheResultIs(OK);
		withNoMessages();
	}

	@Test
	void multipleViolations() {
		String[] messages = new String[] { "A", "B", "C" };
		givenAlwaysFalseValidationWithMessage(messages);
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withMessages(e("A"), e("B"), e("C"));
	}

	private String e(String attribute) {
		return "Expected \"" + attribute + "\", not ANYTHING but was null";
	}

	private void givenValidation(Parameter... parameters) {
		for (Parameter parameter : parameters) {
			Function<Stats, Memory> function = Stats.functionForAttribute(parameter.name);
			if (function != null) {
				validator.addValidation(function, parameter.matcher(), parameter.name);
			}
		}
	}

	private void givenAnalyseHasHeapSpaceUsed(String memory) {
		stats.setMaxHeapSpace(memory(memory));
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
		return validations.isEmpty();
	}

	private void withNoMessages() {
		withMessages();
	}

	private void withMessage(String expected) {
		withMessages(expected);
	}

	private void withMessages(String... expectedMessages) {
		assertThat(errorMessages(), is(asList(expectedMessages)));
	}

	public List<String> errorMessages() {
		return validations.stream().map(ValidationResult::getErrorMessage).collect(toList());
	}

}
