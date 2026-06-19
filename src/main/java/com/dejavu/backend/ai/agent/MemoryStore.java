package com.dejavu.backend.ai.agent;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class MemoryStore {
    public List<WorkingMemoryItem> workingMemory = new ArrayList<>();
    public List<EpisodicMemory> episodicMemory = new ArrayList<>();
    public SemanticIdentity semanticIdentity = new SemanticIdentity();
    
    public void addWorkingMemory(WorkingMemoryItem item) {
        workingMemory.add(item);
        if (workingMemory.size() > 5) workingMemory.remove(0);
    }
    public void tick() {
        workingMemory.forEach(item -> item.expiresInTurns--);
        workingMemory = workingMemory.stream().filter(item -> item.expiresInTurns > 0).collect(Collectors.toList());
    }
}
