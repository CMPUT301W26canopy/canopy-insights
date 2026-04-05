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

    private List<String> coHosts;

    private List<String> invitedHosts;
    private String organizerId;

    private String visibility;
    private String description;
    private String posterImage;

    private boolean geolocationVerification;
    private ArrayList<String> geolocationList;


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

    public List<String> getInvitedHosts() {
        return invitedHosts;
    }

    public void setInvitedHosts(List<String> invitedHosts) {
        this.invitedHosts = invitedHosts;
    }

    public List<String> getCoHosts() {
        return coHosts;
    }

    public void setCoHosts(List<String> coHosts) {
        this.coHosts = coHosts;
    }

    public int getWaitingListCount() {
        return waitingList != null ? waitingList.size() : 0;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterImage() {
        return posterImage;
    }

    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
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

    public boolean isGeolocationVerification() {
        return geolocationVerification;
    }

    public void setGeolocationVerification(boolean geolocationVerification) {
        this.geolocationVerification = geolocationVerification;
    }

    public ArrayList<String> getGeolocationList() {
        return geolocationList;
    }

    public void setGeolocationList(ArrayList<String> geolocationList) {
        this.geolocationList = geolocationList;
    }

    public void addLocation(String location) {
        if (geolocationList == null) geolocationList = new ArrayList<>();
        geolocationList.add(location);
    }

    public void removeLocation(String location) {
        if (geolocationList != null) geolocationList.remove(location);
    }
}