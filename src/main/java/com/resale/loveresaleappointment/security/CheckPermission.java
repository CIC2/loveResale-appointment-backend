package com.resale.loveresaleappointment.security;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CheckPermission {
    String[] value();
    MatchType match() default MatchType.ANY;
}


