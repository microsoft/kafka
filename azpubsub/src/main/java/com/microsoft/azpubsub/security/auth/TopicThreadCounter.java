package com.microsoft.azpubsub.security.auth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TopicThreadCounter {
    private static TopicThreadCounter instance = null;
    private static Object lock = new Object();
    private Long interval = null;
    private int throttlingLevel = 0;
    ConcurrentHashMap<String, TreeMap<Long, Long>> topicThreadMap = new ConcurrentHashMap<>();

    public TopicThreadCounter(Long intvl, int level) {
        interval = intvl;
        throttlingLevel = level;
    }

    static TopicThreadCounter getInstance(Long interval, int level) {
        synchronized (lock) {
            if(null == instance) {
                instance = new TopicThreadCounter(interval, level);
            }
        }
        return instance;
    }

    public int add(String topic, Long currentTimeInMs, Long threadId, String clientId) {
        String key = TopicThreadCounter.makeKey(this.throttlingLevel, topic, clientId, threadId);
        if(!topicThreadMap.containsKey(key)) {
            topicThreadMap.put(key, new TreeMap<>());
        }
        topicThreadMap.get(key).put(currentTimeInMs, threadId);
        NavigableMap<Long, Long> subMap= topicThreadMap.get(key).tailMap(currentTimeInMs - interval, false);
        HashSet<Long> hs = new HashSet<>();
        for(Map.Entry element: subMap.entrySet()) {
            hs.add((Long)element.getValue());
        }
        topicThreadMap.put(key, new TreeMap<>(subMap));
        return hs.size();
    }

    public static String makeKey(int throttlingLevel, String topic, String clientId, Long threaId) {
        if(1 == throttlingLevel) return String.format("ClientId:%s|ThreadId:%d|Topic:%s", clientId, threaId, topic);
        else if(2 == throttlingLevel) return String.format("ClientId:%s|ThreadId:%d", clientId, threaId);
        else if(3 == throttlingLevel) return String.format("Topic:%s|ThreadId:%d", topic, threaId);
        return topic;
    }
}
