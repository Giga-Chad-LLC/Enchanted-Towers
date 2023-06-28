package components.db.dao.game;

import components.db.HibernateUtil;
import components.db.models.game.PositionModel;
import components.db.models.game.ProtectionWallModel;
import jakarta.persistence.RollbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;

public class ProtectionWallModelsDao {
    private static final Logger logger = Logger.getLogger(ProtectionWallModelsDao.class.getName());

    public void save(ProtectionWallModel protectionWall) throws HibernateException {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            // save user
            session.persist(protectionWall);
            transaction.commit();
            logger.info("Protection wall: " + protectionWall + " saved in db");
        }
        catch (RollbackException err) {
            assert transaction != null;
            transaction.rollback();
            throw err;
        }
        finally {
            session.close();
        }
    }

    public Optional<ProtectionWallModel> findByTowerId(int towerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT w FROM ProtectionWallModel w WHERE w.tower.id = :towerId";
            Query<ProtectionWallModel> query = session.createQuery(queryString, ProtectionWallModel.class);
            query.setParameter("towerId", towerId);

            ProtectionWallModel protectionWall = query.uniqueResult();

            if (protectionWall != null) {
                logger.info("Found protection wall: " + protectionWall);
                return Optional.of(protectionWall);
            }
            else {
                logger.info("No protection wall associated with tower id " + towerId + " exists");
                return Optional.empty();
            }
        }
    }
}
