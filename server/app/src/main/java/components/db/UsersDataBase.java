package components.db;

import org.hibernate.Session;
import org.hibernate.query.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class UsersDataBase {
    private final Logger logger = Logger.getLogger(UsersDataBase.class.getName());

    public void saveUserRecord (Session session, User user) {
        session.beginTransaction();
        session.save(user);
        session.getTransaction().commit();
        logger.log(Level.INFO, "Next User is saved");
    }

    public List<User> fetchUserRecordByEmail (Session session, String email) {
        Query query = session.createQuery("SELECT * FROM users WHERE email = " + email);
        List users = query.list();
        return users;
    }

}
