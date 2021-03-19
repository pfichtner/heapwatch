package com.github.pfichtner.heapwatch.library.acl;

import static com.github.pfichtner.heapwatch.library.acl.Memory.memory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MemoryTest {

	@Test
	void canBeUsedAsKeysInHashBasedContainers() {
		Map<Memory, String> map = new HashMap<>();
		map.put(memory("42M"), "42");
		assertEquals("42", map.get(memory("42M")));
	}

}
