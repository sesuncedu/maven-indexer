package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/21/17.
 */

import com.criticollab.osgi.mavenindex.persist.MavenArtifact;
import com.criticollab.osgi.mavenindex.persist.MavenResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Properties;
import java.util.Set;

class DBLoadingIndexAdaptor extends FauxIndexUpdater.IndexAdaptor {
    private static final String INDEX_PROPERTIES_NAME = "nexus-maven-repository-index-updater.properties";
    private static final String UPDATER_PROPERTIES_NAME = "nexus-maven-repository-index-updater.properties";
    private static Logger logger = LoggerFactory.getLogger("DBLoader");
    final FauxIndexUpdater fauxIndexUpdater;
    Date date = null;
    private EntityManagerFactory entityManagerFactory;


    DBLoadingIndexAdaptor(FauxIndexUpdater fauxIndexUpdater, File indexDirectoryFile) {
        super(fauxIndexUpdater, indexDirectoryFile);
        this.fauxIndexUpdater = fauxIndexUpdater;
        this.entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");
    }

    @Override
    public Properties getProperties() {
        if (properties == null) {
            properties = fauxIndexUpdater.loadIndexProperties(dir, INDEX_PROPERTIES_NAME);
        }
        return properties;
    }

    @Override
    public void storeProperties() throws IOException {
        fauxIndexUpdater.storeIndexProperties(dir, UPDATER_PROPERTIES_NAME, properties);
    }

    @Override
    public Date getTimestamp() {
        return date;
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
                final int batch = 10000;

                TypedQuery<MavenArtifact> artifactByTriple = manager.createQuery(
                        "from MavenArtifact where artifactId=:artifactId and groupId=:groupId and version=:version",
                        MavenArtifact.class);
                TypedQuery<MavenResource> resourceByArtifactAndClassifier = manager.createQuery(
                        "from MavenResource where mavenArtifact=:artifact and classifier=:classifier",
                        MavenResource.class);
                FauxDocument doc;
                Set<String> fieldNames = new HashSet<>();
                while ((doc = dr.readDocument()) != null) {
                    n++;

                    fieldNames.addAll(doc.keySet());

                    ArtifactInfo ai = ArtifactInfoBuilder.getArtifactInfoFromDocument(doc);


                    if (ai.getGroupId() == null) {
                        continue;
                    }
                    MavenArtifact artifact = null;
                    try {
                        artifactByTriple.setParameter("groupId", ai.getGroupId());
                        artifactByTriple.setParameter("artifactId", ai.getArtifactId());
                        artifactByTriple.setParameter("version", ai.getVersion());
                        artifact = artifactByTriple.getSingleResult();
                    } catch (NoResultException e) {
                        try {
                            artifact = new MavenArtifact();
                            artifact.setGroupId(ai.getGroupId());
                            artifact.setArtifactId(ai.getArtifactId());
                            artifact.setVersion(ai.getVersion());
                            manager.persist(artifact);
                        } catch (Exception e1) {
                            logger.error("Caught Exception",
                                         e1); //To change body of catch statement use File | Settings | File Templates.
                            throw e1;
                        }
                    }
                    String classifier = ai.getClassifier();
                    if (classifier == null) {
                        classifier = "NA";
                    }
                    MavenResource resource;

                    try {
                        resourceByArtifactAndClassifier.setParameter("artifact", artifact);
                        resourceByArtifactAndClassifier.setParameter("classifier", classifier);
                        resource = resourceByArtifactAndClassifier.getSingleResult();
                    } catch (NoResultException e) {
                        resource = new MavenResource();
                        resource.setMavenArtifact(artifact);
                        artifact.getResources().add(resource);
                        resource.setClassifier(classifier);
                        resource.setPackaging(ai.getPackaging());
                        resource.setFileExtension(ai.getFileExtension());
                        resource.setMD5(ai.getMd5());
                        resource.setSHA1(ai.getSha1());
                        resource.setSize(ai.getSize());
                        if (ai.getLastModified() > 0) {
                            resource.setLastModified(new Date(ai.getLastModified()));
                        }
                        manager.persist(resource);
                    }
                    Set<String> unusedKeys = doc.getUnusedKeys();

                    if ((n % batch) == 0) {
                        manager.flush();
                        manager.getTransaction().commit();
                        manager.getTransaction().begin();
                        manager.clear();
                        long mavenLastModified = ai.getMavenLastModified();
                        long lastModified = ai.getLastModified();
                        logger.info("{}: {} - {} ", String.format("%,d", n), ai, artifact);
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
                entityManagerFactory.close();
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
