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

import org.restcomm.media.control.mgcp.MgcpEvent;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.Parameter;
import org.restcomm.media.control.mgcp.tx.Action;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class ActionSelector  {

	private final static Text AUDIT_CONNECTION = new Text("AUCX");
    private final static Text CREATE_CONNECTION = new Text("CRCX");
    private final static Text MODIFY_CONNECTION = new Text("MDCX");
    private final static Text DELETE_CONNECTION = new Text("DLCX");
    private final static Text REQUEST_NOTIFICATION = new Text("RQNT");
    private final static Text REQUEST_NOTIFY = new Text("NTFY");
    private final static Text ENDPOINT_CONFIGURATION = new Text("EPCF");
    
    private Text command;
    private MgcpRequest request;
    
    //actions
    private AuditConnectionCmd aucx;
    private CreateConnectionCmd crcx;
    private ModifyConnectionCmd mdcx;
    private DeleteConnectionCmd dlcx;
    private NotificationRequestCmd rqnt;
    private EndpointConfigurationCmd epcf;
    private NotifyCmd ntfy;
    
    /**
     * Creates new action selector.
     * 
     * @param scheduler 
     */
    public ActionSelector(Scheduler scheduler) {
    	aucx = new AuditConnectionCmd(scheduler);
        crcx = new CreateConnectionCmd(scheduler);
        mdcx = new ModifyConnectionCmd(scheduler);
        dlcx = new DeleteConnectionCmd(scheduler);
        rqnt = new NotificationRequestCmd(scheduler);
        ntfy = new NotifyCmd(scheduler);
        epcf = new EndpointConfigurationCmd(scheduler);
    }
    
    public Action getAction(MgcpEvent event) {
        switch (event.getEventID()) {
            case MgcpEvent.REQUEST :
                request = (MgcpRequest) event.getMessage();
                command = request.getCommand();
        
                //select action using message type and execute action
                if (command.equals(CREATE_CONNECTION)) {
                    return crcx;
                } else if (command.equals(MODIFY_CONNECTION)) {
                    return mdcx;
                } else if (command.equals(ENDPOINT_CONFIGURATION)) {
                    return epcf;
                } else if (command.equals(DELETE_CONNECTION)) {
                	if(request.getParameter(Parameter.REASON_CODE)!=null) {
                		//its connection deletion from ms
                		return ntfy;	
                	}
                	else {
                		//its delete connection request
                		return dlcx;
                	}
                } else if (command.equals(REQUEST_NOTIFICATION)) {
                    return rqnt;
                } else if (command.equals(REQUEST_NOTIFY)) {
                    return ntfy;
                } else if (command.equals(AUDIT_CONNECTION)) {
                	return aucx;
                }
                
                break;
            case MgcpEvent.RESPONSE :
                break;
        }
        
        return null;
    }
    
}
