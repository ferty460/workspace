package com.example.workspace.util;

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class IdGenerator {

    private static final int INITIAL_ID = 1;
    private static final Map<Class<?>, AtomicLong> ENTITY_ID_COUNTER = new ConcurrentHashMap<>();

    public static long generate(Class<?> clazz) {
        AtomicLong counter = ENTITY_ID_COUNTER.computeIfAbsent(clazz, _ -> new AtomicLong(INITIAL_ID));
        return counter.getAndIncrement();
    }

}
