package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.ValidationResult.error;
import static com.github.pfichtner.heapwatch.library.ValidationResult.ok;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matcher;

import com.github.pfichtner.heapwatch.library.acl.Stats;

public class Validator {

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
