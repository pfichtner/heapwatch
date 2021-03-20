package com.github.pfichtner.heapwatch.library.acl;

import java.util.function.Function;

public class Stats {

	public static final String HEAP_OCCUPANCY = "heapOccupancy";
	public static final String HEAP_AFTER_GC = "heapAfterGC";
	public static final String HEAP_SPACE = "heapSpace";
	public static final String METASPACE_OCCUPANCY = "metaspaceOccupancy";
	public static final String METASPACE_AFTER_GC = "metaspaceAfterGC";
	public static final String METASPACE_SPACE = "metaspaceSpace";

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
		case HEAP_OCCUPANCY:
			return Stats::getMaxHeapOccupancy;
		case HEAP_AFTER_GC:
			return Stats::getMaxHeapAfterGC;
		case HEAP_SPACE:
			return Stats::getMaxHeapSpace;
		case METASPACE_OCCUPANCY:
			return Stats::getMaxMetaspaceOccupancy;
		case METASPACE_AFTER_GC:
			return Stats::getMaxMetaspaceAfterGC;
		case METASPACE_SPACE:
			return Stats::getMaxMetaspaceSpace;
		}
		return null;
	}

}
