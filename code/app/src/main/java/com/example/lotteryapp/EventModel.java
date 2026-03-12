package com.example.lotteryapp;

import java.util.ArrayList;
import java.util.List;

public class EventModel {
    private String id;
    private String name;
    private String date;
    private String ageGroup;
    private String location;
    private double price;
    private int totalSpots;
    // In EventModel.java
    private List<String> waitingList = new ArrayList<>();

    // Add a special setter for Firestore to handle legacy Long values
    @com.google.firebase.firestore.PropertyName("waitingList")
    public void setWaitingListFromFirestore(Object value) {
        if (value instanceof List) {
            this.waitingList = (List<String>) value;
        } else {
            // If it's a number (Long), just initialize an empty list
            this.waitingList = new ArrayList<>();
        }
    }// Track user IDs
    private String organizerId;

    public EventModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getTotalSpots() { return totalSpots; }
    public void setTotalSpots(int totalSpots) { this.totalSpots = totalSpots; }
    public List<String> getWaitingList() { return waitingList; }
    public void setWaitingList(List<String> waitingList) { this.waitingList = waitingList; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
}