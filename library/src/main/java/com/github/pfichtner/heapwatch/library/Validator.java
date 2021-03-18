package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Validator.ValidationResult.error;
import static com.github.pfichtner.heapwatch.library.Validator.ValidationResult.ok;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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

	public abstract static class ValidationResult {

		private static class ValidationSuccess extends ValidationResult {

			private String message;

			public ValidationSuccess(FunctionBasedMatcher<?> matcher, Stats stats) {
				this.message = text(matcher, stats);
			}

			private static String text(FunctionBasedMatcher<?> matcher, Stats stats) {
				return new StringDescription() //
						.appendDescriptionOf(matcher) //
						.appendText(" matched for value ") //
						.appendValue(matcher.function.apply(stats)) //
						.toString();
			}

			@Override
			public boolean isError() {
				return false;
			}

			@Override
			public String getMessage() {
				return this.message;
			}

		}

		private static class ValidationError extends ValidationResult {

			private final String errorMessage;

			public ValidationError(FunctionBasedMatcher<?> matcher, Stats stats) {
				this.errorMessage = text(matcher, stats);
			}

			private static String text(FunctionBasedMatcher<?> matcher, Stats stats) {
				Description description = new StringDescription().appendText("Expected ").appendDescriptionOf(matcher)
						.appendText(" but ");
				matcher.matcher.describeMismatch(matcher.function.apply(stats), description);
				return description.toString();
			}

			@Override
			public boolean isError() {
				return true;
			}

			@Override
			public String getMessage() {
				return this.errorMessage;
			}

		}

		public abstract boolean isError();

		public abstract String getMessage();

		public static ValidationResult ok(FunctionBasedMatcher<?> matcher, Stats stats) {
			return new ValidationSuccess(matcher, stats);
		}

		public static ValidationResult error(FunctionBasedMatcher<?> matcher, Stats stats) {
			return new ValidationError(matcher, stats);
		}

		public static List<ValidationResult> errors(List<ValidationResult> validations) {
			return filter(validations, ValidationResult::isError);
		}

		public static List<ValidationResult> oks(List<ValidationResult> validations) {
			return filter(validations, ((Predicate<? super ValidationResult>) ValidationResult::isError).negate());
		}

		@SuppressWarnings("hiding")
		private static <ValidationResult> List<ValidationResult> filter(List<ValidationResult> validations,
				Predicate<? super ValidationResult> p) {
			return validations.stream().filter(p).collect(toList());
		}

	}

	private final List<FunctionBasedMatcher<?>> validations = new ArrayList<>();

	public <T> Validator addValidation(Function<Stats, T> function, Matcher<T> matcher, String name) {
		validations.add(new FunctionBasedMatcher<T>(name, matcher, function));
		return this;
	}

	public List<ValidationResult> validate(Stats stats) {
		return validations.stream().map(m -> m.matches(stats) ? ok(m, stats) : error(m, stats)).collect(toList());
	}

	public int getValidations() {
		return validations.size();
	}

}
