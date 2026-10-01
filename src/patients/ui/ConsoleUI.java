package patients.ui;

import patients.model.Patient;
import patients.model.Profession;
import patients.model.Action;

import patients.service.PatientService;
import patients.service.ProfessionPermissions;

import java.io.UncheckedIOException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class ConsoleUI {

    private final PatientService service;
    private final Scanner scan = new Scanner(System.in);

    public ConsoleUI(PatientService service) {
        this.service = service;
    }

    public void run() {

        System.out.println("Hello, please choose your profession");
        Profession profession = askProfession();
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

            if (actionChoice < 0 || actionChoice > possibleActions.size() ) {
                System.out.println("Invalid choice. Try again.");
                continue;
            }

            if (actionChoice == 0) {
                return;
            }

            int actionNumber = actionChoice - 1;

            Action chosenAction = possibleActions.get(actionNumber);
            switch (chosenAction) {
                case VIEW_LIST:
                    printPatients();
                    break;
                case ADD_PATIENT:
                    service.add(readPatient()); 
                    break;
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
                Profession profession = professionSwitch(readInt("Please choose your profession 1-" + Profession.values().length));
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
        int age = readInt("Enter the new patient's age: ");
        double weight = readDouble("Enter the new patient's weight (kg): ");
        double height = readDouble("Enter the new patient's height (m): ");
        String address = readLine("Enter the patient's address: ");
    
        Patient.Specialists specialists = new Patient.Specialists(
                readLine("Enter the patient's doctor: "),
                readLine("Enter the patient's pharmacist: "),
                readLine("Enter the patient's physiotherapist: "),
                readLine("Enter the patient's dentist: "));
    
        return new Patient(firstName, lastName, age, weight, height, address, specialists);
    }

    private void printPatients() {
        for (Patient p : service.getAll()) {
            System.out.printf("%s %s, age: %d, weight: %.1f kg, address: %s, BMI: %.1f%n",
                    p.firstName(), p.lastName(), p.age(), p.weight(), p.address(), p.bmi());
        }
    }

    private void savePatients() {
        try {
            service.save();
        } catch (UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
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

    private double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readLine(prompt).replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("That's not a number, try again.");
            }
        }
    }

    // private boolean askYesNo(String prompt) {
    //     return readLine(prompt).equalsIgnoreCase("y");
    // }
}