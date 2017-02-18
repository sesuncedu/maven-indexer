package com.criticollab.osgi.mavenindex;

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
import javax.persistence.EntityManagerFactory;
import java.io.BufferedInputStream;
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


        //entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");
    }
    //private  EntityManagerFactory entityManagerFactory;
    //@PersistenceContext(unitName = "MavenBundles")

    public static void main(String[] args) throws Exception {
        FetchAndProcessMavenIndex foo = new FetchAndProcessMavenIndex();
        foo.fetcher();
    }


    private void fetcher() throws Exception {

        doIncrementalFetch("http://repo1.maven.org/maven2", "central-context", "central", centralLocalCache);

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
        httpWagon.addTransferListener(new MyAbstractTransferListener());
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

            try (BufferedInputStream is = new BufferedInputStream(source.retrieve(filename))) {
                Date timestamp = null;

                if (filename.endsWith(".gz")) {
                    FauxIndexDataReader dr = new FauxIndexDataReader(is);

                    long timestamp1 = dr.readHeader();

                    Date date = null;

                    if (timestamp1 != -1) {
                        date = new Date(timestamp1);

                    }

                    int n = 0;

                    FauxDocument doc;
                    Set<String> fieldNames = new HashSet<>();
                    while ((doc = dr.readDocument()) != null) {
                        fieldNames.addAll(doc.keySet());

                        ArtifactInfo ai = ArtifactInfoBuilder.getArtifactInfoFromDocument(doc);
                        Set<String> unusedKeys = doc.getUnusedKeys();
                        if (unusedKeys.size() > 0) {
                            logger.info("unused keys: {}", unusedKeys);
                        }
                        n++;
                        if ((n % 10000) == 0) {
                            long mavenLastModified = ai.getMavenLastModified();
                            long lastModified = ai.getLastModified();
                            logger.info("{}: {} - m={} vs {} ", String.format("%,d", n), ai,
                                        new Date(mavenLastModified), new Date(lastModified));
                        }
                    }
                    FauxIndexDataReader.IndexDataReadResult result112 = new FauxIndexDataReader.IndexDataReadResult();
                    result112.setDocumentCount(n);
                    result112.setTimestamp(date);

                    timestamp = result112.getTimestamp();
                }
                result11 = timestamp;
            }
            date = result11;
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
