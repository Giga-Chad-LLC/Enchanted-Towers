package components.db.dao;

import components.db.HibernateUtil;
import components.db.models.User;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;



public class UsersDao {
    public record UserDto(@NonNull String email, @NonNull String username, @NonNull String password) {}

    private static final Logger logger = Logger.getLogger(UsersDao.class.getName());

    public void save(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // save user
            session.persist(user);
            transaction.commit();
            logger.info("User: " + user + " saved in db");
        }
        catch (Exception err) {
            assert transaction != null;
            transaction.rollback();
            throw new RuntimeException(err);
        }
    }

    public void updateUserRecord (User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        User newUser = session.get(User.class, user.getId());//?
        newUser = user;
        session.merge(newUser);
        transaction.commit();
        session.close();
        logger.log(Level.INFO, "User is updated");
    }

    public Optional<User> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT u FROM User u WHERE u.email = :email";
            Query<User> query = session.createQuery(queryString, User.class);
            query.setParameter("email", email);

            User user = query.uniqueResult();

            if (user != null) {
                logger.info("Found user: " + user);
                return Optional.of(user);
            }
            else {
                logger.info("No user with email '" + email + "' exists");
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public Optional<User> findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT u FROM User u WHERE u.username = :username";
            Query<User> query = session.createQuery(queryString, User.class);
            query.setParameter("username", username);

            User user = query.uniqueResult();

            if (user != null) {
                logger.info("Found user: " + user);
                return Optional.of(user);
            }
            else {
                logger.info("No user with username '" + username + "' exists");
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public boolean existsByEmail(String email) {
        Optional<User> user = findByEmail(email);
        return user.isPresent();
    }

    public boolean existsByUsername(String username) {
        Optional<User> user = findByUsername(username);
        return user.isPresent();
    }
}
