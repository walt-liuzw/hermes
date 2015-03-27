package com.ctrip.hermes.core.pipeline.spi.internal;

import com.ctrip.hermes.core.constants.CatConstants;
import com.ctrip.hermes.core.message.ProducerMessage;
import com.ctrip.hermes.core.pipeline.PipelineContext;
import com.ctrip.hermes.core.pipeline.spi.Valve;
import com.dianping.cat.Cat;
import com.dianping.cat.configuration.NetworkInterfaceManager;
import com.dianping.cat.message.Event;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;

public class TracingMessageValve implements Valve {
	public static final String ID = "tracing";

	@Override
	public void handle(PipelineContext<?> ctx, Object payload) {
		ProducerMessage<?> msg = (ProducerMessage<?>) payload;
		String topic = msg.getTopic();

		Transaction t = Cat.newTransaction("Message.Produced", topic);
		t.addData("key", msg.getKey());

		MessageTree tree = Cat.getManager().getThreadLocalMessageTree();
		try {
			String childMsgId = Cat.createMessageId();
			String rootMsgId = tree.getRootMessageId();
			String msgId = tree.getMessageId();
			rootMsgId = rootMsgId == null ? msgId : rootMsgId;

			String ip = NetworkInterfaceManager.INSTANCE.getLocalHostAddress();
			Cat.logEvent("Message:" + topic, "Produced:" + ip, Event.SUCCESS, "key=" + msg.getKey());
			Cat.logEvent("Producer:" + ip, topic, Event.SUCCESS, "key=" + msg.getKey());

			msg.addSysProperty(CatConstants.CURRENT_MESSAGE_ID, msgId);
			msg.addSysProperty(CatConstants.SERVER_MESSAGE_ID, childMsgId);
			msg.addSysProperty(CatConstants.ROOT_MESSAGE_ID, rootMsgId);
			Cat.logEvent(CatConstants.TYPE_REMOTE_CALL, "", Event.SUCCESS, childMsgId);

			ctx.next(payload);

			// TODO
			t.setStatus(System.currentTimeMillis() + "");
		} catch (RuntimeException | Error e) {
			Cat.logError(e);
			t.setStatus(e);
			throw e;
		} finally {
			t.complete();
		}
	}

}
