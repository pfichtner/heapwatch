package com.github.pfichtner.heapwatch.gradleplugin;

import java.io.File;
import java.util.Map;

public class HeapWatchPluginExtension {

	public File gclog;

	public Map<String, String> heapOccupancy;
	public Map<String, String> heapAfterGC;
	public Map<String, String> heapSpace;
	public Map<String, String> metaspaceOccupancy;
	public Map<String, String> metaspaceAfterGC;
	public Map<String, String> metaspaceSpace;

	public boolean breakBuildOnValidationError = true;

	
}