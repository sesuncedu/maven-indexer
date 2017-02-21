package com.criticollab.osgi.mavenindex;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.MavenVersion;
import aQute.lib.hex.Hex;
import aQute.lib.tag.Tag;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;
import org.apache.commons.io.output.NullOutputStream;
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
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateSideEffect;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.repository.Repository;
import org.osgi.framework.Constants;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//import org.apache.maven.index.ArtifactInfo;

public class IndexFetchAndPrintTest {
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
    private final File centralLocalCache;
    private  EntityManagerFactory entityManagerFactory;
    private Indexer indexer;
    private FauxIndexUpdater indexUpdater;
    private MavenBundlesDBIndex dbIndex;
    private boolean shouldFetchAndComputeHash;
    private  ServiceManager starter;
    private EntityManager entityManager;

    private IndexFetchAndPrintTest() throws ConnectionException, AuthenticationException {
        repoBase = "http://repo1.maven.org/maven2/";
        centralLocalCache = new File(System.getProperty("user.home"), "central-cache");
        //noinspection ResultOfMethodCallIgnored
        centralLocalCache.mkdirs();
        wagon = createWagon();
        shouldFetchAndComputeHash = false;

        // lookup the indexer components from plexus
        this.indexer = new DefaultIndexer(new DefaultSearchEngine(), new DefaultIndexerEngine(), new DefaultQueryCreator());
        ArrayList<IndexUpdateSideEffect> sideEffects = new ArrayList<>();
        this.indexUpdater = new FauxIndexUpdater(new FauxIncrementalHandler());
        // lookup wagon used to remotely fetch index

        //entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");
    }
    //private  EntityManagerFactory entityManagerFactory;
    //@PersistenceContext(unitName = "MavenBundles")

    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new JpaPersistModule("MavenBundles"));
        IndexFetchAndPrintTest foo = new IndexFetchAndPrintTest();
        injector.injectMembers(foo);
        foo.fetcher();
        foo.close();
        foo.starter.stop();
    }

    public ServiceManager getStarter() {
        return starter;
    }

    @Inject  public void setStarter(ServiceManager starter) {
        this.starter = starter;
        starter.start();
    }

    private EntityManager getEntityManager() {
        return entityManager;
    }

    @Inject
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private void close() {
        entityManager.close();
    }

    private MavenBundlesDBIndex getDbIndex() throws SQLException {
        if (dbIndex == null) {
            dbIndex = createMavenBundlesDBIndex(new File("/Users/ses/maven-index-central-cache/sha256-derby"));

        }
        return dbIndex;
    }

    public void setDbIndex(MavenBundlesDBIndex dbIndex) {
        this.dbIndex = dbIndex;
    }

    private MavenBundlesDBIndex createMavenBundlesDBIndex(File dbFile) throws SQLException {
        String dbURL = "jdbc:derby:" + dbFile.getAbsolutePath() + ";create=true";
        return new MavenBundlesDBIndex(dbURL);
    }

    private void fetcher() throws Exception {

        //doJpaFun();

//        doIncrementalFetch("http://repo1.maven.org/maven2", "central-context", "central", centralLocalCache);

    }


    private boolean hackjob() throws SQLException {
        Connection conn = getDbIndex().getConnection();
        Statement statement = conn.createStatement();
        int b;
        try {
            b = statement.executeUpdate("ALTER TABLE MAVENRESOURCE ADD COLUMN SHA256 CHAR(64)");
            logger.info("b = {}", b);
        } catch (SQLException e) {
            logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
        }
        statement.close();
        conn.setAutoCommit(false);
        statement = conn.createStatement();
        b = statement.executeUpdate("MERGE INTO MAVENRESOURCE m " +
                "USING SHA256 s " +
                "ON m.resourcename = s.resourcename " +
                "WHEN MATCHED " +
                "THEN UPDATE SET sha256 = s.hash ");

        logger.info("b = {}", b);
        conn.commit();
        //noinspection ConstantConditions
        if (true)
            return true;
        return false;
    }

    public void doItTheOldWay() throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException, SQLException, IOException {
        File file = new File(centralLocalCache, INDEX_FILE_NAME);
        boolean generateXML = false;

        wagon.getIfNewer(".index/" + INDEX_FILE_NAME, file, file.lastModified());

        PrintWriter out = null;
        XMLResourceGenerator generator = null;
        try {

            //noinspection ConstantConditions
            if (generateXML) {
                out = initXmlWriter();
                generator = new XMLResourceGenerator().compress();
            }
            GZIPInputStream bin = new GZIPInputStream(new FileInputStream(file), 8192);
            FauxIndexDataReader ir = new FauxIndexDataReader(bin);
            long l = ir.readHeader();
            FauxDocument document;
            int n = 0;
            getDbIndex().setAutoCommit(false);
            getDbIndex().commit();
            while ((document = ir.readDocument()) != null) {
                try {
                    ArtifactInfo ai = ArtifactInfoBuilder.getArtifactInfoFromDocument(document);
                    if (ai == null) {
                        continue;
                    }
                    if (ai.getGroupId() == null) {
                        logger.error("MavenGroup is null: {}", ai);
                        continue;
                    }
                    String classifier = ai.getClassifier();
                    if (classifier == null || !classifier.equals("sources")) {
                        if (ai.getBundleSymbolicName() != null) {
                            Resource resource = createResourceFromArtifactInfo(ai);
                            saveToDatabase(ai);
                            //noinspection ConstantConditions
                            if (generateXML) {
                                Tag t = generator.getTagForResource(resource);
                                t.print(2, out);
                            }
                            n++;
                            if ((n % 100) == 0) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("{}: Adding {}", String.format("%,d", n), resource);
                                }
                                getDbIndex().commit();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
                }
            }
            getDbIndex().commit();
        } catch (Exception e) {
            //noinspection ConstantConditions
            if (generateXML) {
                out.println("</repository>");
                out.close();
            }
            logger.error("Failure: ", e);
            getDbIndex().rollback();
            throw e;
        }
    }

//    private void doIncrementalFetch(String repositoryUrl, String id, String central, File localCache) throws IOException {
//        IndexingContext context = indexer.createIndexingContext(id, central, localCache, new File("/tmp/foobar"),
//                repositoryUrl, null, true, true, INDEXERS);
//        MyIndexUpdateRequest updateRequest =
//                new MyIndexUpdateRequest(context, new WagonHelper.WagonFetcher(wagon, new MyAbstractTransferListener(), null, null));
//
//        updateRequest.setLocalIndexCacheDir(centralLocalCache);
//
//
//        FauxResourceFetcher fetcher = updateRequest.getResourceFetcher();
//        IndexUpdateResult result = new IndexUpdateResult();
//
//        // If no resource fetcher passed in, use the wagon fetcher by default
//        // and put back in request for future use
//        if (fetcher == null) {
//            throw new IOException("Update of the index without provided ResourceFetcher is impossible.");
//        }
//
//        fetcher.connect(context.getId(), context.getIndexUpdateUrl());
//
//        File cacheDir = updateRequest.getLocalIndexCacheDir();
//        Locker locker = updateRequest.getLocker();
//        Lock lock = locker != null && cacheDir != null ? locker.lock(cacheDir) : null;
//        try {
//            if (cacheDir != null) {
//                FauxIndexUpdater.LocalCacheIndexAdaptor cache = new FauxIndexUpdater.LocalCacheIndexAdaptor(indexUpdater, cacheDir, result);
//
//                cacheDir.mkdirs();
//
//                IndexUpdateResult result11 = doAnUpdate(fetcher, cache);
//                if (result11 != null)
//                    fetcher = cache.getFetcher();
//            }
//
//            try {
//                //noinspection ConstantConditions
//                if (true) {
//                    FauxIndexUpdater.IndexAdaptor target = new MyIndexAdaptor(indexUpdater, updateRequest, updateRequest.getIndexingContext().getIndexDirectoryFile());
//                    result = doAnUpdate(fetcher, target);
//
//                    if (result.isSuccessful()) {
//                        target.commit();
//                    }
//                }
//            } finally {
//                fetcher.disconnect();
//            }
//        } finally {
//            if (lock != null) {
//                lock.release();
//            }
//        }
//
//    }

//    private IndexUpdateResult doAnUpdate(FauxResourceFetcher fetcher1, FauxIndexUpdater.IndexAdaptor target) throws IOException {
//        IndexUpdateResult result = new IndexUpdateResult();
//        try {
//            boolean done = false;
//
//            Properties localProperties = target.getProperties();
//            Date localTimestamp = null;
//
//            if (localProperties != null) {
//                localTimestamp = indexUpdater.getTimestamp(localProperties, IndexingContext.INDEX_TIMESTAMP);
//            }
//
//            // this will download and store properties in the target, so next run
//            // target.getProperties() will retrieve it
//            Properties remoteProperties = target.setProperties(fetcher1);
//
//            Date updateTimestamp = indexUpdater.getTimestamp(remoteProperties, IndexingContext.INDEX_TIMESTAMP);
//
//            // If new timestamp is missing, dont bother checking incremental, we have an old file
//            if (updateTimestamp != null) {
//                List<String> filenames =
//                        indexUpdater.incrementalHandler.loadRemoteIncrementalUpdates(localProperties, remoteProperties);
//
//                // if we have some incremental files, merge them in
//                if (filenames != null) {
//                    for (String filename : filenames) {
//                        target.addIndexChunk(fetcher1, filename);
//                    }
//
//                    result.setTimestamp(updateTimestamp);
//                    result.setSuccessful(true);
//                    done = true;
//                }
//            } else {
//                updateTimestamp = indexUpdater.getTimestamp(remoteProperties, IndexingContext.INDEX_LEGACY_TIMESTAMP);
//            }
//
//            // fallback to timestamp comparison, but try with one coming from local properties, and if not possible (is
//            // null)
//            // fallback to context timestamp
//            if (!done) {
//                if (localTimestamp != null) {
//                    // if we have localTimestamp
//                    // if incremental can't be done for whatever reason, simply use old logic of
//                    // checking the timestamp, if the same, nothing to do
//                    if (updateTimestamp != null && !updateTimestamp.after(localTimestamp)) {
//                        //Index is up to date
//                        result.setSuccessful(true);
//                        done = true;
//                    }
//                }
//            }
//            Exception ex3 = null;
//            if (!done) {
//                Date timestamp = null;
//                try {
//                    timestamp = target.setIndexFile(fetcher1, IndexingContext.INDEX_FILE_PREFIX + ".gz");
//                    if (fetcher1 instanceof FauxIndexUpdater.LocalIndexCacheFetcher) {
//                        // local cache has inverse organization compared to remote indexes,
//                        // i.e. initial index file and delta chunks to apply on top of it
//                        for (String filename : ((FauxIndexUpdater.LocalIndexCacheFetcher) fetcher1).getChunks()) {
//                            target.addIndexChunk(fetcher1, filename);
//                        }
//                    }
//                } catch (IOException ex) {
//                    // try to look for legacy index transfer format
//                    try {
//                        timestamp = target.setIndexFile(fetcher1, IndexingContext.INDEX_FILE_PREFIX + ".zip");
//                    } catch (IOException ex2) {
//                        indexUpdater.getLogger().error("Fallback to *.zip also failed: " + ex2); // do not bother with stack trace
//                        done = true;
//                        ex3 = ex; // original exception more likely to be interesting
//                    }
//                }
//                if (ex3 != null) {
//                    result = null;
//
//                } else {
//                    result.setTimestamp(timestamp);
//                    result.setSuccessful(true);
//                    result.setFullUpdate(true);
//                }
//            }
//
//            if (!done) {
//                if (result.isSuccessful()) {
//                    target.commit();
//                }
//            }
//        } finally {
//            fetcher1.disconnect();
//        }
//        return result;
//    }

    private void saveToDatabase(ArtifactInfo ai) throws SQLException, ExecutionException {
        int groupNameId = getDbIndex().getOrCreateId("maven_group", ai.getGroupId());
        int artifactNameId = getDbIndex().getOrCreateId("maven_artifactId", ai.getArtifactId());
        Integer classifierId = null;
        if (ai.getClassifier() != null) {
            classifierId = getDbIndex().getOrCreateId("maven_classifier", ai.getClassifier());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("id {},{},{} for {},{}", groupNameId, artifactNameId, classifierId,
                    ai.getGroupId(), ai.getArtifactId(), ai.getClassifier());
        }
        Integer mavenArtifactId = getDbIndex().getOrCreateMavenArtifactId(groupNameId, artifactNameId);
        Integer mavenArtifactVersionId = getDbIndex().getOrCreateMavenArtifactVersionId(mavenArtifactId, ai.getVersion());
        long lastModified = ai.getLastModified();

        Integer mavenResource = getDbIndex().lookupMavenResource(mavenArtifactVersionId, classifierId);
        if(mavenResource == null) {
            mavenResource = getDbIndex().addMavenResource(
                    mavenArtifactVersionId, classifierId,
                    lastModified, ai.getPackaging(),
                    ai.getFileExtension(), ai.getSha1(),getResourceName(ai) );
        }

        Integer bundleId = getDbIndex().lookupBundle(mavenResource, ai.getBundleSymbolicName(), ai.getBundleVersion());
        if(bundleId == null) {
            bundleId = getDbIndex().insertBundle(mavenResource, ai.getBundleSymbolicName(), ai.getBundleVersion());
        }
        String exportPackage = ai.getBundleExportPackage();
        if(exportPackage != null) {
            Parameters exports = new Parameters(exportPackage);
            logger.debug("exports {}",exports);
            for (Map.Entry<String, Attrs> entry : exports.entrySet()) {
                String packageName = entry.getKey();
                Attrs attrs = entry.getValue();
                String version = attrs.getVersion();
                attrs.remove("version");
                if(version == null) {
                    version = "0.0.0";
                }
                String attrString = attrs.toString();
                int packageId = getDbIndex().getOrCreateId("package", packageName);
                getDbIndex().insertBundleExportPackage(bundleId, packageId, version, attrString);
            }
        }
    }

    private PrintWriter initXmlWriter() throws IOException {
        File indexXmlOut = new File(centralLocalCache, "index-tmp.xml.gz");
        PrintWriter out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(indexXmlOut), 8192));
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.format("<repository xmlns=\"%s\" name=\"central\" increment=%d>\n", "http://www.osgi.org/xmlns/repository/v1.0.0", System.currentTimeMillis());
        return out;
    }

    private Resource createResourceFromArtifactInfo(ArtifactInfo ai) throws Exception {
        ResourceBuilder builder = new ResourceBuilder();
        Domain aidom = Domain.domain(new HashMap<>());
        aidom.setBundleSymbolicName(ai.getBundleSymbolicName());
        String bundleVersion = ai.getBundleVersion();
        try {
            aidom.setBundleVersion(bundleVersion);
        } catch (Exception e) {
            aidom.setBundleVersion(MavenVersion.parseString(bundleVersion).getOSGiVersion());
        }
        aidom.setImportPackage(ai.getBundleImportPackage());
        aidom.setExportPackage(ai.getBundleExportPackage());
        //noinspection deprecation
        aidom.set(Constants.EXPORT_SERVICE, ai.getBundleExportService());
        aidom.set(Constants.BUNDLE_NAME, ai.getBundleName());
        aidom.set(Constants.BUNDLE_LICENSE, ai.getBundleLicense());
        aidom.set(Constants.BUNDLE_DOCURL, ai.getBundleDocUrl());
        aidom.set(Constants.BUNDLE_DESCRIPTION, ai.getBundleDescription());
        builder.addRequireBundles(OSGiHeader.parseHeader(ai.getBundleRequireBundle()));
        builder.addManifest(aidom);
        addContentInfo(builder, ai);
        return builder.build();
    }

    private LightweightHttpWagon createWagon() throws ConnectionException, AuthenticationException {
        final LightweightHttpWagon httpWagon = new LightweightHttpWagon();
        httpWagon.addTransferListener(new MyAbstractTransferListener());
        httpWagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
        httpWagon.connect(new Repository("central", repoBase));
        return httpWagon;
    }

    private void addContentInfo(ResourceBuilder builder, ArtifactInfo ai) throws Exception {
        String resourceName = getResourceName(ai);
        URI uri = new URI(repoBase + resourceName);

        CapabilityBuilder c = new CapabilityBuilder(ContentNamespace.CONTENT_NAMESPACE);
        String sha256 = getOrComputeSHA256(resourceName);
        if (sha256 != null) {
            c.addAttribute(ContentNamespace.CONTENT_NAMESPACE, sha256);
        }
        c.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri.toString());
        c.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, ai.getSize());
        //noinspection ConstantConditions
        c.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, null == null ? "vnd.osgi.bundle" : null);
        builder.addCapability(c);
    }

    private String getResourceName(ArtifactInfo ai) {
        String groupPath = ai.getGroupId().replace('.', '/');
        String fileName = ai.getFileName();
        if (fileName == null) {

            String extension = ai.getFileExtension();
            if (ai.getClassifier() != null) {
                if (ai.getPackaging() != null) {
                    String packaging = ai.getPackaging();
                    if (!packaging.equals(extension)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("ai: {}  {} vs {} {}", ai, packaging, extension, ai.getClassifier());
                        }
                        extension = packaging;
                    }
                }
            }
            String classifierPart = ai.getClassifier() != null ? ("-" + ai.getClassifier()) : "";
            if (!classifierPart.equals("")) {
                if (logger.isDebugEnabled()) {
                    logger.debug("have classifier Part {}", classifierPart);
                }
            }
            fileName = ai.getArtifactId() + "-" + ai.getVersion() + classifierPart + "." + extension;
        }
        String dirPath = groupPath + "/" + ai.getArtifactId() + "/" + ai.getVersion() + "/";
        return dirPath + fileName;
    }

    private String getOrComputeSHA256(String resourceName) throws SQLException {
        try {

            Function<String, String> hashProvider;
            if (shouldFetchAndComputeHash) {
                hashProvider = this::fetchAndComputeHash;
            } else {
                hashProvider = r -> null;
            }
            return getDbIndex().getOrCreateSHA256(resourceName, hashProvider);
        } catch (SQLException e) {
            logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    private String fetchAndComputeHash(String resourceName) {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            try (DigestOutputStream out = new DigestOutputStream(new NullOutputStream(), sha256Digest)) {
                wagon.getToStream(resourceName, out);
            }
            return Hex.toHexString(sha256Digest.digest());
        } catch (Exception e) {
            logger.error("Caught Exception", e);
            throw new RuntimeException("fetch and compute hash failed", e);
        }
    }

    private URI getContentURI(String repoBase, ArtifactInfo ai) throws URISyntaxException {
        String groupPath = ai.getGroupId().replace("/", ".");
        String fileName = ai.getFileName();
        if (fileName == null) {
            fileName = ai.getArtifactId() + "-" + ai.getVersion() + "." + ai.getFileExtension();
        }
        String uriString = repoBase + groupPath + "/" + ai.getArtifactId() + "/" + ai.getVersion() + "/" + fileName;
        return new URI(uriString);
    }

    public void setShouldFetchAndComputeHash(boolean shouldFetchAndComputeHash) {
        this.shouldFetchAndComputeHash = shouldFetchAndComputeHash;
    }

    public static class ServiceManager {
        @Inject
        PersistService persistService;

        public ServiceManager() {
        }

        public void start() {
            persistService.start();
        }

        public void stop() {
            persistService.stop();
        }
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

    private static class MyIndexUpdateRequest extends IndexUpdateRequest {
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

//    private class MyIndexAdaptor extends FauxIndexUpdater.IndexAdaptor {
//        Date date = null;
//        final  MyIndexUpdateRequest updateRequest;
//        final FauxIndexUpdater fauxIndexUpdater;
//
//        MyIndexAdaptor(FauxIndexUpdater fauxIndexUpdater, MyIndexUpdateRequest updateRequest, File indexDirectoryFile) {
//            super(fauxIndexUpdater, indexDirectoryFile);
//            this.updateRequest = updateRequest;
//            this.fauxIndexUpdater = fauxIndexUpdater;
//        }
//
//
//        @Override
//        public Properties getProperties() {
//            if (properties == null) {
//                properties = fauxIndexUpdater.loadIndexProperties(dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE);
//            }
//            return properties;
//        }
//
//        @Override
//        public void storeProperties() throws IOException {
//            fauxIndexUpdater.storeIndexProperties(dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, properties);
//        }
//
//        @Override
//        public Date getTimestamp() {
//            return updateRequest.getIndexingContext().getTimestamp();
//        }
//
//        @Override
//        public void addIndexChunk(ResourceFetcher source, String filename) throws IOException {
//            Date result11 = null;
//
//            try (BufferedInputStream is = new BufferedInputStream(source.retrieve(filename))) {
//                Date timestamp = null;
//
//                if (filename.endsWith(".gz")) {
//                    FauxIndexDataReader dr = new FauxIndexDataReader(is);
//
//                    long timestamp1 = dr.readHeader();
//
//                    Date date = null;
//
//                    if (timestamp1 != -1) {
//                        date = new Date(timestamp1);
//
//                    }
//
//                    int n = 0;
//
//                    FauxDocument doc;
//                    Set<String> fieldNames = new HashSet<>();
//                    while ((doc = dr.readDocument()) != null) {
//                        fieldNames.addAll(doc.keySet());
//
//                        ArtifactInfo ai = ArtifactInfoBuilder.getArtifactInfoFromDocument(doc);
//
//                        n++;
//                        if ((n % 10000) == 0) {
//                            logger.info("{}: {} - {} field names", ai, String.format("%,d", n), fieldNames);
//                        }
//                    }
//                    FauxIndexDataReader.IndexDataReadResult result112 = new FauxIndexDataReader.IndexDataReadResult();
//                    result112.setDocumentCount(n);
//                    result112.setTimestamp(date);
//
//                    timestamp = result112.getTimestamp();
//                }
//                result11 = timestamp;
//            }
//            date = result11;
//        }
//
//        @Override
//        public Date setIndexFile(ResourceFetcher source, String filename) throws IOException {
//            addIndexChunk(source, filename);
//            return date;
//        }
//
//        @Override
//        public void commit() throws IOException {
//            super.commit();
//
//            updateRequest.getIndexingContext().commit();
//        }
//    }
}
