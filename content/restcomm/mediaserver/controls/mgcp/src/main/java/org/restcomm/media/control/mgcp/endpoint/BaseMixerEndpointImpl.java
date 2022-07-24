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

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.connection.BaseConnection;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.ResourceUnavailableException;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public class BaseMixerEndpointImpl extends BaseEndpointImpl {

	protected AudioMixer audioMixer;
	protected OOBMixer oobMixer;

	private AtomicInteger loopbackCount = new AtomicInteger(0);
	private AtomicInteger readCount = new AtomicInteger(0);
	private AtomicInteger writeCount = new AtomicInteger(0);

	public BaseMixerEndpointImpl(String localName) {
		super(localName);
	}

	@Override
	public void start() throws ResourceUnavailableException {
		super.start();
		audioMixer = new AudioMixer(getScheduler());
		oobMixer = new OOBMixer(getScheduler());
	}

	@Override
	public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
		Connection connection = super.createConnection(type, isLocal);
		audioMixer.addComponent(((BaseConnection) connection).getAudioComponent());
		oobMixer.addComponent(((BaseConnection) connection).getOOBComponent());
		return connection;
	}

	@Override
	public void deleteConnection(Connection connection, ConnectionType connectionType) {
		super.deleteConnection(connection, connectionType);
		audioMixer.release(((BaseConnection) connection).getAudioComponent());
		oobMixer.release(((BaseConnection) connection).getOOBComponent());
	}
	
	@Override
	public void configure(boolean isALaw) {
	    
	}

	@Override
	public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
		int readCount = 0, loopbackCount = 0, writeCount = 0;
		switch (oldMode) {
		case RECV_ONLY:
			readCount -= 1;
			break;
		case SEND_ONLY:
			writeCount -= 1;
			break;
		case SEND_RECV:
		case CONFERENCE:
			readCount -= 1;
			writeCount -= 1;
			break;
		case NETWORK_LOOPBACK:
			loopbackCount -= 1;
			break;
		default:
			// XXX handle default case
			break;
		}

		switch (newMode) {
		case RECV_ONLY:
			readCount += 1;
			break;
		case SEND_ONLY:
			writeCount += 1;
			break;
		case SEND_RECV:
		case CONFERENCE:
			readCount += 1;
			writeCount += 1;
			break;
		case NETWORK_LOOPBACK:
			loopbackCount += 1;
			break;
		default:
			// XXX handle default case
			break;
		}

		if (readCount != 0 || writeCount != 0 || loopbackCount != 0) {
			// something changed
			loopbackCount = this.loopbackCount.addAndGet(loopbackCount);
			readCount = this.readCount.addAndGet(readCount);
			writeCount = this.writeCount.addAndGet(writeCount);

			if (loopbackCount > 0 || readCount == 0 || writeCount == 0) {
				audioMixer.stop();
				oobMixer.stop();
			} else {
				audioMixer.start();
				oobMixer.start();
			}
		}
	}
}
