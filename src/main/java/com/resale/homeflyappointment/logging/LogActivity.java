package com.resale.homeflyappointment.logging;

import com.resale.homeflyappointment.model.ActionType;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {
    ActionType value();
}


