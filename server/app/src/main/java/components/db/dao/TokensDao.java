package components.db.dao;

import components.db.HibernateUtil;
import components.db.models.Token;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;


// TODO: NEVER throw RuntimeException, only checked exceptions!
public class TokensDao {
    private static final Logger logger = Logger.getLogger(TokensDao.class.getName());

    public void save(Token token) {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            // save user
            session.persist(token);
            transaction.commit();
            logger.info("Token: " + token + " saved in db");
        }
        catch (Exception err) {
            assert transaction != null;
            transaction.rollback();
            throw new RuntimeException(err);
        }
        finally {
            session.close();
        }
    }

    public boolean deleteByUserId(int userId) {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();

            Optional<Token> token = findByUserId(userId);
            boolean removed = false;

            if (token.isPresent()) {
                session.remove(token.get());
                removed = true;
            }

            transaction.commit();
            return removed;
        }
        catch (Exception err) {
            assert transaction != null;
            transaction.rollback();
            throw new RuntimeException(err);
        }
        finally {
            session.close();
        }
    }

    public Optional<Token> findByUserId(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // SELECT u FROM User u WHERE u.email = :email
            String queryString = "SELECT t FROM Token t WHERE t.user.id = :userId";
            Query<Token> query = session.createQuery(queryString, Token.class);
            query.setParameter("userId", userId);

            Token token = query.uniqueResult();

            if (token != null) {
                logger.info("Found token: " + token);
                return Optional.of(token);
            }
            else {
                logger.info("No token associated with user id " + userId + " exists");
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new RuntimeException(err);
        }
    }

    public boolean existsByUserId(int userId) {
        return findByUserId(userId).isPresent();
    }
}
