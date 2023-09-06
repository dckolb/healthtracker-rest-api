package com.navigatingcancer.healthtracker.api.data.util;

import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

public class ValidatorUtils {
    public static void raiseValidationError(BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            List<FieldError> errors = bindingResult.getFieldErrors();
            for (FieldError error : errors) {
                String msg = error.getField() + ' ' + error.getDefaultMessage();
                sb.append(msg);
                sb.append("\n");
            }

            String msg = sb.toString();
            throw new BadDataException(msg);
        }
    }
}
