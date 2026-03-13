package com.example.lotteryapp;

/**
 * Represents a single event stored in Firestore.
 * Each field maps to a Firestore document field.
 */
public class EventModel {

    private String id;
    private String name;
    private String date;
    private String ageGroup;
    private String location;
    private double price;
    private int totalSpots;
    private int waitingList;
    private String organizerId;

    /**
     * Firestore requires an empty constructor to deserialize documents
     */
    public EventModel() {}

    /**
     * Constructs a new EventModel with specified details.
     * @param name the name of the event.
     * @param date the date of the event.
     * @param ageGroup the target age group for the event.
     * @param location the location where the event is held.
     * @param price the price of admission for the event.
     * @param totalSpots the total number of spots available for the event.
     * @param waitingList the maximum number of people on the waiting list.
     * @param organizerId the ID of the organizer of the event.
     */
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

    /**
     * Gets the event ID.
     * @return the event ID.
     */
    public String getId()            { return id; }
    /**
     * Sets the event ID.
     * @param id the event ID.
     */
    public void setId(String id)     { this.id = id; }
    
    /**
     * Gets the name of the event.
     * @return the name of the event.
     */
    public String getName()          { return name; }
    /**
     * Sets the name of the event.
     * @param name the name of the event.
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Gets the date of the event.
     * @return the date of the event.
     */
    public String getDate()          { return date; }
    /**
     * Sets the date of the event.
     * @param date the date of the event.
     */
    public void setDate(String date) { this.date = date; }
    
    /**
     * Gets the target age group.
     * @return the target age group.
     */
    public String getAgeGroup()      { return ageGroup; }
    /**
     * Sets the target age group.
     * @param ageGroup the target age group.
     */
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    
    /**
     * Gets the location of the event.
     * @return the location of the event.
     */
    public String getLocation()      { return location; }
    /**
     * Sets the location of the event.
     * @param location the location of the event.
     */
    public void setLocation(String location) { this.location = location; }
    
    /**
     * Gets the price of the event.
     * @return the price of the event.
     */
    public double getPrice()         { return price; }
    /**
     * Sets the price of the event.
     * @param price the price of the event.
     */
    public void setPrice(double price) { this.price = price; }
    
    /**
     * Gets the total spots available.
     * @return the total spots available.
     */
    public int getTotalSpots()       { return totalSpots; }
    /**
     * Sets the total spots available.
     * @param totalSpots the total spots available.
     */
    public void setTotalSpots(int totalSpots) { this.totalSpots = totalSpots; }
    
    /**
     * Gets the waiting list limit.
     * @return the waiting list limit.
     */
    public int getWaitingList()      { return waitingList; }
    /**
     * Sets the waiting list limit.
     * @param waitingList the waiting list limit.
     */
    public void setWaitingList(int waitingList) { this.waitingList = waitingList; }
    
    /**
     * Gets the organizer's ID.
     * @return the organizer's ID.
     */
    public String getOrganizerId()   { return organizerId; }
    /**
     * Sets the organizer's ID.
     * @param organizerId the organizer's ID.
     */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
}
