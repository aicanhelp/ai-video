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

package org.restcomm.media.client.mgcp.test.notify;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.Constants;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;

import org.apache.log4j.Logger;
import org.restcomm.media.client.mgcp.stack.JainMgcpExtendedListener;
import org.restcomm.media.client.mgcp.stack.JainMgcpStackProviderImpl;

public class CA implements JainMgcpExtendedListener {

	private static Logger logger = Logger.getLogger(CA.class);

	private JainMgcpStackProviderImpl caProvider;
	private int mgStack = 0;
	private boolean responseReceived = false;

	public CA(JainMgcpStackProviderImpl caProvider, JainMgcpStackProviderImpl mgwProvider) {
		this.caProvider = caProvider;
		mgStack = mgwProvider.getJainMgcpStack().getPort();
	}

	public void sendNotify() {

		try {
			caProvider.addJainMgcpListener(this);

			EndpointIdentifier endpointID = new EndpointIdentifier("media/trunk/Announcement/enp-1", "127.0.0.1:"
					+ mgStack);

			Notify notify = new Notify(this, endpointID, caProvider.getUniqueRequestIdentifier(),
					new EventName[] { new EventName(PackageName.Announcement, MgcpEvent.oc, new ConnectionIdentifier(
							"1")) });
			notify.setTransactionHandle(caProvider.getUniqueTransactionHandler());

			// TODO We are forced to set the NotifiedEntity, but this should
			// happen automatically. Fix this in MGCP Stack
			NotifiedEntity notifiedEntity = new NotifiedEntity("127.0.0.1", "127.0.0.1", mgStack);
			notify.setNotifiedEntity(notifiedEntity);

			caProvider.sendMgcpEvents(new JainMgcpEvent[] { notify });

			logger.debug(" Notify command sent for TxId " + notify.getTransactionHandle());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			NotifyTest.fail("Unexpected Exception");
		}
	}

	public void checkState() {
		NotifyTest.assertTrue("Expect to receive NTFY Response", responseReceived);

	}

	public void transactionEnded(int handle) {
		logger.info("transactionEnded " + handle);

	}

	public void transactionRxTimedOut(JainMgcpCommandEvent command) {
		logger.info("transactionRxTimedOut " + command);

	}

	public void transactionTxTimedOut(JainMgcpCommandEvent command) {
		logger.info("transactionTxTimedOut " + command);

	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent jainmgcpcommandevent) {
		logger.info("processMgcpCommandEvent " + jainmgcpcommandevent);
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent jainmgcpresponseevent) {
		logger.debug("processMgcpResponseEvent = " + jainmgcpresponseevent);
		switch (jainmgcpresponseevent.getObjectIdentifier()) {
		case Constants.RESP_NOTIFY:
			responseReceived = true;
			break;
		default:
			logger.warn("This RESPONSE is unexpected " + jainmgcpresponseevent);
			responseReceived = false;
			break;

		}

	}

}
