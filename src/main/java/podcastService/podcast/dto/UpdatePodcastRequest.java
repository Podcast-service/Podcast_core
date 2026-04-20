package podcastService.podcast.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdatePodcastRequest {

        @Size(min = 3, max = 255, message = "title must be between 3 and 255 characters")
        private String title;

        @Size(max = 5000, message = "the description cannot be longer than 5000 characters")
        private String description;

        private UUID categoryId;

        private String coverImageUrl;

        @JsonIgnore
        private boolean titleSet;

        @JsonIgnore
        private boolean descriptionSet;

        @JsonIgnore
        private boolean categoryIdSet;

        @JsonIgnore
        private boolean coverImageUrlSet;

        public void setTitle(String title) {
                this.title = title;
                this.titleSet = true;
        }

        public void setDescription(String description) {
                this.description = description;
                this.descriptionSet = true;
        }


        public void setCategoryId(UUID categoryId) {
                this.categoryId = categoryId;
                this.categoryIdSet = true;
        }


        public void setCoverImageUrl(String coverImageUrl) {
                this.coverImageUrl = coverImageUrl;
                this.coverImageUrlSet = true;
        }
}
