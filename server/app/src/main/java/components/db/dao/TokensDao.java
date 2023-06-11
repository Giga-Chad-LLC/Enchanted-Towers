package components.db.dao;

import components.db.HibernateUtil;
import components.db.models.Token;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;

public class TokensDao {
    private static final Logger logger = Logger.getLogger(TokensDao.class.getName());

    public void save(Token token) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
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
    }

    public Optional<Token> findByUserId(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT t FROM Token t WHERE t.user_id = :userId";
            Query<Token> query = session.createQuery(queryString, Token.class);
            query.setParameter("userId", userId);

            Token token = query.uniqueResult();

            if (token != null) {
                logger.info("Found token: " + token);
                return Optional.of(token);
            }
            else {
                logger.info("No token exists for associated with user id " + userId);
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
