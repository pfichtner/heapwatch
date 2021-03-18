[![Java CI with Maven](https://github.com/pfichtner/heapwatch/actions/workflows/maven.yml/badge.svg)](https://github.com/pfichtner/heapwatch/actions/workflows/maven.yml)
[![Maven Package](https://github.com/pfichtner/heapwatch/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/pfichtner/heapwatch/actions/workflows/maven-publish.yml)
# heapwatch
Have you ever had the problem that your application functionally fulfilled all the requirements, but hours or days after deployment you noticed that your application suddenly allocated twice as much memory as before? A big problem is of course finding the cause in all the commits between the last and the current release. Wouldn't it be nice to be informed at the moment of the commit that your application is suddenly consuming significantly more memory than before?

This is exactly what this pluign solves! How does this work?  All you have to do is tell your JVM to write the information about the garbage collector to a log file. So if you want to view the memory used during the test execution, you can do this by making the following change in your pom:
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


This plugin uses the following FOSS software components: 
https://github.com/mgm3746/garbagecat (EPLv1.0)
