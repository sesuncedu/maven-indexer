package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/18/17.
 */

import org.apache.lucene.index.Term;
import org.apache.maven.index.context.IndexingContext;

import java.io.IOException;

/**
 * The default {@link IndexingContext} implementation.
 *
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
class FauxIndexingContext {
    private static final String FLD_DESCRIPTOR = "DESCRIPTOR";
    private static final String FLD_DESCRIPTOR_CONTENTS = "NexusIndex";
    private static final String FLD_IDXINFO = "IDXINFO";
    private static final String VERSION = "1.0";
    private static final Term DESCRIPTOR_TERM = new Term(FLD_DESCRIPTOR, FLD_DESCRIPTOR_CONTENTS);
    private static final String INDEX_PACKER_PROPERTIES_FILE = "nexus-maven-repository-index-packer.properties";

    private final String id;

    //private Date timestamp;

    FauxIndexingContext(String id, String repositoryId  //
    ) throws IOException {
        this.id = id;


        // this.indexDirectory = indexDirectory;


        //prepareIndex(reclaimIndex);

        //setIndexDirectoryFile(null);
    }


    // ==

    void commit() throws IOException {
    }

    // groups

    @Override
    public String toString() {
        return id + ":" + "notimestamp";
    }
}
