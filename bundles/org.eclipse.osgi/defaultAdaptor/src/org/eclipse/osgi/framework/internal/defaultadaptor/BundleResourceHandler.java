/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.defaultadaptor;

import java.io.IOException;
import java.net.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Bundle;
import org.eclipse.osgi.framework.internal.protocol.ProtocolActivator;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.BundleContext;

/**
 * URLStreamHandler the bundleentry and bundleresource protocols.
 */

public abstract class BundleResourceHandler extends URLStreamHandler implements ProtocolActivator
{
	public static final String SECURITY_AUTHORIZED = "SECURITY_AUTHORIZED";
	protected BundleContext context;
	protected BundleEntry bundleEntry;

	/** Single object for permission checks */
	protected AdminPermission adminPermission;

    /**
     * Constructor for a bundle protocol resource URLStreamHandler.
     */
    public BundleResourceHandler()
    {
    }

    public BundleResourceHandler(BundleEntry bundleEntry, BundleContext context) {
    	this.context = context;
    	this.bundleEntry = bundleEntry;
    }

    /** 
     * Parse reference URL. 
     */
    protected void parseURL(URL url, String str, int start, int end)
    {
    	if (end <= start) {
    		return;
    	}

		// Check the permission of the caller to see if they
		// are allowed access to the resource.
		checkAdminPermission();

    	String bundleId,path;
    	if (str.startsWith(url.getProtocol())) {
    		if (str.charAt(start) != '/' || str.charAt(start+1) != '/')
    			throw new IllegalArgumentException(AdaptorMsg.formatter.getString("URL_IS_OPAQUE",url.getProtocol()));
    		start += 2;
    		int slash = str.indexOf("/",start);
    		if (slash < 0) {
    			throw new IllegalArgumentException(AdaptorMsg.formatter.getString("URL_NO_PATH"));
    		}
    		bundleId = str.substring(start,slash);
    		path = str.substring(slash,end);
    	}
    	else {
    		// A call to a URL constructor has been made that
    		// uses an authorized URL as its context.
    		bundleId = url.getHost();
    		if (str.length() > 0 && str.charAt(0) == '/') {
    			// does not specify a relative path.
				path = str;
			} 
    		else {
    			// relative path specified.
				StringBuffer pathBuffer = new StringBuffer(url.getPath());
				if (pathBuffer.charAt(pathBuffer.length() - 1) != '/')
					pathBuffer.append('/');
				pathBuffer.append(str);
				path = pathBuffer.toString();
			}
    		// null out bundleEntry because it will not be valid for the new path
    		bundleEntry = null;
    	}

    	// Setting the authority portion of the URL to SECURITY_ATHORIZED
    	// ensures that this URL was created by using this parseURL
    	// method.  The openConnection method will only open URLs
    	// that have the authority set to this.
    	setURL(url, url.getProtocol(), bundleId, 0, SECURITY_AUTHORIZED, null, path, null, null);
    }

    /**
     * Establishes a connection to the resource specified by <code>URL</code>.
     * Since different protocols may have unique ways of connecting, it must be
     * overridden by the subclass.
     *
     * @return java.net.URLConnection
     * @param url java.net.URL
     *
     * @exception	IOException 	thrown if an IO error occurs during connection establishment
     */
    protected URLConnection openConnection(URL url) throws IOException
    {
    	String authority = url.getAuthority();
    	// check to make sure that this URL was created using the
    	// parseURL method.  This ensures the security check was done
    	// at URL construction.
    	if (!url.getAuthority().equals(SECURITY_AUTHORIZED)) {
    		// No admin security check was made better check now.
    		checkAdminPermission();
    	}

    	if (bundleEntry != null){
    		return (new BundleURLConnection(url,bundleEntry));
    	}
    	else {
    		String bidString = url.getHost();
    		if (bidString == null) {
    			throw new IOException(AdaptorMsg.formatter.getString("URL_NO_BUNDLE_ID", url.toExternalForm()));
    		}
    		Bundle bundle = null;
    		try {
    			Long bundleID = new Long(bidString);
    			bundle = (Bundle) context.getBundle(bundleID.longValue());
    		} catch (NumberFormatException nfe) {
    			throw new MalformedURLException(AdaptorMsg.formatter.getString("URL_INVALID_BUNDLE_ID", bidString));
    		}

    		if (bundle == null) {
    			throw new IOException(AdaptorMsg.formatter.getString("URL_NO_BUNDLE_FOUND", url.toExternalForm()));
    		}
    		return(new BundleURLConnection(url, findBundleEntry(url,bundle)));	
    	}
    }

    /**
     * Finds the bundle entry for this protocal.  This is handled
     * differently for Bundle.gerResource() and Bundle.getEntry()
     * because getResource uses the bundle classloader and getEntry
     * only used the base bundle file.
     * @param url The URL to find the BundleEntry for.
     * @return
     */
    abstract protected BundleEntry findBundleEntry(URL url,Bundle bundle) throws IOException;

    /**
     * Converts a bundle URL to a String.
     *
     * @param   url   the URL.
     * @return  a string representation of the URL.
     */
    protected String toExternalForm(URL url)
    {
        StringBuffer result = new StringBuffer(url.getProtocol());
        result.append("://");

        String bundleId = url.getHost();
        if ((bundleId != null) && (bundleId.length() > 0))
        {
            result.append(bundleId);
        }

        String path = url.getPath();
        if (path != null)
        {
            if ((path.length() > 0) && (path.charAt(0) != '/'))  /* if name doesn't have a leading slash */
            {
                result.append("/");
            }

            result.append(path);
        }

        return (result.toString());
    }

	public void start(BundleContext context, FrameworkAdaptor adaptor) {
		this.context = context;
	}

	protected int hashCode(URL url) {
		int hash=0;
		String protocol = url.getProtocol();
		if (protocol != null)
			hash += protocol.hashCode();

		String host = url.getHost();
		if (host != null)
			hash += host.hashCode();

		String path = url.getPath();
		if (path != null)
			hash += path.hashCode();
		return hash;
	}


	protected boolean equals(URL url1, URL url2) {
		return sameFile(url1, url2);
	}

	protected synchronized InetAddress getHostAddress(URL url) {
		return null;
	}

	protected boolean hostsEqual(URL url1, URL url2) {
		String host1 = url1.getHost();
		String host2 = url2.getHost();
		if (host1 != null && host2 != null)
			return host1.equalsIgnoreCase(host2);
		else
			return (host1 == null && host2 == null);
	}

	protected boolean sameFile(URL url1, URL url2) {
		String p1 = url1.getProtocol();
		String p2 = url2.getProtocol();
		if (!((p1 == p2) || (p1 != null && p1.equalsIgnoreCase(p2))))
			return false;

		if (!hostsEqual(url1,url2))
			return false;

		String a1 = url1.getAuthority();
		String a2 = url2.getAuthority();
		if (!((a1 == a2) || (a1 != null && a1.equals(a2))))
			return false;

		String path1 = url1.getPath();
		String path2 = url2.getPath();
		if (!((path1 == path2) || (path1 != null && path1.equals(path2))))
			return false;

		return true;
	}

	protected void checkAdminPermission() {
		SecurityManager sm = System.getSecurityManager();

		if (sm != null) {
			if (adminPermission == null) {
				adminPermission = new AdminPermission();
			}

			sm.checkPermission(adminPermission);
		}
	}
}
