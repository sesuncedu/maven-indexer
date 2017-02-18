package com.criticollab.osgi.mavenindex;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0    
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class FauxIncrementalHandler

{

    private static final String LAST_INCREMENTAL = "nexus.index.last-incremental";
    private static final String NEXUS_INDEX_CHAIN_ID = "nexus.index.chain-id";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    List<String> loadRemoteIncrementalUpdates(Properties localProperties, Properties remoteProperties) {
        List<String> filenames = null;
        // If we have local properties, will parse and see what we need to download
        if ( canRetrieveAllChunks( localProperties, remoteProperties ) )
        {
            filenames = new ArrayList<>();

            int maxCounter = Integer.parseInt(remoteProperties.getProperty(LAST_INCREMENTAL));
            int currentCounter = Integer.parseInt(localProperties.getProperty(LAST_INCREMENTAL));

            // Start with the next one
            currentCounter++;

            while ( currentCounter <= maxCounter )
            {
                filenames.add("nexus-maven-repository-index" + "." + currentCounter++ + ".gz");
            }
        }

        return filenames;
    }

    private boolean canRetrieveAllChunks( Properties localProps, Properties remoteProps )
    {
        // no localprops, can't retrieve chunks
        if ( localProps == null )
        {
            return false;
        }

        String localChainId = localProps.getProperty(NEXUS_INDEX_CHAIN_ID);
        String remoteChainId = remoteProps.getProperty(NEXUS_INDEX_CHAIN_ID);

        // If no chain id, or not the same, do whole download
        if ( StringUtils.isEmpty( localChainId ) || !localChainId.equals( remoteChainId ) )
        {
            return false;
        }

        String counterProp = localProps.getProperty(LAST_INCREMENTAL);

        // no counter, cant retrieve chunks
        // not a number, cant retrieve chunks
        if ( StringUtils.isEmpty( counterProp ) || !StringUtils.isNumeric( counterProp ) )
        {
            return false;
        }

        int currentLocalCounter = Integer.parseInt( counterProp );

        // check remote props for existence of next chunk after local
        // if we find it, then we are ok to retrieve the rest of the chunks
        for ( Object key : remoteProps.keySet() )
        {
            String sKey = (String) key;

            if (sKey.startsWith("nexus.index.incremental-"))
            {
                String value = remoteProps.getProperty( sKey );

                // If we have the current counter, or the next counter, we are good to go
                if ( Integer.toString( currentLocalCounter ).equals( value ) || Integer.toString(
                    currentLocalCounter + 1 ).equals( value ) )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
