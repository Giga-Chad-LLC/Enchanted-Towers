package components.db;
import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "name", length = 50)
    private String name;
    @Column(name = "hashed_password", length = 100)
    private String hashedPassword;
    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "jwt_access", length = 100)
    private String jwtAccess;
    @Column(name = "jwt_refresh", length = 100)
    private String jwtRefresh;

    public Integer getId() {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword (String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail (String email) {
        this.email = email;
    }

    public String getJwtAccess() {
        return jwtAccess;
    }

    public void setJwtAccess (String jwtAccess) {
        this.jwtAccess = jwtAccess;
    }

    public String getJwtRefresh() {
        return jwtRefresh;
    }

    public void setJwtRefresh (String jwtRefresh) {
        this.jwtRefresh = jwtRefresh;
    }

    public String getName() {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }
}
