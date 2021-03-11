package com.github.pfichtner.heapwatch.library.acl;
import java.util.function.Function;

import org.eclipselabs.garbagecat.util.Memory;

public class Stats {

	private Memory maxHeapSpace;

	public Memory getMaxHeapSpace() {
		return maxHeapSpace;
	}

	public void setMaxHeapSpace(Memory maxHeapSpace) {
		this.maxHeapSpace = maxHeapSpace;
	}

	public static Function<Stats, Memory> functionForAttribute(String name) {
		if ("heapSpace".equals(name)) {
			return Stats::getMaxHeapSpace;
		}
		return null;
	}

}
