package com.github.pfichtner.heapwatch.library;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.hamcrest.Matcher;

public enum Comparison {

	LT() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return lessThan(t);
		}
	},
	GT() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return greaterThan(t);
		}
	},
	LE() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return not(greaterThan(t));
		}
	},
	GE() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return not(lessThan(t));
		}
	},
	EQ() {
		public <T extends Comparable<T>> Matcher<T> matcher(T t) {
			return equalTo(t);
		}
	};

	public abstract <T extends Comparable<T>> Matcher<T> matcher(T t);
}