package com.pds.pfe.ui.utils;

public class SessionManager {
    private static String currentUser = null;
    private static String currentUserId = null;

    public static void login(String username, String userId) {
        currentUser = username;
        currentUserId = userId;
        System.out.println("Session créée - Username: " + username + ", UserId: " + userId);
    }

    public static void logout() {
        currentUser = null;
        currentUserId = null;
        System.out.println("Session détruite");
    }

    public static boolean isLoggedIn() {
        return currentUser != null && currentUserId != null;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }
}