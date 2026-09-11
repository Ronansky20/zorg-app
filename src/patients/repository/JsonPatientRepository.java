package patients.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import patients.model.Patient;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonPatientRepository implements PatientRepository {

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonPatientRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Patient> loadAll() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            List<Patient> loaded = gson.fromJson(reader,
                    new TypeToken<List<Patient>>() {}.getType());
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + file, e);
        }
    }

    @Override
    public void saveAll(List<Patient> patients) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(patients, writer);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }
}