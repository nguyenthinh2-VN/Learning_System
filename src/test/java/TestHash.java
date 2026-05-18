import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$wE731Tclb.E0n85sU1YhluFj1L5b4P9B2B6L/1zO8.m6m5t8Cq/J2";
        System.out.println("Matches password123? " + encoder.matches("password123", hash));
        System.out.println("Matches 123456? " + encoder.matches("123456", hash));
        System.out.println("New hash for password123: " + encoder.encode("password123"));
    }
}
