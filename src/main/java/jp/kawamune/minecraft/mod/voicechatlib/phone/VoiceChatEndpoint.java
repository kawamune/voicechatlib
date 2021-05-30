package jp.kawamune.minecraft.mod.voicechatlib.phone;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.device.AudioMixerMediaDevice;
import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.AudioSystem.DataFlow;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.impl.neomedia.device.DeviceConfiguration;
import org.jitsi.impl.neomedia.device.MediaDeviceImpl;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.AudioMediaStream;
import org.jitsi.service.neomedia.BasicVolumeControl;
import org.jitsi.service.neomedia.DefaultStreamConnector;
import org.jitsi.service.neomedia.MediaDirection;
import org.jitsi.service.neomedia.MediaStream;
import org.jitsi.service.neomedia.MediaStreamTarget;
import org.jitsi.service.neomedia.MediaUseCase;
import org.jitsi.service.neomedia.StreamConnector;
import org.jitsi.service.neomedia.VolumeControl;
import org.jitsi.service.neomedia.device.MediaDevice;
import org.jitsi.service.neomedia.format.MediaFormat;
import org.jitsi.utils.MediaType;

/**
 * @author kawamune
 *
 */
public class VoiceChatEndpoint {

	public static final int DEFAULT_RTP_PORT = 9000;
	
	protected final String localHostName;
	protected final int localRtpPort;

	protected AudioMixerMediaDevice mixerDevice = null;
	protected MediaDevice captureDevice = null;
	protected MediaServiceImpl mediaService = null;
	protected AudioSystem audioSystem = null;
	protected DeviceConfiguration deviceConfiguration = null;

	protected Map<String, MediaStream> streams = null;
	protected VolumeControl volumeControl;

	public VolumeControl getVolumeControl() {

		return volumeControl;
	}
	
	public MediaStream getMediaStream() {
		for (MediaStream stream : streams.values()) {
			return stream;
		}
		return null;
	}
	
	public VoiceChatEndpoint(String localHostName) {
		this(localHostName, -1);
	}
	
	public VoiceChatEndpoint(String localHostName, int localRtpPort) {
		
		this.localHostName = localHostName;
		if (localRtpPort == -1) { 
			this.localRtpPort = DEFAULT_RTP_PORT;
		}
		else {
			this.localRtpPort = localRtpPort;
		}
		streams = new HashMap<String, MediaStream>();
		initialize();
	}

	private void initialize() {

		LibJitsi.start();
		mediaService = (MediaServiceImpl)LibJitsi.getMediaService();
		deviceConfiguration = mediaService.getDeviceConfiguration();
		audioSystem = deviceConfiguration.getAudioSystem();
	}

	protected MediaDevice getDefaultCaputureDevice() {

		return mediaService.getDefaultDevice(MediaType.AUDIO, null);
	}

	public List<String> getAvailablePlaybackDeviceNames() {

		List<String> deviceNames = new LinkedList<String>();
		List<CaptureDeviceInfo2> captureDeviceInfo2s = audioSystem.getDevices(DataFlow.PLAYBACK);
		for (CaptureDeviceInfo2 captureDeviceInfo : captureDeviceInfo2s) {
			deviceNames.add(captureDeviceInfo.getModelIdentifier());
		}
		return deviceNames;
	}
	
	/**
	 * Set the playback device by given device name.
	 * @param deviceName Name of the device. It must be one of the names got by getAvailablePlaybackDeviceNames()
	 * @return true if the device is set. Otherwise, false.
	 */
	public boolean setPlaybackDevice(String deviceName) {
		
		try {
			List<CaptureDeviceInfo2> captureDeviceInfo2s = audioSystem.getDevices(DataFlow.PLAYBACK);
			for (CaptureDeviceInfo2 captureDeviceInfo : captureDeviceInfo2s) {
				if (captureDeviceInfo.getModelIdentifier().equals(deviceName)) {
					audioSystem.setDevice(DataFlow.PLAYBACK, captureDeviceInfo, true);
					return true;
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	public List<String> getAvailableCaptureDeviceNames() {

		List<String> deviceNames = new LinkedList<String>();
		List<CaptureDeviceInfo2> captureDeviceInfo2s = deviceConfiguration.getAvailableAudioCaptureDevices();
		for (CaptureDeviceInfo2 captureDeviceInfo : captureDeviceInfo2s) {
			deviceNames.add(captureDeviceInfo.getModelIdentifier());
		}
		return deviceNames;
	}

	/**
	 * Set the capture device by given device name.
	 * @param captureDeviceName Name of the device. It must be one of the names got by getAvailableCaptureDeviceNames()
	 * @return true if the device is set. Otherwise, false.
	 */
	public boolean setCaptureDevice(String captureDeviceName) {

		if (!streams.isEmpty()) {
			// TODO: Allow changing capture device even after one or more streams are started.
			throw new UnsupportedOperationException("Can't change the capture device after any streams has been started.");
		}
		List<MediaDevice> mediaDevices = mediaService.getDevices(MediaType.AUDIO, MediaUseCase.CALL);
		for (MediaDevice mediaDevice : mediaDevices) {
			CaptureDeviceInfo2 captureDeviceInfo = (CaptureDeviceInfo2)((MediaDeviceImpl)mediaDevice).getCaptureDeviceInfo();
			if (captureDeviceInfo.getModelIdentifier().equals(captureDeviceName)) {
				captureDevice = mediaDevice;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Create a new stream. remoteHost will be used for stream ID.
	 * @param remoteHost remoteHost's name. It's also used for stream ID.
	 * @param remoteRtpPort RTP port number of the peer. RTCP port will be decided automatically by incrementing this.
	 * @return If it succeeded or not.
	 */
	public boolean createNewStream(String remoteHost, int remoteRtpPort) {

		return createNewStream(remoteHost, remoteHost, remoteRtpPort);
	}
	
	/**
	 * Create a new stream.
	 * @param peerId ID for the stream that will be created.
	 * @param remoteHost remoteHost's name. It's also used for stream ID.
	 * @param remoteRtpPort RTP port number of the peer. RTCP port will be decided automatically by incrementing this.
	 * @return If it succeeded or not.
	 */
	public boolean createNewStream(String peerId, String remoteHostName, int remoteRtpPort) {

		try {
			MediaStream mediaStream = null;

			InetAddress localHost = InetAddress.getByName(localHostName);
			DatagramSocket rtpSocket = new DatagramSocket(localRtpPort, localHost);
			DatagramSocket rtcpSocket = new DatagramSocket(localRtpPort + 1, localHost);
			StreamConnector connector = new DefaultStreamConnector(rtpSocket, rtcpSocket);

			// Create media stream.
			// TODO: Now it's a default implementation.
			if (captureDevice == null) {
				captureDevice = getDefaultCaputureDevice();
			}

			MediaDevice mixer = mediaService.createMixer(captureDevice);
			mediaStream = mediaService.createMediaStream(connector, mixer);
			mediaStream.setDirection(MediaDirection.SENDRECV);
			
			// Set format.
			// TODO: Now it's a default implementation.
			MediaFormat format = mediaService.getFormatFactory().createMediaFormat("PCMU", 8000);
			if (format == null) {
				System.err.println("Couldn't create a format");
				return false;
			}
			mediaStream.setFormat(format);
			
			// Set target.
			InetAddress remoteHost = InetAddress.getByName(remoteHostName);
			InetSocketAddress remoteRtpAddress = new InetSocketAddress(remoteHost, remoteRtpPort);
			InetSocketAddress remoteRtcpAddress = new InetSocketAddress(remoteHost, remoteRtpPort + 1);
			mediaStream.setTarget(new MediaStreamTarget(remoteRtpAddress, remoteRtcpAddress));
			
			volumeControl = new BasicVolumeControl("hoge");
			((AudioMediaStream)mediaStream).setOutputVolumeControl(volumeControl);
			
			Thread.sleep(4000);
			mediaStream.start();
			streams.put(peerId, mediaStream);
			Thread.sleep(4000);
			
			return true;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

}
