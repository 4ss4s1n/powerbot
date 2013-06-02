package org.powerbot.script.internal.wrappers;

import org.powerbot.client.Node;

public class HashTable {
	private final org.powerbot.client.HashTable nc;
	private Node curr;
	private int pos = 0;

	public HashTable(final org.powerbot.client.HashTable nc) {
		if (nc == null) throw new IllegalArgumentException();
		this.nc = nc;
	}

	public Node getFirst() {
		pos = 0;
		return getNext();
	}

	public Node getNext() {
		Node[] b = nc.getBuckets();
		if (b == null) return null;
		if (pos > 0 && pos <= b.length && b[pos - 1] != curr) {
			Node n = curr;
			curr = n.getNext();
			return n;
		}
		while (pos < b.length) {
			Node n = b[pos++].getNext();
			if (b[pos - 1] != n) {
				curr = n.getNext();
				return n;
			}
		}
		return null;
	}
}