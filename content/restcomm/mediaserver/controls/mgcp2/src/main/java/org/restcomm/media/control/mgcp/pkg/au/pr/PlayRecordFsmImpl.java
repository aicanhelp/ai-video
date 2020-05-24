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

package org.restcomm.media.control.mgcp.pkg.au.pr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.control.mgcp.pkg.au.OperationComplete;
import org.restcomm.media.control.mgcp.pkg.au.OperationFailed;
import org.restcomm.media.control.mgcp.pkg.au.Playlist;
import org.restcomm.media.control.mgcp.pkg.au.ReturnCode;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.listener.TooManyListenersException;
import org.restcomm.media.spi.player.Player;
import org.restcomm.media.spi.player.PlayerListener;
import org.restcomm.media.spi.recorder.Recorder;
import org.restcomm.media.spi.recorder.RecorderListener;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PlayRecordFsmImpl extends AbstractStateMachine<PlayRecordFsm, PlayRecordState, PlayRecordEvent, PlayRecordContext>
        implements PlayRecordFsm {

    private static final Logger log = Logger.getLogger(PlayRecordFsmImpl.class);

    // Event Listener
    private final MgcpEventSubject mgcpEventSubject;

    // Media Components
    private final DtmfDetector detector;
    final DtmfDetectorListener detectorListener;

    private final Player player;
    final PlayerListener playerListener;

    private final Recorder recorder;
    final RecorderListener recorderListener;

    // Execution Context
    private final PlayRecordContext context;

    public PlayRecordFsmImpl(MgcpEventSubject mgcpEventSubject, Recorder recorder, RecorderListener recorderListener,
            DtmfDetector detector, DtmfDetectorListener detectorListener, Player player, PlayerListener playerListener,
            PlayRecordContext context) {
        super();
        // Event Listener
        this.mgcpEventSubject = mgcpEventSubject;

        // Media Components
        this.recorder = recorder;
        this.recorderListener = recorderListener;

        this.detector = detector;
        this.detectorListener = detectorListener;

        this.player = player;
        this.playerListener = playerListener;

        // Execution Context
        this.context = context;
    }

    private void playAnnouncement(String url, long delay) {
        try {
            this.player.setInitialDelay(delay);
            this.player.setURL(url);
            this.player.activate();
        } catch (MalformedURLException e) {
            log.warn("Could not play malformed segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayRecordEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        } catch (ResourceUnavailableException e) {
            log.warn("Could not play unavailable segment " + url);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayRecordEvent.FAIL, context);
            // TODO create transition from PROMPTING to FAILED
        }
    }
    
    private void deleteRecording() {
        try {
            Path path = Paths.get(context.getRecordId());
            if(Files.exists(path)) {
                Files.delete(path);
            }
            if(log.isTraceEnabled()) {
                log.trace("Deleted temporary recording file before restart.");
            }
        } catch (IOException e) {
            log.warn("Failed to delete temporary recording file before restart.", e);
        }
    }

    @Override
    public void enterLoadingPlaylist(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered LOADING PLAYLIST state");
        }
    }

    @Override
    public void exitLoadingPlaylist(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited LOADING PLAYLIST state");
        }
    }

    @Override
    public void enterPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }

    @Override
    public void onPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PROMPTING state");
        }

        final Playlist prompt = context.getInitialPrompt();
        final String next = prompt.next();

        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayRecordEvent.PROMPT_END, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
    }

    @Override
    public void exitPrompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }
    
    @Override
    public void enterReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered REPROMPTING state");
        }
        
        final Playlist prompt = context.getReprompt();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }
    
    @Override
    public void onReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On REPROMPTING state");
        }
        
        final Playlist prompt = context.getReprompt();
        final String next = prompt.next();
        
        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayRecordEvent.PROMPT_END, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
    }
    
    @Override
    public void exitReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited REPROMPTING state");
        }
        
        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }
    
    @Override
    public void enterNoSpeechReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered NO SPEECH REPROMPTING state");
        }

        final Playlist prompt = context.getNoSpeechReprompt();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
        
    }
    
    @Override
    public void onNoSpeechReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On NO SPEECH REPROMPTING state");
        }

        final Playlist prompt = context.getNoSpeechReprompt();
        final String next = prompt.next();

        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayRecordEvent.PROMPT_END, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
        
    }
    
    @Override
    public void exitNoSpeechReprompting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited NO SPEECH REPROMPTING state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTING state");
        }

        try {
            // Activate DTMF detector and bind listener
            this.detector.addListener(this.detectorListener);
            this.detector.activate();
        } catch (TooManyListenersException e) {
            log.error("Too many DTMF listeners", e);
        }
    }

    @Override
    public void onCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On COLLECTING state");
        }

        // TODO Make decision when DTMF is collected
        final char tone = context.getTone();
        if(context.getRestartKey() == tone) {
            fire(PlayRecordEvent.RESTART, context);
        } else if(context.getReinputKey() == tone) {
            fire(PlayRecordEvent.REINPUT, context);
        } else if(context.getEndInputKey() == tone) {
            fire(PlayRecordEvent.END_RECORD, context);
        }
    }

    @Override
    public void exitCollecting(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited COLLECTING state");
        }

        // Deactivate DTMF detector and release listener
        this.detector.removeListener(this.detectorListener);
        this.detector.deactivate();
    }

    @Override
    public void enterCollected(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered COLLECTED state");
        }
    }

    @Override
    public void enterRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered RECORDING state");
        }

        try {
            this.recorder.setMaxRecordTime(context.getTotalRecordingLengthTimer() * 1000000L);
            this.recorder.setPreSpeechTimer(context.getPreSpeechTimer() * 1000000L);
            this.recorder.setPostSpeechTimer(context.getPostSpeechTimer() * 1000000L);
            this.recorder.setRecordFile(context.getRecordId(), false);
            this.recorder.addListener(this.recorderListener);
            this.recorder.activate();
        } catch (IOException e) {
            log.error("Recording URL cannot be found:" + context.getRecordId(), e);
            context.setReturnCode(ReturnCode.BAD_AUDIO_ID.code());
            fire(PlayRecordEvent.FAIL, context);
        } catch (TooManyListenersException e) {
            log.error("Too many recorder listeners.");
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }

    @Override
    public void onRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("on RECORDING state");
        }

        if (PlayRecordEvent.SPEECH_DETECTED.equals(event)) {
            // AND && !context.getNonInterruptibleAudio()
            log.info("SPEECH DETECTED !!!!!!!!");
        }

    }

    @Override
    public void exitRecording(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited RECORDING state");
        }

        recorder.deactivate();
        recorder.removeListener(this.recorderListener);
    }

    @Override
    public void enterRecorded(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered RECORDED state");
        }
    }
    
    @Override
    public void enterCanceled(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered CANCELED state");
        }
        
        if(context.isSpeechDetected()) {
            fire(PlayRecordEvent.SUCCEED, context);
        } else {
            context.setReturnCode(ReturnCode.NO_SPEECH.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }
    
    @Override
    public void exitCanceled(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited CANCELED state");
        }
    }

    @Override
    public void enterSucceeding(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDING state");
        }
        
        final Playlist playlist = context.getSuccessAnnouncement();
        if(playlist.isEmpty()) {
            fire(PlayRecordEvent.NO_PROMPT, context);
        } else {
            fire(PlayRecordEvent.PROMPT, context);
        }
    }
    
    @Override
    public void exitSucceeding(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited SUCCEEDING state");
        }
    }

    @Override
    public void enterPlayingSuccess(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING SUCCESS state");
        }

        final Playlist prompt = context.getSuccessAnnouncement();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingSuccess(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING SUCCESS state");
        }
        
        final Playlist prompt = context.getSuccessAnnouncement();
        final String next = prompt.next();

        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayRecordEvent.PROMPT_END, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
    }

    @Override
    public void exitPlayingSuccess(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING SUCCESS state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterSucceeded(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if(log.isTraceEnabled()) {
            log.trace("Entered SUCCEEDED state");
        }
        
        final OperationComplete oc = new OperationComplete(PlayRecord.SYMBOL, ReturnCode.SUCCESS.code());
        oc.setParameter("na", String.valueOf(context.getAttempt()));
        oc.setParameter("vi", Boolean.FALSE.toString());
        oc.setParameter("ri", context.getRecordId());

        this.mgcpEventSubject.notify(this.mgcpEventSubject, oc);
    }
    
    @Override
    public void enterFailing(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered FAILING state");
        }
        
        if(context.hasMoreAttempts()) {
            context.newAttempt();
            deleteRecording();
            fire(event, context);
        } else {
            switch (event) {
                case MAX_DURATION_EXCEEDED:
                    context.setReturnCode(ReturnCode.SPOKE_TOO_LONG.code());
                    break;
                    
                case NO_SPEECH:
                    context.setReturnCode(ReturnCode.NO_SPEECH.code());
                    break;
                    
                case RESTART:
                case REINPUT:
                    context.setReturnCode(ReturnCode.MAX_ATTEMPTS_EXCEEDED.code());
                    break;

                default:
                    context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
                    break;
            }
            
            final Playlist playlist = context.getFailureAnnouncement();
            if(playlist.isEmpty()) {
                fire(PlayRecordEvent.NO_PROMPT, context);
            } else {
                fire(PlayRecordEvent.PROMPT, context);
            }
        }
    }
    
    @Override
    public void exitFailing(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited FAILING state");
        }
    }

    @Override
    public void enterPlayingFailure(PlayRecordState from, PlayRecordState to, PlayRecordEvent event,
            PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Entered PLAYING FAILURE state");
        }

        final Playlist prompt = context.getFailureAnnouncement();
        try {
            this.player.addListener(this.playerListener);
            playAnnouncement(prompt.next(), 0L);
        } catch (TooManyListenersException e) {
            log.error("Too many player listeners", e);
            context.setReturnCode(ReturnCode.UNSPECIFIED_FAILURE.code());
            fire(PlayRecordEvent.FAIL, context);
        }
    }

    @Override
    public void onPlayingFailure(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("On PLAYING FAILURE state");
        }
        
        final Playlist prompt = context.getFailureAnnouncement();
        final String next = prompt.next();

        if (next.isEmpty()) {
            // No more announcements to play
            fire(PlayRecordEvent.PROMPT_END, context);
        } else {
            playAnnouncement(next, 10 * 100);
        }
    }

    @Override
    public void exitPlayingFailure(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if (log.isTraceEnabled()) {
            log.trace("Exited PLAYING FAILURE state");
        }

        this.player.removeListener(this.playerListener);
        this.player.deactivate();
    }

    @Override
    public void enterFailed(PlayRecordState from, PlayRecordState to, PlayRecordEvent event, PlayRecordContext context) {
        if(log.isTraceEnabled()) {
            log.trace("Entered FAILED state");
        }
        
        final OperationFailed of = new OperationFailed(PlayRecord.SYMBOL, context.getReturnCode());
        of.setParameter("na", String.valueOf(context.getAttempt()));

        this.mgcpEventSubject.notify(this.mgcpEventSubject, of);
    }

}