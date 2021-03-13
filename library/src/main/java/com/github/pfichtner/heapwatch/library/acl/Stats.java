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
		case "heapOccupancy":
			return Stats::getMaxHeapOccupancy;
		case "heapAfterGC":
			return Stats::getMaxHeapAfterGC;
		case "heapSpace":
			return Stats::getMaxHeapSpace;
		case "metaspaceOccupancy":
			return Stats::getMaxMetaspaceOccupancy;
		case "metaspaceAfterGC":
			return Stats::getMaxMetaspaceAfterGC;
		case "metaspaceSpace":
			return Stats::getMaxMetaspaceSpace;
		}
		return null;
	}
}
