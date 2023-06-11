package components.db.models;

import jakarta.persistence.*;
import org.checkerframework.checker.nullness.qual.NonNull;

@Entity
@Table(name = "tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Integer userId;

    @Column(name="token")
    private String token;


    Token() {}

    Token(int userId, @NonNull String token) {
        this.userId = userId;
        this.token = token;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "Token[id=" + id + ", userId=" + userId + ", token=" + token + "]";
    }
}
