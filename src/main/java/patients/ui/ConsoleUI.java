package patients.ui;

import patients.auth.Authenticator;
import patients.model.Medication;
import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;
import patients.model.User;
import patients.model.Action;
import patients.service.FieldPermissions;
import patients.service.PatientService;
import patients.service.ProfessionPermissions;
import patients.service.UserService;

import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConsoleUI {

    private static final int MAX_SIGN_IN_ATTEMPTS = 3;

    private final PatientService service;
    private final UserService userService;
    private final Authenticator authenticator;
    private final Scanner scan = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private User user;
    private List<User> users = List.of();

    public ConsoleUI(PatientService service, UserService userService) {
        this(service, userService, null);
    }

    public ConsoleUI(PatientService service, UserService userService, Authenticator authenticator) {
        this.service = service;
        this.userService = userService;
        this.authenticator = authenticator;
    }

    public void run() {
        user = authenticator == null ? chooseAccount() : signIn();
        if (user == null) {
            return;
        }
        System.out.println("Welcome " + formatUser(user));

        System.out.println("Welcome to the patient management system!");

        mainMenu(user.profession());
    }

    private void mainMenu(Profession profession) {
        List<Action> possibleActions = new ArrayList<>(ProfessionPermissions.getAllowedActions(profession));

        while (true) {
            System.out.println("0. Quit");
            for (int i = 0; i < possibleActions.size(); i++) {
                System.out.println((i + 1) + ". " + possibleActions.get(i).label);
            }

            int actionChoice = readInt("Please choose an option 0-" + possibleActions.size());

            if (actionChoice < 0 || actionChoice > possibleActions.size()) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            if (actionChoice == 0) {
                return;
            }

            int actionNumber = actionChoice - 1;

            Action chosenAction = possibleActions.get(actionNumber);

            try {
                switch (chosenAction) {
                    case VIEW_LIST:
                        printPatients(profession);
                        break;
                    case ADD_PATIENT:
                        service.add(user, readPatient());
                        break;
                    case EDIT_PATIENT:
                        editPatient(profession);
                        break;
                }
            } catch (SecurityException | IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (UncheckedIOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private User chooseAccount() {
        loadUsers();
        if (users.isEmpty()) {
            System.out.println("No user accounts found, add them to users.json");
            return null;
        }
        System.out.println("Hello, please choose your account");
        return askUser();
    }

    private User signIn() {
        System.out.println("Hello, please sign in");
        String userId = null;
        for (int attempt = 1; userId == null; attempt++) {
            if (attempt > MAX_SIGN_IN_ATTEMPTS) {
                System.out.println("Too many failed attempts");
                return null;
            }
            String email = readLine("Email: ");
            String password = readLine("Password: ");
            try {
                userId = authenticator.signIn(email, password);
            } catch (SecurityException e) {
                System.out.println(e.getMessage());
            } catch (UncheckedIOException e) {
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }

        loadUsers();
        Optional<User> account = findUser(userId);
        if (account.isEmpty()) {
            System.out.println("Your login has no account in the database, ask an administrator to add it");
        }
        return account.orElse(null);
    }

    private Optional<User> findUser(String userId) {
        return users.stream().filter(u -> u.id().equals(userId)).findFirst();
    }

    private User askUser() {
        for (int i = 0; i < users.size(); i++) {
            System.out.println((i + 1) + ". " + formatUser(users.get(i)));
        }

        while (true) {
            int userChoice = readInt("Please choose your account 1-" + users.size());
            if (userChoice >= 1 && userChoice <= users.size()) {
                return users.get(userChoice - 1);
            }
            System.out.println("That is not an account, please try again.");
        }
    }

    private void loadUsers() {
        try {
            users = userService.getAll();
        } catch (UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private Patient readPatient() {
        String firstName = readLine("Enter the new patient's first name: ");
        String lastName = readLine("Enter the patient's last name: ");
        LocalDate birthDate = readBirthDate("Enter the new patient's date of birth (dd-mm-yyyy): ");
        double weight = readDouble("Enter the new patient's weight (kg): ");
        double height = readDouble("Enter the new patient's height (m): ");
        String address = readLine("Enter the patient's address: ");

        Map<String, Boolean> careTeam = readCareTeam();

        List<Medication> medications = readMedications();

        return new Patient(null, firstName, lastName, birthDate, weight, height, address, careTeam, medications);
    }

    private Map<String, Boolean> readCareTeam() {
        Map<String, Boolean> careTeam = new LinkedHashMap<>();
        for (Profession profession : Profession.values()) {
            List<User> candidates = users.stream().filter(u -> u.profession() == profession).toList();
            User member = chooseUser(candidates,
                    "Choose the patient's " + profession.label.toLowerCase() + " (0 = none)");
            if (member != null) {
                careTeam.put(member.id(), true);
            }
        }
        return careTeam;
    }

    private User chooseUser(List<User> users, String prompt) {
        if (users.isEmpty()) {
            return null;
        }
        while (true) {
            System.out.println("0. None");
            for (int i = 0; i < users.size(); i++) {
                System.out.println((i + 1) + ". " + users.get(i).name());
            }
            int userChoice = readInt(prompt);

            if (userChoice < 0 || userChoice > users.size()) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            return userChoice == 0 ? null : users.get(userChoice - 1);
        }
    }

    private List<Medication> readMedications() {
        List<Medication> medications = new ArrayList<>();
        while (readYesNo("Add medication? (y/n)")) {
            medications.add(readMedication());
        }
        return medications;
    }

    private Medication readMedication() {
        String name = readLine("Enter the medication name: ");
        while (name.isEmpty()) {
            System.out.println("The medication name can't be empty, try again.");
            name = readLine("Enter the medication name: ");
        }
        String dosage = readLine("Enter the dosage (e.g. 500 mg): ");
        String frequency = readLine("Enter the frequency (e.g. 3x daily): ");
        return new Medication(name, dosage, frequency);
    }

    private void printPatients(Profession profession) {
        Set<PatientField> viewableFields = FieldPermissions.getViewableFields(profession);
        for (Patient p : service.getAll(user)) {
            for (PatientField field : viewableFields) {
                System.out.println(field.label + ": " + fieldValue(p, field));
            }
            System.out.println("Care team: " + formatCareTeam(p));
            System.out.println("----------");
        }
    }

    private String formatCareTeam(Patient patient) {
        List<String> members = new ArrayList<>();
        for (User member : users) {
            if (patient.hasMember(member.id())) {
                members.add(formatUser(member));
            }
        }
        for (String userId : patient.careTeam().keySet()) {
            if (patient.hasMember(userId) && findUser(userId).isEmpty()) {
                members.add(userId);
            }
        }
        return members.isEmpty() ? "none" : String.join(", ", members);
    }

    private String formatUser(User user) {
        return user.name() + " (" + user.profession().label + ")";
    }

    private String fieldValue(Patient patient, PatientField patientField) {
        return switch (patientField) {
            case FIRST_NAME -> patient.firstName();
            case LAST_NAME -> patient.lastName();
            case DATE_OF_BIRTH -> String.valueOf(patient.birthDate());
            case WEIGHT -> String.valueOf(patient.weight());
            case HEIGHT -> String.valueOf(patient.height());
            case ADDRESS -> patient.address();
            case MEDICATION -> formatMedications(patient.medications());
        };
    }

    private String formatMedications(List<Medication> medications) {
        if (medications.isEmpty()) {
            return "none";
        }
        List<String> descriptions = new ArrayList<>();
        for (Medication medication : medications) {
            descriptions.add(formatMedication(medication));
        }
        return String.join("; ", descriptions);
    }

    private String formatMedication(Medication medication) {
        return medication.name() + " " + medication.dosage() + ", " + medication.frequency();
    }

    private void editPatient(Profession profession) {
        Patient oldPatient = choosePatient();
        if (oldPatient == null) {
            return;
        }

        PatientField patientField =  chooseField(profession, oldPatient);

        if (patientField == null) {
            return;
        }

        Patient updatedPatient = buildUpdatedPatient(oldPatient, patientField);
        if (updatedPatient == null) {
            return;
        }
        service.update(user, updatedPatient);

        System.out.println("Patient Updated");
    }

    private Patient choosePatient() {
        List<Patient> possiblePatients = service.getAll(user);
        if (possiblePatients.isEmpty()) {
            System.out.println("No patients found");
            return null;
        }
        while (true) {
            System.out.println("0. Back");
            for (int i = 0; i < possiblePatients.size(); i++) {
                System.out.println((i + 1) + ". " + possiblePatients.get(i).firstName() + " " + possiblePatients.get(i).lastName());
            }
            int patientChoice = readInt("What patient would you like to edit?");

            if (patientChoice < 0 || patientChoice > possiblePatients.size()) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            if (patientChoice == 0) {
                return null;
            }

            int patientNumber = patientChoice - 1;

            Patient chosenPatient = possiblePatients.get(patientNumber);

            return chosenPatient;
        }
    }

    private PatientField chooseField(Profession profession, Patient patient) {
        List<PatientField> editableFields = new ArrayList<>(FieldPermissions.getEditableFields(profession));
        if (editableFields.isEmpty()) {
            System.out.println("You can't edit any fields");
            return null;
        }
        while (true) {
            System.out.println("0. Back");
            for (int i = 0; i < editableFields.size(); i++) {
                PatientField field = editableFields.get(i);
                System.out.println((i + 1) + ". " + field.label + ": " + fieldValue(patient, field));
            }
            int fieldChoice = readInt("Which field would you like to edit?");

            if (fieldChoice < 0 || fieldChoice > editableFields.size()) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            if (fieldChoice == 0) { return null; }

            int fieldNumber = fieldChoice - 1;

            PatientField chosenField = editableFields.get(fieldNumber);

            return chosenField;
        }
    }

    private Patient buildUpdatedPatient(Patient oldPatient, PatientField field) {
        String prompt = "New " + field.label + ": ";
        return switch (field) {
            case FIRST_NAME -> oldPatient.withFirstName(readLine(prompt));
            case LAST_NAME -> oldPatient.withLastName(readLine(prompt));
            case DATE_OF_BIRTH -> oldPatient.withBirthDate(readBirthDate(prompt));
            case WEIGHT -> oldPatient.withWeight(readDouble(prompt));
            case HEIGHT -> oldPatient.withHeight(readDouble(prompt));
            case ADDRESS -> oldPatient.withAddress(readLine(prompt));
            case MEDICATION -> editMedications(oldPatient);
        };
    }

    private Patient editMedications(Patient oldPatient) {
        List<Medication> medications = new ArrayList<>(oldPatient.medications());
        while (true) {
            System.out.println("0. Back");
            System.out.println("1. Add medication");
            System.out.println("2. Change medication");
            System.out.println("3. Remove medication");
            int choice = readInt("What would you like to do?");

            switch (choice) {
                case 0:
                    return null;
                case 1:
                    medications.add(readMedication());
                    return oldPatient.withMedications(medications);
                case 2: {
                    int index = chooseMedication(medications, "Which medication would you like to change?");
                    if (index == -1) {
                        return null;
                    }
                    medications.set(index, readMedication());
                    return oldPatient.withMedications(medications);
                }
                case 3: {
                    int index = chooseMedication(medications, "Which medication would you like to remove?");
                    if (index == -1) {
                        return null;
                    }
                    medications.remove(index);
                    return oldPatient.withMedications(medications);
                }
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private int chooseMedication(List<Medication> medications, String prompt) {
        if (medications.isEmpty()) {
            System.out.println("No medication recorded");
            return -1;
        }
        while (true) {
            System.out.println("0. Back");
            for (int i = 0; i < medications.size(); i++) {
                System.out.println((i + 1) + ". " + formatMedication(medications.get(i)));
            }
            int medicationChoice = readInt(prompt);

            if (medicationChoice < 0 || medicationChoice > medications.size()) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            return medicationChoice - 1;
        }
    }

    private String readLine(String prompt) {
        System.out.println(prompt);
        return scan.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readLine(prompt));
            } catch (NumberFormatException e) {
                System.out.println("That's not a whole number, try again.");
            }
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt).toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readLine(prompt).replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("That's not a number, try again.");
            }
        }
    }

    private LocalDate readBirthDate(String prompt) {
        while (true) {
            try {
                LocalDate date = LocalDate.parse(readLine(prompt), DATE_FORMAT);
                if (date.isAfter(LocalDate.now())) {
                    System.out.println("A date of birth can't be in the future, try again.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("That's not a valid date (dd-mm-yyyy), try again.");
            }
        }
    }
}