package com.jobconnect.service;

public interface SmsService {
    void sendRegistrationConfirmation(String toPhone, String userName);
    void sendApplicationConfirmation(String toPhone, String userName, String jobTitle);
    void sendApplicationStatusUpdate(String toPhone, String userName, String jobTitle, String status);
    void sendJobPostingAlert(String toPhone, String employerName, String jobTitle);
}
