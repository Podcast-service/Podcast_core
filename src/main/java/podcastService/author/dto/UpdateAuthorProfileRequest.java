package podcastService.author.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAuthorProfileRequest {

    @Size(min = 2, max = 100, message = "authorName must be between 2 and 100 characters")
    private String authorName;

    @Size(max = 1000, message = "description must not be longer than 1000 characters")
    private String description;

    @JsonIgnore
    private boolean authorNameSet;

    @JsonIgnore
    private boolean descriptionSet;

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
        this.authorNameSet = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionSet = true;
    }
}
