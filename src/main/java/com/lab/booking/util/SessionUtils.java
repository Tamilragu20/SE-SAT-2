package com.lab.booking.util;

import com.lab.booking.model.Role;
import com.lab.booking.model.User;
import jakarta.servlet.http.HttpSession;

public class SessionUtils {

    public static final String SESSION_USER_KEY = "LOGGED_IN_USER";

    public static User getLoggedInUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(SESSION_USER_KEY);
    }

    public static void setLoggedInUser(HttpSession session, User user) {
        if (session != null) {
            session.setAttribute(SESSION_USER_KEY, user);
        }
    }

    public static void clearLoggedInUser(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_USER_KEY);
            session.invalidate();
        }
    }

    public static boolean isAuthenticated(HttpSession session) {
        return getLoggedInUser(session) != null;
    }

    public static boolean isUser(HttpSession session) {
        User user = getLoggedInUser(session);
        return user != null && user.getRole() == Role.USER;
    }

    public static boolean isAdmin(HttpSession session) {
        User user = getLoggedInUser(session);
        return user != null && user.getRole() == Role.ADMINISTRATOR;
    }
}
