package components.db.dao.game;

import components.db.HibernateUtil;
import components.db.models.game.PositionModel;
import components.db.models.game.TowerModel;
import jakarta.persistence.RollbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.logging.Logger;

public class TowerModelsDao {
    private static final Logger logger = Logger.getLogger(TowerModelsDao.class.getName());

    public void save(TowerModel tower) throws HibernateException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            // save user
            session.persist(tower);
            transaction.commit();
            logger.info("Tower:\n" + tower + "\nsaved in db");
        }
        catch (RollbackException err) {
            assert transaction != null;
            transaction.rollback();
            throw err;
        }
    }

    public List<TowerModel> getAllTowers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Retrieve all towers using HQL query
            String hql = "FROM TowerModel";
            return session.createQuery(hql, TowerModel.class).getResultList();
        }
    }

}
