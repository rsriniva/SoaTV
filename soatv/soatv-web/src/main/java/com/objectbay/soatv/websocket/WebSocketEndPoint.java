package com.objectbay.soatv.websocket;

import java.io.IOException;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.objectbay.soatv.core.SoaTV;
import com.objectbay.soatv.websocket.messaging.Messaging.MessageSender;
import com.objectbay.soatv.websocket.messaging.MessagingContext;

@ServerEndpoint("/soatv")
public class WebSocketEndPoint {
	
	private static Logger log = LoggerFactory.getLogger(WebSocketEndPoint.class);
	
	private static String PING_MESSAGE = "ping";
	
	@Inject
	private SoaTV soaTV;
	@Inject
	private MessagingContext messagingContext;
	
	public WebSocketEndPoint() {
	}
	
    @OnMessage
    public void onMessage(String message, Session session) {
    	if(!PING_MESSAGE.equals(message)){
    		messagingContext.getMessenger().onMessage(message);
    	}
    }
    
    @OnOpen
    public void onOpen(final Session session) {
    	MessageSender messageSender = createMessageSender(session);
		configureMessagingContext(session, messageSender);
		log.debug("Web socket opened. Session maxIdleTime  is {}", session.getMaxIdleTimeout());
    }

	private void configureMessagingContext(final Session session, MessageSender messageSender) {
		soaTV.getMessagingContexts().add(messagingContext);
		messagingContext.setSession(session);
		messagingContext.getMessenger().setMessageSender(messageSender);
		messagingContext.getMonitor().addListener(messagingContext);
	}

	private MessageSender createMessageSender(final Session session) {
		MessageSender messageSender = new MessageSender(){
			
    		public synchronized void sendMessage(String msg) throws IOException {
				session.getBasicRemote().sendText(msg);
			}
		};
		return messageSender;
	}
    
    @OnClose
    public void onClose(CloseReason reason) {
    	messagingContext.getMessenger().onClose(reason);
    	if(messagingContext.getMonitor() != null){
    		messagingContext.getMonitor().removeListener(messagingContext);
    	}
    	soaTV.destroyMessagingContext(messagingContext);
    	log.debug("Web socket closed");
    }
}
