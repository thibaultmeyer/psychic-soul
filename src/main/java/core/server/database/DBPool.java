package core.server.database;

import core.Settings;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * The role of the DBPool class is to provide an easy-to-use access to
 * the SQL database connections.
 *
 * @author Thibault Meyer
 * @version 1.0.0
 * @since 1.0.0
 */
public class DBPool {

    /**
     * Logger.
     *
     * @since 1.0.0
     */
    private static final Logger LOG = LoggerFactory.getLogger(DBPool.class.getName());

    /**
     * The data source.
     *
     * @see javax.sql.DataSource
     * @since 1.0.0
     */
    private DataSource dataSource;

    /**
     * Default constructor.
     *
     * @since 1.0.0
     */
    private DBPool() {
        try {
            Class.forName(Settings.databaseDriver);
            final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(Settings.databaseUrl, Settings.databaseUsername, Settings.databasePassword);
            final PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
            final ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
            poolableConnectionFactory.setPool(connectionPool);
            this.dataSource = new PoolingDataSource<>(connectionPool);
        } catch (ClassNotFoundException ignore) {
            LOG.error("Can't load driver {}", Settings.databaseDriver);
        }
    }

    /**
     * Get the existing instance of the {@code DBPool} class.
     *
     * @return Existing instance of the {@code DBPool} class
     * @since 1.0.0
     */
    public static DBPool getInstance() {
        return DBPoolSingletonHolder.instance;
    }

    /**
     * Check if the DBMS is online.
     *
     * @return {@code true} if the DBMS is online, otherwise, {@code false}
     * @since 1.0.0
     */
    public boolean isOnline() {
        try {
            Connection testConn = this.dataSource.getConnection();
            testConn.close();
            return true;
        } catch (SQLException e) {
            String errInfo;
            switch (e.getErrorCode()) {
                case 0:
                    errInfo = "Cannot connect to server";
                    break;
                default:
                    errInfo = e.getMessage();
                    break;
            }
            LOG.error("Something goes wrong with your DBMS: {}", errInfo);
            return false;
        }
    }

    /**
     * Get a new SQL connection from the pool. This connection must
     * be explicitly closed. This method return {@code null} in case
     * or error.
     *
     * @return A new SQL connection
     * @since 1.0.0
     */
    public Connection getSQLConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            LOG.error("Can't get JDBC connection from pool", e);
        }
        return null;
    }

    /**
     * DBPool singleton Holder.
     *
     * @author Thibault Meyer
     * @version 1.0.0
     * @since 1.0.0
     */
    private static class DBPoolSingletonHolder {
        private final static DBPool instance = new DBPool();
    }
}
