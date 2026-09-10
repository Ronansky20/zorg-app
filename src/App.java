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

public class App {

    record Person(
        int id, 
        String firstName, 
        String lastName, 
        int age, 
        double weight, 
        double length, 
        double bmi, 
        String address) {}

    List<Person> users = new ArrayList<>();

    void run() {
        users.add(new Person(0, "Jane", "Doe", 42, 50, 170, 22.5, "123 Main St"));
        users.add(new Person(1, "John", "Doe",42, 42, 42, 22.5, "456 Elm St"));
        users.add(new Person(2, "John", "Doe",42, 42, 42, 22.5, "456 Elm St"));


        for (Person person : users) {
            System.out.println((person.id() + ": " + person.firstName() + " age: " + person.age() + " weight: " + person.weight()));
        }
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
