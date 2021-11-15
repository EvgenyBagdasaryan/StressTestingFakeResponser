package ru.diasoft.integration.vtb.service.stub.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Sequence {

	private final static ConcurrentHashMap<String, AtomicLong> SEQUENCES = new ConcurrentHashMap<String, AtomicLong>();
	
	public static Long nextVal(String name) {
		AtomicLong seq = SEQUENCES.get(name);
		if (seq == null) {
			AtomicLong _seq = new AtomicLong();
			seq = SEQUENCES.putIfAbsent(name, _seq);
			if (seq == null) {
				seq = _seq;
			}
		}
		return seq.incrementAndGet();
	}
}
