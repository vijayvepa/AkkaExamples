package common;

import akka.japi.function.Function;
import akka.projection.jdbc.JdbcSession;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Hibernate based implementation of Akka Projection JdbcSession. This class is required when
 * building a JdbcProjection. It provides the means for the projection to start a transaction
 * whenever a new event envelope is to be delivered to the user defined projection handler.
 *
 * <p>The JdbcProjection will use the transaction manager to initiate a transaction to commit the
 * envelope offset. Then used in combination with JdbcProjection.exactlyOnce method, the user
 * handler code and the offset store operation participates on the same transaction.
 */
public class JpaSession extends DefaultTransactionDefinition implements JdbcSession {

  private final JpaTransactionManager transactionManager;
  private final TransactionStatus transactionStatus;

  public JpaSession(JpaTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
    this.transactionStatus = transactionManager.getTransaction(this);
  }

  public EntityManager entityManager() {
    return EntityManagerFactoryUtils.getTransactionalEntityManager(
        Objects.requireNonNull(transactionManager.getEntityManagerFactory()));
  }

  @SuppressWarnings({
      "resource" //causes RuntimeException
  })
  @Override
  public <Result> Result withConnection(Function<Connection, Result> func) {
    EntityManager entityManager = entityManager();
    Session hibernateSession = ((Session) entityManager.getDelegate());
    return hibernateSession.doReturningWork(
        connection -> {
          try {
            return func.apply(connection);
          } catch (SQLException e) {
            throw e;
          } catch (Exception e) {
            throw new SQLException(e);
          }
        });
  }

  @SuppressWarnings({
      "resource" //causes RuntimeException
  })
  @Override
  public void commit() {
    if (entityManager().isOpen()) transactionManager.commit(transactionStatus);
  }

  @SuppressWarnings({
      "resource" //causes RuntimeException
  })
  @Override
  public void rollback() {
    if (entityManager().isOpen()) transactionManager.rollback(transactionStatus);
  }

  @Override
  public void close() {
    entityManager().close();
  }
}
