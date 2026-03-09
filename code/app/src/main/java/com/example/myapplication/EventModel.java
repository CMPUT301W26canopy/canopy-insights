package com.example.myapplication;

/**
 * Represents a single event stored in Firestore.
 * Each field maps to a Firestore document field.
 */
public class EventModel {

    private String id;          // Firestore document ID
    private String name;
    private String date;
    private String ageGroup;
    private String location;
    private double price;
    private int totalSpots;
    private int waitingList;
    private String organizerId;

    // Firestore requires an empty constructor to deserialize documents
    public EventModel() {}

    public EventModel(String name, String date, String ageGroup, String location,
                      double price, int totalSpots, int waitingList, String organizerId) {
        this.name = name;
        this.date = date;
        this.ageGroup = ageGroup;
        this.location = location;
        this.price = price;
        this.totalSpots = totalSpots;
        this.waitingList = waitingList;
        this.organizerId = organizerId;
    }

    public String getId()            { return id; }
    public void setId(String id)     { this.id = id; }
    
    public String getName()          { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDate()          { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getAgeGroup()      { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    
    public String getLocation()      { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public double getPrice()         { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public int getTotalSpots()       { return totalSpots; }
    public void setTotalSpots(int totalSpots) { this.totalSpots = totalSpots; }
    
    public int getWaitingList()      { return waitingList; }
    public void setWaitingList(int waitingList) { this.waitingList = waitingList; }
    
    public String getOrganizerId()   { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
}