package nextstep.session.image.infrastructure;

import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import nextstep.session.image.domain.SessionCoverImage;
import nextstep.session.image.entity.SessionCoverImageEntity;
import nextstep.session.image.mapper.SessionCoverImageMapper;
import nextstep.session.image.repository.SessionCoverImageRepository;

import static java.util.stream.Collectors.toList;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_HEIGHT;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_ID;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_SESSION_ID;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_SIZE;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_TYPE;
import static nextstep.session.image.entity.SessionCoverImageEntity.COL_WIDTH;

@Repository("sessionCoverImageRepository")
public class JdbcSessionCoverImageRepository implements SessionCoverImageRepository {
    private final JdbcOperations jdbcTemplate;

    public JdbcSessionCoverImageRepository(JdbcOperations jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(SessionCoverImage sessionCoverImage) {
        SessionCoverImageMapper mapper = new SessionCoverImageMapper();
        SessionCoverImageEntity entity = mapper.toEntity(sessionCoverImage);

        String sql = "INSERT INTO session_cover_image (session_id, size, type, width, height) VALUES (?, ?, ?, ?, ?)";

        return jdbcTemplate.update(sql,
            entity.getSessionId(),
            entity.getSize(),
            entity.getType(),
            entity.getWidth(),
            entity.getHeight());
    }

    @Transactional(readOnly = true)
    @Override
    public List<SessionCoverImage> findBySessionId(long sessionId) {
        List<SessionCoverImageEntity> entity = selectSessionCoverImageById(sessionId);
        SessionCoverImageMapper mapper = new SessionCoverImageMapper();
        return entity.stream()
            .map(mapper::toDomain)
            .collect(toList());
    }

    private List<SessionCoverImageEntity> selectSessionCoverImageById(long session_id) {
        String sql = "SELECT id, session_id, size, type, width, height FROM session_cover_image WHERE session_id = ?";

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new SessionCoverImageEntity(
                rs.getLong(COL_ID),
                rs.getLong(COL_SESSION_ID),
                rs.getInt(COL_SIZE),
                rs.getString(COL_TYPE),
                rs.getInt(COL_WIDTH),
                rs.getInt(COL_HEIGHT)
            ),
            session_id
        );
    }
}
