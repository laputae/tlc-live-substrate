package com.tlc.live.agent.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 直播间弹幕滑动窗口：按房间聚合最近 N 条弹幕，供情绪分析取样。
 */
@Service
public class DanmakuWindowService {

    private final Map<String, Deque<String>> windows = new ConcurrentHashMap<>();

    public void add(String roomId, String content) {
        windows.computeIfAbsent(roomId, k -> new ArrayDeque<>()).addLast(content);
        Deque<String> window = windows.get(roomId);
        while (window.size() > 100) {
            window.pollFirst();
        }
    }

    /** 取样并清空：分析过的弹幕不重复消费。 */
    public List<String> drain(String roomId) {
        Deque<String> window = windows.get(roomId);
        if (window == null || window.isEmpty()) {
            return List.of();
        }
        List<String> snapshot = new ArrayList<>(window);
        window.clear();
        return snapshot;
    }

    public int pending(String roomId) {
        Deque<String> window = windows.get(roomId);
        return window == null ? 0 : window.size();
    }

    public Map<String, Deque<String>> rooms() {
        return windows;
    }
}
