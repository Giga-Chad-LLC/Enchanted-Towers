package components.db.models;

import jakarta.persistence.*;
import org.checkerframework.checker.nullness.qual.NonNull;

@Entity
@Table(name = "game_session_tokens")
public class GameSessionToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name="token")
    private String token;


    public GameSessionToken() {}

    public GameSessionToken(User user, @NonNull String token) {
        this.user = user;
        this.token = token;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return user.getId();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "GameSessionToken[id=" + id + ", userId=" + user.getId() + ", token=" + token + "]";
    }
}
