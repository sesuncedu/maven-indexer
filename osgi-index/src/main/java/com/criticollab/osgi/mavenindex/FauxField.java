package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/17/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FauxField {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(FauxField.class);

    private String name;
    private String value;

    public FauxField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String stringValue() {
        return getValue();
    }

    public void setStringValue(String s) {
        setValue(s);
    }
}
