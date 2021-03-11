package com.github.pfichtner.heapwatch.library.acl;

import java.util.Objects;

public class Memory implements Comparable<Memory> {

	private final org.eclipselabs.garbagecat.util.Memory delegate;

	private Memory(org.eclipselabs.garbagecat.util.Memory delegate) {
		this.delegate = delegate;
	}

	public static Memory memory(String value) {
		return adapt(org.eclipselabs.garbagecat.util.Memory.fromOptionSize(value));
	}

	public static Memory adapt(org.eclipselabs.garbagecat.util.Memory delegate) {
		return new Memory(delegate);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(delegate);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Memory && Objects.equals(delegate, ((Memory) obj).delegate);
	}

	@Override
	public int compareTo(Memory other) {
		return delegate.compareTo(other.delegate);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

}
