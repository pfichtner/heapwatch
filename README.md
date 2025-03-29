[![Java CI with Maven](https://github.com/pfichtner/heapwatch/actions/workflows/maven.yml/badge.svg)](https://github.com/pfichtner/heapwatch/actions/workflows/maven.yml)
[![Maven Package](https://github.com/pfichtner/heapwatch/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/pfichtner/heapwatch/actions/workflows/maven-publish.yml)
[![BCH compliance](https://bettercodehub.com/edge/badge/pfichtner/heapwatch?branch=master)](https://bettercodehub.com/)
[![Maintainability](https://api.codeclimate.com/v1/badges/9d65dad82c39b80a27e2/maintainability)](https://codeclimate.com/github/pfichtner/heapwatch/maintainability)
[![codecov](https://codecov.io/gh/pfichtner/heapwatch/branch/master/graph/badge.svg?token=xC6rGPBJVC)](https://codecov.io/gh/pfichtner/heapwatch)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fpfichtner%2Fheapwatch.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fpfichtner%2Fheapwatch?ref=badge_shield)
# heapwatch
Have you ever had the problem that your application functionally fulfilled all the requirements, but hours or days after deployment you noticed that your application suddenly allocated twice as much memory as before? A big problem is of course finding the cause in all the commits between the last and the current release. Wouldn't it be nice to be informed at the moment of the commit that your application is suddenly consuming significantly more memory than before?

This is exactly what this plugin solves! How does this work?  All you have to do is tell your JVM to write the information about the garbage collector to a log file. So if you want to view the memory used during the test execution, you can do this by making the following change in your pom:
```
<plugin>
	<artifactId>maven-surefire-plugin</artifactId>
	<version>...</version>
	<configuration>
		<argLine>-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${project.build.directory}/gc.log</argLine>
	</configuration>
</plugin>
```

In order to abort the build (or to issue a warning in the build log) when a certain limit value is exceeded, integrate this plugin as follows:
```
<plugin>
	<groupId>com.github.pfichtner.heapwatch</groupId>
	<artifactId>heapwatch-maven-plugin</artifactId>
	<version>0.0.1</version>
        <configuration>
                <gclog>${project.build.directory}/gc.log</gclog>
                <heapSpace><le>800M</le></heapSpace>
        </configuration>
	<executions>
		<execution>
			<phase>verify</phase>
			<goals>
				<goal>verify</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

If the heapspace exceeds 800 MB, the build aborts.

You can also verify if the actual run does not consume more that x percent more (or less) memory than the run before. 
To make this work you have to specify where those previous stats can be read. If you want those stats to be updated you can specify ```writeStatsTo```. 
```
<configuration>
	<gclog>${project.build.directory}/gc.log</gclog>
	<heapSpace><le>+10%</le></heapSpace>
	<readStatsFrom>
		<file>/a/b/c/prev.json</file>
	</readStatsFrom>
	<writeStatsTo>
		<out>
			<file>/a/b/c/prev.json</file> <!-- overwrite content in the file the stats have been read from -->
			<onSuccess>true</onSuccess>
			<onFailure>false</onFailure>
		</out>
		<out>
			<file>/a/b/c/prev-on-validation-failure.json</file>
			<onSuccess>false</onSuccess>
			<onFailure>true</onFailure>
		</out>
	</writeStatsTo>
</configuration>
```

Backlog: 
- Release the maven plugin to maven-central
- ~Create gradle plugin with same functionality~ working via https://github.com/pfichtner/maedle (WIP)
- ~Support relative values ("not more than 10% more than memory used last time")~ implemented
- Support more meassurements (currently supported: heapOccupancy, heapAfterGC, heapSpace, metaspaceOccupancy, metaspaceAfterGC, metaspaceSpace) 
- Investigate if we can attach the plugin analog to jacoco-maven-plugin (via prepare-agent goal)

This plugin uses the following FOSS software components: 
- garbagecat https://github.com/mgm3746/garbagecat (EPLv1.0)
- JavaHamcrest http://hamcrest.org/JavaHamcrest/ (BSD-3-Clause)

## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fpfichtner%2Fheapwatch.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fpfichtner%2Fheapwatch?ref=badge_large)

