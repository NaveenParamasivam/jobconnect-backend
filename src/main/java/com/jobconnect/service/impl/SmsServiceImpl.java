package com.jobconnect.service.impl;

import com.jobconnect.service.SmsService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.phone-number}")
    private String fromPhone;

    @Override
    public void sendRegistrationConfirmation(String toPhone, String userName) {
        String body = String.format(
                "Hi %s! Welcome to JobConnect. Your account has been created successfully. " +
                "Start exploring jobs today!", userName);
        send(toPhone, body);
    }

    @Override
    public void sendApplicationConfirmation(String toPhone, String userName, String jobTitle) {
        String body = String.format(
                "Hi %s! Your application for '%s' has been submitted successfully on JobConnect. " +
                "Good luck!", userName, jobTitle);
        send(toPhone, body);
    }

    @Override
    public void sendApplicationStatusUpdate(String toPhone, String userName,
                                             String jobTitle, String status) {
        String body = String.format(
                "Hi %s! Update from JobConnect: Your application for '%s' has been marked as %s.",
                userName, jobTitle, status);
        send(toPhone, body);
    }

    @Override
    public void sendJobPostingAlert(String toPhone, String employerName, String jobTitle) {
        String body = String.format(
                "Hi %s! Your job posting '%s' is now live on JobConnect. " +
                "Applications will start coming in soon!", employerName, jobTitle);
        send(toPhone, body);
    }

    private void send(String toPhone, String body) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(fromPhone),
                    body
            ).create();
            log.info("SMS sent to {} | SID: {}", toPhone, message.getSid());
        } catch (Exception ex) {
            log.error("Failed to send SMS to {}: {}", toPhone, ex.getMessage());
            throw ex;
        }
    }
}
