package com.microsoft.azpubsub.security.auth;

import java.util.TimerTask;

public class ThreadCounterTimerTask extends TimerTask {
    private String topicName = null;
    private String clientId = null;
    private int threadCount = 0;
    private Long ioThreadId = 0L;
    private int throttlingLevel = 0;
    private Long intervalInMs = 300000L;
    private TopicThreadCounter topicThreadCounterInstance = null;

    public ThreadCounterTimerTask(long interval, int level) {
        intervalInMs = interval;
        throttlingLevel = level;
        topicThreadCounterInstance = TopicThreadCounter.getInstance(this.intervalInMs, this.throttlingLevel);
    }

    public void setTopicName(String topic) {
        topicName = topic;
    }

    public void setClientId(String client) { clientId = client; }

    public void setIoThreadId(Long threadId) {
        ioThreadId = threadId;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThrottlingLevel (int level) { throttlingLevel = level; }
    public int getThrottlingLevel () { return throttlingLevel; }

    @Override
    public void run() {
        if(null != topicName) {
            threadCount = topicThreadCounterInstance.add(topicName, System.currentTimeMillis(), ioThreadId, clientId);
        }
    }
}
