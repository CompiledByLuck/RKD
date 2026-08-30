# PORTS.md — lastname-todo port map

Student index: `20`  
Formula: `OFFSET = index × 10 = 200`

| Service | Formula | Port | Published externally? |
|---|---:|---:|---|
| Spring Boot API | `5000 + OFFSET` | `5200` | Yes, until Nginx is configured |
| MongoDB | `27017 + OFFSET` | `27217` | No; Compose-internal only |
| Nginx | `8080 + OFFSET` | `8280` | Yes, after Nginx is configured |

Reserved: `22` for SSH; `80` and `443` are reserved for any VM-level proxy policy.

The course examples use group indices `0`, `1`, and `2`. If the instructor meant
“group slot 2” rather than student index `20`, use API `5020`, MongoDB `27037`,
and Nginx `8100` instead, and update `.env`, `docker-compose.yml` defaults,
and the Nginx configuration accordingly.
