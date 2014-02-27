package org.powerbot.bot.rs3.loader.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.powerbot.bot.rs3.client.Client;

/**
 */
class Scanner {
	private final static int EOL = 0xA;
	private final InputStream in;

	public Scanner(final byte[] data) {
		this(new ByteArrayInputStream(data));
	}

	public Scanner(final InputStream in) {
		this.in = in;
	}

	public int readByte() {
		try {
			return in.read();
		} catch (final IOException ignored) {
			return -1;
		}
	}

	public int readShort() {
		return readByte() << 8 | readByte();
	}

	public int readInt() {
		return readShort() << 16 | readShort();
	}

	public long readLong() {
		return ((long) readInt()) << 32 | readInt() & 0xFFFFFFFFL;
	}

	public String readString() {
		return normalize(new String(readSegment()));
	}

	private String normalize(final String s) {
		return s.replace("org/powerbot/game/client", Client.class.getPackage().getName().replace('.', '/'));
	}

	public byte[] readSegment() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		int b;
		while ((b = readByte()) != EOL && b != -1) {
			out.write(b);
		}
		return out.toByteArray();
	}

	public void readSegment(final byte[] data, final int len, final int off) {
		for (int i = off; i < off + len; i++) {
			data[i] = (byte) readByte();
		}
	}
}
