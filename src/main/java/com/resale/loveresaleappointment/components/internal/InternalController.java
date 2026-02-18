package com.resale.loveresaleappointment.components.internal;

import com.resale.loveresaleappointment.logging.LogActivity;
import com.resale.loveresaleappointment.model.ActionType;
import com.resale.loveresaleappointment.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final InternalService internalService;

    @GetMapping("/onCall/{userId}")
    @LogActivity(ActionType.IS_USER_ON_CALL_INTERNAL)
    public ResponseEntity<ReturnObject<?>> isUserOnCall(
            @PathVariable Integer userId
    ) {
        ReturnObject<?> result =
                internalService.isUserOnCall(userId);

        return ResponseEntity.ok(result);
    }
}


