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
    private List<String> waitingList;
    private String organizerId;

    public EventModel() {}

    public EventModel(String name, String date, String ageGroup, String location,
                      double price, int totalSpots, String organizerId) {
        this.name = name;
        this.date = date;
        this.ageGroup = ageGroup;
        this.location = location;
        this.price = price;
        this.totalSpots = totalSpots;
        this.organizerId = organizerId;
        this.waitingList = new ArrayList<>();
    }

    public String getId()                        { return id; }
    public void setId(String id)                 { this.id = id; }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }

    public String getDate()                      { return date; }
    public void setDate(String date)             { this.date = date; }

    public String getAgeGroup()                  { return ageGroup; }
    public void setAgeGroup(String ageGroup)     { this.ageGroup = ageGroup; }

    public String getLocation()                  { return location; }
    public void setLocation(String location)     { this.location = location; }

    public double getPrice()                     { return price; }
    public void setPrice(double price)           { this.price = price; }

    public int getTotalSpots()                   { return totalSpots; }
    public void setTotalSpots(int totalSpots)    { this.totalSpots = totalSpots; }

    public String getOrganizerId()               { return organizerId; }
    public void setOrganizerId(String id)        { this.organizerId = id; }

    public List<String> getWaitingList()         { return waitingList; }
    public void setWaitingList(List<String> w)   { this.waitingList = w; }

    public int getWaitingListCount() {
        return waitingList != null ? waitingList.size() : 0;
    }

    public void addToWaitingList(String userId) {
        if (waitingList == null) waitingList = new ArrayList<>();
        if (!waitingList.contains(userId)) waitingList.add(userId);
    }

    public void removeFromWaitingList(String userId) {
        if (waitingList != null) waitingList.remove(userId);
    }

    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }
}