package com.github.pfichtner.heapwatch.library;

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

	@Test
	void valueOfIgnoreCase() {
		asList(Comparison.values())
				.forEach(c -> assertSame(loadByName(c, String::toLowerCase), loadByName(c, String::toUpperCase)));
	}

	@Test
	void lowerValue() {
		int value = 42;
		verify(value, value - 1, true, true, false, false, false);
	}

	@Test
	void equalValue() {
		int value = 42;
		verify(value, value, false, true, true, true, false);
	}

	@Test
	void greaterValue() {
		int value = 42;
		verify(value, value + 1, false, false, false, true, true);
	}

	private void verify(int value, int compTo, Boolean... expected) {
		assertEquals(asList(expected), matches(value, compTo));
	}

	private List<Boolean> matches(int value, int compTo) {
		return stream(Comparison.values()).map(c -> matches(matcher(c, value), compTo)).collect(toList());
	}

	private boolean matches(Matcher<Integer> matcher, int compTo) {
		return matcher.matches(compTo);
	}

	private Matcher<Integer> matcher(Comparison comparison, int value) {
		return comparison.matcher(value);
	}

	private Comparison loadByName(Comparison comparison, Function<String, String> function) {
		return Comparison.valueOfIgnoreCase(function.apply(comparison.name()));
	}

}
