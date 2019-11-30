import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class Main {

	private static ExecutorService threadService = Executors.newFixedThreadPool(2);
	private static ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private static boolean notified = false;

	// Settings
	private static long averagePing;
	private static int tolerance;
	private static String server1;
	private static String server2;
	private static boolean notifications;
	private static long updateInterval;
	private static boolean canStart;

	// Window
	private static JFrame frame = new JFrame();
	private static PopupMenu popup;
	private static SystemTray tray;
	private static TrayIcon trayIcon;

	// Other
	private static long lastNotified;

	public static void main(String[] args) {
		canStart = false;
		loadSettings();
		setupTray();
		if (canStart) {
			initiate();
			new PacketLoss();
		}
	}

	private static void loadSettings() {
		try {
			File theDir = new File(System.getProperty("user.home") + "/LeaguePingChecker");
			lastNotified = System.currentTimeMillis() - 300000;
			if (!theDir.exists()) {
				System.out.println("directory doesnt exist.");
				boolean result = false;

				try {
					theDir.mkdir();
					System.out.println("created directory.");
					result = true;
				} catch (SecurityException se) {
				}
				if (result) {
					System.out.println("Settings dir created.");
					final JDialog dialog = new JDialog();
					dialog.setAlwaysOnTop(true);
					List<String> lines = Arrays.asList("", "");
					Path file = Paths.get(System.getProperty("user.home") + "/LeaguePingChecker/settings.txt");
					Files.write(file, lines, Charset.forName("UTF-8"));
					System.out.println("Settings file created.");
					PrintWriter writer = new PrintWriter(
							System.getProperty("user.home") + "/LeaguePingChecker/settings.txt", "UTF-8");
					if (JOptionPane.showConfirmDialog(null,
							"Would you like to receive notifications when your ping spikes higher than the set tolerance?",
							"Notifications", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						writer.println("notifications=true");
						System.out.println("notifications=" + true);
						validInput(dialog, writer,
								"Set the ping tolerance before a notification is triggered\r\rFor example: If your average ping were 30 and the tolerance were 50, you'd receive a notification if your ping went above 80",
								"tolerance");
					} else {
						writer.println("notifications=false");
						System.out.println("notifications=" + false);
						writer.println("tolerance=50");
					}
					validInput(dialog, writer, "What is your average in-game ping?:", "averagePing");
					Object[] possibilities = { "EUW", "EUNE", "NA", "OCE", "LAN", "BR" };
					String s = (String) JOptionPane.showInputDialog(dialog, "Which server do you play on?:", "Server",
							JOptionPane.PLAIN_MESSAGE, null, possibilities, "EUW");
					if (s != null) {
						writer.println("server=" + s.toLowerCase());
						System.out.println("server=" + s.toLowerCase());
					}
					writer.println("updateInterval=1");
					if (JOptionPane.showConfirmDialog(null,
							"Would you like League Ping Checker to automatically run when you startup your pc?",
							"Startup", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						System.out.println("startup=" + true);
						try {
							Path autoStart = Paths.get(getAutoStart());
							Path currentPath = Paths.get(getRunningDir());
							Files.copy(currentPath.resolve("LeaguePingChecker.exe"),
									autoStart.resolve("LeaguePingChecker.exe"));
						} catch (Throwable e) {
							e.printStackTrace();
						}
					} else {
						System.out.println("startup=" + false);
					}
					writer.println("euw1=104.160.141.3");
					writer.println("euw2=104.160.141.2");
					writer.println("na1=104.160.131.3");
					writer.println("na2=104.160.131.2");
					writer.println("eune1=104.160.142.3");
					writer.println("eune2=104.160.142.2");
					writer.println("oce1=104.160.156.1");
					writer.println("oce2=104.160.156.2");
					writer.println("lan1=104.160.136.3");
					writer.println("lan2=104.160.136.2");
					writer.println("br1=104.160.152.3");
					writer.println("br2=104.160.152.2");
					writer.close();
					JOptionPane.showMessageDialog(dialog,
							"You've finished the initial setup. From now on, your ping will be shown on your taskbar (you'll need to press the arrow to expand it, then drag it onto your main taskbar for it to always be visible).");
				} else {
					System.out.println("Couldn't create settings dir.");
				}
			}
			System.out.println("directory done.");
			canStart = true;
			Path path = filePrep(System.getProperty("user.home") + "/LeaguePingChecker/settings.txt", false);
			if (canStart) {
				Properties prop = new Properties();
				InputStream input = new FileInputStream(path.toString());
				prop.load(input);
				averagePing = Long.parseLong(prop.getProperty("averagePing"));
				tolerance = Integer.parseInt(prop.getProperty("tolerance"));
				switch (prop.getProperty("server").toLowerCase()) {
				case "euw":
					server1 = prop.getProperty("euw1");
					server2 = prop.getProperty("euw2");
					break;
				case "na":
					server1 = prop.getProperty("na1");
					server2 = prop.getProperty("na2");
					break;
				case "eune":
					server1 = prop.getProperty("eune1");
					server2 = prop.getProperty("eune1");
					break;
				case "oce":
					server1 = prop.getProperty("oce1");
					server2 = prop.getProperty("oce2");
					break;
				case "lan":
					server1 = prop.getProperty("lan1");
					server2 = prop.getProperty("lan2");
					break;
				case "br":
					server1 = prop.getProperty("br1");
					server2 = prop.getProperty("br2");
					break;
				}
				notifications = Boolean.valueOf(prop.getProperty("notifications"));
				updateInterval = Long.parseLong(prop.getProperty("updateInterval"));
				OutputStream output = new FileOutputStream(path.toString());
				prop.store(output, null);
				input.close();
				output.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getAutoStart() {
		return System.getProperty("java.io.tmpdir").replace("Local\\Temp\\",
				"Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup");
	}

	public static String getRunningDir() {
		String runningdir = Paths.get(".").toAbsolutePath().normalize().toString();
		return runningdir;
	}

	private static void initiate() {
		Runnable runnable = new Runnable() {
			public void run() {
				long ping = pingServer(server1);
				// if the main server isn't responding to pings, try the second server
				if (ping < 0 && !server2.equals("") && server2 != null) {
					ping = pingServer(server2);
				}
				// add 5 to ping as the server ping is not accurate compared to in-game ping
				ping = ping + 5;
				// checks if the user needs to be notified
				notifyPing(ping);
			}
		};
		service.scheduleAtFixedRate(runnable, 0L, updateInterval, TimeUnit.SECONDS);
	}

	/*
	 * TODO private static void initiatePacketLoss() { Runnable runnable = new
	 * Runnable() { public void run() { } }; service.scheduleAtFixedRate(runnable,
	 * 0L, updateInterval, TimeUnit.SECONDS); }
	 */

	private static long pingServer(String server) {
		try {
			long start = System.currentTimeMillis();
			InetAddress address = InetAddress.getByName(server);
			address.isReachable(10000);
			return System.currentTimeMillis() - start;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static void notifyPing(long ping) {
		try {
			final long latency = ping;
			trayIcon.setImage(textToImage(ping));
			if (notifications) {
				if (ping < 0 && !notified) {
					threadService.submit(new Runnable() {
						public void run() {
							if (Math.abs(lastNotified - System.currentTimeMillis()) >= 300_000) {
								lastNotified = System.currentTimeMillis();
								trayIcon.displayMessage("League Ping Checker - Error",
										"There was an error connecting to the server.", MessageType.ERROR);
							} else {
								System.out.println("Already notified connection error: "
										+ Math.abs(lastNotified - System.currentTimeMillis()));
							}
						}
					});
					notified = true;
				} else if (ping >= averagePing + tolerance && !notified) {
					threadService.submit(new Runnable() {

						public void run() {
							if (Math.abs(lastNotified - System.currentTimeMillis()) >= 300_000) {
								lastNotified = System.currentTimeMillis();
								trayIcon.displayMessage("League Ping Checker - High Ping Warning",
										"Your ping is: " + latency, MessageType.WARNING);
							} else {
								System.out.println("Already notified high ping: "
										+ Math.abs(lastNotified - System.currentTimeMillis()));
							}
						}
					});
					notified = true;
				} else if (ping < averagePing + tolerance && notified) {
					threadService.submit(new Runnable() {
						public void run() {
							// JOptionPane.showMessageDialog(frame, "Ping is " + latency + "ms");
						}
					});
					notified = false;
				}
			}
		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}

	private static void setupTray() {
		try {
			// Window
			frame.setTitle("Ping Check");
			frame.setAlwaysOnTop(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			// Tray Options
			popup = new PopupMenu();
			MenuItem defaultItem = new MenuItem("Exit");
			defaultItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
			popup.add(defaultItem);

			// Tray
			tray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(textToImage(0), "League Ping Checker", popup);
			trayIcon.setImageAutoSize(true);
			frame.addWindowStateListener(new WindowStateListener() {
				public void windowStateChanged(WindowEvent e) {
					try {
						if (e.getNewState() == JFrame.ICONIFIED) {
							tray.add(trayIcon);
							frame.setVisible(false);
						}
						if (e.getNewState() == 7) {
							tray.add(trayIcon);
							frame.setVisible(false);
						}
						if (e.getNewState() == JFrame.MAXIMIZED_BOTH) {
							tray.remove(trayIcon);
							frame.setVisible(true);
						}
						if (e.getNewState() == JFrame.NORMAL) {
							tray.remove(trayIcon);
							frame.setVisible(true);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});

			tray.add(trayIcon);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Image textToImage(long number) {
		String text = Long.toString(number);
		Font font = new Font(Font.DIALOG, Font.PLAIN, 18);
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		g2d.setFont(font);
		FontMetrics fm = g2d.getFontMetrics();
		int width = fm.stringWidth(text);
		int height = fm.getHeight();
		g2d.dispose();
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(font);
		fm = g2d.getFontMetrics();
		Color color = Color.GREEN;
		if (number >= averagePing + 10)
			color = Color.ORANGE;
		else if (number >= averagePing + 5)
			color = Color.YELLOW;
		g2d.setColor(color);
		g2d.drawString(text, 0, fm.getAscent());
		g2d.dispose();
		int trayIconWidth = new TrayIcon(img).getSize().width;
		return img.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH);
	}

	public static Path filePrep(String dir, boolean... createFile) {
		dir = dir.replaceAll("/", "\\" + System.getProperty("file.separator"));
		Path path = Paths.get(dir);
		try {
			if ((createFile.length == 0 || (createFile.length > 0 && createFile[0]))) {
				if (path.getParent() != null) {
					Files.createDirectories(path.getParent());
				}
				if (!Files.exists(path) && path.toString().substring(path.toString().length() - 4).contains(".")) {
					Files.createFile(path);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return path;
	}

	public static String validInput(JDialog dialog, PrintWriter writer, String message, String name) {
		String input = "";
		while (input.equals("")) {
			input = JOptionPane.showInputDialog(dialog, message);
			try {
				writer.println(name + "=" + input);
				System.out.println(name + "=" + input);
			} catch (Throwable e) {
				JOptionPane.showMessageDialog(dialog, "An error has occured:\r\r" + e.toString());
			}
		}
		return input;
	}
}