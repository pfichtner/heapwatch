package com.github.pfichtner.heapwatch.mavenplugin;

import static com.github.pfichtner.heapwatch.mavenplugin.TestUtil.lowerThan;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

import com.github.pfichtner.heapwatch.mavenplugin.HeapWatchMojo.StatsOut;

public class HeapWatchMojoConfigTest {

	@Rule
	public MojoRule rule = new MojoRule();

	@Rule
	public TestResources resources = new TestResources();

	@Test
	public void canParse() throws Exception {
		File pom = new File(resources.getBasedir("project-to-test"), "pom.xml");
		assertThat(pom).exists();

		assertThat((HeapWatchMojo) this.rule.lookupMojo(HeapWatchMojo.GOAL, pom)).isNotNull().satisfies(mojo -> {
			assertThat(mojo.gclog).isNotNull()
					.satisfies(f -> assertThat(f.getAbsolutePath()).isEqualTo("/some/path/to/gc.log"));
			assertThat(mojo.heapSpace).containsExactly(lowerThan("42M"));
			assertThat(mojo.readStatsFrom).isNotNull().satisfies(in -> {
				assertThat(in.file).isEqualTo(new File("/some/path/to/prev-in.json"));
				assertThat(in.failIfMissing).isTrue();
			});
			assertThat(mojo.writeStatsTo).isNotNull().hasSize(2).satisfies(l -> {
				assertAreEqual(l.get(0), new StatsOut(new File("/some/path/to/prev-out1.json"), true, true));
				assertAreEqual(l.get(1), new StatsOut(new File("/some/path/to/prev-out2.json"), false, false));
			});
		});

	}

	private static void assertAreEqual(StatsOut statsOut1, StatsOut statsOut2) {
		assertThat(statsOut1).usingRecursiveComparison().isEqualTo(statsOut2);
	}

}
