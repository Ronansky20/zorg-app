package patients.repository;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;

public final class GsonFactory {

    private GsonFactory() {}

    public static GsonBuilder builder() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
    }

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
