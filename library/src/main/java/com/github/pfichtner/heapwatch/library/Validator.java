package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Comparison.valueOfIgnoreCase;
import static com.github.pfichtner.heapwatch.library.ValidationResults.error;
import static com.github.pfichtner.heapwatch.library.ValidationResults.ok;
import static com.github.pfichtner.heapwatch.library.acl.Stats.functionForAttribute;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matcher;

import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public class Validator {

	private final List<FunctionBasedMatcher<?>> validations = new ArrayList<>();

	public Validator addValidation(String name, String comparison, Memory memory) {
		return addValidation( //
				functionForAttribute(name), //
				valueOfIgnoreCase(comparison).matcher(memory), // fs
				name //
		);
	}

	public Validator addValidation(String name, String comparison, double mul, Stats prev) {
		Function<Stats, Memory> functionForAttribute = functionForAttribute(name);
		Memory memory = functionForAttribute.apply(prev).multiply(mul);
		return addValidation( //
				functionForAttribute, //
				valueOfIgnoreCase(comparison).matcher(memory), //
				name //
		);
	}

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
