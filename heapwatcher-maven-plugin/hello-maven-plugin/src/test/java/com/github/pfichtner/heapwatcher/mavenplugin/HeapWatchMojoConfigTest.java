package com.github.pfichtner.heapwatcher.mavenplugin;

import static com.github.pfichtner.heapwatcher.mavenplugin.TestUtil.lowerThan;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

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
		});

	}
}
