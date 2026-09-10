import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

    private static final Path DATA_FILE = Paths.get("patients.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    Scanner scan = new Scanner(System.in);

    record Person( 
        String firstName, 
        String lastName, 
        int age, 
        double weight, 
        double height, 
        double bmi, 
        String address) {}

    List<Person> users = new ArrayList<>();

    Person readPerson() {
        System.out.println("Enter the new patient's first name: ");
        String firstName = scan.nextLine();
        System.out.println("Enter the patient's last name: ");
        String lastName = scan.nextLine();
        System.out.println("Enter the new patient's age: ");
        int age = Integer.parseInt(scan.nextLine().trim());
        System.out.println("Enter the new patient's weight (kg): ");
        double weight = Double.parseDouble(scan.nextLine().trim());
        System.out.println("Enter the new patient's height (m): ");
        double height = Double.parseDouble(scan.nextLine().trim());
        System.out.println("Enter the patient's address: ");
        String address = scan.nextLine();
    
        double bmi = weight / (height * height);
        return new Person(firstName, lastName, age, weight, height, bmi, address);
    }

    void run() {
        load();
        System.out.println("Welcome to the patient management system!");

        boolean addingMore = true;
        while (addingMore) {
            users.add(readPerson());
            System.out.println("Add another patient? (y/n) ");
            addingMore = scan.nextLine().trim().equalsIgnoreCase("y");
        }
        for (Person person : users) {
            System.out.println((person.firstName() + " age: " + person.age() + " weight: " + person.weight() + " persons address: " + person.address()) + " Persons BMI: " + person.bmi());
        }
        save();
    }

    private void save() {
        try (Writer writer = Files.newBufferedWriter(DATA_FILE)) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(DATA_FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(DATA_FILE)) {
            List<Person> loaded = gson.fromJson(reader,
                    new TypeToken<List<Person>>() {}.getType());
            if (loaded != null) {
                users.addAll(loaded);
            }
        } catch (IOException e) {
            System.out.println("Could not load: " + e.getMessage());
        }
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
