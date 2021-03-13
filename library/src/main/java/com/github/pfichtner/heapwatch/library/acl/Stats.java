package com.github.pfichtner.heapwatch.library.acl;

import java.util.function.Function;

public class Stats {

	public Memory maxHeapOccupancy;
	public Memory maxHeapAfterGC;
	public Memory maxHeapSpace;
	public Memory maxMetaspaceOccupancy;
	public Memory maxMetaspaceAfterGC;
	public Memory maxMetaspaceSpace;

	public Memory getMaxHeapAfterGC() {
		return maxHeapAfterGC;
	}

	public Memory getMaxHeapOccupancy() {
		return maxHeapOccupancy;
	}

	public Memory getMaxHeapSpace() {
		return maxHeapSpace;
	}

	public Memory getMaxMetaspaceAfterGC() {
		return maxMetaspaceAfterGC;
	}

	public Memory getMaxMetaspaceOccupancy() {
		return maxMetaspaceOccupancy;
	}

	public Memory getMaxMetaspaceSpace() {
		return maxMetaspaceSpace;
	}

	public static Function<Stats, Memory> functionForAttribute(String name) {
		switch (name) {
		case "maxHeapOccupancy":
			return Stats::getMaxHeapOccupancy;
		case "maxHeapAfterGC":
			return Stats::getMaxHeapAfterGC;
		case "maxHeapSpace":
			return Stats::getMaxHeapSpace;
		case "maxMetaspaceOccupancy":
			return Stats::getMaxMetaspaceOccupancy;
		case "maxMetaspaceAfterGC":
			return Stats::getMaxMetaspaceAfterGC;
		case "maxMetaspaceSpace":
			return Stats::getMaxMetaspaceSpace;
		}
		return null;
	}
}
