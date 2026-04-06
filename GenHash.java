import org.mindrot.jbcrypt.BCrypt;
public class GenHash {
    public static void main(String[] args) {
        String[] passwords = {"Admin@123", "Staff@123", "Student@123"};
        for (String pw : passwords) {
            System.out.println(pw + "=" + BCrypt.hashpw(pw, BCrypt.gensalt(12)));
        }
    }
}
