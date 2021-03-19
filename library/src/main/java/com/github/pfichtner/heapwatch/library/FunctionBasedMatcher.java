package com.github.pfichtner.heapwatch.library;

import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.github.pfichtner.heapwatch.library.acl.Stats;

class FunctionBasedMatcher<T> extends TypeSafeMatcher<Stats> {

	final String name;
	final Matcher<T> matcher;
	final Function<Stats, T> function;

	public FunctionBasedMatcher(String name, Matcher<T> matcher, Function<Stats, T> function) {
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