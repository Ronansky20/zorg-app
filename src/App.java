/* TODO: Currently I have to manually add the id, this should be automated. */
/* Way to go about it I think
    What I could probably do is make a radar, that checks for a response to a question, like name, age, bla bla bla.
    these get stored in a String, int etc.
    Then I insert these using users.add.
    I just need to check if I can give it variables.

    This makes it interactive.

    Still need to check for finding people. However I have the id, so I could probably print like a name.
    Then with the corresponding id, for them to find it easily.

    However they would have to remember id's. So I don't know why I added ID's

    I could probably remove that, but for now its whatever and keep it, maybe its useful down the line.
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class App {

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
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
