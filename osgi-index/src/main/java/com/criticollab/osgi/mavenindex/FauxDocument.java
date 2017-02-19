package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/17/17.
 */

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

class FauxDocument extends HashMap<String,String> {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(FauxDocument.class);
    private static Set<String> allProbedKeys = new HashSet<>();
    private static Set<String> allMatchedKeys = new HashSet<>();
    private static Set<String> allUnusedKeys = new HashSet<>();
    private final Set<String> usedKeys = new HashSet<>();

    public static Set<String> getAllProbedKeys() {
        return allProbedKeys;
    }

    public static void setAllProbedKeys(Set<String> allProbedKeys) {
        FauxDocument.allProbedKeys = allProbedKeys;
    }

    public static Set<String> getAllMatchedKeys() {
        return allMatchedKeys;
    }

    public static void setAllMatchedKeys(Set<String> allMatchedKeys) {
        FauxDocument.allMatchedKeys = allMatchedKeys;
    }

    public static Set<String> getAllUnusedKeys() {
        return allUnusedKeys;
    }

    public static void setAllUnusedKeys(Set<String> allUnusedKeys) {
        FauxDocument.allUnusedKeys = allUnusedKeys;
    }

    public static void dumpKeyUsage() {
        logger.info("probedKeys = {}", allProbedKeys);
        logger.info("matchedKeys = {}", allMatchedKeys);
        logger.info("unmatchedKeys = {}", Sets.difference(allProbedKeys, allMatchedKeys));
        logger.info("keys in document but unused: {}", allUnusedKeys);
    }

    @Override
    public String get(Object key) {
        String stringKey = (String) key;
        allProbedKeys.add(stringKey);
        String v = super.get(key);
        if (v != null) {
            allMatchedKeys.add(stringKey);
            usedKeys.add(stringKey);

        }
        return v;
    }

    public Set<String> getUnusedKeys() {
        Sets.SetView<String> difference = Sets.difference(keySet(), usedKeys);
        if (logger.isDebugEnabled()) {
            if (difference.size() > 0) {
                logger.debug("unused = {}, all = {}", usedKeys, entrySet());
            }
        }
        allUnusedKeys.addAll(difference);
        return difference;
    }

    FauxField getField(String k) {
        String v = get(k);
        if(v != null) {
            return new FauxField(k,v);
        }  else {
            return null;
        }
    }

    void add(FauxField fauxField) {
        put(fauxField.getName(),fauxField.getValue());
    }

    public Set<String> getUsedKeys() {
        return usedKeys;
    }
}
