package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.library.StatsReader.stats;
import static com.github.pfichtner.heapwatch.mavenplugin.JsonIO.write;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.greaterThan;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.lowerThan;
import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.touch;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.pfichtner.heapwatch.library.StatsReader;
import com.github.pfichtner.heapwatch.library.acl.Stats;
import com.github.pfichtner.heapwatch.mavenplugin.HeapWatchMojo.StatsIn;
import com.github.pfichtner.heapwatch.mavenplugin.HeapWatchMojo.StatsOut;

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
		givenAnyValidation();
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class) //
					.hasMessageContaining("previous").hasMessageContaining("not configured") //
			;
		});
	}

	@Test
	public void doesPassIfNotGrown_AndPreviousFileIsNotOverwritten() throws Exception {
		File statsFile = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc-stats.json");
		Stats statsWithMaxHeapSpaceOnly = emptyStats();
		statsWithMaxHeapSpaceOnly.maxHeapSpace = StatsReader.stats(statsFile).maxHeapSpace;
		write(previousStats, statsWithMaxHeapSpaceOnly);
		givenGcLog(statsFile);
		givenPreviousStats(previousStats);

		givenAnyValidation();
		whenExecuted();
		assertEquals(previousStats, statsWithMaxHeapSpaceOnly);
	}

	@Test
	public void doesPassIfNoPreviousValue_AndPreviousFileIsNotCreated() throws Exception {
		File statsFile = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc-stats.json");
		write(previousStats, emptyStats());
		givenGcLog(statsFile);
		givenPreviousStats(previousStats);
		givenAnyValidation();
		whenExecuted();
		assertEquals(previousStats, emptyStats());
	}

	@Test
	public void doesCreatePreviousStatsIfFailureOnMissingOnIsFalse() throws Exception {
		File previousFileThatShouldBeCreated = pathInTempFolder("non-existing-previous-gc-stats.json");
		previousFileThatShouldBeCreated.delete();
		File statsFile = resourceInTestFolder("gc.log");
		givenGcLog(statsFile);
		givenAnyValidation();
		givenPreviousStats(previousFileThatShouldBeCreated, false);
		givenWriteStatsToStatsRead(false);
		whenExecuted();
		assertEquals(previousFileThatShouldBeCreated, stats(statsFile));
	}

	@Test
	public void doesUpdatePreviousStatsIfSet() throws Exception {
		File statsFile = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc-stats.json");
		write(previousStats, emptyStats());
		givenGcLog(statsFile);
		givenPreviousStats(previousStats);
		givenWriteStatsToStatsRead(false);
		givenAnyValidation();
		whenExecuted();
		assertEquals(previousStats, stats(statsFile));
	}

	@Test
	public void doesNotUpdatePreviousStatsIfValidationWasNotSuccessfull() throws Exception {
		boolean onFailure = false;
		File statsFile = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc-stats.json");
		write(previousStats, emptyStats());
		givenGcLog(statsFile);
		givenPreviousStats(previousStats);
		givenWriteStatsToStatsRead(onFailure);
		givenHeapSpaceValidation(lowerThan("0B"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class);
			assertEquals(previousStats, emptyStats());
		});
	}

	@Test
	public void doesUpdatePreviousStatsIfValidationWasNotSuccessfull() throws Exception {
		boolean onFailure = true;
		File statsFile = resourceInTestFolder("gc.log");
		File previousStats = pathInTempFolder("previous-gc-stats.json");
		write(previousStats, emptyStats());
		givenGcLog(statsFile);
		givenPreviousStats(previousStats);
		givenWriteStatsToStatsRead(onFailure);
		givenHeapSpaceValidation(lowerThan("0B"));
		assertThatThrownBy(() -> whenExecuted()).satisfies(e -> {
			assertThat(e).isInstanceOf(MojoFailureException.class);
			assertEquals(previousStats, stats(statsFile));
		});
	}

	@Test
	public void doesCreatePreviousStatsIfSet() throws Exception {
		File statsFile = resourceInTestFolder("gc.log");
		givenGcLog(statsFile);
		givenPreviousStats(pathInTempFolder("previous-gc-stats.json"), false);
		givenWriteStatsToStatsRead(false);
		givenAnyValidation();
		whenExecuted();
		assertEquals(pathInTempFolder("previous-gc-stats.json"), stats(statsFile));
	}

	private static void assertEquals(File previousStats, Stats stats) {
		try {
			assertThat(read(previousStats)).usingRecursiveComparison().isEqualTo(stats);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Stats read(File file) throws IOException {
		return JsonIO.read(file);
	}

	private Stats emptyStats() {
		return new Stats();
	}

	private void givenWriteStatsToStatsRead(boolean onFailure) {
		sut.writeStatsTo = asList(new StatsOut(sut.readStatsFrom.file, true, onFailure));
	}

	private void givenGcLog(File file) {
		sut.gclog = file;
	}

	private void givenAnyValidation() {
		givenHeapSpaceValidation(lowerThan("0.001%"));
	}

	private void givenHeapSpaceValidation(Map<String, String> hashMap) {
		sut.heapSpace = hashMap;
	}

	private void givenHeapSpaceValidation(MapEntry<String, String> entry) {
		if (sut.heapSpace == null) {
			sut.heapSpace = new HashMap<>();
		}
		sut.heapSpace.put(entry.getKey(), entry.getValue());
	}

	private void givenPreviousStats(File previous) {
		givenPreviousStats(previous, true);
	}

	private void givenPreviousStats(File previous, boolean failIfMissing) {
		sut.readStatsFrom = new StatsIn(previous, failIfMissing);
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
