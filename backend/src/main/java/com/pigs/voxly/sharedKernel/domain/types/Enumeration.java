package com.pigs.voxly.sharedKernel.domain.types;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Enumeration<T extends Enumeration<T>> implements Comparable<Enumeration<T>> {

    private static final Map<Class<?>, Map<Integer, ?>> BY_ID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, ?>> BY_NAME_CACHE = new ConcurrentHashMap<>();

    private final int id;
    private final String name;

    protected Enumeration(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enumeration<T>> Collection<T> getAll(Class<T> type) {
        return ((Map<Integer, T>) BY_ID_CACHE.computeIfAbsent(type, Enumeration::buildByIdMap)).values();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enumeration<T>> Optional<T> fromId(Class<T> type, int id) {
        Map<Integer, T> map = (Map<Integer, T>) BY_ID_CACHE.computeIfAbsent(type, Enumeration::buildByIdMap);
        return Optional.ofNullable(map.get(id));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enumeration<T>> Optional<T> fromName(Class<T> type, String name) {
        Map<String, T> map = (Map<String, T>) BY_NAME_CACHE.computeIfAbsent(type, Enumeration::buildByNameMap);
        return Optional.ofNullable(map.get(name.toUpperCase(Locale.ROOT)));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enumeration<T>> Map<Integer, T> buildByIdMap(Class<?> type) {
        Map<Integer, T> map = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && type.isAssignableFrom(field.getType())) {
                try {
                    T value = (T) field.get(null);
                    map.put(value.getId(), value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to read enumeration field: " + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enumeration<T>> Map<String, T> buildByNameMap(Class<?> type) {
        Map<String, T> map = new LinkedHashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && Modifier.isPublic(field.getModifiers())
                    && type.isAssignableFrom(field.getType())) {
                try {
                    T value = (T) field.get(null);
                    map.put(value.getName().toUpperCase(Locale.ROOT), value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to read enumeration field: " + field.getName(), e);
                }
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enumeration<?> that = (Enumeration<?>) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public int compareTo(Enumeration<T> other) {
        return Integer.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return name;
    }
}
