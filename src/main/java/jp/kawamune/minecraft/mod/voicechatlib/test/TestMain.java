package jp.kawamune.minecraft.mod.voicechatlib.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.jitsi.service.neomedia.AudioMediaStream;
import org.jitsi.service.neomedia.BasicVolumeControl;
import org.jitsi.service.neomedia.MediaStream;

import jp.kawamune.minecraft.mod.voicechatlib.phone.VoiceChatEndpoint;
import jp.kawamune.minecraft.mod.voicechatlib.phone.VoiceChatPeer;

public class TestMain implements Runnable {

	private enum Type {
		CALLER,
		RECEIVER,
	};
	
	private final Type type;
	
	public static void main(String[] args) {

		TestMain main = null;
		
		if (args.length > 0) {
			if (args[0].equals("caller")) {
				main = new TestMain(Type.CALLER);
			}
			else if (args[0].equals("receiver")) {
				main = new TestMain(Type.RECEIVER);
			}
		}
		else {
			while (true) {
				
				
				String type = readCommand("Enter \"caller\" or \"receiver\"");
		
				if (type == null) {
					System.out.println("type == null.");
					return;
				}
				else if (type.equals("caller")) {
					main = new TestMain(Type.CALLER);
					break;
				}
				else if (type.equals("receiver")) {
					main = new TestMain(Type.RECEIVER);
					break;
				}
			}
		}
		(new Thread(main)).start();
	}

	public TestMain(Type type) {
		
		this.type = type;
	}
	
	public void run() {

		switch (type) {
		case CALLER:
			runCaller();
			break;
		case RECEIVER:
			runReceiver();
			break;
		default:
			
		}
	}

	protected void runReceiver() {

		VoiceChatEndpoint endpoint = new VoiceChatEndpoint("192.168.1.15", 9000);
		chooseDevices(endpoint);

		VoiceChatPeer peer = endpoint.createNewPeer("peer", "192.168.1.41", 10000);

		while (true) {
			setVolume(peer);
		}
	}

	protected void runCaller() {

		VoiceChatEndpoint endpoint = new VoiceChatEndpoint("192.168.1.41", 10000);
		chooseDevices(endpoint);

		VoiceChatPeer peer = endpoint.createNewPeer("peer", "192.168.1.15", 9000);

		while (true) {
			setVolume(peer);
		}
	}

	protected void setVolume(VoiceChatPeer peer) {
		
		try {
			String muted = peer.isMuted() ? "muted" : "note muted";
			String volumeString = readCommand("set volume or mute/unmute"
					+ "(current volume is "
					+ peer.getVolume()
					+ "("
					+ muted
					+ "))");
			
			if (volumeString.equalsIgnoreCase("mute")) {
				peer.mute();
			}
			else if (volumeString.equals("unmute")) {
				peer.unmute();
			}
			else {
				float volume = Float.parseFloat(volumeString);
				peer.setVolume(volume);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void chooseDevices(VoiceChatEndpoint endpoint) {
		
		// Choose playback device.
		List<String> playbackDeviceNames = endpoint.getAvailablePlaybackDeviceNames();
		for (String playbackDeviceName : playbackDeviceNames) {
			System.out.println(playbackDeviceName);
		}
		String index = readCommand("Choose one by index [0..]");
		if (!endpoint.setPlaybackDevice(playbackDeviceNames.get(Integer.parseInt(index)))) {
			System.err.println("Failed to set playback device.");
			return;
		}

		List<String> captureDeviceNames = endpoint.getAvailableCaptureDeviceNames();
		for (String captureDeviceName : captureDeviceNames) {
			System.out.println(captureDeviceName);
		}
		index = readCommand("Choose one by index [0..]");
		if (!endpoint.setCaptureDevice(captureDeviceNames.get(Integer.parseInt(index)))) {
			System.err.println("Failed to set playback device.");
			return;
		}
	}
	
	protected static String readCommand() {
		return readCommand(null);
	}
	
	protected static String readCommand(String query) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		if (query != null) {
			System.out.println(query);
		}
		System.out.print("> ");

		try {
			String line = reader.readLine();
			return line;
		}
		catch (IOException iex) {
			iex.printStackTrace();
			return null;
		}
	}

	
}
