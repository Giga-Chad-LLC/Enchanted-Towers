package components.db.dao;

import components.db.HibernateUtil;
import components.db.models.GameSessionToken;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;

public class GameSessionTokensDao {
    private static final Logger logger = Logger.getLogger(GameSessionTokensDao.class.getName());

    public void save(GameSessionToken sessionToken) throws HibernateException {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            // save user
            session.persist(sessionToken);
            transaction.commit();
            logger.info("sessionToken: " + sessionToken + " saved in db");
        }
        catch (Exception err) {
            assert transaction != null;
            transaction.rollback();
            throw new HibernateException(err);
        }
        finally {
            session.close();
        }
    }

    public void delete(GameSessionToken sessionToken) throws HibernateException {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            session.remove(sessionToken);
            transaction.commit();
        }
        catch (Exception err) {
            assert transaction != null;
            transaction.rollback();
            throw new HibernateException(err);
        }
        finally {
            session.close();
        }
    }

    public boolean deleteByUserId(int userId) throws HibernateException {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();

            Optional<GameSessionToken> token = findByUserId(userId);
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
            throw new HibernateException(err);
        }
        finally {
            session.close();
        }
    }

    public Optional<GameSessionToken> findByUserId(int userId) throws HibernateException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT t FROM GameSessionToken t WHERE t.user.id = :userId";
            Query<GameSessionToken> query = session.createQuery(queryString, GameSessionToken.class);
            query.setParameter("userId", userId);

            GameSessionToken sessionToken = query.uniqueResult();

            if (sessionToken != null) {
                logger.info("Found sessionToken: " + sessionToken);
                return Optional.of(sessionToken);
            }
            else {
                logger.info("No sessionToken associated with user id " + userId + " exists");
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new HibernateException(err);
        }
    }

    public Optional<GameSessionToken> findByToken(String token) throws HibernateException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT t FROM GameSessionToken t WHERE t.token = :token";
            Query<GameSessionToken> query = session.createQuery(queryString, GameSessionToken.class);
            query.setParameter("token", token);

            GameSessionToken sessionToken = query.uniqueResult();

            if (sessionToken != null) {
                logger.info("Found sessionToken: " + sessionToken);
                return Optional.of(sessionToken);
            }
            else {
                logger.info("No sessionToken associated with token '" + token + "' exists");
                return Optional.empty();
            }
        }
        catch (Exception err) {
            throw new HibernateException(err);
        }
    }

    public boolean existsByUserId(int userId) throws HibernateException {
        return findByUserId(userId).isPresent();
    }

    public boolean existsByToken(String token) throws HibernateException {
        return findByToken(token).isPresent();
    }
}


