package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/17/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

class FauxDocument extends HashMap<String,String> {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(FauxDocument.class);

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
