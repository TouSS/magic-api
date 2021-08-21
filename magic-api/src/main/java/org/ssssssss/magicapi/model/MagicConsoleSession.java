package org.ssssssss.magicapi.model;

import org.springframework.web.socket.WebSocketSession;
import org.ssssssss.script.MagicScriptDebugContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagicConsoleSession {

	private static final Map<String, MagicConsoleSession> cached = new ConcurrentHashMap<>();

	private final String id = UUID.randomUUID().toString();

	private WebSocketSession webSocketSession;

	private MagicScriptDebugContext magicScriptDebugContext;

	private String sessionId;

	public MagicConsoleSession(WebSocketSession webSocketSession) {
		this.webSocketSession = webSocketSession;
	}

	public MagicConsoleSession(String sessionId, MagicScriptDebugContext magicScriptDebugContext) {
		this.sessionId = sessionId;
		this.magicScriptDebugContext = magicScriptDebugContext;
	}

	public String getId() {
		return id;
	}

	public WebSocketSession getWebSocketSession() {
		return webSocketSession;
	}

	public void setWebSocketSession(WebSocketSession webSocketSession) {
		this.webSocketSession = webSocketSession;
	}

	public MagicScriptDebugContext getMagicScriptDebugContext() {
		return magicScriptDebugContext;
	}

	public void setMagicScriptDebugContext(MagicScriptDebugContext magicScriptDebugContext) {
		this.magicScriptDebugContext = magicScriptDebugContext;
	}

	public boolean writeable(){
		return webSocketSession != null && webSocketSession.isOpen();
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public static MagicConsoleSession from(WebSocketSession session){
		MagicConsoleSession magicConsoleSession = cached.get(session.getId());
		if(magicConsoleSession == null){
			magicConsoleSession = new MagicConsoleSession(session);
			cached.put(session.getId(), magicConsoleSession);
		}
		return magicConsoleSession;
	}

	public static void remove(WebSocketSession session){
		cached.remove(session.getId());
	}
}
