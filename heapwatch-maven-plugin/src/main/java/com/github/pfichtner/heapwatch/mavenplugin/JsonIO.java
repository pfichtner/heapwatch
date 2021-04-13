package com.github.pfichtner.heapwatch.mavenplugin;

import static com.eclipsesource.json.WriterConfig.PRETTY_PRINT;
import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.HEAP_SPACE;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_AFTER_GC;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_OCCUPANCY;
import static com.github.pfichtner.heapwatch.library.acl.Stats.METASPACE_SPACE;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public final class JsonIO {

	public static Stats read(File file) throws IOException {
		try (FileReader reader = new FileReader(file)) {
			JsonObject jsonObject = Json.parse(reader).asObject();
			Stats stats = new Stats();
			stats.maxHeapOccupancy = mem(jsonObject, HEAP_OCCUPANCY);
			stats.maxHeapAfterGC = mem(jsonObject, HEAP_AFTER_GC);
			stats.maxHeapSpace = mem(jsonObject, HEAP_SPACE);
			stats.maxMetaspaceOccupancy = mem(jsonObject, METASPACE_OCCUPANCY);
			stats.maxMetaspaceAfterGC = mem(jsonObject, METASPACE_AFTER_GC);
			stats.maxMetaspaceSpace = mem(jsonObject, METASPACE_SPACE);
			return stats;
		}
	}

	public static void write(File file, Stats stats) throws IOException {
		JsonObject jsonObject = new JsonObject();
		add(stats, jsonObject, HEAP_OCCUPANCY, stats.maxHeapOccupancy);
		add(stats, jsonObject, HEAP_AFTER_GC, stats.maxHeapAfterGC);
		add(stats, jsonObject, HEAP_SPACE, stats.maxHeapSpace);
		add(stats, jsonObject, METASPACE_OCCUPANCY, stats.maxMetaspaceOccupancy);
		add(stats, jsonObject, METASPACE_AFTER_GC, stats.maxMetaspaceAfterGC);
		add(stats, jsonObject, METASPACE_SPACE, stats.maxMetaspaceSpace);
		try (FileWriter writer = new FileWriter(file)) {
			jsonObject.writeTo(writer, PRETTY_PRINT);
		}
	}

	private static void add(Stats stats, JsonObject jsonObject, String key, Memory value) {
		if (value != null) {
			jsonObject.add(key, value.toString());
		}
	}

	private static Memory mem(JsonObject jsonObject, String attributeName) {
		JsonValue jsonValue = jsonObject.get(attributeName);
		return jsonValue == null ? null : memory(jsonValue.asString());
	}

}
