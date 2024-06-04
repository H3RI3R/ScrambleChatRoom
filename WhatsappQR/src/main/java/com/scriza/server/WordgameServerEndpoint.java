package com.scriza.server;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/game")
public class WordgameServerEndpoint {

    private static Map<Session, String> clients = new ConcurrentHashMap<>();
    private static WordRepository wordRepository = WordRepository.getInstance();
    private static Word currentWord;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected ... " + session.getId());
        sendMessageToAll("A new player has joined the game.");
        sendNewScrambledWord(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (message.startsWith("name:")) {
            String name = message.substring(5);
            clients.put(session, name);
            sendMessageToAll(name + " has joined the game.");
        } else {
            String name = clients.get(session);
            if (currentWord != null && currentWord.getUnscrambbledWord().equalsIgnoreCase(message.trim())) {
                sendMessageToAll(name + " unscrambled the word correctly: " + message);
                sendNewScrambledWord(session);
            } else {
                sendMessageToAll(name + " attempted to unscramble the word: " + message);
                sendNewScrambledWord(session);
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String name = clients.remove(session);
        sendMessageToAll(name + " has left the game.");
        logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
    }

    private void sendNewScrambledWord(Session session) {
        currentWord = wordRepository.getRandomWord();
        sendMessageToAll("Unscramble the word: " + currentWord.getScrambledWord());
    }

    private void sendMessageToAll(String message) {
        for (Session client : clients.keySet()) {
            try {
                client.getBasicRemote().sendText(message);
            } catch (IOException e) {
                logger.severe("Failed to send message to client: " + e.getMessage());
            }
        }
    }
}
