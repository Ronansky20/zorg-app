package patients.repository;

import patients.model.Patient;
import java.util.List;

public interface PatientRepository {
    List<Patient> loadAll();
    void saveAll(List<Patient> patients);
}