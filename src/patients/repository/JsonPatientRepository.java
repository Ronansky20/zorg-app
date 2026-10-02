package patients.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.google.gson.reflect.TypeToken;
import patients.model.Patient;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class JsonPatientRepository implements PatientRepository {

    private static final Type FILE_TYPE =
            new TypeToken<Map<String, List<Patient>>>() {}.getType();

    private final Path file;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    private String groupKey; // read from the file, or generated on first save

    public JsonPatientRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Patient> loadAll() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, List<Patient>> data = gson.fromJson(reader, FILE_TYPE);
            if (data == null || data.isEmpty()) {
                return new ArrayList<>();
            }
            Map.Entry<String, List<Patient>> entry = data.entrySet().iterator().next();
            groupKey = entry.getKey();
            return entry.getValue() != null ? new ArrayList<>(entry.getValue()) : new ArrayList<>();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + file, e);
        }
    }

    @Override
    public void saveAll(List<Patient> patients) {
        if (groupKey == null) {
            groupKey = generateKey();
        }
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(Map.of(groupKey, patients), writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }

    private static String generateKey() {
        byte[] bytes = new byte[9];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Gson can't reflect into java.time on modern JDKs, so dates are stored as ISO strings (yyyy-MM-dd).
    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate date) throws IOException {
            if (date == null) {
                out.nullValue();
            } else {
                out.value(date.toString());
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString());
        }
    }
}
