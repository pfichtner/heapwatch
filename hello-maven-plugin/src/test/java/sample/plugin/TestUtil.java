package sample.plugin;

import static foo.Comparison.GT;
import static foo.Comparison.LT;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.entry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.assertj.core.data.MapEntry;

import foo.Comparison;

public final class TestUtil {

	private TestUtil() {
		super();
	}

	public static MapEntry<String, String> lowerThan(String value) {
		return comparison(LT, value);
	}

	public static MapEntry<String, String> greaterThan(String value) {
		return comparison(GT, value);
	}

	private static MapEntry<String, String> comparison(Comparison comparison, String value) {
		return entry(comparison.name().toLowerCase(), value);
	}

	public static File touch(File file) throws IOException {
		if (file.exists()) {
			file.setLastModified(currentTimeMillis());
		} else {
			try (FileOutputStream outputStream = new FileOutputStream(file)) {
				outputStream.close();
			}
		}
		return file;
	}

}
