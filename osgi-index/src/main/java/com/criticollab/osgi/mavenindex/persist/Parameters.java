package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import aQute.bnd.header.Attrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Parameters {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Parameters.class);
    long id;
    private Map<String, ParameterValue> parameters = new HashMap<>();

    public Parameters() {
    }

    public Parameters(aQute.bnd.header.Parameters parameters) {
        for (Map.Entry<String, Attrs> entry : parameters.entrySet()) {
            ParameterValue value = new ParameterValue(entry.getValue());
        }
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToMany
    public Map<String, ParameterValue> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, ParameterValue> parameters) {
        this.parameters = parameters;
    }
}
