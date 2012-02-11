package com.brackeen.scared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MessageQueue {
    
    private static class Message {
        String text;
        int ticksRemaining;
        
        public Message(String text) {
            this.text = text;
            ticksRemaining = 100;
        }
    }
    
    private List<Message> messages;
    private final int maxSize;

    public MessageQueue(int size) {
        maxSize = size;
        messages = new ArrayList<Message>();
    }
    
    public void tick() {
        Iterator<Message> i = messages.iterator();
        while (i.hasNext()) {
            Message m = i.next();
            m.ticksRemaining--;
            if (m.ticksRemaining <= 0) {
                i.remove();
            }
        }
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public int size() {
        return messages.size();
    }
    
    public String get(int i) {
        if (i < size()) {
            return messages.get(i).text;
        }
        else {
            return null;
        }
    }
    
    public void add(String text) {
        messages.add(new Message(text));
        while (messages.size() > maxSize) {
            messages.remove(0);
        }
    }
}
