package patients.repository;

import patients.model.Patient;
import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    List<Patient> findAll();
    Optional<Patient> findById(String id);
    void save(Patient patient);
}
