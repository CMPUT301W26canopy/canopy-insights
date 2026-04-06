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

    private String registrationStartDate;
    private String registrationEndDate;

    /**
     * Empty constructor required for Firestore mapping.
     */
    public EventModel() {}

    /**
     * Creates an event model with the main fields used during creation.
     * @param name The display name of the event.
     * @param date The event date string.
     * @param ageGroup The configured age group label.
     * @param location The event location text.
     * @param price The event price.
     * @param totalSpots The number of available event spots.
     * @param organizerId The organizer account ID.
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
     * @return The Firestore document ID.
     */
    public String getId()                        { return id; }
    /**
     * Sets the Firestore document ID for the event.
     * @param id The Firestore document ID.
     */
    public void setId(String id)                 { this.id = id; }

    /**
     * Returns the display name of the event.
     * @return The display name.
     */
    public String getName()                      { return name; }
    /**
     * Sets the display name of the event.
     * @param name The display name.
     */
    public void setName(String name)             { this.name = name; }

    /**
     * Returns the event date string.
     * @return The event date.
     */
    public String getDate()                      { return date; }
    /**
     * Sets the event date string.
     * @param date The event date.
     */
    public void setDate(String date)             { this.date = date; }

    /**
     * Returns the configured age group label.
     * @return The age group label.
     */
    public String getAgeGroup()                  { return ageGroup; }
    /**
     * Sets the configured age group label.
     * @param ageGroup The age group label.
     */
    public void setAgeGroup(String ageGroup)     { this.ageGroup = ageGroup; }

    /**
     * Returns the event location text.
     * @return The event location.
     */
    public String getLocation()                  { return location; }
    /**
     * Sets the event location text.
     * @param location The event location.
     */
    public void setLocation(String location)     { this.location = location; }

    /**
     * Returns the event price.
     * @return The event price.
     */
    public double getPrice()                     { return price; }
    /**
     * Sets the event price.
     * @param price The event price.
     */
    public void setPrice(double price)           { this.price = price; }

    /**
     * Returns the number of available event spots.
     * @return The number of total spots.
     */
    public int getTotalSpots()                   { return totalSpots; }
    /**
     * Sets the number of available event spots.
     * @param totalSpots The number of total spots.
     */
    public void setTotalSpots(int totalSpots)    { this.totalSpots = totalSpots; }

    /**
     * Returns the optional waiting-list limit.
     * @return The waiting-list limit.
     */
    public int getWaitingListLimit()             { return waitingListLimit; }
    /**
     * Sets the optional waiting-list limit. Negative values are clamped to zero.
     * @param waitingListLimit The waiting-list limit.
     */
    public void setWaitingListLimit(int waitingListLimit) {
        this.waitingListLimit = Math.max(waitingListLimit, 0);
    }

    /**
     * Returns the organizer account ID.
     * @return The organizer ID.
     */
    public String getOrganizerId()               { return organizerId; }
    /**
     * Sets the organizer account ID.
     * @param id The organizer ID.
     */
    public void setOrganizerId(String id)        { this.organizerId = id; }

    /**
     * Returns the stored waiting-list user IDs.
     * @return The list of waiting-list user IDs.
     */
    public List<String> getWaitingList()         { return waitingList; }
    /**
     * Replaces the stored waiting-list user IDs.
     * @param w The list of waiting-list user IDs.
     */
    public void setWaitingList(List<String> w)   { this.waitingList = w; }

    /**
     * Returns the users currently assigned as invited co-hosts.
     * @return The list of invited co-host IDs.
     */
    public List<String> getInvitedHosts() {
        return invitedHosts;
    }

    /**
     * Sets the users currently assigned as invited co-hosts.
     * @param invitedHosts The list of invited co-host IDs.
     */
    public void setInvitedHosts(List<String> invitedHosts) {
        this.invitedHosts = invitedHosts;
    }

    /**
     * Returns the confirmed co-host list when used.
     * @return The list of confirmed co-host IDs.
     */
    public List<String> getCoHosts() {
        return coHosts;
    }

    /**
     * Sets the confirmed co-host list when used.
     * @param coHosts The list of confirmed co-host IDs.
     */
    public void setCoHosts(List<String> coHosts) {
        this.coHosts = coHosts;
    }

    /**
     * Returns the users invited to join a private event waiting list.
     * @return The list of invited participant IDs.
     */
    public List<String> getInvitedParticipants() {
        return invitedParticipants;
    }

    /**
     * Sets the users invited to join a private event waiting list.
     * @param invitedParticipants The list of invited participant IDs.
     */
    public void setInvitedParticipants(List<String> invitedParticipants) {
        this.invitedParticipants = invitedParticipants;
    }

    /**
     * Returns users who turned down a private waiting-list invite.
     * @return The list of IDs who declined the invitation.
     */
    public List<String> getDeclinedParticipantInvites() {
        return declinedParticipantInvites;
    }

    /**
     * Sets users who turned down a private waiting-list invite.
     * @param declinedParticipantInvites The list of IDs who declined the invitation.
     */
    public void setDeclinedParticipantInvites(List<String> declinedParticipantInvites) {
        this.declinedParticipantInvites = declinedParticipantInvites;
    }

    /**
     * Returns the best available waiting count, preferring the explicit count when set.
     * @return The waiting-list count.
     */
    public int getWaitingListCount() {
        if (waitingCount >= 0) {
            return waitingCount;
        }
        return waitingList != null ? waitingList.size() : 0;
    }

    /**
     * Stores a computed waiting count for UI display.
     * @param waitingCount The waiting count for display.
     */
    public void setWaitingCount(int waitingCount) {
        this.waitingCount = Math.max(waitingCount, 0);
    }

    /**
     * Returns whether the event is public or private.
     * @return The visibility status.
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * Sets whether the event is public or private.
     * @param visibility The visibility status.
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    /**
     * Returns the event description.
     * @return The event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     * @param description The event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the poster image data for the event.
     * @return The poster image data (Base64 or URL).
     */
    public String getPosterImage() {
        return posterImage;
    }

    /**
     * Sets the poster image data for the event.
     * @param posterImage The poster image data (Base64 or URL).
     */
    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
    }

    /**
     * Adds a user to the waiting list if they are not already present.
     * @param userId The ID of the user to add.
     */
    public void addToWaitingList(String userId) {
        if (waitingList == null) waitingList = new ArrayList<>();
        if (!waitingList.contains(userId)) waitingList.add(userId);
    }

    /**
     * Removes a user from the waiting list if present.
     * @param userId The ID of the user to remove.
     */
    public void removeFromWaitingList(String userId) {
        if (waitingList != null) waitingList.remove(userId);
    }

    /**
     * Returns true when the given user is already on the waiting list.
     * @param userId The ID of the user to check.
     * @return True if the user is on the waiting list.
     */
    public boolean isOnWaitingList(String userId) {
        return waitingList != null && waitingList.contains(userId);
    }

    /**
     * Returns whether location verification is required for joining.
     * @return True if geolocation verification is required.
     */
    public boolean isGeolocationVerification() {
        return geolocationVerification;
    }

    /**
     * Sets whether location verification is required for joining.
     * @param geolocationVerification True if geolocation verification is required.
     */
    public void setGeolocationVerification(boolean geolocationVerification) {
        this.geolocationVerification = geolocationVerification;
    }

    /**
     * Returns the allowed geolocation labels for the event.
     * @return The list of allowed locations.
     */
    public ArrayList<String> getGeolocationList() {
        return geolocationList;
    }

    /**
     * Replaces the allowed geolocation labels for the event.
     * @param geolocationList The list of allowed locations.
     */
    public void setGeolocationList(ArrayList<String> geolocationList) {
        this.geolocationList = geolocationList;
    }

    /**
     * Adds an allowed join location.
     * @param location The location label to add.
     */
    public void addLocation(String location) {
        if (geolocationList == null) geolocationList = new ArrayList<>();
        geolocationList.add(location);
    }

    /**
     * Removes an allowed join location.
     * @param location The location label to remove.
     */
    public void removeLocation(String location) {
        if (geolocationList != null) geolocationList.remove(location);
    }

    /**
     * Returns the registration start date string.
     * @return The registration start date.
     */
    public String getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Sets the registration start date string.
     * @param registrationStartDate The registration start date.
     */
    public void setRegistrationStartDate(String registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Returns the registration end date string.
     * @return The registration end date.
     */
    public String getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Sets the registration end date string.
     * @param registrationEndDate The registration end date.
     */
    public void setRegistrationEndDate(String registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }
}
