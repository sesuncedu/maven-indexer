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

    private final Set<String> usedKeys = new HashSet<>();

    @Override
    public String get(Object key) {
        String v = super.get(key);
        if (v != null) {
            usedKeys.add((String) key);

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
}
