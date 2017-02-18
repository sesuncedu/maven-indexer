package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/18/17.
 */

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class WagonFetcher implements FauxResourceFetcher {
    private final TransferListener listener;

    private final AuthenticationInfo authenticationInfo;

    private final ProxyInfo proxyInfo;

    private final Wagon wagon;

    public WagonFetcher(final Wagon wagon, final TransferListener listener, final AuthenticationInfo authenticationInfo,
                        final ProxyInfo proxyInfo) {
        this.wagon = wagon;
        this.listener = listener;
        this.authenticationInfo = authenticationInfo;
        this.proxyInfo = proxyInfo;
    }

    public void connect(final String id, final String url) throws IOException {
        Repository repository = new Repository(id, url);

        try {
            // wagon = wagonManager.getWagon( repository );

            if (listener != null) {
                wagon.addTransferListener(listener);
            }

            // when working in the context of Maven, the WagonManager is already
            // populated with proxy information from the Maven environment

            if (authenticationInfo != null) {
                if (proxyInfo != null) {
                    wagon.connect(repository, authenticationInfo, proxyInfo);
                } else {
                    wagon.connect(repository, authenticationInfo);
                }
            } else {
                if (proxyInfo != null) {
                    wagon.connect(repository, proxyInfo);
                } else {
                    wagon.connect(repository);
                }
            }
        } catch (AuthenticationException ex) {
            String msg = "Authentication exception connecting to " + repository;
            logError(msg, ex);
            IOException ioException = new IOException(msg);
            ioException.initCause(ex);
            throw ioException;
        } catch (WagonException ex) {
            String msg = "Wagon exception connecting to " + repository;
            logError(msg, ex);
            IOException ioException = new IOException(msg);
            ioException.initCause(ex);
            throw ioException;
        }
    }

    public void disconnect() throws IOException {
        if (wagon != null) {
            try {
                wagon.disconnect();
            } catch (ConnectionException ex) {
                IOException ioe = new IOException(ex.toString());
                ioe.initCause(ex);
                throw ioe;
            }
        }
    }

    public InputStream retrieve(String name) throws IOException, FileNotFoundException {
        final File target = File.createTempFile(name, "");
        retrieve(name, target);
        return new FileInputStream(target) {
            @Override
            public void close() throws IOException {
                super.close();
                target.delete();
            }
        };
    }

    public void retrieve(final String name, final File targetFile) throws IOException {
        try {
            wagon.get(name, targetFile);
        } catch (AuthorizationException e) {
            String msg = "Authorization exception retrieving " + name;
            logError(msg, e);
            IOException ioException = new IOException(msg);
            ioException.initCause(e);
            throw ioException;
        } catch (ResourceDoesNotExistException e) {
            String msg = "Resource " + name + " does not exist";
            logError(msg, e);
            FileNotFoundException fileNotFoundException = new FileNotFoundException(msg);
            fileNotFoundException.initCause(e);
            throw fileNotFoundException;
        } catch (WagonException e) {
            String msg = "Transfer for " + name + " failed";
            logError(msg, e);
            IOException ioException = new IOException(msg + "; " + e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
    }

    private void logError(final String msg, final Exception ex) {
        if (listener != null) {
            listener.debug(msg + "; " + ex.getMessage());
        }
    }
}
