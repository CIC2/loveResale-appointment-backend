package com.resale.loveresaleappointment.logging;

import com.resale.loveresaleappointment.model.ActionType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {
    ActionType value();
}


