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

    void run() {
        // users.add(new Person("Jane", "Doe", 42, 50, 170, 22.5, "123 Main St"));
        // users.add(new Person("John", "Doe",42, 42, 42, 22.5, "456 Elm St"));
        // users.add(new Person("Nomen", "Nescio",42, 42, 42, 22.5, "678 Elm St"));
        // users.add(new Person("Gnu", "Lu",42, 42, 42, 22.5, "890 Elm St"));
        // users.add(new Person("Joe", "Blogs",42, 42, 42, 22.5, "890 Elm St"));
        // users.add(new Person("Alan", "Smithee",42, 42, 42, 22.5, "890 Elm St"));

        System.out.println("Enter the new patients first name: ");
        String personFirstName = scan.nextLine();
        System.out.println("Enter the patients last name: ");
        String personLastName = scan.nextLine();
        System.out.println("Enter the new patients age: ");
        int personAge = Integer.parseInt(scan.nextLine().trim());
        System.out.println("Enter the new patients weight: ");
        double personWeight = Double.parseDouble(scan.nextLine().trim());
        System.out.println("Enter the new patients height: ");
        double personHeight = Double.parseDouble(scan.nextLine().trim());
        System.out.println("Enter the patients adress: ");
        String personAdress = scan.nextLine();

        double personBmi = personWeight / (personHeight * personHeight);

        users.add(new Person(personFirstName, personLastName, personAge, personWeight, personHeight, personBmi, personAdress));
        
        for (Person person : users) {
            System.out.println((person.firstName() + " age: " + person.age() + " weight: " + person.weight() + " persons address: " + person.address()) + " Persons BMI: " + person.bmi());
        }
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
