package com.github.pfichtner.heapwatch.library;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;

import com.github.pfichtner.heapwatch.library.acl.Stats;

public class Validator {

	private final class FunctionBasedMatcher<T> extends TypeSafeMatcher<Stats> {

		private final String name;
		private final Matcher<T> matcher;
		private final Function<Stats, T> function;

		private FunctionBasedMatcher(String name, Matcher<T> matcher, Function<Stats, T> function) {
			this.name = name;
			this.matcher = matcher;
			this.function = function;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("\"").appendText(name).appendText("\", ");
			matcher.describeTo(description);
		}

		@Override
		protected boolean matchesSafely(Stats item) {
			return matcher.matches(function.apply(item));
		}
	}

	public static class ValidationResult {

		private final String errorMessage;

		private ValidationResult(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public static ValidationResult error(String errorMessage) {
			return new ValidationResult(errorMessage);
		}

	}

	private final List<FunctionBasedMatcher<?>> validations = new ArrayList<>();

	public <T> Validator addValidation(Function<Stats, T> function, Matcher<T> matcher, String name) {
		validations.add(new FunctionBasedMatcher<T>(name, matcher, function));
		return this;
	}

	public List<ValidationResult> validate(Stats stats) {
		return validations.stream().filter(m -> !m.matches(stats)).map(m -> ValidationResult.error(text(m, stats)))
				.collect(toList());
	}

	private String text(FunctionBasedMatcher<?> matcher, Stats stats) {
		Description description = new StringDescription().appendText("Expected ").appendDescriptionOf(matcher).appendText(" but ");
		matcher.matcher.describeMismatch(matcher.function.apply(stats), description);
		return description.toString();
	}

}
