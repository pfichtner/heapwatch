package com.github.pfichtner.heapwatcher.mavenplugin;

import static com.github.pfichtner.heapwatcher.library.StatsReader.stats;
import static com.github.pfichtner.heapwatcher.mavenplugin.TestUtil.greaterThan;
import static com.github.pfichtner.heapwatcher.mavenplugin.TestUtil.lowerThan;
import static com.github.pfichtner.heapwatcher.mavenplugin.TestUtil.touch;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.pfichtner.heapwatcher.mavenplugin.HeapWatchMojo;

public class HeapWatchMojoTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private HeapWatchMojo sut = new HeapWatchMojo();

	@Test
	public void failsIfGcLogIsNotGiven() {
		givenGcLog(null);
		assertThatThrownBy(() -> whenExecuted()).satisfies(
				e -> assertThat(e).isExactlyInstanceOf(NullPointerException.class).hasMessageContaining("gclog"));
	}

	@Test
	public void failsIfGcLogDoesNotExist() {
		String filename = "someNonExistingFile";
		givenGcLog(pathInTempFolder(filename));
		givenHeapSpaceValidation(lowerThan("42M"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isExactlyInstanceOf(IllegalStateException.class) //
					.hasMessageContaining(filename) //
					.hasMessageContaining("not exist") //
			;
		});
	}

	@Test
	public void failsIfNoValidationWasConfigured() {
		givenGcLog(anyValidFile());
		givenHeapSpaceValidation(nullMap());
		assertThatThrownBy(() -> whenExecuted()).satisfies(HeapWatchMojoTest::verifyTheNoValidationExceptionIsThrown);
	}

	@Test
	public void failsIfNoValidationWasConfigured_emptyMap() {
		givenGcLog(anyValidFile());
		givenHeapSpaceValidation(emptyMap());
		assertThatThrownBy(() -> whenExecuted()).satisfies(HeapWatchMojoTest::verifyTheNoValidationExceptionIsThrown);
	}

	private static void verifyTheNoValidationExceptionIsThrown(Throwable t) {
		assertThat(t).isExactlyInstanceOf(IllegalStateException.class).hasMessageContaining("no validation");
	}

	@Test
	public void throwsExceptionIfHeapSpaceIsMoreThan42M() throws Exception {
		String maxHeapSpaceAllowed = "42M";
		givenGcLog(resourceInTestFolder("gc.log"));
		givenHeapSpaceValidation(lowerThan(maxHeapSpaceAllowed));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(RuntimeException.class) //
					.hasMessageContaining("heapSpace") //
					.hasMessageContaining(maxHeapSpaceAllowed) //
					.hasMessageContaining(maxHeapSpaceUsedInGcLog()) //
			;
		});
	}

	@Test
	public void multipleValidationViolations() throws Exception {
		givenGcLog(resourceInTestFolder("gc.log"));
		givenHeapSpaceValidation(lowerThan("42M"));
		givenHeapSpaceValidation(greaterThan("1G"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(RuntimeException.class) //
					.hasMessageContaining("heapSpace") //
					.hasMessageContaining("42M") //
					.hasMessageContaining("1G") //
					.hasMessageContaining(maxHeapSpaceUsedInGcLog()) //
			;
		});
	}

	private void givenGcLog(File file) {
		sut.gclog = file;
	}

	private void givenHeapSpaceValidation(Map<String, String> hashMap) {
		sut.heapSpace = hashMap;
	}

	private void givenHeapSpaceValidation(MapEntry<String, String> entry) {
		if (sut.heapSpace == null) {
			sut.heapSpace = new HashMap<String, String>();
		}
		sut.heapSpace.put(entry.getKey(), entry.getValue());
	}

	private Map<String, String> nullMap() {
		return null;
	}

	private String maxHeapSpaceUsedInGcLog() {
		return valueOf(stats(sut.gclog).getMaxHeapSpace());
	}

	private File anyValidFile() {
		try {
			return touch(pathInTempFolder("anyfile.txt"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private File pathInTempFolder(String name) {
		return new File(temporaryFolder.getRoot(), name);
	}

	private File resourceInTestFolder(String name) throws URISyntaxException {
		return new File(HeapWatchMojoTest.class.getClassLoader().getResource(name).toURI());
	}

	private void whenExecuted() throws MojoExecutionException {
		sut.execute();
	}

}
