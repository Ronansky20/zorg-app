import java.util.ArrayList;
import java.util.List;

public class App {

    record Person(int id, String firstName, String lastName) {}
    
    public static void main(String[] args){
        List<Person> users = new ArrayList<>();
        users.add(new Person(1, "Jane", "Doe"));
    }
}
