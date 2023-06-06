package components.db;
import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "name")
    private String name;
    @Column(name = "hashed_password")
    private String hashedPassword;
    @Column(name = "email")
    private String email;
    @Column(name = "jwt_access")
    private String jwtAccess;
    @Column(name = "jwt_refresh")
    private String jwtRefresh;

    Long getId() {
        return id;
    }

    void setId (Long id) {
        this.id = id;
    }

    String getHashedPassword() {
        return hashedPassword;
    }

    void setHashedPassword (String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    String getEmail() {
        return email;
    }

    void setEmail (String email) {
        this.email = email;
    }

    String getJwtAccess() {
        return jwtAccess;
    }

    void setJwtAccess (String jwtAccess) {
        this.jwtAccess = jwtAccess;
    }

    String getJwtRefresh() {
        return jwtRefresh;
    }

    void setJwtRefresh (String jwtRefresh) {
        this.jwtRefresh = jwtRefresh;
    }

    String getName() {
        return name;
    }

    void setName (String name) {
        this.name = name;
    }
}
