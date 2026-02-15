package com.resale.homeflyappointment.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MessageUtil {

    private final MessageSource messageSource;
    private final HttpServletRequest request;

    public String getMessage(String key, Object... args) {
        try {   String localeHeader = request.getHeader("locale");
            Locale locale = (localeHeader != null && !localeHeader.isEmpty())
                    ? new Locale(localeHeader)
                    : Locale.ENGLISH;
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return key;
        }
    }


    public Locale getCurrentLocale() {
        String localeHeader = request.getHeader("locale");
        return (localeHeader != null && !localeHeader.isEmpty())
                ? new Locale(localeHeader)
                : Locale.ENGLISH;
    }

}




