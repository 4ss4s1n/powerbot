package org.powerbot.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Calendar;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.powerbot.Boot;
import org.powerbot.Configuration;
import org.powerbot.bot.Bot;
import org.powerbot.script.event.BotMenuListener;
import org.powerbot.misc.Tracker;
import org.powerbot.script.Script;
import org.powerbot.script.internal.ScriptController;

public class BotMenuBar extends JMenuBar {
	private static final long serialVersionUID = -4186554435386744949L;
	private final BotChrome chrome;
	private final JMenuItem play, stop;

	public BotMenuBar(final BotChrome chrome) {
		this.chrome = chrome;

		final JMenu file = new JMenu(BotLocale.FILE), edit = new JMenu(BotLocale.EDIT), view = new JMenu(BotLocale.VIEW),
				input = new JMenu(BotLocale.INPUT), help = new JMenu(BotLocale.HELP);

		final JMenuItem newtab = new JMenuItem(BotLocale.NEWWINDOW);
		file.add(newtab);
		newtab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Boot.fork();
			}
		});

		if (Configuration.OS != Configuration.OperatingSystem.MAC) {
			file.addSeparator();
			final JMenuItem exit = new JMenuItem(BotLocale.EXIT);
			file.add(exit);
			exit.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					chrome.close();
				}
			});
		}

		view.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(final MenuEvent e) {
				final JMenu menu = (JMenu) e.getSource();
				menu.removeAll();
				new BotMenuView(chrome, menu);
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
			}

			@Override
			public void menuCanceled(final MenuEvent e) {
			}
		});

		final ImageIcon[] playIcons = new ImageIcon[]{createControlIcon(1), createControlIcon(2)};
		play = new JMenuItem(BotLocale.PLAYSCRIPT);
		play.setIcon(playIcons[0]);
		edit.add(play);
		play.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				scriptPlayPause();
			}
		});
		stop = new JMenuItem(BotLocale.STOPSCRIPT);
		stop.setIcon(createControlIcon(0));
		edit.add(stop);
		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				scriptStop();
			}
		});

		edit.addSeparator();
		final JMenu options = new JMenu(BotLocale.OPTIONS);
		edit.add(options);

		options.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(final MenuEvent e) {
				final JMenu m = (JMenu) e.getSource();
				m.removeAll();

				final ScriptController c = chrome.getBot().controller;
				if (!c.isValid()) {
					return;
				}

				final Script s = c.bundle.get().instance.get();
				if (s == null || !(s instanceof BotMenuListener)) {
					return;
				}

				try {
					((BotMenuListener) s).menuSelected(e);
				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
				final ScriptController c = chrome.getBot().controller;
				if (!c.isValid()) {
					return;
				}

				final Script s = c.bundle.get().instance.get();
				if (s == null || !(s instanceof BotMenuListener)) {
					return;
				}

				try {
					((BotMenuListener) s).menuDeselected(e);
				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}

			@Override
			public void menuCanceled(final MenuEvent e) {
				final ScriptController c = chrome.getBot().controller;
				if (!c.isValid()) {
					return;
				}

				final Script s = c.bundle.get().instance.get();
				if (s == null || !(s instanceof BotMenuListener)) {
					return;
				}

				try {
					((BotMenuListener) s).menuCanceled(e);
				} catch (final Throwable t) {
					t.printStackTrace();
				}
			}
		});

		edit.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(final MenuEvent e) {
				final ScriptController c = chrome.getBot().controller;
				final boolean active = c.isValid() && !c.isStopping(), running = active && !c.isSuspended();

				play.setEnabled(chrome.getBot().ctx.getClient() != null && !BotPreferences.loading.get());
				play.setText(running ? BotLocale.PAUSESCRIPT : active ? BotLocale.RESUMESCRIPT : BotLocale.PLAYSCRIPT);
				play.setIcon(playIcons[running ? 1 : 0]);
				stop.setEnabled(active);

				if (active) {
					final Script script = c.bundle.get().instance.get();
					options.setEnabled(script != null && script instanceof BotMenuListener);
				} else {
					options.setEnabled(false);
				}
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
			}

			@Override
			public void menuCanceled(final MenuEvent e) {
			}
		});

		input.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(final MenuEvent e) {
				final JMenu menu = (JMenu) e.getSource();
				if (menu.getItemCount() != 0) {
					menu.removeAll();
				}
				new BotMenuInput(menu);
			}

			@Override
			public void menuDeselected(final MenuEvent e) {
			}

			@Override
			public void menuCanceled(final MenuEvent e) {
			}
		});

		if (Configuration.OS != Configuration.OperatingSystem.MAC) {
			final JMenuItem about = new JMenuItem(BotLocale.ABOUT);
			help.add(about);
			about.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					showAbout();
				}
			});
		}

		final JMenuItem license = new JMenuItem(BotLocale.LICENSE);
		help.add(license);
		license.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showLicense();
			}
		});

		add(file);
		add(edit);
		add(view);
		add(input);
		add(help);
	}

	private ImageIcon createControlIcon(final int s) {
		final Image img = new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g2 = (Graphics2D) img.getGraphics();
		g2.setColor(Color.BLACK);

		switch (s) {
		case 0:
			g2.fillRect(1, 1, 14, 14);
			break;
		case 1:
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			final Polygon p = new Polygon();
			p.addPoint(1, 1);
			p.addPoint(14, 8);
			p.addPoint(1, 14);
			g2.fillPolygon(p);
			break;
		case 2:
			g2.fillRect(2, 1, 5, 14);
			g2.fillRect(16 - 2 - 5, 1, 5, 14);
			break;
		}

		return new ImageIcon(img);
	}

	public void showAbout() {
		final String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
		final String msg = "<b>" + Configuration.NAME + " " + Integer.toString(Configuration.VERSION) + "</b>\n\n" +
				"Copyright \u00a9 2011 - " + year + " Dequeue Ltd and its licensors.\n" +
				"By using this software you agree to be bound by the terms of the license agreement.\n\n" +
				"RuneScape\u00ae is a trademark of Jagex \u00a9 1999 - " + year + " Jagex Ltd.\n" +
				"RuneScape content and materials are trademarks and copyrights of Jagex or its licensees.\n" +
				"This program is not affiliated with Jagex Ltd., nor do they endorse usage of our software.";
		final JLabel text = new JLabel("<html>" + msg.replace("\n", "<br>") + "</html>");
		final Font f = text.getFont();
		text.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 2));
		JOptionPane.showMessageDialog(chrome, text, BotLocale.ABOUT, JOptionPane.PLAIN_MESSAGE);
		Tracker.getInstance().trackPage("about/", BotLocale.ABOUT);
	}

	public void showLicense() {
		BotChrome.openURL(Configuration.URLs.LICENSE);
		Tracker.getInstance().trackPage("license/", BotLocale.LICENSE);
	}

	public synchronized void scriptPlayPause() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final Bot bot = chrome.getBot();
				final ScriptController c = chrome.getBot().controller;

				if (c.isValid()) {
					if (c.isSuspended()) {
						c.resume();
					} else {
						c.suspend();
					}
				} else {
					if (bot.ctx.getClient() != null) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								new BotPreferences(chrome);
							}
						});
					}
				}
			}
		}).start();
	}

	public synchronized void scriptStop() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				chrome.getBot().controller.stop();
			}
		}).start();
	}
}
