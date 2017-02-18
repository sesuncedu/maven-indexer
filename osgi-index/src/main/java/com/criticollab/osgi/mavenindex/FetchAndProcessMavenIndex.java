package com.criticollab.osgi.mavenindex;

import org.apache.maven.index.DefaultIndexer;
import org.apache.maven.index.DefaultIndexerEngine;
import org.apache.maven.index.DefaultQueryCreator;
import org.apache.maven.index.DefaultSearchEngine;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.creator.OsgiArtifactIndexCreator;
import org.apache.maven.index.fs.Lock;
import org.apache.maven.index.fs.Locker;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdateSideEffect;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class FetchAndProcessMavenIndex {
    private static final String INDEX_FILE_NAME = "nexus-maven-repository-index.gz";
    private static final List<IndexCreator> INDEXERS = Arrays.asList(
            new MinimalArtifactInfoIndexCreator(),
            new JarFileContentsIndexCreator(),
            new MavenArchetypeArtifactInfoIndexCreator(),
            new MavenPluginArtifactInfoIndexCreator(),
            new OsgiArtifactIndexCreator());
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger("IndexFetch");
    private final String repoBase;
    private final LightweightHttpWagon wagon;
    private  EntityManagerFactory entityManagerFactory;
    private Indexer indexer;
    private FauxIndexUpdater indexUpdater;
    private File centralLocalCache;

    public FetchAndProcessMavenIndex() throws ConnectionException, AuthenticationException, SQLException, PlexusContainerException, ComponentLookupException {
        repoBase = "http://repo1.maven.org/maven2/";
        centralLocalCache = new File(System.getProperty("user.home"), "central-cache");
        //noinspection ResultOfMethodCallIgnored
        centralLocalCache.mkdirs();
        wagon = createWagon();

        // lookup the indexer components from plexus
        this.indexer = new DefaultIndexer(new DefaultSearchEngine(), new DefaultIndexerEngine(), new DefaultQueryCreator());
        ArrayList<IndexUpdateSideEffect> sideEffects = new ArrayList<>();
        this.indexUpdater = new FauxIndexUpdater(new FauxIncrementalHandler(), sideEffects);
        // lookup wagon used to remotely fetch index

        //entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");
    }
    //private  EntityManagerFactory entityManagerFactory;
    //@PersistenceContext(unitName = "MavenBundles")

    public static void main(String[] args) throws Exception {
        FetchAndProcessMavenIndex foo = new FetchAndProcessMavenIndex();
        foo.fetcher();
    }


    public void fetcher() throws Exception {
        
        doIncrementalFetch("http://repo1.maven.org/maven2", "central-context", "central", centralLocalCache);

    }

    public IndexUpdateResult doIncrementalFetch(String repositoryUrl, String id, String central, File localCache) throws IOException {
        IndexingContext context = indexer.createIndexingContext(id, central, localCache, new File("/tmp/foobar"),
                repositoryUrl, null, true, true, INDEXERS);
        MyIndexUpdateRequest updateRequest =
                new MyIndexUpdateRequest(context, new WagonHelper.WagonFetcher(wagon, new MyAbstractTransferListener(), null, null));

        updateRequest.setLocalIndexCacheDir(centralLocalCache);


        ResourceFetcher fetcher = updateRequest.getResourceFetcher();
        IndexUpdateResult result = new IndexUpdateResult();

        // If no resource fetcher passed in, use the wagon fetcher by default
        // and put back in request for future use
        if (fetcher == null) {
            throw new IOException("Update of the index without provided ResourceFetcher is impossible.");
        }

        fetcher.connect(context.getId(), context.getIndexUpdateUrl());

        File cacheDir = updateRequest.getLocalIndexCacheDir();
        Locker locker = updateRequest.getLocker();
        Lock lock = locker != null && cacheDir != null ? locker.lock(cacheDir) : null;
        try {
            if (cacheDir != null) {
                FauxIndexUpdater.LocalCacheIndexAdaptor cache = new FauxIndexUpdater.LocalCacheIndexAdaptor(indexUpdater, cacheDir, result);

                cacheDir.mkdirs();

                IndexUpdateResult result11 = doAnUpdate(fetcher, cache);
                if (result11 != null)
                    fetcher = cache.getFetcher();
            }

            try {
                FauxIndexUpdater.IndexAdaptor target = new MyIndexAdaptor(FetchAndProcessMavenIndex.this.indexUpdater, updateRequest, updateRequest.getIndexingContext().getIndexDirectoryFile());
                result = doAnUpdate(fetcher, target);

                if (result.isSuccessful()) {
                    target.commit();
                }
            } finally {
                fetcher.disconnect();
            }
        } finally {
            if (lock != null) {
                lock.release();
            }
        }

        return result;
    }

    public IndexUpdateResult doAnUpdate(ResourceFetcher fetcher1, FauxIndexUpdater.IndexAdaptor target) throws IOException {
        IndexUpdateResult result = new IndexUpdateResult();
        try {
            boolean done = false;

            Properties localProperties = target.getProperties();
            Date localTimestamp = null;

            if (localProperties != null) {
                localTimestamp = indexUpdater.getTimestamp(localProperties, IndexingContext.INDEX_TIMESTAMP);
            }

            // this will download and store properties in the target, so next run
            // target.getProperties() will retrieve it
            Properties remoteProperties = target.setProperties(fetcher1);

            Date updateTimestamp = indexUpdater.getTimestamp(remoteProperties, IndexingContext.INDEX_TIMESTAMP);

            // If new timestamp is missing, dont bother checking incremental, we have an old file
            if (updateTimestamp != null) {
                List<String> filenames =
                        indexUpdater.incrementalHandler.loadRemoteIncrementalUpdates(localProperties, remoteProperties);

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
                updateTimestamp = indexUpdater.getTimestamp(remoteProperties, IndexingContext.INDEX_LEGACY_TIMESTAMP);
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
                    timestamp = target.setIndexFile(fetcher1, IndexingContext.INDEX_FILE_PREFIX + ".gz");
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
                        timestamp = target.setIndexFile(fetcher1, IndexingContext.INDEX_FILE_PREFIX + ".zip");
                    } catch (IOException ex2) {
                        indexUpdater.getLogger().error("Fallback to *.zip also failed: " + ex2); // do not bother with stack trace
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

    public LightweightHttpWagon createWagon() throws ConnectionException, AuthenticationException {
        final LightweightHttpWagon httpWagon = new LightweightHttpWagon();
        httpWagon.addTransferListener(new MyAbstractTransferListener());
        httpWagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
        httpWagon.connect(new Repository("central", repoBase));
        return httpWagon;
    }

    private static class MyAbstractTransferListener extends AbstractTransferListener {
        int t = 0;

        public void transferStarted(TransferEvent transferEvent) {
            logger.info("  Downloading " + transferEvent.getResource().getName());
            t = 0;
        }

        public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            t += length;
            if ((t % 1024 * 1024) == 0)
                logger.info("Fetching {} : {} ", transferEvent.getResource().getName(), String.format("%,d", t));
        }

        public void transferCompleted(TransferEvent transferEvent) {
            logger.info(" {} - Done", transferEvent.getResource());
        }
    }

    static class MyIndexUpdateRequest extends IndexUpdateRequest {
        public MyIndexUpdateRequest(IndexingContext context, WagonHelper.WagonFetcher fetcher) {
            super(context, fetcher);
        }

        @Override
        public DocumentFilter getDocumentFilter() {
            return null;
        }

        @Override
        public boolean isForceFullUpdate() {
            return false;
        }

        @Override
        public boolean isIncrementalOnly() {
            return false;
        }

        @Override
        public boolean isOffline() {
            return false;
        }

        @Override
        public boolean isCacheOnly() {
            return true;
        }
    }

    private class MyIndexAdaptor extends FauxIndexUpdater.IndexAdaptor {
        Date date = null;
        final MyIndexUpdateRequest updateRequest;
        FauxIndexUpdater fauxIndexUpdater;

        MyIndexAdaptor(FauxIndexUpdater fauxIndexUpdater, MyIndexUpdateRequest updateRequest, File indexDirectoryFile) {
            super(fauxIndexUpdater, indexDirectoryFile);
            this.updateRequest = updateRequest;
            this.fauxIndexUpdater = fauxIndexUpdater;
        }


        @Override
        public Properties getProperties() {
            if (properties == null) {
                properties = fauxIndexUpdater.loadIndexProperties(dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE);
            }
            return properties;
        }

        @Override
        public void storeProperties() throws IOException {
            fauxIndexUpdater.storeIndexProperties(dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, properties);
        }

        @Override
        public Date getTimestamp() {
            return updateRequest.getIndexingContext().getTimestamp();
        }

        @Override
        public void addIndexChunk(ResourceFetcher source, String filename) throws IOException {
            Date result11 = null;

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

                        n++;
                        if ((n % 10000) == 0) {
                            logger.info("{}: {} - {} field names", ai, String.format("%,d", n), fieldNames);
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
        public Date setIndexFile(ResourceFetcher source, String filename) throws IOException {
            addIndexChunk(source, filename);
            return date;
        }

        @Override
        public void commit() throws IOException {
            super.commit();

            updateRequest.getIndexingContext().commit();
        }
    }
}
