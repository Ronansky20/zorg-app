package patients.repository;

import patients.model.Patient;
import patients.model.PatientField;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PatientRepository {
    List<Patient> findAll();
    Optional<Patient> findById(String id);
    void save(Patient patient);

    default List<Patient> findAllForUser(String userId) {
        return findAll().stream()
                .filter(p -> p.hasMember(userId))
                .toList();
    }

    default List<Patient> findAllForUser(String userId, Set<PatientField> fields) {
        return findAllForUser(userId);
    }
}
