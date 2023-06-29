package components.db.dao;

import components.db.HibernateUtil;
import components.db.models.JwtToken;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;


// TODO: NEVER throw RuntimeException, only checked exceptions!
public class JwtTokensDao {
    private static final Logger logger = Logger.getLogger(JwtTokensDao.class.getName());

    public void save(JwtToken jwtToken) {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            // save user
            session.persist(jwtToken);
            transaction.commit();
            logger.info("JwtToken: " + jwtToken + " saved in db");
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

            Optional<JwtToken> token = findByUserId(userId);
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

    public Optional<JwtToken> findByUserId(int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // SELECT u FROM User u WHERE u.email = :email
            String queryString = "SELECT t FROM JwtToken t WHERE t.user.id = :userId";
            Query<JwtToken> query = session.createQuery(queryString, JwtToken.class);
            query.setParameter("userId", userId);

            JwtToken jwtToken = query.uniqueResult();

            if (jwtToken != null) {
                logger.info("Found jwtToken: " + jwtToken);
                return Optional.of(jwtToken);
            }
            else {
                logger.info("No jwtToken associated with user id " + userId + " exists");
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
