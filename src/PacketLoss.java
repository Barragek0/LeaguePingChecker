import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PacketLoss implements Runnable {

	@Override
	public void run() {
		try {
			//TODO - include packet loss if there is any in seperate taskbar icon
			String lost = new String();
			Process p = Runtime.getRuntime().exec("ping google.com");
			BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = new String();
			while ((str = buf.readLine()) != null) {
				if (str.contains("loss")) {
					int i = str.indexOf("(");
					int j = str.indexOf("%");
					lost = str.substring(i+1, j);
				}
			}
			System.out.println("Packet loss: "+lost+"%");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
