package com.example.lotteryapp;

import java.util.List;

/**
 * Represents a comment or reply posted on an event.
 * The same model also tracks likes and reply depth for threaded display.
 */
public class CommentModel {

    private String message;
    private String posterID;
    private String messageID;
    private String parentID;
    private String timestamp;
    private List<String> likedBy;
    private int depth;

    /**
     * Returns the comment body.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the comment body.
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the account ID of the commenter.
     */
    public String getPosterID() {
        return posterID;
    }

    /**
     * Sets the account ID of the commenter.
     */
    public void setPosterID(String posterID) {
        this.posterID = posterID;
    }

    /**
     * Returns the unique ID used for this comment.
     */
    public String getMessageID() {
        return messageID;
    }

    /**
     * Sets the unique ID used for this comment.
     */
    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    /**
     * Returns the parent comment ID when this is a reply.
     */
    public String getParentID() {
        return parentID;
    }

    /**
     * Sets the parent comment ID when this is a reply.
     */
    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    /**
     * Returns the saved comment timestamp.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the saved comment timestamp.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the list of users who liked this comment.
     */
    public List<String> getLikedBy() {
        return likedBy;
    }

    /**
     * Sets the list of users who liked this comment.
     */
    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }

    /**
     * Returns the reply nesting depth.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the reply nesting depth.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
