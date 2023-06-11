package components.db;

import components.db.models.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.logging.Level;
import java.util.logging.Logger;



public class UsersDataBase {
    private final Logger logger = Logger.getLogger(UsersDataBase.class.getName());

    public void saveUserRecord (User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.save(user);
        transaction.commit();
        session.close();
        logger.log(Level.INFO, "Next User is saved");
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

    public User findUserRecordByEmail (String email) {
        logger.log(Level.INFO, "Find Users in DataBase by email");
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        String queryString = "SELECT u FROM User u WHERE u.email = :email";
        Query<User> query = session.createQuery(queryString, User.class);
        query.setParameter("email", email);

        User user = query.uniqueResult();

        transaction.commit();
        session.close();

        return user;
    }

    public User findUserRecordByName (String name) {
        logger.log(Level.INFO, "Find Users in DataBase by name");
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        Query<User> query = session.createQuery("FROM User WHERE name = :name", User.class);
        query.setParameter("name", name);

        User user = query.uniqueResult();

        transaction.commit();
        session.close();

        return user;
    }

}
