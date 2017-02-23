package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import aQute.bnd.header.Attrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Parameters {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(Parameters.class);
    long id;
    private Map<String, ParameterValue> params = new HashMap<>();

    public Parameters() {
    }

    public Parameters(aQute.bnd.header.Parameters parameters) {
        for (Map.Entry<String, Attrs> entry : parameters.entrySet()) {
            ParameterValue value = new ParameterValue(entry.getKey(), entry.getValue());
            this.params.put(entry.getKey(), value);
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

    @MapKey(name = "key")
    @OneToMany(cascade = CascadeType.ALL)
    public Map<String, ParameterValue> getParams() {
        return params;
    }

    public void setParams(Map<String, ParameterValue> parameters) {
        this.params = parameters;
    }
}
