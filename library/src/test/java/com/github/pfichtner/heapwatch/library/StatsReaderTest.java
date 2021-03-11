package com.github.pfichtner.heapwatch.library;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

class StatsReaderTest {

	@Test
	void canParseGcLog() throws URISyntaxException {
		assertThat(stats(file("gc.log")).getMaxHeapSpace(), is(memory("914432K")));
	}

	private static File file(String name) throws URISyntaxException {
		return new File(resource(name).toURI());
	}

	private static URL resource(String name) {
		return StatsReaderTest.class.getClassLoader().getResource(name);
	}

}
