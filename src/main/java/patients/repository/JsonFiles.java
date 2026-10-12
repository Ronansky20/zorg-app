package patients.repository;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class JsonFiles {

    private JsonFiles() {}

    static <T> Map<String, T> readSection(Gson gson, Path file, String key, Type mapType) {
        Map<String, T> records = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return records;
        }
        JsonElement root;
        try (Reader reader = Files.newBufferedReader(file)) {
            root = JsonParser.parseReader(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + file, e);
        }
        if (root.isJsonNull()) {
            return records;
        }
        if (!root.isJsonObject()) {
            throw unknownFormat(file, key);
        }
        JsonObject object = root.getAsJsonObject();
        if (object.isEmpty()) {
            return records;
        }
        if (!object.has(key)) {
            throw unknownFormat(file, key);
        }
        Map<String, T> stored = gson.fromJson(object.get(key), mapType);
        if (stored != null) {
            stored.forEach((id, record) -> {
                if (record != null) {
                    records.put(id, record);
                }
            });
        }
        return records;
    }

    private static UncheckedIOException unknownFormat(Path file, String key) {
        String message = file + " is not in the expected format ({ \"" + key + "\": { ... } })."
                + " Files from older versions need to be rewritten, see the README";
        return new UncheckedIOException(message, new IOException(message));
    }
}
