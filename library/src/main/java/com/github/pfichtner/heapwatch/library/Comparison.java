package com.github.pfichtner.heapwatch.library;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.hamcrest.Matcher;

public enum Comparison {

	LT() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return lessThan(t);
		}
	},
	LE() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return not(greaterThan(t));
		}
	},
	EQ() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return equalTo(t);
		}
	},
	GE() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return not(lessThan(t));
		}
	},
	GT() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return greaterThan(t);
		}
	};

	public abstract <T extends Comparable<T>> Matcher<T> matcher(T t);

	public static Comparison valueOfIgnoreCase(String name) {
		return streamOf(Comparison.class).filter(e -> e.name().equalsIgnoreCase(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Invalid comparison " + name));
	}

	private static <T extends Enum<T>> Stream<T> streamOf(Class<T> clazz) {
		return EnumSet.allOf(clazz).stream();
	}

}