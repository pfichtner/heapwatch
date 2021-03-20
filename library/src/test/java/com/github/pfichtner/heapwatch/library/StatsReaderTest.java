package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.github.pfichtner.heapwatch.library.acl.Stats;

class StatsReaderTest {

	@Test
	void canParseGcLog() throws URISyntaxException {
		assertThat(stats(file("gc.log")), samePropertyValuesAs(expectedStats()));
	}

	private static Stats expectedStats() {
		Stats stats = new Stats();
		stats.maxHeapOccupancy = memory("679166K");
		stats.maxHeapAfterGC = memory("364048K");
		stats.maxHeapSpace = memory("914432K");
		stats.maxMetaspaceOccupancy = memory("17470K");
		stats.maxMetaspaceAfterGC = memory("17470K");
		stats.maxMetaspaceSpace = memory("1067008K");
		return stats;
	}

	private static File file(String name) throws URISyntaxException {
		return new File(resource(name).toURI());
	}

	private static URL resource(String name) {
		return StatsReaderTest.class.getClassLoader().getResource(name);
	}

}
