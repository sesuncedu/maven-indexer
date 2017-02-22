package com.criticollab.osgi.mavenindex;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
            FauxIndexUpdater.IndexAdaptor target = new DBLoadingIndexAdaptor(this.indexUpdater, indexDirectory);
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
        return new WagonFetcher(wagon, new MyTransferListener(), null, null);
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

}
