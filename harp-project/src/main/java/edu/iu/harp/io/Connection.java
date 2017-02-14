/*
 * Copyright 2013-2016 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.harp.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/*******************************************************
 * The connection object as a client
 ******************************************************/
public class Connection {

    private final String node;
    private final int port;
    private OutputStream out;
    private InputStream in;
    private Socket socket;
    private final boolean useCache;

    /**
     * Construct a connection
     * 
     * @param node
     *            the host
     * @param port
     *            the port
     * @param timeOutMs
     *            the timeout value to be used in milliseconds.
     * @param useCache
     *            use cache or not
     * @throws Exception
     */
    Connection(String node, int port, int timeOutMs, boolean useCache) throws Exception {
	this.node = node;
	this.port = port;
	this.useCache = useCache;
	try {
	    InetAddress addr = InetAddress.getByName(node);
	    SocketAddress sockaddr = new InetSocketAddress(addr, port);
	    this.socket = new Socket();
	    IOUtil.setSocketOptions(socket);
	    this.socket.connect(sockaddr, timeOutMs);
	    this.out = socket.getOutputStream();
	    this.in = socket.getInputStream();
	} catch (Exception e) {
	    close();
	    throw e;
	}
    }

    /**
     * Get the host
     * 
     * @return the host
     */
    public String getNode() {
	return this.node;
    }

    /**
     * Get the port
     * 
     * @return the port
     */
    public int getPort() {
	return this.port;
    }

    /**
     * Get the OutputStream
     * 
     * @return the OutputStream
     */
    public OutputStream getOutputStream() {
	return this.out;
    }

    /**
     * Get the InputStream
     * 
     * @return the InputStream
     */
    public InputStream getInputDtream() {
	return this.in;
    }

    /**
     * Close the connection
     */
    void close() {
	if (out != null || in != null || socket != null) {
	    try {
		if (out != null) {
		    out.close();
		}
	    } catch (IOException e) {
	    }
	    try {
		if (in != null) {
		    in.close();
		}
	    } catch (IOException e) {
	    }
	    try {
		if (socket != null) {
		    socket.close();
		}
	    } catch (IOException e) {
	    }
	    out = null;
	    in = null;
	    socket = null;
	}
    }

    /**
     * Get a connection from the ConnPool
     * 
     * @param host
     *            the host
     * @param port
     *            the port
     * @param useCache
     *            use cache or not
     * @return the connection object
     */
    public static Connection create(String host, int port, boolean useCache) {
	return ConnPool.get().getConn(host, port, useCache);
    }

    /**
     * Release the connection object
     */
    public void release() {
	if (useCache) {
	    ConnPool.get().releaseConn(this);
	} else {
	    close();
	}

    }

    /**
     * Free the connection object
     */
    public void free() {
	if (useCache) {
	    ConnPool.get().freeConn(this);
	} else {
	    close();
	}
    }
}
