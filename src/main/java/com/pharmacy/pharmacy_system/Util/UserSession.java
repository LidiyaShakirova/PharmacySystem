package com.pharmacy.pharmacy_system.Util;

import com.pharmacy.pharmacy_system.Entity.User;

public final class UserSession {


    private static volatile UserSession instance;
    private User currentUser;

    private UserSession() {}


    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }


    public String getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }



    public void clearCurrentUser() {
        this.currentUser = null;
    }

}