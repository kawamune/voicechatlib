package jp.kawamune.minecraft.mod.voicechatlib.phone;

import org.jitsi.service.neomedia.AudioMediaStream;
import org.jitsi.service.neomedia.BasicVolumeControl;
import org.jitsi.service.neomedia.VolumeControl;

public class VoiceChatPeer {

	protected final AudioMediaStream audioMediaStream;
	protected final VolumeControl volumeControl;
	
	public VoiceChatPeer(AudioMediaStream audioMediaStream) {

		if (audioMediaStream.isStarted()) {
			throw new IllegalArgumentException("VoiceChatPeer must be constructed before the AudioMediaStream is started.");
		}
		
		this.audioMediaStream = audioMediaStream;
		this.volumeControl = new BasicVolumeControl("voicechat.default.volume");
		audioMediaStream.setOutputVolumeControl(volumeControl);
	}

	public void start() {
		audioMediaStream.start();
	}

	public void setMute(boolean mute) {
		volumeControl.setMute(mute);
	}

	public void mute() {
		volumeControl.setMute(true);
	}

	public void unmute() {
		volumeControl.setMute(false);
	}

	public boolean isMuted() {
		return volumeControl.getMute();
	}

	public float setVolume(float value) {
		return volumeControl.setVolume(value);
	}

	public float getVolume() {
		return volumeControl.getVolume();
	}
	
	
}
