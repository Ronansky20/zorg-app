package patients.ui;

import patients.model.Patient;
import patients.model.PatientField;
import patients.model.Profession;
import patients.model.Action;
import patients.service.FieldPermissions;
import patients.service.PatientService;
import patients.service.ProfessionPermissions;

import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class ConsoleUI {

    private final PatientService service;
    private final Scanner scan = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public ConsoleUI(PatientService service) {
        this.service = service;
    }

    public void run() {

        System.out.println("Hello, please choose your profession");
        Profession profession = askProfession();
        service.login(profession);
        System.out.println("Welcome " + profession.label);

        loadPatients();

        System.out.println("Welcome to the patient management system!");

        mainMenu(profession);

        savePatients();
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
                        service.add(readPatient());
                        break;
                    case EDIT_PATIENT:
                        editPatient(profession);
                        break;
                }
            } catch (SecurityException | IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private Profession askProfession() {
        int i = 1;
        for (Profession p : Profession.values()) {
            System.out.println(i + ". " + p.label);
            i++;
        }

        while (true) {
            try {
                Profession profession = professionSwitch(
                        readInt("Please choose your profession 1-" + Profession.values().length));
                return profession;
            } catch (IllegalArgumentException e) {
                System.out.println("That is not a profession, please try again.");
            }
        }
    }

    private Profession professionSwitch(int professionChoice) {
        switch (professionChoice) {
            case 1:
                return Profession.DOCTOR;
            case 2:
                return Profession.PHARMACIST;
            case 3:
                return Profession.PHYSIOTHERAPIST;
            case 4:
                return Profession.DENTIST;
            default:
                throw new IllegalArgumentException("Invalid profession choice: " + professionChoice);
        }
    }

    private void loadPatients() {
        try {
            service.load();
        } catch (UncheckedIOException e) {
            System.out.println("Warning: " + e.getMessage() + " - starting with an empty list.");
        }
    }

    private Patient readPatient() {
        String firstName = readLine("Enter the new patient's first name: ");
        String lastName = readLine("Enter the patient's last name: ");
        LocalDate birthDate = readBirthDate("Enter the new patient's date of birth (dd-mm-yyyy): ");
        double weight = readDouble("Enter the new patient's weight (kg): ");
        double height = readDouble("Enter the new patient's height (m): ");
        String address = readLine("Enter the patient's address: ");

        Patient.Specialists specialists = new Patient.Specialists(
                readLine("Enter the patient's doctor: "),
                readLine("Enter the patient's pharmacist: "),
                readLine("Enter the patient's physiotherapist: "),
                readLine("Enter the patient's dentist: "));

        return new Patient(firstName, lastName, birthDate, weight, height, address, specialists);
    }

    private void printPatients(Profession profession) {
        Set<PatientField> viewableFields = FieldPermissions.getViewableFields(profession);
        for (Patient p : service.getAll()) {
            for (PatientField field : viewableFields) {
                System.out.println(field.label + ": " + fieldValue(p, field));
            }
            System.out.println("----------");
        }
    }

    private String fieldValue(Patient patient, PatientField patientField) {
        return switch (patientField) {
            case FIRST_NAME -> patient.firstName();
            case LAST_NAME -> patient.lastName();
            case DATE_OF_BIRTH -> String.valueOf(patient.birthDate());
            case WEIGHT -> String.valueOf(patient.weight());
            case HEIGHT -> String.valueOf(patient.height());
            case ADDRESS -> patient.address();
        };
    }

    private void savePatients() {
        try {
            service.save();
        } catch (UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
        }
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
        service.update(oldPatient, updatedPatient, patientField);
        savePatients();

        System.out.println("Patient Updated");
    }

    private Patient choosePatient() {
        List<Patient> possiblePatients = service.getAll(); 
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
        };
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