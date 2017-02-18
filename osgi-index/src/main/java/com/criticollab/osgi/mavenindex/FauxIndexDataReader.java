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

import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;

/**
 * An index data reader used to parse transfer index format.
 * 
 * @author Eugene Kuleshov
 */
 class FauxIndexDataReader
{
    private static final int VERSION = 1;
    private final DataInputStream dis;

    public FauxIndexDataReader(InputStream is )
        throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream( is, 1024 * 8 );

        // MINDEXER-13
        // LightweightHttpWagon may have performed automatic decompression
        // Handle it transparently
        bis.mark( 2 );
        InputStream data;
        if ( bis.read() == 0x1f && bis.read() == 0x8b ) // GZIPInputStream.GZIP_MAGIC
        {
            bis.reset();
            data = new GZIPInputStream( bis, 2 * 1024 );
        }
        else
        {
            bis.reset();
            data = bis;
        }

        this.dis = new DataInputStream( data );
    }

    public long readHeader()
        throws IOException
    {
        final byte HDRBYTE = (byte) ((VERSION << 24) >> 24);

        if ( HDRBYTE != dis.readByte() )
        {
            // data format version mismatch
            throw new IOException( "Provided input contains unexpected data (0x01 expected as 1st byte)!" );
        }

        return dis.readLong();
    }

    public FauxDocument readDocument()
        throws IOException
    {
        int fieldCount;
        try
        {
            fieldCount = dis.readInt();
        }
        catch ( EOFException ex )
        {
            return null; // no more documents
        }

        FauxDocument doc = new FauxDocument();

        for ( int i = 0; i < fieldCount; i++ )
        {
            FauxField fauxField = readField();
            doc.add(fauxField);
        }

        // Fix up UINFO field wrt MINDEXER-41
        final FauxField uinfoField =  doc.getField("u");
        final String info =  doc.get("i");
        if (uinfoField!= null && !Strings.isNullOrEmpty(info)) {
            final String[] splitInfo = ArtifactInfo.FS_PATTERN.split( info );
            if ( splitInfo.length > 6 )
            {
                final String extension = splitInfo[6];
                final String uinfoString = uinfoField.stringValue();
                if (uinfoString.endsWith( ArtifactInfo.FS + ArtifactInfo.NA )) {
                    uinfoField.setStringValue( uinfoString + ArtifactInfo.FS + ArtifactInfo.nvl( extension ) );
                }
            }
        }

        return doc;
    }

    private FauxField readField()
        throws IOException
    {
        int flags = dis.read();
        String name = dis.readUTF();
        String value = readLongString();
        return new FauxField( name, value );
    }

    private String readLongString() throws IOException {
        String value;
        int hiWord = dis.readUnsignedShort();
        if(hiWord !=0) {
            hiWord <<= 16;
            hiWord |= dis.readUnsignedShort();
            byte buf[] = new byte[hiWord];
            dis.readFully(buf);
            value = new String(buf, Charsets.UTF_8);
        } else {
            value = dis.readUTF();
        }
        return value;
    }

    /**
     * An index data read result holder
     */
    static class IndexDataReadResult
    {
        private Date timestamp;

        private int documentCount;

        public int getDocumentCount()
        {
            return documentCount;
        }

        public void setDocumentCount(int documentCount)
        {
            this.documentCount = documentCount;
        }

        public Date getTimestamp()
        {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

    }

}
