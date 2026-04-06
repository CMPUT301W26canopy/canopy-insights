package com.example.lotteryapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event document in Firestore, including organizer settings,
 * waiting-list state, invite state, and poster metadata.
 */
public class EventModel {

    private String id;
    private String name;
    private String date;
    private String ageGroup;
    private String location;
    private double price;
    private int totalSpots;
    private int waitingListLimit;
    private List<String> waitingList;
    private int waitingCount = -1;

    private List<String> coHosts;

    private List<String> invitedHosts;
    private List<String> invitedParticipants;
    private List<String> declinedParticipantInvites;
    private String organizerId;

    private String visibility;
    private String description;
    private String posterImage; // Base64 or URL

    private boolean geolocationVerification;
    private ArrayList<String> geolocationList;

    /**
     * Empty constructor required for Firestore mapping.
     */
    public EventModel() {}

    /**
     * Creates an event model with the main fields used during creation.
     */
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

    /**
     * Returns the Firestore document ID for the event.
     */
    public String getId()                        { return id; }
    /**
     * Sets the Firestore document ID for the event.
     */
    public void setId(String id)                 { this.id = id; }

    /**
     * Returns the display name of the event.
     */
    public String getName()                      { return name; }
    /**
     * Sets the display name of the event.
     */
    public void setName(String name)             { this.name = name; }

    /**
     * Returns the event date string.
     */
    public String getDate()                      { return date; }
    /**
     * Sets the event date string.
     */
    public void setDate(String date)             { this.date = date; }

    /**
     * Returns the configured age group label.
     */
    public String getAgeGroup()                  { return ageGroup; }
    /**
     * Sets the configured age group label.
     */
    public void setAgeGroup(String ageGroup)     { this.ageGroup = ageGroup; }

    /**
     * Returns the event location text.
     */
    public String getLocation()                  { return location; }
    /**
     * Sets the event location text.
     */
    public void setLocation(String location)     { this.location = location; }

    /**
     * Returns the event price.
     */
    public double getPrice()                     { return price; }
    /**
     * Sets the event price.
     */
    public void setPrice(double price)           { this.price = price; }

    /**
     * Returns the number of available event spots.
     */
    public int getTotalSpots()                   { return totalSpots; }
    /**
     * Sets the number of available event spots.
     */
    public void setTotalSpots(int totalSpots)    { this.totalSpots = totalSpots; }

    /**
     * Returns the optional waiting-list limit.
     */
    public int getWaitingListLimit()             { return waitingListLimit; }
    /**
     * Sets the optional waiting-list limit. Negative values are clamped to zero.
     */
    public void setWaitingListLimit(int waitingListLimit) {
        this.waitingListLimit = Math.max(waitingListLimit, 0);
    }

    /**
     * Returns the organizer account ID.
     */
    public String getOrganizerId()               { return organizerId; }
    /**
     * Sets the organizer account ID.
     */
    public void setOrganizerId(String id)        { this.organizerId = id; }

    /**
     * Returns the stored waiting-list user IDs.
     */
    public List<String> getWaitingList()         { return waitingList; }
    /**
     * Replaces the stored waiting-list user IDs.
     */
    public void setWaitingList(List<String> w)   { this.waitingList = w; }

    /**
     * Returns the users currently assigned as invited co-hosts.
     */
    public List<String> getInvitedHosts() {
        return invitedHosts;
    }

    /**
     * Sets the users currently assigned as invited co-hosts.
     */
    public void setInvitedHosts(List<String> invitedHosts) {
        this.invitedHosts = invitedHosts;
    }

    /**
     * Returns the confirmed co-host list when used.
     */
    public List<String> getCoHosts() {
        return coHosts;
    }

    /**
     * Sets the confirmed co-host list when used.
     */
    public void setCoHosts(List<String> coHosts) {
        this.coHosts = coHosts;
    }

    /**
     * Returns the users invited to join a private event waiting list.
     */
    public List<String> getInvitedParticipants() {
        return invitedParticipants;
    }

    /**
     * Sets the users invited to join a private event waiting list.
     */
    public void setInvitedParticipants(List<String> invitedParticipants) {
        this.invitedParticipants = invitedParticipants;
    }

    /**
     * Returns users who turned down a private waiting-list invite.
     */
    public List<String> getDeclinedParticipantInvites() {
        return declinedParticipantInvites;
    }

    /**
     * Sets users who turned down a private waiting-list invite.
     */
    public void setDeclinedParticipantInvites(List<String> declinedParticipantInvites) {
        this.declinedParticipantInvites = declinedParticipantInvites;
    }

    /**
     * Returns the best available waiting count, preferring the explicit count when set.
     */
    public int getWaitingListCount() {
        if (waitingCount >= 0) {
            return waitingCount;
        }
        return waitingList != null ? waitingList.size() : 0;
    }

    /**
     * Stores a computed waiting count for UI display.
     */
    public void setWaitingCount(int waitingCount) {
        this.waitingCount = Math.max(waitingCount, 0);
    }

    /**
     * Returns whether the event is public or private.
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * Sets whether the event is public or private.
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the poster image data for the event.
     */
    public String getPosterImage() {
        return posterImage;
    }

    /**
     * Sets the poster image data for the event.
     */
    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
    }

    /**
     * Adds a user to the waiting list if they are not already present.
     */
    public void addToWaitingList(String userId) {
        if (waitingList == null) waitingList = new ArrayList<>();
        if (!waitingList.contains(userId)) waitingList.add(userId);
    }

    /**
     * Removes a user from the waiting list if present.
     */
    public void removeFromWaitingList(String userId) {
        if (waitingList != null) waitingList.remove(userId);
    }

    /**
     * Returns true when the given user is already on the waiting list.
     */
    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }

    /**
     * Returns whether location verification is required for joining.
     */
    public boolean isGeolocationVerification() {
        return geolocationVerification;
    }

    /**
     * Sets whether location verification is required for joining.
     */
    public void setGeolocationVerification(boolean geolocationVerification) {
        this.geolocationVerification = geolocationVerification;
    }

    /**
     * Returns the allowed geolocation labels for the event.
     */
    public ArrayList<String> getGeolocationList() {
        return geolocationList;
    }

    /**
     * Replaces the allowed geolocation labels for the event.
     */
    public void setGeolocationList(ArrayList<String> geolocationList) {
        this.geolocationList = geolocationList;
    }

    /**
     * Adds an allowed join location.
     */
    public void addLocation(String location) {
        if (geolocationList == null) geolocationList = new ArrayList<>();
        geolocationList.add(location);
    }

    /**
     * Removes an allowed join location.
     */
    public void removeLocation(String location) {
        if (geolocationList != null) geolocationList.remove(location);
    }
}
