package com.navigatingcancer.healthtracker.api.data.util;

import com.navigatingcancer.healthtracker.api.rest.exception.BadDataException;
import java.util.Optional;

public class MongoUtils {
  public static <T> T getOrThrowBadData(Optional<T> optional, String message) {
    if (!optional.isPresent()) {
      throw new BadDataException(message);
    }

    return optional.get();
  }
}
