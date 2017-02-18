package com.criticollab.osgi.mavenindex;/*
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

import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.fs.Locker;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdateSideEffect;
import org.apache.maven.index.updater.ResourceFetcher;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

/**
 * A default index updater implementation
 * 
 * @author Jason van Zyl
 * @author Eugene Kuleshov
 */

 class FauxIndexUpdater

{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected Logger getLogger()
    {
        return logger;
    }

    final FauxIncrementalHandler incrementalHandler;

    private final List<IndexUpdateSideEffect> sideEffects;


    FauxIndexUpdater(final FauxIncrementalHandler incrementalHandler, final List<IndexUpdateSideEffect> sideEffects)
    {
        this.incrementalHandler = incrementalHandler;
        this.sideEffects = sideEffects;
    }

    Properties loadIndexProperties(final File indexDirectoryFile, final String remoteIndexPropertiesName)
    {
        File indexProperties = new File( indexDirectoryFile, remoteIndexPropertiesName );

        try ( FileInputStream fis = new FileInputStream( indexProperties ))
        {
            Properties properties = new Properties();

            properties.load( fis );

            return properties;
        }
        catch ( IOException e )
        {
            getLogger().debug( "Unable to read remote properties stored locally", e );
        }
        return null;
    }

    void storeIndexProperties(final File dir, final String indexPropertiesName, final Properties properties)
        throws IOException
    {
        File file = new File( dir, indexPropertiesName );

        if ( properties != null )
        {
            try (OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) ))
            {
                properties.store( os, null );
            }
        }
        else
        {
            file.delete();
        }
    }

    Properties downloadIndexProperties(final ResourceFetcher fetcher)
        throws IOException
    {
        try (InputStream fis = fetcher.retrieve( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ))
        {
            Properties properties = new Properties();

            properties.load( fis );

            return properties;
        }
    }

    Date getTimestamp(final Properties properties, final String key)
    {
        String indexTimestamp = properties.getProperty( key );

        if ( indexTimestamp != null )
        {
            try
            {
                SimpleDateFormat df = new SimpleDateFormat( IndexingContext.INDEX_TIME_FORMAT );
                df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                return df.parse( indexTimestamp );
            }
            catch ( ParseException ex )
            {
            }
        }
        return null;
    }

    /**
     * Filesystem-based ResourceFetcher implementation
     */
    public static class FileFetcher
        implements ResourceFetcher
    {
        final File basedir;

        public FileFetcher( File basedir )
        {
            this.basedir = basedir;
        }

        public void connect( String id, String url )
            throws IOException
        {
            // don't need to do anything
        }

        public void disconnect()
            throws IOException
        {
            // don't need to do anything
        }

        public void retrieve( String name, File targetFile )
            throws IOException, FileNotFoundException
        {
            FileUtils.copyFile( getFile( name ), targetFile );

        }

        public InputStream retrieve( String name )
            throws IOException, FileNotFoundException
        {
            return new FileInputStream( getFile( name ) );
        }

        File getFile(String name)
        {
            return new File( basedir, name );
        }

    }

    abstract static class IndexAdaptor {
        protected final File dir;

        protected Properties properties;
        FauxIndexUpdater fauxIndexUpdater;

        protected IndexAdaptor(FauxIndexUpdater fauxIndexUpdater, File dir)
        {
            this.dir = dir;
            this.fauxIndexUpdater = fauxIndexUpdater;
        }

        public abstract Properties getProperties();

        public abstract void storeProperties()
            throws IOException;

        public abstract void addIndexChunk( ResourceFetcher source, String filename )
            throws IOException;

        public abstract Date setIndexFile( ResourceFetcher source, String string )
            throws IOException;

        public Properties setProperties( ResourceFetcher source )
            throws IOException
        {
            this.properties = fauxIndexUpdater.downloadIndexProperties(source);
            return properties;
        }

        public abstract Date getTimestamp();

        public void commit()
            throws IOException
        {
            storeProperties();
        }
    }

    static class LocalCacheIndexAdaptor
            extends IndexAdaptor
    {
        static final String CHUNKS_FILENAME = "chunks.lst";

        static final String CHUNKS_FILE_ENCODING = "UTF-8";

        final IndexUpdateResult result;

        final ArrayList<String> newChunks = new ArrayList<String>();
        FauxIndexUpdater fauxIndexUpdater;

        public LocalCacheIndexAdaptor(FauxIndexUpdater fauxIndexUpdater, File dir, IndexUpdateResult result)
        {
            super(fauxIndexUpdater, dir);
            this.result = result;
            this.fauxIndexUpdater = fauxIndexUpdater;
        }

        public Properties getProperties()
        {
            if ( properties == null )
            {
                properties = fauxIndexUpdater.loadIndexProperties(dir, IndexingContext.INDEX_REMOTE_PROPERTIES_FILE);
            }
            return properties;
        }

        public void storeProperties()
            throws IOException
        {
            fauxIndexUpdater.storeIndexProperties(dir, IndexingContext.INDEX_REMOTE_PROPERTIES_FILE, properties);
        }

        public Date getTimestamp()
        {
            Properties properties = getProperties();
            if ( properties == null )
            {
                return null;
            }

            Date timestamp = fauxIndexUpdater.getTimestamp(properties, IndexingContext.INDEX_TIMESTAMP);

            if ( timestamp == null )
            {
                timestamp = fauxIndexUpdater.getTimestamp(properties, IndexingContext.INDEX_LEGACY_TIMESTAMP);
            }

            return timestamp;
        }

        public void addIndexChunk( ResourceFetcher source, String filename )
            throws IOException
        {
            File chunk = new File( dir, filename );
            FileUtils.copyStreamToFile( new RawInputStreamFacade( source.retrieve( filename ) ), chunk );
            newChunks.add( filename );
        }

        public Date setIndexFile( ResourceFetcher source, String filename )
            throws IOException
        {
            fauxIndexUpdater.cleanCacheDirectory(dir);

            result.setFullUpdate( true );

            File target = new File( dir, filename );
            FileUtils.copyStreamToFile( new RawInputStreamFacade( source.retrieve( filename ) ), target );

            return null;
        }

        @Override
        public void commit()
            throws IOException
        {
            File chunksFile = new File( dir, CHUNKS_FILENAME );
            try (BufferedOutputStream os = new BufferedOutputStream( new FileOutputStream( chunksFile, true ) ); //
                 Writer w = new OutputStreamWriter( os, CHUNKS_FILE_ENCODING ))
            {
                for ( String filename : newChunks )
                {
                    w.write( filename + "\n" );
                }
                w.flush();
            }
            super.commit();
        }

        public List<String> getChunks()
            throws IOException
        {
            ArrayList<String> chunks = new ArrayList<String>();

            File chunksFile = new File( dir, CHUNKS_FILENAME );
            try (BufferedReader r =
                     new BufferedReader( new InputStreamReader( new FileInputStream( chunksFile ), CHUNKS_FILE_ENCODING ) ))
            {
                String str;
                while ( ( str = r.readLine() ) != null )
                {
                    chunks.add( str );
                }
            }
            return chunks;
        }

        public ResourceFetcher getFetcher()
        {
            return new LocalIndexCacheFetcher( dir )
            {
                @Override
                public List<String> getChunks()
                    throws IOException
                {
                    return FauxIndexUpdater.LocalCacheIndexAdaptor.this.getChunks();
                }
            };
        }
    }

    abstract static class LocalIndexCacheFetcher
        extends FileFetcher
    {
        public LocalIndexCacheFetcher( File basedir )
        {
            super( basedir );
        }

        public abstract List<String> getChunks()
            throws IOException;
    }

    /**
     * Cleans specified cache directory. If present, Locker.LOCK_FILE will not be deleted.
     */
    void cleanCacheDirectory(File dir)
        throws IOException
    {
        File[] members = dir.listFiles();
        if ( members == null )
        {
            return;
        }

        for ( File member : members )
        {
            if ( !Locker.LOCK_FILE.equals( member.getName() ) )
            {
                FileUtils.forceDelete( member );
            }
        }
    }

}
