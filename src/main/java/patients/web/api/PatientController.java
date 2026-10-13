package patients.web.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import patients.model.Patient;
import patients.model.User;
import patients.service.FieldPermissions;
import patients.web.session.Workspace;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
class PatientController {

    @GetMapping
    List<PatientResponse> list(Workspace workspace) {
        List<User> users = workspace.users().getAll();
        return workspace.patients().getAll(workspace.user()).stream()
                .map(patient -> view(workspace, patient, users))
                .toList();
    }

    @GetMapping("/{id}")
    PatientResponse get(Workspace workspace, @PathVariable String id) {
        Patient patient = workspace.patients().get(workspace.user(), id);
        return view(workspace, patient, workspace.users().getAll());
    }

    @PostMapping
    ResponseEntity<PatientResponse> add(Workspace workspace, @RequestBody NewPatientRequest request,
                                        UriComponentsBuilder uriBuilder) {
        List<User> users = workspace.users().getAll();
        if (request.careTeam() != null) {
            for (String memberId : request.careTeam()) {
                if (users.stream().noneMatch(u -> u.id().equals(memberId))) {
                    throw new IllegalArgumentException("Unknown care team member: " + memberId);
                }
            }
        }
        Patient added = workspace.patients().add(workspace.user(), request.toPatient());
        URI location = uriBuilder.path("/api/patients/{id}").buildAndExpand(added.id()).toUri();
        return ResponseEntity.created(location).body(view(workspace, added, users));
    }

    @PatchMapping("/{id}")
    PatientResponse update(Workspace workspace, @PathVariable String id, @RequestBody PatientChanges changes) {
        Patient existing = workspace.patients().get(workspace.user(), id);
        Patient updated = workspace.patients().update(workspace.user(), changes.applyTo(existing));
        return view(workspace, updated, workspace.users().getAll());
    }

    private static PatientResponse view(Workspace workspace, Patient patient, List<User> users) {
        return PatientResponse.of(patient, FieldPermissions.getViewableFields(workspace.user().profession()), users);
    }
}
