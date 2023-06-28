package components.db.dao.game;

import components.db.HibernateUtil;
import components.db.models.game.PositionModel;
import jakarta.persistence.RollbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;

public class PositionModelsDao {
    private static final Logger logger = Logger.getLogger(PositionModelsDao.class.getName());

    public void save(PositionModel position) throws HibernateException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // save user
            session.persist(position);
            transaction.commit();
            logger.info("Position: " + position + " saved in db");
        }
        catch (RollbackException err) {
            assert transaction != null;
            transaction.rollback();
            throw err;
        }
    }

    public Optional<PositionModel> findByTowerId(int towerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT p FROM PositionModel p WHERE p.tower.id = :towerId";
            Query<PositionModel> query = session.createQuery(queryString, PositionModel.class);
            query.setParameter("towerId", towerId);

            PositionModel position = query.uniqueResult();

            if (position != null) {
                logger.info("Found position: " + position);
                return Optional.of(position);
            }
            else {
                logger.info("No position associated with tower id " + towerId + " exists");
                return Optional.empty();
            }
        }
    }
}
