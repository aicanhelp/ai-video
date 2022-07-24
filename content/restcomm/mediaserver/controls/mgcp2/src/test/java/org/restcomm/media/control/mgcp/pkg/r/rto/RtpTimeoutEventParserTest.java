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

package org.restcomm.media.control.mgcp.pkg.r.rto;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutEvent;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutEventParser;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutStartTime;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTimeoutEventParserTest {

    @Test
    public void testParseTimeoutandStartTimeParameters() throws MgcpParseException {
        // given
        final int connectionId = 5;
        final String timeout = "10";
        final RtpTimeoutStartTime startTime = RtpTimeoutStartTime.WAIT_RTCP;
        final String startTimeParam = "st=" + startTime.symbol();

        // when
        final RtpTimeoutEvent rtpTimeout = RtpTimeoutEventParser.parse(connectionId, timeout, startTimeParam);

        // then
        assertEquals(connectionId, rtpTimeout.getConnectionId());
        assertEquals(Integer.parseInt(timeout), rtpTimeout.getTimeout());
        assertEquals(RtpTimeoutStartTime.WAIT_RTCP, rtpTimeout.getWhen());
    }

    @Test
    public void testDefaultStartTime() throws MgcpParseException {
        // given
        final int connectionId = 5;
        final String timeout = "10";

        // when
        final RtpTimeoutEvent rtpTimeout = RtpTimeoutEventParser.parse(connectionId, timeout);

        // then
        assertEquals(Integer.parseInt(timeout), rtpTimeout.getTimeout());
        assertEquals(RtpTimeoutStartTime.IMMEDIATE, rtpTimeout.getWhen());
    }

    @Test(expected = MgcpParseException.class)
    public void testParseInvalidTimeoutParameter() throws MgcpParseException {
        // given
        final int connectionId = 5;
        final String timeout = "-1";

        // when
        RtpTimeoutEventParser.parse(connectionId, timeout);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseMissingTimeoutParameter() throws MgcpParseException {
        // given
        final int connectionId = 5;
        final RtpTimeoutStartTime startTime = RtpTimeoutStartTime.WAIT_RTCP;
        final String startTimeParam = "st=" + startTime.symbol();

        // when
        RtpTimeoutEventParser.parse(connectionId, startTimeParam);
    }

    @Test(expected = MgcpParseException.class)
    public void testParseUnknownStartTimeParameter() throws MgcpParseException {
        // given
        final int connectionId = 5;
        final String startTime = "st=xxx";

        // when
        RtpTimeoutEventParser.parse(connectionId, startTime);
    }

}
