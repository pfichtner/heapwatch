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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.github.pfichtner.heapwatch.library.acl.Memory;
import com.github.pfichtner.heapwatch.library.acl.Stats;

public final class JsonIO {

	private static class Accessor {
		Function<Stats, Memory> getter;
		BiConsumer<Stats, Memory> setter;

		public Accessor(Function<Stats, Memory> getter, BiConsumer<Stats, Memory> setter) {
			this.getter = getter;
			this.setter = setter;
		}
	}

	private static final Map<String, Accessor> jsonMappings = jsonMappings();

	private static Map<String, Accessor> jsonMappings() {
		Map<String, Accessor> map = new HashMap<>();
		map.put(HEAP_OCCUPANCY, new Accessor(s -> s.maxHeapOccupancy, (s, m) -> s.maxHeapOccupancy = m));
		map.put(HEAP_AFTER_GC, new Accessor(s -> s.maxHeapAfterGC, (s, m) -> s.maxHeapAfterGC = m));
		map.put(HEAP_SPACE, new Accessor(s -> s.maxHeapSpace, (s, m) -> s.maxHeapSpace = m));
		map.put(METASPACE_OCCUPANCY, new Accessor(s -> s.maxMetaspaceOccupancy, (s, m) -> s.maxMetaspaceOccupancy = m));
		map.put(METASPACE_AFTER_GC, new Accessor(s -> s.maxMetaspaceAfterGC, (s, m) -> s.maxMetaspaceAfterGC = m));
		map.put(METASPACE_SPACE, new Accessor(s -> s.maxMetaspaceSpace, (s, m) -> s.maxMetaspaceSpace = m));
		return map;
	}

	public static Stats read(File file) throws IOException {
		try (FileReader reader = new FileReader(file)) {
			Stats stats = new Stats();
			JsonObject jsonObject = Json.parse(reader).asObject();
			for (Entry<String, Accessor> entry : jsonMappings.entrySet()) {
				entry.getValue().setter.accept(stats, mem(jsonObject, entry.getKey()));
			}
			return stats;
		}
	}

	public static void write(File file, Stats stats) throws IOException {
		JsonObject jsonObject = new JsonObject();
		for (Entry<String, Accessor> entry : jsonMappings.entrySet()) {
			Memory value = entry.getValue().getter.apply(stats);
			if (value != null) {
				jsonObject.add(entry.getKey(), value.toString());
			}
		}
		try (FileWriter writer = new FileWriter(file)) {
			jsonObject.writeTo(writer, PRETTY_PRINT);
		}
	}

	private static Memory mem(JsonObject jsonObject, String attributeName) {
		JsonValue jsonValue = jsonObject.get(attributeName);
		return jsonValue == null ? null : memory(jsonValue.asString());
	}

}
