package podcastService.media.messaging.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubtitleContentDto(
        @JsonProperty("vtt_object_key")
        String vttObjectKey,
        @JsonProperty("srt_object_key")
        String srtObjectKey
) {
}
