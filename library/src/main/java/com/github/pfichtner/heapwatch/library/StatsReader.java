package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.acl.Memory.adapt;
import static org.eclipselabs.garbagecat.util.Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD;

import java.io.File;

import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.jdk.Jvm;

import com.github.pfichtner.heapwatch.library.acl.Stats;

public final class StatsReader {

	private StatsReader() {
		super();
	}

	public static Stats stats(File logFile) {
		GcManager gcManager = new GcManager();
		gcManager.store(logFile, true);
		return map(gcManager.getJvmRun(new Jvm(null, null), DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD));
	}

	private static Stats map(JvmRun jvmRun) {
		Stats result = new Stats();
		result.maxHeapAfterGC = adapt(jvmRun.getMaxHeapAfterGc());
		result.maxHeapOccupancy = adapt(jvmRun.getMaxHeapOccupancy());
		result.maxHeapSpace = adapt(jvmRun.getMaxHeapSpace());
		result.maxMetaspaceAfterGC = adapt(jvmRun.getMaxPermAfterGc());
		result.maxMetaspaceOccupancy = adapt(jvmRun.getMaxPermOccupancy());
		result.maxMetaspaceSpace = adapt(jvmRun.getMaxPermSpace());
		return result;
	}

}
