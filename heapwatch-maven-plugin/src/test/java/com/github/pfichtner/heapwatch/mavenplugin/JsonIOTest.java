package com.github.pfichtner.heapwatch.mavenplugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public class JsonIOTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void canWriteAndReadEmptyStats() throws IOException {
		assertWrittenContentEqualsRead(emptyStats());
	}

	@Test
	public void canWriteAndReadCompleteStats() throws IOException {
		assertWrittenContentEqualsRead(fullStats());
	}

	private void assertWrittenContentEqualsRead(Stats stats) throws IOException {
		File file = temporaryFolder.newFile();
		JsonIO.write(file, stats);
		assertThat(JsonIO.read(file)).usingRecursiveComparison().isEqualTo(stats);
	}

	private static Stats fullStats() {
		int i = 1;
		Stats fullStats = emptyStats();
		fullStats.maxHeapOccupancy = memory(i++);
		fullStats.maxHeapAfterGC = memory(i++);
		fullStats.maxHeapSpace = memory(i++);
		fullStats.maxMetaspaceOccupancy = memory(i++);
		fullStats.maxMetaspaceAfterGC = memory(i++);
		fullStats.maxMetaspaceSpace = memory(i++);
		return fullStats;
	}

	private static Stats emptyStats() {
		return new Stats();
	}

	private static Memory memory(int mem) {
		return Memory.memory(String.valueOf(mem));
	}

}
