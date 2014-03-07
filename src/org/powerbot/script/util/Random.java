package org.powerbot.script.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Random {
	private static final java.util.Random random;
	private static final AtomicLong seeded;
	private static final double[] pd;
	private static final double ln2 = Math.log(2);

	static {
		java.util.Random r;
		try {
			r = SecureRandom.getInstance("SHA1PRNG", "SUN");
		} catch (final Exception ignored) {
			r = new java.util.Random();
		}
		r.setSeed(r.nextLong());
		seeded = new AtomicLong(System.nanoTime());
		random = r;

		pd = new double[2];
		final double[] e = {3d, 45d + r.nextInt(11), 12d + r.nextGaussian()};
		final double x[] = {Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() >> 30};
		pd[0] = 4 * Math.log(Math.sin(((Math.PI / x[0]) * Math.PI + 1) / 4)) / Math.PI + 2 * Math.PI * (Math.PI / x[0]) / 3 - 4 * Math.log(Math.sin(.25d)) / Math.PI;
		pd[0] = e[0] * Math.exp(Math.pow(pd[0], 0.75d)) + e[1];
		pd[1] = e[2] * Math.exp(1 / Math.cosh(x[1]));
	}

	private static void reseed() {
		if (System.nanoTime() - seeded.get() > 35 * 60 * 1e+9) {
			seeded.set(System.nanoTime());
			random.setSeed(random.nextLong());
		}
	}

	/**
	 * Returns a suggested human reaction delay.
	 *
	 * @return a random number
	 */
	public static int getDelay() {
		return (int) ((-1 + 2 * nextDouble()) * pd[1] + pd[0]);
	}

	public static void sleep() {
		try {
			Thread.sleep(getDelay() * 10);
		} catch (final InterruptedException ignored) {
		}
	}

	public static void sleepHicks(final int depth) {
		final int d = 105 * (int) (Math.log(depth * 2) / ln2);
		try {
			Thread.sleep(d);
		} catch (final InterruptedException ignored) {
		}
	}

	/**
	 * Generates a random boolean.
	 *
	 * @return returns true or false randomly
	 */
	public static boolean nextBoolean() {
		return random.nextBoolean();
	}

	/**
	 * Returns a pseudo-generated random number.
	 *
	 * @param min minimum bound (inclusive)
	 * @param max maximum bound (exclusive)
	 * @return the random number between min and max (inclusive, exclusive)
	 */
	public static int nextInt(final int min, final int max) {
		final int a = min < max ? min : max, b = max > min ? max : min;
		return a + (b == a ? 0 : random.nextInt(b - a));
	}

	/**
	 * Returns the next pseudo-random double, distributed between min and max.
	 *
	 * @param min the minimum bound
	 * @param max the maximum bound
	 * @return the random number between min and max
	 */
	public static double nextDouble(final double min, final double max) {
		final double a = min < max ? min : max, b = max > min ? max : min;
		return a + random.nextDouble() * (b - a);
	}

	/**
	 * Returns the next pseudo-random double.
	 *
	 * @return the next pseudo-random, a value between {@code 0.0} and {@code 1.0}.
	 */
	public static double nextDouble() {
		return random.nextDouble();
	}

	/**
	 * Returns the next pseudorandom, Gaussian ("normally") distributed {@code double} value with mean {@code 0.0} and
	 * standard deviation {@code 1.0}.
	 *
	 * @return a gaussian distributed number
	 */
	public static double nextGaussian() {
		reseed();
		return random.nextGaussian();
	}

	/**
	 * Returns a pseudo-random gaussian distributed number between the given min and max with the provided standard deviation.
	 *
	 * @param min the minimum bound
	 * @param max the maximum bound
	 * @param sd  the standard deviation from the mean
	 * @return a gaussian distributed number between the provided bounds
	 */
	public static int nextGaussian(final int min, final int max, final double sd) {
		return nextGaussian(min, max, (max - min) / 2, sd);
	}

	/**
	 * Returns a pseudo-random gaussian distributed number between the given min and max with the provided standard deviation.
	 *
	 * @param min  the minimum bound
	 * @param max  the maximum bound
	 * @param mean the mean value
	 * @param sd   the standard deviation from the mean
	 * @return a gaussian distributed number between the provided bounds
	 */
	public static int nextGaussian(final int min, final int max, final int mean, final double sd) {
		return min + Math.abs(((int) (nextGaussian() * sd + mean)) % (max - min));
	}
}
