package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.greaterThan;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.lowerThan;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.touch;
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
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.data.MapEntry;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.Files;

public class HeapWatchMojoTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private HeapWatchMojo sut = new HeapWatchMojo();

	@Test
	public void failsIfGcLogIsNotGiven() {
		givenGcLog(null);
		assertThatThrownBy(() -> whenExecuted()).satisfies(
				e -> assertThat(e).isExactlyInstanceOf(MojoFailureException.class).hasMessageContaining("gclog"));
	}

	@Test
	public void failsIfGcLogDoesNotExist() {
		String nonExistingFile = "someNonExistingFile";
		givenGcLog(pathInTempFolder(nonExistingFile));
		givenHeapSpaceValidation(lowerThan("42M"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isExactlyInstanceOf(MojoFailureException.class) //
					.hasMessageContaining(nonExistingFile) //
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
		assertThat(t).isExactlyInstanceOf(MojoFailureException.class).hasMessageContaining("no validation");
	}

	@Test
	public void throwsExceptionIfHeapSpaceIsMoreThan42M() throws Exception {
		String maxHeapSpaceAllowed = "42M";
		givenGcLog(resourceInTestFolder("gc.log"));
		givenHeapSpaceValidation(lowerThan(maxHeapSpaceAllowed));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class) //
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
			assertThat(e).isInstanceOf(MojoFailureException.class) //
					.hasMessageContaining("heapSpace") //
					.hasMessageContaining("42M") //
					.hasMessageContaining("1G") //
					.hasMessageContaining(maxHeapSpaceUsedInGcLog()) //
			;
		});
	}

	@Test
	public void failsIfThereAreRelativeComparisionAndNoReferenceFileWasGiven() throws Exception {
		givenGcLog(resourceInTestFolder("gc.log"));
		givenHeapSpaceValidation(lowerThan("10%"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class) //
					.hasMessageContaining("previous").hasMessageContaining("not configured") //
			;
		});
	}

	@Test
	public void failsIfThereAreRelativeComparisionAndReferenceFileDoesNotExist() throws Exception {
		String nonExistingFile = "non-existing-previous-stats.file";
		givenGcLog(resourceInTestFolder("gc.log"));
		givenHeapSpaceValidation(lowerThan("10%"));
		givenPreviousStats(pathInTempFolder(nonExistingFile));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class) //
					.hasMessageContaining(nonExistingFile) //
					.hasMessageContaining("previous") //
					.hasMessageContaining("exist") //
			;
		});
	}

	@Test
	public void throwsExceptionIfFilesAreSame() throws Exception {
		String gcLog = "gc.log";
		File stats = resourceInTestFolder(gcLog);
		givenGcLog(stats);
		givenPreviousStats(stats);
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class) //
					.hasMessageContaining(gcLog) //
					.hasMessageContaining("same") //
			;
		});
	}

	@Test
	@Ignore
	public void doesPassIfNotGrown() throws Exception {
		File stats = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc.log");
		copy(stats, previousStats);

		givenGcLog(stats);
		givenPreviousStats(previousStats);

		givenHeapSpaceValidation(lowerThan("0.001%"));
		whenExecuted();
	}

	private void copy(File source, File target) throws IOException {
		Files.copy(source, target);
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

	private void givenPreviousStats(File previous) {
		sut.previous = previous;
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

	private void whenExecuted() throws MojoFailureException, MojoExecutionException {
		sut.execute();
	}

}
