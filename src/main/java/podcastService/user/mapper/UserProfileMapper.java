package podcastService.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import podcastService.user.dto.UserProfilePrivateResponse;
import podcastService.user.dto.UserProfileResponse;
import podcastService.user.entity.UserProfileEntity;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "theme", source = "theme")
    @Mapping(target = "language", source = "language")
    @Mapping(target = "createdAt", source = "createdAt")
    UserProfilePrivateResponse toPrivateResponse(UserProfileEntity entity);

    UserProfileResponse toResponse(UserProfileEntity entity);
}
