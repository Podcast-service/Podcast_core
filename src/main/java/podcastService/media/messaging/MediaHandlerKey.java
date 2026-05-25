package podcastService.media.messaging;

import podcastService.media.messaging.contract.MediaObjectType;

public record MediaHandlerKey<T extends Enum<T>>(
        MediaObjectType objectType,
        T event
) {
}
