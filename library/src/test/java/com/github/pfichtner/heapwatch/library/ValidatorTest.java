package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Comparison.LE;
import static com.github.pfichtner.heapwatch.library.Comparison.LT;
import static com.github.pfichtner.heapwatch.library.ValidationResults.errors;
import static com.github.pfichtner.heapwatch.library.ValidationResults.oks;
import static com.github.pfichtner.heapwatch.library.ValidatorTest.AbsoluteParameter.absolute;
import static com.github.pfichtner.heapwatch.library.ValidatorTest.RelativeParameter.relative;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_SPACE;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_SPACE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public class ValidatorTest {

	protected static class AbsoluteParameter {

		private final String name;
		private final Comparison comparison;
		private final Memory memory;

		protected static AbsoluteParameter absolute(String name, Comparison comparison, String value) {
			return new AbsoluteParameter(name, comparison, value);
		}

		private AbsoluteParameter(String name, Comparison comparison, String value) {
			this(name, comparison, memory(value));
		}

		private AbsoluteParameter(String name, Comparison comparison, Memory memory) {
			this.name = name;
			this.comparison = comparison;
			this.memory = memory;
		}

	}

	protected static class RelativeParameter {

		private final String name;
		private final Comparison comparison;
		private double mul;
		private Stats prev;

		protected static RelativeParameter relative(String name, Comparison comparison, double mul, Stats prev) {
			return new RelativeParameter(name, comparison, mul, prev);
		}

		private RelativeParameter(String name, Comparison comparison, double mul, Stats prev) {
			this.name = name;
			this.comparison = comparison;
			this.mul = mul;
			this.prev = prev;
		}

	}

	static final boolean OK = true;
	static final boolean NOT_OK = false;

	Memory maximumHeap;
	Stats stats = new Stats();
	Validator validator = new Validator();

	List<ValidationResult> validationResults;

	@Test
	void raisedIfMoreHeapSpaceIsUsedThanAllowed() {
		String name = "heapSpace";
		givenValidation(absolute(name, LE, "32K"));
		givenAnalyseHasHeapSpaceUsed("33K");
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withErrorMessage("Expected \"" + name + "\", not a value greater than <32K> but was <33K>");
	}

	@Test
	void doNotRaiseIfNotMoreHeapSpaceIsUsedThanAllowed() {
		givenValidation(absolute("heapSpace", LE, "32K"));
		givenAnalyseHasHeapSpaceUsed("32K");
		whenCheckIsDone();
		thenTheResultIs(OK);
		withNoErrorMessages();
		withOkMessage("\"heapSpace\", not a value greater than <32K> matched for value <32K>");
	}

	@Test
	void multipleViolations() {
		givenAlwaysFalseValidationWithMessage("A", "B", "C");
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withErrorMessages(e("A"), e("B"), e("C"));
	}

	@Test
	void allAttributes() {
		String anyMemSize = "32K";
		givenValidations( //
				absolute(HEAP_AFTER_GC, LT, anyMemSize), //
				absolute(HEAP_OCCUPANCY, LT, anyMemSize), //
				absolute(HEAP_SPACE, LT, anyMemSize), //
				absolute(METASPACE_AFTER_GC, LT, anyMemSize), //
				absolute(METASPACE_OCCUPANCY, LT, anyMemSize), //
				absolute(METASPACE_SPACE, LT, anyMemSize));

		stats.maxHeapAfterGC = memory(anyMemSize);
		stats.maxHeapOccupancy = memory(anyMemSize);
		stats.maxHeapSpace = memory(anyMemSize);
		stats.maxMetaspaceAfterGC = memory(anyMemSize);
		stats.maxMetaspaceOccupancy = memory(anyMemSize);
		stats.maxMetaspaceSpace = memory(anyMemSize);
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		assertThat(validationResults.size(), is(validator.getValidations()));
	}

	@Test
	void raisedIfMoreHeapSpaceIsUsedThanAllowedWithRelativeValue() {
		Stats prev = new Stats();
		prev.maxHeapSpace = memory("100B");
		givenValidation(relative("heapSpace", LT, 1.10, prev));
		givenAnalyseHasHeapSpaceUsed("110B");
		whenCheckIsDone();
		thenTheResultIs(NOT_OK);
		withErrorMessage("Expected \"heapSpace\", a value less than <110B> but <110B> was equal to <110B>");
	}

	@Test
	void doNotRaiseIfNotMoreHeapSpaceIsUsedThanAllowedWithRelativeValue() {
		Stats prev = new Stats();
		prev.maxHeapSpace = memory("100B");
		givenValidation(relative("heapSpace", LT, 1.10, prev));
		givenAnalyseHasHeapSpaceUsed("109B");
		whenCheckIsDone();
		withNoErrorMessages();
		withOkMessage("\"heapSpace\", a value less than <110B> matched for value <109B>");
	}

	private String e(String attribute) {
		return "Expected \"" + attribute + "\", not ANYTHING but was null";
	}

	private void givenValidation(AbsoluteParameter parameter) {
		givenValidations(parameter);
	}

	private void givenValidations(AbsoluteParameter... parameters) {
		for (AbsoluteParameter parameter : parameters) {
			validator.addValidation(parameter.name, parameter.comparison.name(), parameter.memory);
		}
	}

	private void givenValidation(RelativeParameter relativeParameter) {
		givenValidations(relativeParameter);
	}

	private void givenValidations(RelativeParameter... relativeParameters) {
		for (RelativeParameter parameter : relativeParameters) {
			validator.addValidation(parameter.name, parameter.comparison.name(), parameter.mul, parameter.prev);
		}
	}

	private void givenAnalyseHasHeapSpaceUsed(String memory) {
		stats.maxHeapSpace = memory(memory);
	}

	public void givenAlwaysFalseValidationWithMessage(String... messages) {
		for (String message : messages) {
			validator.addValidation(a -> null, not(anything()), message);
		}
	}

	private void whenCheckIsDone() {
		validationResults = validator.validate(stats);
	}

	private void thenTheResultIs(boolean expected) {
		assertThat(isOk(), is(expected));
	}

	private boolean isOk() {
		return errors(validationResults).isEmpty();
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
		return messages(errors(validationResults));
	}

	private void withOkMessage(String... expectedMessages) {
		assertThat(messages(oks(validationResults)), is(asList(expectedMessages)));
	}

	private List<String> messages(List<ValidationResult> results) {
		return results.stream().map(ValidationResult::getMessage).collect(toList());
	}

}
