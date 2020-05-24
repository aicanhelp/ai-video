package org.restcomm.media.control.mgcp;

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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.media.concurrent.ConcurrentMap;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author yulian oifa
 */
@Ignore
public class MgcpProviderLoadTest {
	
    private Clock clock = new WallClock();
    
    private PriorityQueueScheduler mediaScheduler;
    private final Scheduler scheduler = new ServiceScheduler();
    private UdpManager udpInterface;

    private MgcpProvider provider1, provider2;
    
    private Server server;
    
    private ConcurrentMap<Client> clients = new ConcurrentMap<Client>();
    private ClientListener demux;
    
    private volatile int errorCount;
    private AtomicInteger txID = new AtomicInteger(1);
    
    @Before
    public void setUp() throws IOException, TooManyListenersException {
    	mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();
        
        udpInterface = new UdpManager(scheduler, new RtpPortManager(), new RtpPortManager());
        udpInterface.setLocalBindAddress("127.0.0.1");
        udpInterface.setBindAddress("127.0.0.1");
        udpInterface.setLocalNetwork("127.0.0.1");
        udpInterface.setLocalSubnet("255.255.255.255");
        scheduler.start();
        udpInterface.start();
        
        provider1 = new MgcpProvider(udpInterface, 1024);
        provider2 = new MgcpProvider(udpInterface, 1025);
        
        provider1.activate();
        provider2.activate();
        
        server = new Server(provider1);
        
        demux = new ClientListener();
        provider2.addListener(demux);    	
    }
    
    @After
    public void tearDown() {
        if (provider1 != null) provider1.shutdown();
        if (provider2 != null) provider2.shutdown();
        
        udpInterface.stop();
        scheduler.stop();
        mediaScheduler.stop();
    }

    /**
     * Test of createEvent method, of class MgcpProvider.
     */
    @Test
    public void testProcess() throws Exception {
    	for (int i = 0; i < 30; i++) {
            new Thread(new Client(provider2)).start();
            Thread.sleep(10);
        }
        
        Thread.sleep(5000);
        assertEquals(0, errorCount);    	
    }

    private class Server implements MgcpListener {
        
        private MgcpProvider mgcpProvider;
        
        public Server(MgcpProvider mgcpProvider) {
            this.mgcpProvider = mgcpProvider;
            try {
                this.mgcpProvider.addListener(this);
            } catch (Exception e) {
            }
        }

        @Override
        public void process(MgcpEvent event) {
            //CRCX request expected
            MgcpEvent evt = null;
            try {
                MgcpRequest request = (MgcpRequest) event.getMessage();
                if (!request.getCommand().equals(new Text("CRCX"))) {
                    errorCount++;
                }
                SocketAddress destination = event.getAddress();
                evt = provider1.createEvent(MgcpEvent.RESPONSE, destination);
                MgcpResponse resp = (MgcpResponse) evt.getMessage();

                resp.setResponseCode(200);
                resp.setTxID(request.getTxID());
                resp.setResponseString(new Text("Success"));

                try {
                    mgcpProvider.send(evt);
                } catch (Exception e) {                	
                }
            } finally {
                event.recycle();
                evt.recycle();
            }
        }
        
    }
    
    private class Client implements Runnable {
        private MgcpProvider mgcpProvider;
        private volatile boolean passed = false;
        
        public Client(MgcpProvider mgcpProvider) {
            this.mgcpProvider = mgcpProvider;
        }

        @Override
        public void run() {
            MgcpEvent evt = null;

            try {
                InetSocketAddress destination = new InetSocketAddress("127.0.0.1", 1024);

                evt = provider1.createEvent(MgcpEvent.REQUEST, destination);
                MgcpRequest req = (MgcpRequest) evt.getMessage();

                req.setCommand(new Text("CRCX"));
                req.setTxID(txID.getAndIncrement());
                req.setEndpoint(new Text("test@127.0.0.1"));
                req.setParameter(new Text("c"), new Text("abcd"));

                try {
                    mgcpProvider.send(evt);
                } catch (Exception e) {                	
                }

                clients.put(req.getTxID(), this);

                synchronized (this) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                }

                if (!passed) {
                    errorCount++;
                }
            } finally {
                evt.recycle();
            }
        }
        
        public void terminate() {
            this.passed = true;
            synchronized(this) {
                notify();
            }
        }
    }
    
    private class ClientListener implements MgcpListener {

    	@Override
        public void process(MgcpEvent event) {
            MgcpResponse response = (MgcpResponse) event.getMessage();
            try {
                int txID = response.getTxID();

                Client client = clients.remove(txID);
                if (client != null) {
                    client.terminate();
                } else {
                    errorCount++;
                }
            } finally {
                event.recycle();
            }
		}

    }
}
