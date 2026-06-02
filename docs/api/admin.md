# Admin API

Base path: `/podcast/v1/admin`.

All endpoints require a valid JWT with role `admin`, mapped to `ROLE_ADMIN`. Podcast Core does not own roles and does not store them as source of truth; user and role operations are delegated to Auth-service with the incoming `Authorization` header.

## Podcasts

`GET /admin/podcasts`

Returns podcasts in all statuses. This endpoint intentionally does not reuse public podcast listing logic because public listing is `PUBLISHED` only.

Query parameters: `q`, `status`, `authorId`, `categoryId`, `page` default `0`, `size` default `20` max `100`, `sort` one of `DATE_DESC`, `DATE_ASC`, `VIEWS`, `RATING`.

`GET /admin/podcasts/{podcastId}`

Returns any podcast by id regardless of status. Returns `404` if not found.

`DELETE /admin/podcasts/{podcastId}`

Archives any podcast by setting status to `ARCHIVED`. This follows existing soft-delete semantics and does not delete media objects. Returns `204`; returns `404` if not found.

## Playlists

`GET /admin/playlists`

Returns public and private playlists without owner checks.

Query parameters: `q`, `ownerProfileId`, `isPublic`, `page` default `0`, `size` default `20` max `100`, `sort` one of `DATE_DESC`, `DATE_ASC`, `RATING`.

`GET /admin/playlists/{playlistId}`

Returns public or private playlist details without owner checks. Returns `404` if not found.

`DELETE /admin/playlists/{playlistId}`

Deletes any playlist through existing repository cascade behavior. No playlist edit endpoint is exposed. Returns `204`; returns `404` if not found.

## Users

`GET /admin/users`

Calls Auth-service `GET /auth/admin/users` and enriches returned users with local `user_profiles` and `author_profiles` when present. If a local profile is missing, `profile` or `authorProfile` is `null`.

Query parameters forwarded to Auth-service: `q`, `role`, `emailVerified`, `page` default `0`, `size` default `20` max `100`, `sort` one of `DATE_DESC`, `DATE_ASC`, `EMAIL_ASC`, `USERNAME_ASC`.

`GET /admin/users/{userId}`

Calls Auth-service `GET /auth/admin/users/{userId}` and enriches the response with local profile data. Returns `404` when Auth-service reports that the user does not exist.

`POST /admin/users/{userId}/roles`

Request:

```json
{
  "roleName": "admin"
}
```

Allowed roles: `user`, `author`, `admin`. Podcast Core validates this whitelist before calling Auth-service `POST /auth/admin/users/{userId}/roles`.

`DELETE /admin/users/{userId}/roles/admin`

Removes only the `admin` role by calling Auth-service `DELETE /auth/admin/users/{userId}/roles/admin`. Podcast Core does not expose arbitrary role removal.

## Errors

Auth-service `401`, `403`, `404`, and `409` are preserved where possible. Unexpected Auth-service failures are returned as `502 UPSTREAM_SERVICE_ERROR` using the existing error format.
