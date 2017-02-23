package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/18/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

class PersistenceHelper implements AutoCloseable {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(PersistenceHelper.class);
    private EntityManagerFactory entityManagerFactory;


    PersistenceHelper() {
        //entityManagerFactory = setFactory();
    }

    public static void main(String[] args) {
        logger.info("about to try create schema");
        try (PersistenceHelper helper = new PersistenceHelper()) {
            // helper.setFactory();
            Map properties = new HashMap();
            properties.put("javax.persistence.schema-generation.database.action", "drop-and-create");
            properties.put("javax.persistence.schema-generation.scripts.action", "create");
            properties.put("javax.persistence.schema-generation.scripts.create-target", new File("create.sql"));
            properties.put("javax.persistence.schema-generation.scripts.drop-target", new File("drop.sql"));
            properties.put("javax.persistence.schema-generation.create-database-schemas", true);
            helper.generateSchema(properties);
            logger.info("done");
            System.exit(0);
        }
    }

    private EntityManagerFactory setFactory() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory("MavenBundles");
        return this.entityManagerFactory;
    }

    EntityManager createEntityManager() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        return entityManager;
    }

    EntityManager createEntityManager(Map properties) {
        EntityManager entityManager = entityManagerFactory.createEntityManager(properties);
        return entityManager;
    }

    void generateSchema(Map properties) {
        Persistence.generateSchema("MavenBundles", properties);
    }

    public void close() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }
}
