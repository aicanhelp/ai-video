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
package org.restcomm.media.control.mgcp.tx.cmd;

import java.io.IOException;

import org.restcomm.media.control.mgcp.MgcpEvent;
import org.restcomm.media.control.mgcp.controller.MgcpCall;
import org.restcomm.media.control.mgcp.controller.MgcpConnection;
import org.restcomm.media.control.mgcp.controller.MgcpEndpoint;
import org.restcomm.media.control.mgcp.controller.naming.UnknownEndpointException;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.message.Parameter;
import org.restcomm.media.control.mgcp.tx.Action;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.Task;
import org.restcomm.media.scheduler.TaskChain;
import org.restcomm.media.spi.utils.Text;
import org.apache.log4j.Logger;

/**
 * Modify connection command.
 * 
 * @author Oifa Yulian
 */
public class DeleteConnectionCmd extends Action {
	// response strings
	private final static Text CALLID_MISSING = new Text(
			"Missing call identifier");
	private final static Text UNKNOWN_CALL_IDENTIFIER = new Text(
			"Could not find this call with specified identifier");
	private final static Text CONNECTIONID_EXPECTED = new Text(
			"Connection identifier was not specified");
	private final static Text SUCCESS = new Text("Success");

	private MgcpRequest request;

	private Parameter connectionID;

	// local and domain name parts of the endpoint identifier
	private Text localName = new Text();
	private Text domainName = new Text();

	// layout local and domain names into endpoint identifier
	private Text[] endpointName = new Text[] { localName, domainName };

	private MgcpEndpoint endpoint;
	private MgcpEndpoint[] endpoints = new MgcpEndpoint[1];

	private TaskChain handler;

	// error code and message
	private int code;
	private Text message;

	private int tx, rx;

	private final static Logger logger = Logger.getLogger(DeleteConnectionCmd.class);

	public DeleteConnectionCmd(Scheduler scheduler) {
		handler = new TaskChain(2, scheduler);

		Delete delete = new Delete();
		Responder responder = new Responder();

		handler.add(delete);
		handler.add(responder);

		ErrorHandler errorHandler = new ErrorHandler();

		this.setActionHandler(handler);
		this.setRollbackHandler(errorHandler);
	}

	private class Delete extends Task {

		public Delete() {
			super();
		}

		public int getQueueNumber() {
			return PriorityQueueScheduler.MANAGEMENT_QUEUE;
		}

		private void deleteForEndpoint(MgcpRequest request) {
			// getting endpoint name
			request.getEndpoint().divide('@', endpointName);
			// searching endpoint
			try {
				int n = transaction().find(localName, endpoints);
				if (n == 0) {
					throw new MgcpCommandException(
							MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text(
									"Endpoint not available"));
				}
			} catch (UnknownEndpointException e) {
				throw new MgcpCommandException(
						MgcpResponseCode.ENDPOINT_UNKNOWN, new Text(
								"Endpoint not available"));
			}

			// extract found endpoint
			endpoint = endpoints[0];
			endpoint.deleteAllConnections();
		}

		private void deleteForCall(Parameter callID, MgcpRequest request) {
			// getting call
			MgcpCall call = transaction().getCall(
					callID.getValue().hexToInteger(), false);
			if (call == null) {
				throw new MgcpCommandException(
						MgcpResponseCode.INCORRECT_CALL_ID,
						UNKNOWN_CALL_IDENTIFIER);
			}

			call.deleteConnections();
		}

		@Override
		public long perform() {
			request = (MgcpRequest) getEvent().getMessage();

			Parameter callID = request.getParameter(Parameter.CALL_ID);
			connectionID = request.getParameter(Parameter.CONNECTION_ID);

			if (callID == null && connectionID == null) {
				this.deleteForEndpoint(request);
				return 0;
			}

			if (callID != null && connectionID == null) {
				this.deleteForCall(callID, request);
				return 0;
			}

			if (callID == null) {
				throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR,
						CALLID_MISSING);
			}

			// getting call
			MgcpCall call = transaction().getCall(
					callID.getValue().hexToInteger(), false);
			if (call == null) {
				throw new MgcpCommandException(
						MgcpResponseCode.INCORRECT_CALL_ID,
						UNKNOWN_CALL_IDENTIFIER);
			}

			if (connectionID == null) {
				throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR,
						CONNECTIONID_EXPECTED);
			}

			// getting endpoint name
			request.getEndpoint().divide('@', endpointName);
			// searching endpoint
			try {
				int n = transaction().find(localName, endpoints);
				if (n == 0) {
					throw new MgcpCommandException(
							MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text(
									"Endpoint not available"));
				}
			} catch (UnknownEndpointException e) {
				throw new MgcpCommandException(
						MgcpResponseCode.ENDPOINT_UNKNOWN, new Text(
								"Endpoint not available"));
			}
			// extract found endpoint
			endpoint = endpoints[0];

			MgcpConnection connection = endpoint.getConnection(connectionID
					.getValue().hexToInteger());

			if (connection != null) {
				rx = connection.getPacketsReceived();
				tx = connection.getPacketsTransmitted();

				endpoint.deleteConnection(connectionID.getValue()
						.hexToInteger());
			}

			return 0;
		}

	}

	/**
	 * Searches endpoint specified in message.
	 * 
	 * The result will be stored into variable endpoint.
	 */
	private class EndpointLocator extends Task {

		public EndpointLocator() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.MANAGEMENT_QUEUE;
		}

		@Override
		public long perform() {
			try {
				// searching endpoint
				int n = transaction().find(localName, endpoints);

				if (n == 0) {
					throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
				}

				// extract found endpoint
				endpoint = endpoints[0];

				// checking endpoint's state
				if (endpoint.getState() == MgcpEndpoint.STATE_BUSY) {
					throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
				}
			} catch (Exception e) {
				throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
			}
			return 0;
		}

	}

	private class Responder extends Task {

		public Responder() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.MANAGEMENT_QUEUE;
		}

		@Override
		public long perform() {
			MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
			MgcpResponse response = (MgcpResponse) evt.getMessage();
			response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
			response.setResponseString(SUCCESS);
			response.setTxID(transaction().getId());

			if (connectionID != null) {
				response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
			}
			response.setParameter(Parameter.CONNECTION_PARAMETERS, new Text("PS=" + tx + ", PR=" + rx));

			try {
				transaction().getProvider().send(evt);
			} catch (IOException e) {
				logger.error(e);
			} finally {
				evt.recycle();
			}

			return 0;
		}

	}

	private class ErrorHandler extends Task {

		public ErrorHandler() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.MANAGEMENT_QUEUE;
		}

		@Override
		public long perform() {
			code = ((MgcpCommandException) transaction().getLastError()).getCode();
			message = ((MgcpCommandException) transaction().getLastError()).getErrorMessage();

			MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
			MgcpResponse response = (MgcpResponse) evt.getMessage();
			response.setResponseCode(code);
			response.setResponseString(message);
			response.setTxID(transaction().getId());

			if (connectionID != null) {
				response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
			}

			try {
				transaction().getProvider().send(evt);
			} catch (IOException e) {
				logger.error(e);
			} finally {
				evt.recycle();
			}

			return 0;
		}

	}

}
