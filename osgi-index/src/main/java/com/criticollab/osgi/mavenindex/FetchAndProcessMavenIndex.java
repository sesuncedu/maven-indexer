package com.criticollab.osgi.mavenindex;

import com.criticollab.osgi.mavenindex.persist.Artifact;
import com.criticollab.osgi.mavenindex.persist.ArtifactVersion;
import com.criticollab.osgi.mavenindex.persist.MavenGroup;
import com.google.common.cache.CacheStats;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings("Duplicates")
public class FetchAndProcessMavenIndex {
    private static final String INDEX_FILE_NAME = "nexus-maven-repository-index.gz";
    private static final String INDEX_TIMESTAMP = "nexus.index.timestamp";
    private static final String INDEX_LEGACY_TIMESTAMP = "nexus.index.time";
    private static final String INDEX_FILE_PREFIX = "nexus-maven-repository-index";
    private static final String INDEX_UPDATER_PROPERTIES_FILE = "nexus-maven-repository-index-updater.properties";
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger("IndexFetch");
    private final String repoBase;
    private final FauxIndexUpdater indexUpdater = new FauxIndexUpdater(new FauxIncrementalHandler());
    private final File centralLocalCache;
    private EntityManagerFactory entityManagerFactory;

    private FetchAndProcessMavenIndex() throws ConnectionException, AuthenticationException {
        repoBase = "http://repo1.maven.org/maven2/";
        centralLocalCache = new File(System.getProperty("user.home"), "central-cache");
        //noinspection ResultOfMethodCallIgnored
        centralLocalCache.mkdirs();


    }
    //private  EntityManagerFactory entityManagerFactory;
    //@PersistenceContext(unitName = "MavenBundles")

    public static void main(String[] args) throws Exception {
        FetchAndProcessMavenIndex foo = new FetchAndProcessMavenIndex();
        foo.fetcher();
    }


    private void fetcher() throws Exception {
        entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");

        try {

            doIncrementalFetch("http://repo1.maven.org/maven2", "central-context", "central", centralLocalCache);
        } finally {
            entityManagerFactory.close();
        }

    }

    private void doIncrementalFetch(@Nonnull String repositoryUrl, @Nonnull String id, @Nonnull String central,
                                    @Nonnull File localCache) throws IOException, ConnectionException,
                                                                     AuthenticationException {

        File indexDirectory = new File("/tmp/foobar");


        FauxResourceFetcher fetcher = newWagonFetcher(repositoryUrl, id);

        FauxIndexUpdateResult result = new FauxIndexUpdateResult();

        fetcher.connect(id, makeIndexUpdateUrl(repositoryUrl));

        FauxIndexUpdater.LocalCacheIndexAdaptor cache;
        cache = new FauxIndexUpdater.LocalCacheIndexAdaptor(indexUpdater, localCache, result);

        localCache.mkdirs();

        if (doAnUpdate(fetcher, cache) != null) {
            fetcher = cache.getFetcher();
        }

        try {
            FauxIndexUpdater.IndexAdaptor target = new MyIndexAdaptor(this.indexUpdater, indexDirectory);
            result = doAnUpdate(fetcher, target);

            if (result.isSuccessful()) {
                target.commit();
            }
        } finally {
            fetcher.disconnect();
        }

    }

    private String makeIndexUpdateUrl(@Nonnull String repositoryUrl) {
        String indexUpdateUrl;
        indexUpdateUrl = addTrailingSlashIfNeeded(repositoryUrl) + ".index";
        return indexUpdateUrl;
    }

    private FauxResourceFetcher newWagonFetcher(@Nonnull String repositoryUrl, @Nonnull String id) throws
                                                                                                   ConnectionException,
                                                                                                   AuthenticationException {
        final LightweightHttpWagon httpWagon = new LightweightHttpWagon();
        //httpWagon.addTransferListener(new MyAbstractTransferListener());
        httpWagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
        httpWagon.connect(new Repository(id, repositoryUrl));
        LightweightHttpWagon wagon = httpWagon;
        return new WagonFetcher(wagon, new MyAbstractTransferListener(), null, null);
    }

    private String addTrailingSlashIfNeeded(@Nonnull String repositoryUrl) {
        return repositoryUrl + (repositoryUrl.endsWith("/") ? "" : "/");
    }

    private FauxIndexUpdateResult doAnUpdate(FauxResourceFetcher fetcher1, FauxIndexUpdater.IndexAdaptor target) throws
                                                                                                                 IOException {
        FauxIndexUpdateResult result = new FauxIndexUpdateResult();
        try {
            boolean done = false;

            Properties localProperties = target.getProperties();
            Date localTimestamp = null;

            if (localProperties != null) {
                localTimestamp = indexUpdater.getTimestamp(localProperties, INDEX_TIMESTAMP);
            }

            // this will download and store properties in the target, so next run
            // target.getProperties() will retrieve it
            Properties remoteProperties = target.setProperties(fetcher1);

            Date updateTimestamp = indexUpdater.getTimestamp(remoteProperties, INDEX_TIMESTAMP);

            // If new timestamp is missing, dont bother checking incremental, we have an old file
            if (updateTimestamp != null) {
                List<String> filenames = indexUpdater.incrementalHandler.loadRemoteIncrementalUpdates(localProperties,
                                                                                                      remoteProperties);

                // if we have some incremental files, merge them in
                if (filenames != null) {
                    for (String filename : filenames) {
                        target.addIndexChunk(fetcher1, filename);
                    }

                    result.setTimestamp(updateTimestamp);
                    result.setSuccessful(true);
                    done = true;
                }
            } else {
                updateTimestamp = indexUpdater.getTimestamp(remoteProperties, INDEX_LEGACY_TIMESTAMP);
            }

            // fallback to timestamp comparison, but try with one coming from local properties, and if not possible (is
            // null)
            // fallback to context timestamp
            if (!done) {
                if (localTimestamp != null) {
                    // if we have localTimestamp
                    // if incremental can't be done for whatever reason, simply use old logic of
                    // checking the timestamp, if the same, nothing to do
                    if (updateTimestamp != null && !updateTimestamp.after(localTimestamp)) {
                        //Index is up to date
                        result.setSuccessful(true);
                        done = true;
                    }
                }
            }
            Exception ex3 = null;
            if (!done) {
                Date timestamp = null;
                try {
                    timestamp = target.setIndexFile(fetcher1, INDEX_FILE_PREFIX + ".gz");
                    if (fetcher1 instanceof FauxIndexUpdater.LocalIndexCacheFetcher) {
                        // local cache has inverse organization compared to remote indexes,
                        // i.e. initial index file and delta chunks to apply on top of it
                        for (String filename : ((FauxIndexUpdater.LocalIndexCacheFetcher) fetcher1).getChunks()) {
                            target.addIndexChunk(fetcher1, filename);
                        }
                    }
                } catch (IOException ex) {
                    // try to look for legacy index transfer format
                    try {
                        timestamp = target.setIndexFile(fetcher1, INDEX_FILE_PREFIX + ".zip");
                    } catch (IOException ex2) {
                        indexUpdater.getLogger().error(
                                "Fallback to *.zip also failed: " + ex2); // do not bother with stack trace
                        done = true;
                        ex3 = ex; // original exception more likely to be interesting
                    }
                }
                if (ex3 != null) {
                    result = null;

                } else {
                    result.setTimestamp(timestamp);
                    result.setSuccessful(true);
                    result.setFullUpdate(true);
                }
            }

            if (!done) {
                if (result.isSuccessful()) {
                    target.commit();
                }
            }
        } finally {
            fetcher1.disconnect();
        }
        return result;
    }

    private static class MyAbstractTransferListener extends AbstractTransferListener {
        int t = 0;

        public void transferStarted(TransferEvent transferEvent) {
            logger.info("  Downloading " + transferEvent.getResource().getName());
            t = 0;
        }

        public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            t += length;
            if ((t % 1024 * 1024) == 0) logger.info("Fetching {} : {} ", transferEvent.getResource().getName(),
                                                    String.format("%,d", t));
        }

        public void transferCompleted(TransferEvent transferEvent) {
            logger.info(" {} - Done", transferEvent.getResource());
        }
    }

    private class MyIndexAdaptor extends FauxIndexUpdater.IndexAdaptor {
        final FauxIndexUpdater fauxIndexUpdater;
        Date date = null;

        MyIndexAdaptor(FauxIndexUpdater fauxIndexUpdater, File indexDirectoryFile) {
            super(fauxIndexUpdater, indexDirectoryFile);
            this.fauxIndexUpdater = fauxIndexUpdater;
        }


        @Override
        public Properties getProperties() {
            if (properties == null) {
                properties = fauxIndexUpdater.loadIndexProperties(dir, INDEX_UPDATER_PROPERTIES_FILE);
            }
            return properties;
        }

        @Override
        public void storeProperties() throws IOException {
            fauxIndexUpdater.storeIndexProperties(dir, INDEX_UPDATER_PROPERTIES_FILE, properties);
        }

        @Override
        public Date getTimestamp() {
            return null;
        }

        @Override
        public void addIndexChunk(FauxResourceFetcher source, String filename) throws IOException {
            Date result11;

            //InputStream is = new BufferedInputStream(source.retrieve(filename));
            Date timestamp = null;

            if (filename.endsWith(".gz")) {
                FauxIndexDataReader dr = new FauxIndexDataReader(source.getResourceAsFile(filename));

                long timestamp1 = dr.readHeader();

                Date date = null;

                if (timestamp1 != -1) {
                    date = new Date(timestamp1);

                }

                int n = 0;
                final EntityManager manager = entityManagerFactory.createEntityManager();
                manager.setFlushMode(FlushModeType.COMMIT);
                if (!manager.isJoinedToTransaction()) {
                    manager.joinTransaction();
                    manager.getTransaction().begin();
                }
                try {

                    LoadingEntityCache<String, MavenGroup> groupsCache = new LoadingEntityCache<>(manager,
                                                                                                  "groupByName",
                                                                                                  MavenGroup.class,
                                                                                                  false, 5000);
                    LoadingEntityCache<String, Artifact> artifactsCache = new LoadingEntityCache<>(manager,
                                                                                                   "artifactByName",
                                                                                                   Artifact.class,
                                                                                                   false, 5000);
                    TypedQuery<ArtifactVersion> artifactVersionByArtifactAndVersion = manager.createNamedQuery(
                            "artifactVersionByArtifactAndVersion", ArtifactVersion.class);

                    TypedQuery<Artifact> artifactByName = manager.createNamedQuery("artifactByName", Artifact.class);

                    FauxDocument doc;
                    Set<String> fieldNames = new HashSet<>();
                    CacheStats lastStats = null;
                    while ((doc = dr.readDocument()) != null) {
                        n++;

                        fieldNames.addAll(doc.keySet());

                        ArtifactInfo ai = ArtifactInfoBuilder.getArtifactInfoFromDocument(doc);
                        MavenGroup group = null;
                        Artifact artifact = null;
                        String groupId = ai.getGroupId();
                        if (groupId != null) {
                            group = groupsCache.get(groupId);
                        }
                        if (group == null) {
                            continue;
                        }
                        {
                            final MavenGroup g = group;
                            artifactByName.setParameter("name", ai.getArtifactId());
                            try {
                                artifact = artifactByName.getSingleResult();
                            } catch (NoResultException e) {
                                try {
                                    Artifact art = new Artifact();
                                    artifact = art;
                                    art.setName(ai.getArtifactId());
                                    art.setMavenGroup(g);
                                    g.getArtifacts().add(art);
                                    manager.persist(art);
                                } catch (Exception e1) {
                                    logger.error("Caught Exception",
                                                 e1); //To change body of catch statement use File | Settings | File Templates.
                                    throw e1;
                                }
                            }

                        }
                        ArtifactVersion artifactVersion = null;
                        try {
                            artifactVersionByArtifactAndVersion.setParameter("artifact", artifact);
                            artifactVersionByArtifactAndVersion.setParameter("version", ai.getVersion());
                            artifactVersion = artifactVersionByArtifactAndVersion.getSingleResult();
                            logger.debug("got a hit on an artifactVersion");
                        } catch (NoResultException e) {
                            artifactVersion = new ArtifactVersion();
                            artifactVersion.setArtifact(artifact);
                            artifactVersion.setVersion(ai.getVersion());
                            artifact.getVersions().add(artifactVersion);
                            manager.persist(artifactVersion);
                        }
                        Set<String> unusedKeys = doc.getUnusedKeys();

                        if ((n % 10000) == 0) {
                            manager.flush();
                            manager.clear();
                            CacheStats stats = groupsCache.stats();
                            if (lastStats != null) {
                                stats = stats.minus(lastStats);
                            }
                            logger.info(stats.toString());
                            lastStats = groupsCache.stats();
                            groupsCache.invalidateAll();
                            long mavenLastModified = ai.getMavenLastModified();
                            long lastModified = ai.getLastModified();
                            logger.info("{}: {} - {} ", String.format("%,d", n), ai, group.getId());
                        }
                    }
                    if (manager.isJoinedToTransaction()) {
                        EntityTransaction tx = manager.getTransaction();
                        tx.commit();
                    }
                } catch (Exception e) {
                    logger.error("caught exception - rolling back", e);
                    if (manager.isJoinedToTransaction()) {
                        EntityTransaction tx = manager.getTransaction();
                        tx.rollback();
                    }

                    try {
                        throw e;
                    } catch (Exception e1) {
                        throw new IOException(e);
                    }

                } finally {
                    manager.close();
                }
                FauxIndexDataReader.IndexDataReadResult result112 = new FauxIndexDataReader.IndexDataReadResult();
                result112.setDocumentCount(n);
                result112.setTimestamp(date);

                timestamp = result112.getTimestamp();
            }
            result11 = timestamp;

            date = result11;
            FauxDocument.dumpKeyUsage();
        }

        @Override
        public Date setIndexFile(FauxResourceFetcher source, String filename) throws IOException {
            addIndexChunk(source, filename);
            return date;
        }

        @Override
        public void commit() throws IOException {
            super.commit();

        }


    }

}
