import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.resource.CapabilityBuilder;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.bnd.version.MavenVersion;
import aQute.lib.hex.Hex;
import aQute.lib.tag.Tag;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.creator.OsgiArtifactIndexCreator;
import org.apache.maven.index.updater.IndexDataReader;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.util.IndexCreatorSorter;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.osgi.framework.Constants;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IndexFetchAndPrintTest {
    private static final String INDEX_FILE_NAME = "nexus-maven-repository-index.gz";
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger("IndexFetch");
    private final DefaultPlexusContainer plexusContainer;
    private final Indexer indexer;
    private final IndexUpdater indexUpdater;
    private File centralLocalCache;
    private final String repoBase;
    private MavenBundlesDBIndex dbIndex;
    private final LightweightHttpWagon wagon;
    private boolean shouldFetchAndComputeHash;

    public static void main(String[] args) throws Exception {
        IndexFetchAndPrintTest foo = new IndexFetchAndPrintTest();
        foo.fetcher();
    }

    public IndexFetchAndPrintTest() throws ConnectionException, AuthenticationException, SQLException, PlexusContainerException, ComponentLookupException {
        repoBase = "http://repo1.maven.org/maven2/";
        centralLocalCache = new File("../central-cache");
        //noinspection ResultOfMethodCallIgnored
        centralLocalCache.mkdirs();
        wagon = createWagon();
        dbIndex = createMavenBundlesDBIndex();
        shouldFetchAndComputeHash = false;
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
        this.plexusContainer = new DefaultPlexusContainer( config );

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup( Indexer.class );
        this.indexUpdater = plexusContainer.lookup( IndexUpdater.class );
        // lookup wagon used to remotely fetch index

    }

    private MavenBundlesDBIndex createMavenBundlesDBIndex() throws SQLException {
        File dbFile = new File(centralLocalCache, "sha256-derby");
        String dbURL = "jdbc:derby:" + dbFile.getAbsolutePath() + ";create=true";
        return new MavenBundlesDBIndex(dbURL);
    }


    public void fetcher() throws Exception {

        boolean generateXML = false;
        File file = new File(centralLocalCache, INDEX_FILE_NAME);
        wagon.getIfNewer(".index/" + INDEX_FILE_NAME, file, file.lastModified());

        List<IndexCreator> indexers = getIndexCreators();
        PrintWriter out = null;
        XMLResourceGenerator generator = null;
        try {

            if (generateXML) {
                out = initXmlWriter();
                generator = new XMLResourceGenerator().compress();
            }
            GZIPInputStream bin = new GZIPInputStream(new FileInputStream(file), 8192);
            IndexDataReader ir = new IndexDataReader(bin);
            long l = ir.readHeader();
            Document document;
            int n = 0;
            dbIndex.setAutoCommit(false);
            dbIndex.commit();
            while ((document = ir.readDocument()) != null) {
                try {
                    if (document.get("del") != null) {
                        logger.debug("deleted: {}", document);
                        continue;
                    }
                    ArtifactInfo ai = new ArtifactInfo();

                    for (IndexCreator indexer : indexers) {
                        indexer.updateArtifactInfo(document, ai);
                    }
                    if (ai.getGroupId() == null) {
                        logger.error("Group is null: {}", ai);
                        continue;
                    }
                    String classifier = ai.getClassifier();
                    if (classifier == null || !classifier.equals("sources")) {
                        if (ai.getBundleSymbolicName() != null) {
                            Resource resource = createResourceFromArtifactInfo(ai);
                            saveToDatabase(ai);
                            if (generateXML) {
                                Tag t = generator.getTagForResource(resource);
                                t.print(2, out);
                            }
                            n++;
                            if ((n % 100) == 0) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("{}: Adding {}", String.format("%,d", n), resource);
                                }
                                dbIndex.commit();
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Caught Exception", e); //To change body of catch statement use File | Settings | File Templates.
                }
            }
            dbIndex.commit();
        } catch (Exception e) {
            if (generateXML) {
                out.println("</repository>");
                out.close();
            }
            logger.error("Failure: ", e);
            dbIndex.rollback();
            throw e;
        }


    }

    public void saveToDatabase(ArtifactInfo ai) throws SQLException, ExecutionException {
        int groupNameId = dbIndex.getOrCreateId("maven_group", ai.getGroupId());
        int artifactNameId = dbIndex.getOrCreateId("maven_artifactId", ai.getArtifactId());
        Integer classifierId = null;
        if (ai.getClassifier() != null) {
            classifierId = dbIndex.getOrCreateId("maven_classifier", ai.getClassifier());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("id {},{},{} for {},{}", groupNameId, artifactNameId, classifierId,
                    ai.getGroupId(), ai.getArtifactId(), ai.getClassifier());
        }
        Integer mavenArtifactId = dbIndex.getOrCreateMavenArtifactId(groupNameId, artifactNameId);
        Integer mavenArtifactVersionId = dbIndex.getOrCreateMavenArtifactVersionId(mavenArtifactId, ai.getVersion());
        long lastModified = ai.getLastModified();

        Integer mavenResource = dbIndex.lookupMavenResource(mavenArtifactVersionId,classifierId);
        if(mavenResource == null) {
            mavenResource = dbIndex.addMavenResource(
                    mavenArtifactVersionId, classifierId,
                    lastModified, ai.getPackaging(),
                    ai.getFileExtension(), ai.getSha1(),getResourceName(ai) );
        }

        Integer bundleId = dbIndex.lookupBundle(mavenResource,ai.getBundleSymbolicName(),ai.getBundleVersion());
        if(bundleId == null) {
            bundleId = dbIndex.insertBundle(mavenResource,ai.getBundleSymbolicName(),ai.getBundleVersion());
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
                int packageId = dbIndex.getOrCreateId("package", packageName);
                dbIndex.insertBundleExportPackage(bundleId, packageId,version,attrString);
            }
        }
    }

    public PrintWriter initXmlWriter() throws IOException {
        File indexXmlOut = new File(centralLocalCache, "index-tmp.xml.gz");
        PrintWriter out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(indexXmlOut), 8192));
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.format("<repository xmlns=\"%s\" name=\"central\" increment=%d>\n", "http://www.osgi.org/xmlns/repository/v1.0.0", System.currentTimeMillis());
        return out;
    }


    public Resource createResourceFromArtifactInfo(ArtifactInfo ai) throws Exception {
        ResourceBuilder builder = new ResourceBuilder();
        Domain aidom = Domain.domain(new HashMap<String, String>());
        aidom.setBundleSymbolicName(ai.getBundleSymbolicName());
        String bundleVersion = ai.getBundleVersion();
        try {
            aidom.setBundleVersion(bundleVersion);
        } catch (Exception e) {
            aidom.setBundleVersion(MavenVersion.parseString(bundleVersion).getOSGiVersion());
        }
        aidom.setImportPackage(ai.getBundleImportPackage());
        aidom.setExportPackage(ai.getBundleExportPackage());
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

    public LightweightHttpWagon createWagon() throws ConnectionException, AuthenticationException {
        final LightweightHttpWagon httpWagon = new LightweightHttpWagon();
        httpWagon.addTransferListener(new MyAbstractTransferListener());
        httpWagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
        httpWagon.connect(new Repository("central", repoBase));
        return httpWagon;
    }

//    private void loadProps() throws IOException {
//        shaPropFile = new File(centralLocalCache, "sha256.properties");
//        shaProps = new Properties();
//        if (shaPropFile.exists()) {
//            try (BufferedReader reader = new BufferedReader(new FileReader(shaPropFile))) {
//                shaProps.load(reader);
//            }
//        }
//    }

    private List<IndexCreator> getIndexCreators() {
        List<IndexCreator> indexers = new ArrayList<IndexCreator>();
        indexers.add(new MinimalArtifactInfoIndexCreator());
        indexers.add(new JarFileContentsIndexCreator());
        indexers.add(new MavenArchetypeArtifactInfoIndexCreator());
        indexers.add(new MavenPluginArtifactInfoIndexCreator());
        indexers.add(new OsgiArtifactIndexCreator());
        List<IndexCreator> sorted = IndexCreatorSorter.sort(indexers);
        indexers = sorted;
        return indexers;
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
            return dbIndex.getOrCreateSHA256(resourceName, hashProvider);
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

    private static class MyAbstractTransferListener extends AbstractTransferListener {
        int t = 0;

        public void transferStarted(TransferEvent transferEvent) {
            logger.info("  Downloading " + transferEvent.getResource().getName());
            t = 0;
        }

        public void transferProgress(TransferEvent transferEvent, byte[] buffer, int length) {
            t += length;
            if ((t % 1024 * 1024) == 0)
                logger.debug("Fetching {} : {} ", transferEvent.getResource().getName(), String.format("%,d", t));
        }

        public void transferCompleted(TransferEvent transferEvent) {
            logger.info(" {} - Done", transferEvent.getResource());
        }
    }
}
