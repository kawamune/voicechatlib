package jp.kawamune.minecraft.mod.voicechatlib.phone;

import java.net.URL;

import javax.media.Manager;
import javax.media.protocol.DataSource;

import org.jitsi.impl.neomedia.conference.AudioMixingPushBufferDataSource;
import org.jitsi.impl.neomedia.device.AudioMixerMediaDevice;
import org.jitsi.service.neomedia.AudioMediaStream;
import org.jitsi.service.neomedia.BasicVolumeControl;
import org.jitsi.service.neomedia.VolumeControl;
import org.jitsi.service.neomedia.device.MediaDevice;

public class VoiceChatPeer {

	protected final AudioMixerMediaDevice mixer;
	protected final AudioMediaStream audioMediaStream;
	protected final VolumeControl volumeControl;
	
	public VoiceChatPeer(AudioMediaStream audioMediaStream) {

		if (audioMediaStream.isStarted()) {
			throw new IllegalArgumentException("VoiceChatPeer must be constructed before the AudioMediaStream is started.");
		}

		MediaDevice device = audioMediaStream.getDevice();
		
		this.mixer = (AudioMixerMediaDevice)audioMediaStream.getDevice();
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

	public void injectAudioFile(String uri) {

		try {
			DataSource dataSource = Manager.createDataSource(new URL(uri));
			AudioMixingPushBufferDataSource mixingDataSource = mixer.createOutputDataSource();

			mixingDataSource.addInDataSource(dataSource);
			dataSource.start();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
