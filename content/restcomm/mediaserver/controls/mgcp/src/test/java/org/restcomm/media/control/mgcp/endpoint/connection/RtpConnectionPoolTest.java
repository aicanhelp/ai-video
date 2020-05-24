/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
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

package org.restcomm.media.control.mgcp.endpoint.connection;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.junit.Test;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionImpl;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.dsp.DspFactory;

import junit.framework.Assert;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionPoolTest {
    
    private final Clock clock;
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler taskScheduler;
    private final UdpManager udpManager;
    
    // Dtls provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);
    
    private final ChannelsManager connectionFactory;
    private final DspFactory dspFactory;
    
    public RtpConnectionPoolTest() {
        this.clock = new WallClock();
        this.mediaScheduler = new PriorityQueueScheduler();
        this.taskScheduler = new ServiceScheduler(clock);
        this.udpManager = new UdpManager(taskScheduler, new RtpPortManager(), new RtpPortManager());
        this.connectionFactory = new ChannelsManager(udpManager, dtlsServerProvider);
        this.dspFactory = new DspFactoryImpl();
        
        this.mediaScheduler.setClock(clock);
        this.connectionFactory.setScheduler(mediaScheduler);
    }

    @Test
    public void testConnectionRecycle() {
        // given
        RtpConnectionFactory factory = new RtpConnectionFactory(connectionFactory, dspFactory);
        RtpConnectionPool pool = new RtpConnectionPool(1, factory);
        
        // when
        RtpConnectionImpl connection = pool.poll();
        
        String oldCname = connection.getCname();
        int oldId = connection.getId();
        
        pool.offer(connection);
        connection = pool.poll();
        
        // then
        Assert.assertTrue(pool.isEmpty());
        Assert.assertTrue(!oldCname.equals(connection.getCname()));
        Assert.assertEquals(oldId, connection.getId());
    }
    
}
