package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.acl.Memory.adapt;
import static java.util.stream.Collectors.toList;
import static org.eclipselabs.garbagecat.util.Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;

import com.github.pfichtner.heapwatch.library.acl.Stats;

public final class StatsReader {

	private StatsReader() {
		super();
	}

	public static Stats stats(File logFile) {
		GcManager gcManager = new GcManager();
		gcManager.store(readFile(logFile), true);
		return map(gcManager.getJvmRun(null, DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD));
	}

	private static List<String> readFile(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			return reader.lines().collect(toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Stats map(JvmRun jvmRun) {
		Stats result = new Stats();
		result.maxHeapAfterGC = adapt(jvmRun.getMaxHeapAfterGc());
		result.maxHeapOccupancy = adapt(jvmRun.getMaxHeapOccupancy());
		result.maxHeapSpace = adapt(jvmRun.getMaxHeap());
		result.maxMetaspaceAfterGC = adapt(jvmRun.getMaxClassSpaceAfterGc());
		result.maxMetaspaceOccupancy = adapt(jvmRun.getMaxClassSpaceOccupancy());
		result.maxMetaspaceSpace = adapt(jvmRun.getMaxClassSpace());
		return result;
	}

}
