package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/17/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FauxField {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(FauxField.class);

    private final String name;
    private String value;

    public FauxField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    private void setValue(String value) {
        this.value = value;
    }

    public String stringValue() {
        return getValue();
    }

    public void setStringValue(String s) {
        setValue(s);
    }
}
