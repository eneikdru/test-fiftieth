package com.eneik.epidemiology.auth;

public record MoodleProfile(String username, String moodleRole, String department, String email, String fullName, String courses) {}
