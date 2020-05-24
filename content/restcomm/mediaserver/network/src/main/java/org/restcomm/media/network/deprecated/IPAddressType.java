/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.restcomm.media.network.deprecated;

/**
 * Defines relation between audio/video format and RTP payload number as
 * specified by Audio/Video Profile spec.
 * 
 * @author Oleg Kulikov
 */
public enum IPAddressType {

    IPV4(0),IPV6(1),INVALID(2);
    
    private int value;
    
    IPAddressType(int value)
    {
    	this.value=value;
    }
    
    public int getValue()
    {
    	return value;
    }
}
