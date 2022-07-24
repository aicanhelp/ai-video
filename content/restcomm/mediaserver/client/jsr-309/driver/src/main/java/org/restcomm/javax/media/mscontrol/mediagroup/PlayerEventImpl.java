package org.restcomm.javax.media.mscontrol.mediagroup;

import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.resource.Action;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.EventType;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.resource.Trigger;

/**
 * 
 * @author amit bhayani
 * 
 */
public class PlayerEventImpl implements PlayerEvent {
	private Player player = null;
	private EventType eventType = null;
	private Qualifier qualifier = NO_QUALIFIER;
	private Trigger rtcTrigger = null;
	private boolean isSuccessful = false;
	private int offset = 0;

	private String errorText = null;
	private MediaErr error = MediaErr.NO_ERROR;

	public PlayerEventImpl(EventType eventType) {
		this.eventType = eventType;
	}

	public PlayerEventImpl(Player player, EventType eventType, boolean isSuccessful) {
		this(eventType);
		this.player = player;
		this.isSuccessful = isSuccessful;
	}

	public PlayerEventImpl(Player player, EventType eventType, boolean isSuccessful, Qualifier qualifier,
			Trigger rtcTrigger) {
		this(player, eventType, isSuccessful);
		this.qualifier = qualifier;
		this.rtcTrigger = rtcTrigger;
	}

	public PlayerEventImpl(Player player, EventType eventType, boolean isSuccessful, MediaErr error, String errorText) {
		this(player, eventType, isSuccessful);
		this.error = error;
		this.errorText = errorText;
	}

	public PlayerEventImpl(Player player, EventType eventType, boolean isSuccessful, Qualifier qualifier,
			Trigger rtcTrigger, MediaErr error, String errorText) {
		this(player, eventType, isSuccessful, qualifier, rtcTrigger);

		this.error = error;
		this.errorText = errorText;
	}

	public Action getChangeType() {
		return null;
	}

	public int getIndex() {
		return 0;
	}

	public int getOffset() {
		return this.offset;
	}

	public Qualifier getQualifier() {
		return this.qualifier;
	}

	public Trigger getRTCTrigger() {
		return this.rtcTrigger;
	}

	public MediaErr getError() {
		return this.error;
	}

	public String getErrorText() {
		return this.errorText;
	}

	public EventType getEventType() {
		return this.eventType;
	}

	public Player getSource() {
		return this.player;
	}

	public boolean isSuccessful() {
		return this.isSuccessful;
	}

	protected void setPlayer(Player player) {
		this.player = player;
	}

	protected void setQualifier(Qualifier qualifier) {
		this.qualifier = qualifier;
	}

	protected void setRtcTrigger(Trigger rtcTrigger) {
		this.rtcTrigger = rtcTrigger;
	}

	protected void setSuccessful(boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
	}

	protected void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	protected void setError(MediaErr error) {
		this.error = error;
	}
	
	protected void setOffset(int offset){
		this.offset = offset;
	}

}
