import java.util.ArrayList;
import java.util.List;

public class App {

    record Person(int id, String firstName, String lastName) {}

    List<Person> users = new ArrayList<>();

    void run() {
        users.add(new Person(0, "Jane", "Doe"));
        users.add(new Person(1, "John", "Doe"));

        for (Person person : users) {
            System.out.println((person.id() + ": " + person.firstName()));
        }
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
