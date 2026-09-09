public class App {

    record Person(int id, String firstName, String lastName) {}
    
    public static void main(String[] args){
        Person user1 = new Person(1, "Jane", "Doe");

        System.out.println(user1);

        System.out.println(user1.id);
    }
}
