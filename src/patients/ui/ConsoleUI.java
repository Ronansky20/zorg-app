package patients.ui;

import patients.model.Patient;
import patients.service.PatientService;

import java.io.UncheckedIOException;
import java.util.Scanner;

public class ConsoleUI {

    private final PatientService service;
    private final Scanner scan = new Scanner(System.in);

    public ConsoleUI(PatientService service) {
        this.service = service;
    }

    public void run() {
        try {
            service.load();
        } catch (UncheckedIOException e) {
            System.out.println("Warning: " + e.getMessage() + " - starting with an empty list.");
        }

        System.out.println("Welcome to the patient management system!");

        do {
            service.add(readPatient());
        } while (askYesNo("Add another patient? (y/n) "));

        printPatients();

        try {
            service.save();
        } catch (UncheckedIOException e) {
            System.out.println("Error: " + e.getMessage());
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
                readLine("Enter the patient's apothecary: "),
                readLine("Enter the patient's physician: "),
                readLine("Enter the patient's dentist: "));
    
        return new Patient(firstName, lastName, age, weight, height, address, specialists);
    }

    private void printPatients() {
        for (Patient p : service.getAll()) {
            System.out.printf("%s %s, age: %d, weight: %.1f kg, address: %s, BMI: %.1f%n",
                    p.firstName(), p.lastName(), p.age(), p.weight(), p.address(), p.bmi());
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

    private boolean askYesNo(String prompt) {
        return readLine(prompt).equalsIgnoreCase("y");
    }
}