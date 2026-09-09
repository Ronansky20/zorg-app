import java.util.ArrayList;
import java.util.List;

public class App {

    record Person(int id, String firstName, String lastName) {}

    List<Person> users = new ArrayList<>();

    void run() {
        users.add(new Person(1, "Jane", "Doe"));
        System.out.println(users.get(0).id);
    }
    
    public static void main(String[] args){
        new App().run();
    }
}
