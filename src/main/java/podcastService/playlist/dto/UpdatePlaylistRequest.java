package podcastService.playlist.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePlaylistRequest {

    @Size(min = 1, max = 255, message = "title must be between 1 and 255 characters")
    private String title;

    @Size(max = 1000, message = "description must not be longer than 1000 characters")
    private String description;

    @Size(max = 2048, message = "coverImageUrl must not be longer than 2048 characters")
    private String coverImageUrl;

    private Boolean isPublic;

    @JsonIgnore
    private boolean titleSet;

    @JsonIgnore
    private boolean descriptionSet;

    @JsonIgnore
    private boolean coverImageUrlSet;

    @JsonIgnore
    private boolean publicSet;

    public void setTitle(String title) {
        this.title = title;
        this.titleSet = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        this.coverImageUrlSet = true;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
        this.publicSet = true;
    }
}
