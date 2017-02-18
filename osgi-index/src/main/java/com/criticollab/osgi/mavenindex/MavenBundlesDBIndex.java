package com.criticollab.osgi.mavenindex; /**
 * Created by ses on 2/3/17.
 */

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

class MavenBundlesDBIndex {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(MavenBundlesDBIndex.class);
    private final PreparedStatement mavenArtifactVersionLookup;
    private final PreparedStatement mavenArtifactVersionInserter;
    private final PreparedStatement lookupExportPackage;
    private final PreparedStatement hashQuery;
    private final PreparedStatement hashInsert;
    private final PreparedStatement mavenArtifactInserter;
    private final PreparedStatement mavenArtifactLookup;
    private PreparedStatement mavenResourceInserter;
    private PreparedStatement mavenResourceLookup;
    private PreparedStatement bundleInsert;
    private PreparedStatement insertExportPackage;
    private Connection connection;
    private LoadingCache<String, PreparedStatement> getterCache;
    private LoadingCache<String, PreparedStatement> setterCache;
    private PreparedStatement bundleLookup;

    public MavenBundlesDBIndex(String dbURL) throws SQLException {
        logger.info("init Bundles DB with jdbc url {}", dbURL);
        connection = DriverManager.getConnection(dbURL);
        createTables();
        hashQuery = connection.prepareStatement("SELECT hash FROM SHA256 WHERE RESOURCENAME=?");
        hashInsert = connection.prepareStatement("INSERT INTO sha256 (resourceName,hash)VALUES (?,?)");

        mavenArtifactInserter = connection.prepareStatement("INSERT INTO mavenArtifact (groupId,ArtifactId) VALUES (?,?)",
                Statement.RETURN_GENERATED_KEYS);
        mavenArtifactLookup = connection.prepareStatement("SELECT id FROM mavenArtifact WHERE groupId=? AND artifactId=?");

        mavenArtifactVersionInserter = connection.prepareStatement("INSERT INTO mavenArtifactVersion (mavenArtifact,mavenVersion) VALUES (?,?)",
                Statement.RETURN_GENERATED_KEYS);

        mavenArtifactVersionLookup = connection.prepareStatement("SELECT id FROM mavenArtifactVersion WHERE mavenArtifact=? AND mavenVersion=?");

        mavenResourceLookup = connection.prepareStatement("SELECT ID,CLASSIFIER FROM mavenResource WHERE MAVENARTIFACTVERSION=? AND (CLASSIFIER = ? OR CLASSIFIER IS NULL)");
        mavenResourceInserter = connection.prepareStatement("INSERT INTO mavenResource " +
                "(mavenArtifactVersion,classifier,lastModified,packaging,ext,sha1,resourceName) " +
                "VALUES (?,?,?,?,?,?,?)", RETURN_GENERATED_KEYS);

        getterCache = buildLoaderCache(buildGetterStatementLoader());
        setterCache = buildLoaderCache(buildSetterStatementLoader());
        bundleInsert = connection.prepareStatement("INSERT INTO bundle (mavenResourceId,bsn,bundleVersion) VALUES (?,?,?)", RETURN_GENERATED_KEYS);
        bundleLookup = connection.prepareStatement("SELECT ID FROM BUNDLE WHERE MAVENRESOURCEID = ? AND bsn = ? AND BUNDLEVERSION = ?");
        insertExportPackage = connection.prepareStatement("INSERT INTO bundleExportPackage " +
                "(BUNDLEID,PACKAGEID,version,attrs) VALUES (?,?,?,?)",RETURN_GENERATED_KEYS);
        lookupExportPackage = connection.prepareStatement(
                "SELECT ID FROM BUNDLEEXPORTPACKAGE WHERE BUNDLEID = ? AND PACKAGEID = ?");

    }

    public static void main(String[] args) throws SQLException, IOException {
        File cacheDir = new File("target/central-cache");
        File db = new File(cacheDir, "sha256-derby");
        String dbURL = "jdbc:derby:" + db.getAbsolutePath() + ";create=true";
        new MavenBundlesDBIndex(dbURL).doMain(args);
    }

    public Connection getConnection() {
        return connection;
    }

    private LoadingCache<String, PreparedStatement> buildLoaderCache(CacheLoader<String, PreparedStatement> loader) {
        return CacheBuilder.newBuilder().
                <String, PreparedStatement>removalListener(notification -> {
                    try {
                        PreparedStatement value = java.util.Objects.requireNonNull(notification.getValue());
                        value.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).maximumSize(31).build(loader);
    }

    private CacheLoader<String, PreparedStatement> buildGetterStatementLoader() {
        return new CacheLoader<String, PreparedStatement>() {
            @Override
            public PreparedStatement load(@Nonnull String tableName) throws Exception {
                return connection.prepareStatement("SELECT id FROM " + tableName + " WHERE  value = ?");
            }
        };
    }

    private CacheLoader<String, PreparedStatement> buildSetterStatementLoader() {
        return new CacheLoader<String, PreparedStatement>() {
            @Override
            public PreparedStatement load(@Nonnull String tableName) throws Exception {
                String sqlStr = "INSERT INTO " + tableName + " (value)  values (?)";
                return connection.prepareStatement(sqlStr, RETURN_GENERATED_KEYS);
            }
        };
    }

    private void createTables() throws SQLException {
        createTableIfNeeded("sha256",
                "resourceName  VARCHAR(256) PRIMARY KEY",
                "hash CHAR(64) NOT NULL");
        createLookupTableIfNeeded("maven_group");
        createLookupTableIfNeeded("maven_artifactId");
        createLookupTableIfNeeded("maven_classifier");

        createTableIfNeeded("mavenArtifact",
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "groupId int NOT NULL REFERENCES maven_group(id)",
                "artifactId int NOT NULL REFERENCES maven_artifactId (id)," +
                        "CONSTRAINT UNIQ_MAVA UNIQUE (groupId,artifactId)"
        );
        createTableIfNeeded("mavenArtifactVersion",
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "mavenArtifact INT NOT NULL REFERENCES mavenArtifact (id)",
                "mavenVersion VARCHAR(256) NOT NULL",
                "CONSTRAINT UNIQ_MAV UNIQUE (mavenArtifact,mavenVersion)"
        );
        createTableIfNeeded("mavenResource",
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "mavenArtifactVersion INT NOT NULL REFERENCES mavenArtifactVersion (id)",
                "classifier INT REFERENCES maven_classifier(id)",
                "lastModified TIMESTAMP",
                "packaging VARCHAR(256)",
                "ext VARCHAR(256)",
                "sha1 VARCHAR(256)",
                "resourceName VARCHAR(1024)",
                "CONSTRAINT UNIQ_MR UNIQUE (mavenArtifactVersion,classifier)"
        );
        createTableIfNeeded("bundle",
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "mavenResourceId INT NOT NULL REFERENCES mavenResource (id)",
                "bsn VARCHAR(8192) NOT NULL",
                "bundleVersion VARCHAR(256) NOT NULL",

                "CONSTRAINT UNIQ_BUN UNIQUE (mavenResourceId,bsn,bundleVersion)"
        );

        createLookupTableIfNeeded("package");
        createTableIfNeeded("bundleExportPackage",
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "bundleId INT NOT NULL REFERENCES bundle (id)",
                "packageId INT NOT NULL REFERENCES package (id)",
                "version VARCHAR(256) NOT NULL",
                "attrs VARCHAR(8192)",
                "CONSTRAINT UNIQ_BEX UNIQUE (bundleId,packageId,version)"
        );

        try {
            connection.createStatement().execute("CREATE VIEW PRETTY AS SELECT MAVENRESOURCE.ID,MAVEN_GROUP.VALUE AS \"groupId\",\n" +
                    "  MAVEN_ARTIFACTID.VALUE AS name, MAVENARTIFACTVERSION.MAVENVERSION AS version,\n" +
                    "  MAVEN_CLASSIFIER.VALUE AS classifier, LASTMODIFIED, PACKAGING,EXT,SHA1\n" +
                    "\n" +
                    "FROM MAVENRESOURCE  JOIN\n" +
                    "  MAVENARTIFACTVERSION ON MAVENRESOURCE.MAVENARTIFACTVERSION = MAVENARTIFACTVERSION.ID JOIN\n" +
                    "  MAVENARTIFACT ON MAVENARTIFACTVERSION.MAVENARTIFACT = MAVENARTIFACT.ID JOIN\n" +
                    "  MAVEN_GROUP ON MAVENARTIFACT.GROUPID = MAVEN_GROUP.ID JOIN\n" +
                    "  MAVEN_ARTIFACTID ON MAVENARTIFACT.ARTIFACTID = MAVEN_ARTIFACTID.ID LEFT JOIN\n" +
                    "  MAVEN_CLASSIFIER ON MAVENRESOURCE.CLASSIFIER = MAVEN_CLASSIFIER.ID\n");
        } catch (SQLException e) {
        }
    }

    private void createLookupTableIfNeeded(String name) throws SQLException {
        createTableIfNeeded(name,
                "id  INT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                "value VARCHAR(1024) UNIQUE NOT NULL");
    }

    public Integer lookupBundleExportPackage(int bundleId, int packageId) throws SQLException {
        lookupExportPackage.setInt(1,bundleId);
        lookupExportPackage.setInt(2,packageId);
        return getId(this.lookupExportPackage);


    }

    private Integer getId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return null;
            }
        }
    }

    public Integer insertBundleExportPackage(int bundleId, int packageId, String version, String attrs) throws
                                                                                                        SQLException {
        insertExportPackage.setInt(1,bundleId);
        insertExportPackage.setInt(2,packageId);
        insertExportPackage.setString(3,version);
        insertExportPackage.setString(4,attrs);
        insertExportPackage.executeUpdate();
        return getGeneratedKey(this.insertExportPackage);

    }

    private Integer getGeneratedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.getGeneratedKeys()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return null;
            }
        }
    }

    public Integer lookupBundle(int resourceId, String bsn, String version) throws SQLException {
        bundleLookup.setInt(1, resourceId);
        bundleLookup.setString(2, bsn);
        bundleLookup.setString(3, version);
        return getId(bundleLookup);
    }

    public Integer insertBundle(int resourceId, String bsn, String version) throws SQLException {
        bundleInsert.setInt(1, resourceId);
        bundleInsert.setString(2, bsn);
        bundleInsert.setString(3, version);
        bundleInsert.executeUpdate();

        return getGeneratedKey(bundleInsert);
    }

    public Integer lookupMavenResource(int artifactVersionId, Integer classifier) throws SQLException {
        mavenResourceLookup.setInt(1, artifactVersionId);
        if (classifier != null) {
            mavenResourceLookup.setInt(2, classifier);
        } else {
            mavenResourceLookup.setNull(2, Types.INTEGER);
        }
        try (ResultSet rs = mavenResourceLookup.executeQuery()) {
            while (rs.next()) {
                int c2 = rs.getInt(2);
                if (rs.wasNull()) {
                    if (classifier == null) {
                        return rs.getInt(1);
                    }
                } else {
                    if (classifier != null && classifier == c2) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return null;
    }

    public Integer addMavenResource(int artifactVersionId, Integer classifier, long lastModified, String packaging,
                                    String ext, String sha1, String resourceName) throws SQLException {
        mavenResourceInserter.setInt(1, artifactVersionId);
        if (classifier != null) {
            mavenResourceInserter.setInt(2, classifier);
        } else {
            mavenResourceInserter.setNull(2, Types.INTEGER);
        }
        Timestamp timestamp = new Timestamp(lastModified);
        mavenResourceInserter.setTimestamp(3, timestamp);
        mavenResourceInserter.setString(4, packaging);
        mavenResourceInserter.setString(5, ext);
        mavenResourceInserter.setString(6, sha1);
        mavenResourceInserter.setString(7, resourceName);
        int i = mavenResourceInserter.executeUpdate();
        return getGeneratedKey(mavenResourceInserter);

    }

    public Integer getOrCreateMavenArtifactId(int groupNameId, int artifactNameId) throws SQLException {
        Integer mavenArtifactId = null;
        mavenArtifactLookup.setInt(1, groupNameId);
        mavenArtifactLookup.setInt(2, artifactNameId);
        try (ResultSet resultSet = mavenArtifactLookup.executeQuery()) {
            if (resultSet.next()) {
                mavenArtifactId = resultSet.getInt(1);
            }
        }
        if (mavenArtifactId == null) {
            mavenArtifactInserter.setInt(1, groupNameId);
            mavenArtifactInserter.setInt(2, artifactNameId);
            mavenArtifactInserter.execute();
            try (ResultSet generatedKeys = mavenArtifactInserter.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mavenArtifactId = generatedKeys.getInt(1);
                }
            }
        }
        return mavenArtifactId;
    }

    public Integer getOrCreateMavenArtifactVersionId(int artifactId, String version) throws SQLException {
        Integer mavID = null;
        mavenArtifactVersionLookup.setInt(1, artifactId);
        mavenArtifactVersionLookup.setString(2, version);
        try (ResultSet resultSet = mavenArtifactVersionLookup.executeQuery()) {
            if (resultSet.next()) {
                mavID = resultSet.getInt(1);
            }
        }
        if (mavID == null) {
            mavenArtifactVersionInserter.setInt(1, artifactId);
            mavenArtifactVersionInserter.setString(2, version);
            mavenArtifactVersionInserter.execute();
            try (ResultSet generatedKeys = mavenArtifactVersionInserter.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mavID = generatedKeys.getInt(1);
                }
            }
        }
        return mavID;
    }

    public OptionalInt getId(String tableName, String key) throws ExecutionException, SQLException {
        PreparedStatement getter = getterCache.get(tableName);
        getter.setString(1, key);
        try (ResultSet resultSet = getter.executeQuery()) {
            if (resultSet.next()) {
                return OptionalInt.of(resultSet.getInt(1));
            } else {
                return OptionalInt.empty();
            }
        }
    }

    public int getOrCreateId(String tableName, String key) throws SQLException, ExecutionException {
        boolean saveAuto = connection.getAutoCommit();
        setAutoCommit(false);
        try {
            int id = -1;
            PreparedStatement getter = getterCache.get(tableName);
            getter.setString(1, key);
            try (ResultSet resultSet = getter.executeQuery()) {
                if (resultSet.next()) {
                    id = resultSet.getInt(1);
                }
            }
            if (id == -1) {
                PreparedStatement setter = setterCache.get(tableName);
                setter.setString(1, key);
                setter.executeUpdate();
                try (ResultSet generatedKeys = setter.getGeneratedKeys()) {
                    generatedKeys.next();
                    id = generatedKeys.getInt(1);
                }
            }
            if (saveAuto) {
                commit();
            }
            return id;
        } catch (Exception e) {
            rollback();
            throw e;
        } finally {
            setAutoCommit(saveAuto);
        }
    }

    public String getOrCreateSHA256(String resourceName, java.util.function.Function<String, String> hashBuilder) throws SQLException {
        boolean saveAuto = connection.getAutoCommit();
        setAutoCommit(false);
        String sha256;
        try {
            sha256 = getSHA256(resourceName);
            if (sha256 == null) {
                sha256 = hashBuilder.apply(resourceName);
                if (sha256 != null) {
                    insertHash(resourceName, sha256);
                }
            }
            commit();
        } catch (Exception e) {
            rollback();
            throw e;
        } finally {
            setAutoCommit(saveAuto);
        }
        return sha256;

    }

    private String getSHA256(String resourceName) throws SQLException {
        hashQuery.setString(1, resourceName);
        try (ResultSet resultSet = hashQuery.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                return null;
            }
        }
    }

    private void createTableIfNeeded(String tableName, String... fields) throws SQLException {
        String command = "CREATE TABLE " + tableName + " (" + String.join(", ", fields) + ')';

        if (!tableExists(tableName)) {
            try (Statement st = connection.createStatement()) {
                if (st.execute(command)) {
                    throw new SQLException("failed " + st.getWarnings());
                }
            }
        }
    }

    private Integer insertHash(String resourceName, String hash) throws SQLException {
        hashInsert.setString(1, resourceName);
        hashInsert.setString(2, hash);
        return hashInsert.executeUpdate();
    }

    private void doInsertHashes(Properties props) throws SQLException {

        try {
            setAutoCommit(false);
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO sha256 (resourceName,hash)VALUES (?,?)");
            try (PreparedStatement st = preparedStatement) {
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    st.setString(1, entry.getKey().toString());
                    st.setString(2, entry.getValue().toString());
                    st.addBatch();
                }
                int[] ints = st.executeBatch();
                if (Arrays.stream(ints).anyMatch(it -> it != 1)) {
                    throw new SQLException("batch fail: " + st.getWarnings());
                }
                commit();
            }
        } catch (SQLException e) {
            logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
            rollback();
        } finally {
            setAutoCommit(true);
        }
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName.toUpperCase(), null)) {
            return tables.next();
        }
    }

    private void doMain(String[] args) throws IOException, SQLException {
        logger.info("begin");
        Properties props = new Properties();
        File cacheDir = new File("target/central-cache");
        File file = new File(cacheDir, "sha256.properties");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            props.load(reader);
        }
        File db = new File(cacheDir, "sha256-derby");
        String dbURL = "jdbc:derby:" + db.getAbsolutePath() + ";create=true";
        MavenBundlesDBIndex dbIndex = new MavenBundlesDBIndex(dbURL);
        try (Connection connection = DriverManager.getConnection(dbURL)) {
            logger.info("connection opened");
            connection.setAutoCommit(false);
            createTableIfNeeded("sha256", new String[]{
                    "resourceName  VARCHAR(256) PRIMARY KEY",
                    "hash CHAR(64) NOT NULL"
            });
            connection.setAutoCommit(true);
            doInsertHashes(props);
            logger.info("About to do fetch test");
            int n = 0;
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT hash FROM SHA256 WHERE RESOURCENAME=?");
            try (PreparedStatement st = preparedStatement) {
                logger.info("statement prepared");
                for (String key : props.stringPropertyNames()) {
                    st.setString(1, key);
                    ResultSet resultSet = st.executeQuery();
                    while (resultSet.next()) {
                        String fetchedHash = resultSet.getString(1);
                        String storedHash = props.getProperty(key);
                        assert fetchedHash.equals(storedHash);
                        if ((++n % 1000) == 0) {
                            logger.info("matched {} : {} = {}", n, key, fetchedHash);
                        }
                    }
                }
            }
        }

    }

    public void setAutoCommit(boolean autocommit) throws SQLException {
        connection.setAutoCommit(autocommit);
    }
}
