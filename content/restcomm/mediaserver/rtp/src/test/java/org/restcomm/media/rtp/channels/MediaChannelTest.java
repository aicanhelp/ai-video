/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.media.rtp.channels;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.rtcp.RtcpChannel;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.channels.AudioChannel;
import org.restcomm.media.rtp.crypto.DtlsSrtpServer;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.rtp.sdp.SdpFactory;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaChannelTest {
	
	private final PriorityQueueScheduler mediaScheduler;
	private final Scheduler scheduler;
	private final UdpManager udpManager;
	private final ChannelsManager channelsManager;
	private final Clock wallClock;
	
	private final ChannelFactory factory;
	
	private final AudioChannel localChannel;
	private final AudioChannel remoteChannel;
	
    private static final int cipherSuites[] = { 0xc030, 0xc02f, 0xc028, 0xc027, 0xc014, 0xc013, 0x009f, 0x009e, 0x006b, 0x0067,
            0x0039, 0x0033, 0x009d, 0x009c, 0x003d, 0x003c, 0x0035, 0x002f, 0xc02b };
	
	public MediaChannelTest() throws IOException {
	    // given
	    DtlsSrtpServerProvider mockedDtlsServerProvider = mock(DtlsSrtpServerProvider.class);
	    DtlsSrtpServer mockedDtlsSrtpServer = mock(DtlsSrtpServer.class);
	    
	    // when
	    when(mockedDtlsServerProvider.provide()).thenReturn(mockedDtlsSrtpServer);
	    when(mockedDtlsSrtpServer.getCipherSuites()).thenReturn(cipherSuites);
	    
	    // then
		this.wallClock = new WallClock();
		this.mediaScheduler = new PriorityQueueScheduler();
		this.mediaScheduler.setClock(this.wallClock);
		this.scheduler = new ServiceScheduler();
		this.udpManager = new UdpManager(scheduler, new RtpPortManager(), new RtpPortManager());
		this.channelsManager = new ChannelsManager(udpManager, mockedDtlsServerProvider);
		this.channelsManager.setScheduler(this.mediaScheduler);
		
		this.factory = new ChannelFactory();
		this.localChannel = factory.buildAudioChannel();
		this.remoteChannel = factory.buildAudioChannel();
	}
	
	@Before
	public void before() throws InterruptedException {
		this.mediaScheduler.start();
		this.scheduler.start();
		this.udpManager.start();
	}
	
	@After
	public void after() {
		this.mediaScheduler.stop();
		this.udpManager.stop();
		if(this.localChannel.isOpen()) {
			this.localChannel.close();
		}
		if(this.remoteChannel.isOpen()) {
			this.remoteChannel.close();
		}
		this.scheduler.stop();
	}

	@Test
	public void testSipCallNonRtcpMux() throws IllegalStateException, IOException, InterruptedException {
		/* GIVEN */
		boolean rtcpMux = false;
		
		/* WHEN */
		// activate local channel and bind it to local address
		// there will be two underlying channels for RTP and RTCP
		localChannel.open();
		localChannel.bind(false, false);
		
		String localAddress = localChannel.rtpChannel.getLocalHost();
		int localRtpPort = localChannel.rtpChannel.getLocalPort();
		int localRtcpPort = localChannel.rtcpChannel.getLocalPort(); 
		MediaDescriptionField audioOffer = SdpFactory.buildMediaDescription(localChannel, true);
		
		// activate "remote" channel and bind it to local address
		// there will be two underlying channels for RTP and RTCP
		remoteChannel.open();
		remoteChannel.bind(false, rtcpMux);
		
		String remoteAddress = remoteChannel.rtpChannel.getLocalHost();
		int remoteRtpPort = remoteChannel.rtpChannel.getLocalPort();
		int remoteRtcpPort = remoteChannel.rtcpChannel.getLocalPort();
		MediaDescriptionField audioAnswer = SdpFactory.buildMediaDescription(remoteChannel, false);
		
		// ... remote peer receives SDP offer from local peer
		// negotiate codecs with local peer
		remoteChannel.negotiateFormats(audioOffer);
		
		// connect to RTP and RTCP endpoints of local channel
		remoteChannel.connectRtp(localAddress, localRtpPort);
		remoteChannel.connectRtcp(localAddress, localRtcpPort);
		
		// ... local peer receives SDP answer from remote peer
		// negotiate codecs with remote peer
		localChannel.negotiateFormats(audioAnswer);

		// connect to RTP and RTCP endpoints of remote channel
		localChannel.connectRtp(remoteAddress, remoteRtpPort);
		localChannel.connectRtcp(remoteAddress, remoteRtcpPort);
		
		// THEN
		assertTrue(localChannel.isOpen());
		assertTrue(localChannel.isAvailable());
		assertFalse(localChannel.isRtcpMux());
		assertEquals(remoteAddress, localChannel.rtpChannel.getRemoteHost());
		assertEquals(remoteRtpPort, localChannel.rtpChannel.getRemotePort());
		assertEquals(remoteAddress, localChannel.rtcpChannel.getRemoteHost());
		assertEquals(remoteRtcpPort, localChannel.rtcpChannel.getRemotePort());

		assertTrue(remoteChannel.isOpen());
		assertTrue(remoteChannel.isAvailable());
		assertFalse(remoteChannel.isRtcpMux());
		assertEquals(localAddress, remoteChannel.rtpChannel.getRemoteHost());
		assertEquals(localRtpPort, remoteChannel.rtpChannel.getRemotePort());
		assertEquals(localAddress, remoteChannel.rtcpChannel.getRemoteHost());
		assertEquals(localRtcpPort, remoteChannel.rtcpChannel.getRemotePort());
	}

	@Test
	public void testSipCallWithRtcpMux() throws IllegalStateException, IOException, InterruptedException {
		/* GIVEN */
		boolean rtcpMux = true;
		
		/* WHEN */
		// activate local channel and bind it to local address
		// there will be two underlying channels for RTP and RTCP
		localChannel.open();
		localChannel.bind(false, rtcpMux);
		
		String localAddress = localChannel.rtpChannel.getLocalHost();
		int localPort = localChannel.rtpChannel.getLocalPort();
		MediaDescriptionField audioOffer = SdpFactory.buildMediaDescription(localChannel, true);
		
		// activate "remote" channel and bind it to local address
		// there will be two underlying channels for RTP and RTCP
		remoteChannel.open();
		remoteChannel.bind(false, rtcpMux);
		
		String remoteAddress = remoteChannel.rtpChannel.getLocalHost();
		int remotePort = remoteChannel.rtpChannel.getLocalPort();
		MediaDescriptionField audioAnswer = SdpFactory.buildMediaDescription(remoteChannel, false);
		
		// ... remote peer receives SDP offer from local peer
		// negotiate codecs with local peer
		remoteChannel.negotiateFormats(audioOffer);
		
		// connect to RTP and RTCP endpoints of local channel
		remoteChannel.connectRtp(localAddress, localPort);
		remoteChannel.connectRtcp(localAddress, localPort);
		
		// ... local peer receives SDP answer from remote peer
		// negotiate codecs with remote peer
		localChannel.negotiateFormats(audioAnswer);
		
		// connect to RTP and RTCP endpoints of remote channel
		localChannel.connectRtp(remoteAddress, remotePort);
		localChannel.connectRtcp(remoteAddress, remotePort);
		
		// THEN
		assertTrue(localChannel.isOpen());
		assertTrue(localChannel.isAvailable());
		assertTrue(localChannel.isRtcpMux());
		assertEquals(remoteAddress, localChannel.rtpChannel.getRemoteHost());
		assertEquals(remotePort, localChannel.rtpChannel.getRemotePort());
		assertFalse(localChannel.rtcpChannel.isOpen());
		
		assertTrue(remoteChannel.isOpen());
		assertTrue(remoteChannel.isAvailable());
		assertTrue(remoteChannel.isRtcpMux());
		assertEquals(localAddress, remoteChannel.rtpChannel.getRemoteHost());
		assertEquals(localPort, remoteChannel.rtpChannel.getRemotePort());
		assertFalse(remoteChannel.rtcpChannel.isOpen());
	}
	
    @Test
    public void testSupportedCodecsFiltering() {
        // given
        final RTPFormats codecs = new RTPFormats(3);
        codecs.add(AVProfile.audio.find(0));
        codecs.add(AVProfile.audio.find(8));
        codecs.add(AVProfile.audio.find(3));
        codecs.add(AVProfile.audio.find(101));

        final ChannelsManager channelProvider = mock(ChannelsManager.class);
        final RtpChannel rtpChannel = mock(RtpChannel.class);
        final RtcpChannel rtcpChannel = mock(RtcpChannel.class);
        final Clock clock = mock(Clock.class);

        // when
        when(channelProvider.getCodecs()).thenReturn(codecs);
        when(channelProvider.getRtpChannel(any(RtpStatistics.class), any(RtpClock.class), any(RtpClock.class))).thenReturn(rtpChannel);
        when(channelProvider.getRtcpChannel(any(RtpStatistics.class))).thenReturn(rtcpChannel);

        final AudioChannel audioChannel = new AudioChannel(clock, channelProvider);
        RTPFormats supportedCodecs = audioChannel.getFormats();

        // then
        assertEquals(codecs, supportedCodecs);
    }
	
	/**
	 * Produces Media Channels
	 * 
	 * @author Henrique Rosa
	 * 
	 */
	private class ChannelFactory {
		
		public AudioChannel buildAudioChannel() {
			return new AudioChannel(wallClock, channelsManager);
		}
		
	}
	
}
