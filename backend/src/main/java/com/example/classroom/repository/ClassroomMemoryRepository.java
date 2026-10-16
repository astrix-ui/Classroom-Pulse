package com.example.classroom.repository;

import com.example.classroom.model.ClassroomSession;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ClassroomMemoryRepository {

    private final ConcurrentHashMap<String, ClassroomSession> classrooms = new ConcurrentHashMap<>();

    public ClassroomSession save(ClassroomSession session) {
        if (session != null && session.getRoomCode() != null) {
            classrooms.put(session.getRoomCode().toUpperCase(), session);
        }
        return session;
    }

    public Optional<ClassroomSession> findById(String roomCode) {
        if (roomCode == null) return Optional.empty();
        return Optional.ofNullable(classrooms.get(roomCode.trim().toUpperCase()));
    }

    public boolean existsById(String roomCode) {
        if (roomCode == null) return false;
        return classrooms.containsKey(roomCode.trim().toUpperCase());
    }

    public Collection<ClassroomSession> findAll() {
        return Collections.unmodifiableCollection(classrooms.values());
    }

    public void deleteById(String roomCode) {
        if (roomCode != null) {
            classrooms.remove(roomCode.trim().toUpperCase());
        }
    }

    public void clear() {
        classrooms.clear();
    }

    public int count() {
        return classrooms.size();
    }
}

