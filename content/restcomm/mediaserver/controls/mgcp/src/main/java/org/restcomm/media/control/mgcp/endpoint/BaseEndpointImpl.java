/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.endpoint;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.concurrent.ConcurrentMap;
import org.restcomm.media.control.mgcp.connection.BaseConnection;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.Endpoint;
import org.restcomm.media.spi.EndpointState;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public abstract class BaseEndpointImpl implements Endpoint {

    private static final Logger logger = Logger.getLogger(BaseEndpointImpl.class);

    // Core Components
    private PriorityQueueScheduler scheduler;
    protected ResourcesPool resourcesPool;
    
    // Endpoint state
	private final String localName;
	private EndpointState state = EndpointState.READY;
	protected MediaGroup mediaGroup;
	private final ConcurrentMap<Connection> connections;

	public BaseEndpointImpl(String localName) {
		this.localName = localName;
		this.connections = new ConcurrentMap<Connection>();
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public void setScheduler(PriorityQueueScheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public PriorityQueueScheduler getScheduler() {
		return scheduler;
	}

	/**
	 * Assigns resources pool.
	 * 
	 * @param resourcesPool
	 *            the resources pool instance.
	 */
	public void setResourcesPool(ResourcesPool resourcesPool) {
		this.resourcesPool = resourcesPool;
	}

	/**
	 * Provides access to the resources pool.
	 * 
	 * @return scheduler instance.
	 */
	public ResourcesPool getResourcesPool() {
		return resourcesPool;
	}

	@Override
	public EndpointState getState() {
		return state;
	}

	/**
	 * Modifies state indicator.
	 * 
	 * @param state
	 *            the new value of the state indicator.
	 */
	public void setState(EndpointState state) {
		this.state = state;
	}

	@Override
	public void start() throws ResourceUnavailableException {
		// do checks before start
		if (scheduler == null) {
			throw new ResourceUnavailableException("Scheduler is not available");
		}

		if (resourcesPool == null) {
			throw new ResourceUnavailableException("Resources pool is not available");
		}

		// create connections subsystem
		mediaGroup = new MediaGroup(resourcesPool, this);
	}

	@Override
	public void stop() {
		mediaGroup.releaseAll();
		deleteAllConnections();
		// TODO: unregister at scheduler level
        if (logger.isInfoEnabled()) {
            logger.info("Stopped endpoint " + localName);
        }
	}

	@Override
	public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
		Connection connection = null;
		switch (type) {
		case RTP:
			connection = resourcesPool.newConnection(false);
			break;
		case LOCAL:
			connection = resourcesPool.newConnection(true);
			break;
		}

		connection.setIsLocal(isLocal);

		try {
			((BaseConnection) connection).bind();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceUnavailableException(e.getMessage());
		}

		connection.setEndpoint(this);
		connections.put(connection.getId(), connection);
		return connection;
	}

	@Override
	public void deleteConnection(Connection connection) {
		((BaseConnection) connection).close();
	}

	@Override
	public void deleteConnection(Connection connection, ConnectionType connectionType) {
		connections.remove(connection.getId());

		switch (connectionType) {
		case RTP:
			resourcesPool.releaseConnection(connection, false);
			break;
		case LOCAL:
			resourcesPool.releaseConnection(connection, true);
			break;
		}

		if (connections.size() == 0) {
			mediaGroup.releaseAll();
		}
	}

	@Override
	public void deleteAllConnections() {
	    Iterator<Connection> connectionsIterator = connections.valuesIterator();
		while (connectionsIterator.hasNext()) {
			((BaseConnection) connectionsIterator.next()).close();
		}
	}

	public Connection getConnection(int connectionID) {
		return connections.get(connectionID);
	}

	@Override
	public int getActiveConnectionsCount() {
		return connections.size();
	}

	@Override
	public Component getResource(MediaType mediaType, ComponentType componentType) {
		switch (mediaType) {
		case AUDIO:
			switch (componentType) {
			case PLAYER:
				return mediaGroup.getPlayer();
			case RECORDER:
				return mediaGroup.getRecorder();
			case DTMF_DETECTOR:
				return mediaGroup.getDtmfDetector();
			case DTMF_GENERATOR:
				return mediaGroup.getDtmfGenerator();
			default:
				break;
			}
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public boolean hasResource(MediaType mediaType, ComponentType componentType) {
		switch (mediaType) {
		case AUDIO:
			switch (componentType) {
			case PLAYER:
				return mediaGroup.hasPlayer();
			case RECORDER:
				return mediaGroup.hasRecorder();
			case DTMF_DETECTOR:
				return mediaGroup.hasDtmfDetector();
			case DTMF_GENERATOR:
				return mediaGroup.hasDtmfGenerator();
			default:
				break;
			}
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void releaseResource(MediaType mediaType, ComponentType componentType) {
		switch (mediaType) {
		case AUDIO:
			switch (componentType) {
			case PLAYER:
				mediaGroup.releasePlayer();
			case RECORDER:
				mediaGroup.releaseRecorder();
			case DTMF_DETECTOR:
				mediaGroup.releaseDtmfDetector();
			case DTMF_GENERATOR:
				mediaGroup.releaseDtmfGenerator();
			default:
				break;
			}
			break;
		default:
			break;
		}
	}

}
