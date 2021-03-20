package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.Comparison.valueOfIgnoreCase;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

class ComparisonTest {

	Integer referenceValue = 42;

	@Test
	void canLoadUsingCaseInsensitivStringValue() {
		asList(Comparison.values())
				.forEach(c -> assertSame(loadByName(c, String::toLowerCase), loadByName(c, String::toUpperCase)));
	}

	@Test
	void lowerValue() {
		verify(referenceValue - 1, true, true, false, false, false);
	}

	@Test
	void equalValue() {
		verify(referenceValue * 1, false, true, true, true, false);
	}

	@Test
	void greaterValue() {
		verify(referenceValue + 1, false, false, false, true, true);
	}

	private void verify(Integer compareTo, Boolean... expected) {
		assertEquals(asList(expected), matches(referenceValue, compareTo));
	}

	private static <T extends Comparable<T>> List<Boolean> matches(T value, T compareTo) {
		return stream(Comparison.values()).map(c -> matcher(c, value).matches(compareTo)).collect(toList());
	}

	private static <T extends Comparable<T>> Matcher<T> matcher(Comparison comparison, T value) {
		return comparison.matcher(value);
	}

	private static Comparison loadByName(Comparison comparison, Function<String, String> function) {
		return valueOfIgnoreCase(function.apply(comparison.name()));
	}

}
