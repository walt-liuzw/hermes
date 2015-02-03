package com.ctrip.hermes.message.codec;

public interface Codec {
	public byte[] encode(Object obj);

	public <T> T decode(byte[] raw, Class<T> clazz);
}
