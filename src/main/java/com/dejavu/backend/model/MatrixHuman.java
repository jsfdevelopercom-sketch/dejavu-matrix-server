package com.dejavu.backend.model;

import jakarta.persistence.*;

@Entity
public class MatrixHuman {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int age;
    private String gender;
    private String occupation;
    private String city;

    @Column(length = 5000)
    private String personality;

    @Column(length = 5000)
    private String relations;

    @Column(columnDefinition = "TEXT")
    private String memory;

    private int currentDay;

    @Column(columnDefinition = "TEXT")
    private String workingMemory;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    public String getRelations() { return relations; }
    public void setRelations(String relations) { this.relations = relations; }
    public String getMemory() { return memory; }
    public void setMemory(String memory) { this.memory = memory; }
    public int getCurrentDay() { return currentDay; }
    public void setCurrentDay(int currentDay) { this.currentDay = currentDay; }
    public String getWorkingMemory() { return workingMemory; }
    public void setWorkingMemory(String workingMemory) { this.workingMemory = workingMemory; }
}
