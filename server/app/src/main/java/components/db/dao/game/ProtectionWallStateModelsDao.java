package components.db.dao.game;

import components.db.HibernateUtil;
import components.db.models.game.PositionModel;
import components.db.models.game.ProtectionWallStateModel;
import jakarta.persistence.RollbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Optional;
import java.util.logging.Logger;

public class ProtectionWallStateModelsDao {
    private static final Logger logger = Logger.getLogger(ProtectionWallStateModelsDao.class.getName());

    public void save(ProtectionWallStateModel wallState) throws HibernateException {
        Transaction transaction = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            transaction = session.beginTransaction();
            // save user
            session.persist(wallState);
            transaction.commit();
            logger.info("Wall state: " + wallState + " saved in db");
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

    public Optional<ProtectionWallStateModel> findByProtectionWallId(int protectionWallId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String queryString = "SELECT s FROM ProtectionWallStateModel s WHERE s.protectionWall.id = :protectionWallId";
            Query<ProtectionWallStateModel> query = session.createQuery(queryString, ProtectionWallStateModel.class);
            query.setParameter("protectionWallId", protectionWallId);

            ProtectionWallStateModel wallState = query.uniqueResult();

            if (wallState != null) {
                logger.info("Found wall state: " + wallState);
                return Optional.of(wallState);
            }
            else {
                logger.info("No state associated with protection wall id " + protectionWallId + " exists");
                return Optional.empty();
            }
        }
    }
}
