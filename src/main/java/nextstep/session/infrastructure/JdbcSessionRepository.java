package nextstep.session.infrastructure;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import nextstep.session.domain.Student;
import nextstep.session.entity.StudentEntity;
import nextstep.session.mapper.StudentMapper;
import nextstep.session.domain.Session;
import nextstep.session.entity.SessionEntity;
import nextstep.session.mapper.SessionMapper;
import nextstep.session.repository.SessionRepository;

@Repository("sessionRepository")
public class JdbcSessionRepository implements SessionRepository {
    private final int SESSION_ID = 1;
    private final int USER_ID = 2;
    private final int NAME = 3;
    private final int STATUS = 4;

    private final JdbcOperations jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcSessionRepository(JdbcOperations jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Transactional
    @Override
    public int save(Session session) {
        SessionMapper mapper = new SessionMapper();
        SessionEntity entity = mapper.toEntity(session);

        int sessionResult = saveSession(entity);
        int[] enrollmentResults = saveEnrollments(entity);
        return sessionResult + Arrays.stream(enrollmentResults).sum();
    }

    private int saveSession(SessionEntity entity) {
        String sql = "INSERT INTO session " +
            "(id, course_id, progress_status, enrollment_status, fee, capacity, start_date, end_date) " +
            "VALUES (:id, :course_id, :progress_status, :enrollment_status, :fee, :capacity, :start_date, :end_date)";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue(SessionEntity.COL_ID, entity.getId())
            .addValue(SessionEntity.COL_COURSE_ID, entity.getCourseId())
            .addValue(SessionEntity.COL_PROGRESS_STATUS, entity.getProgressStatus())
            .addValue(SessionEntity.COL_ENROLLMENT_STATUS, entity.getEnrollmentStatus())
            .addValue(SessionEntity.COL_FEE, entity.getFee())
            .addValue(SessionEntity.COL_CAPACITY, entity.getCapacity())
            .addValue(SessionEntity.COL_START_DATE, entity.getStartDate())
            .addValue(SessionEntity.COL_END_DATE, entity.getEndDate());

        return namedParameterJdbcTemplate.update(sql, params);
    }

    private int[] saveEnrollments(SessionEntity entity) {
        String sql = "INSERT INTO student (session_id, user_id, name, status) VALUES (?, ?, ?, ?)";

        return jdbcTemplate.batchUpdate(sql,
            new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    StudentEntity studentEntity = entity.getStudentEntities().get(i);
                    ps.setLong(SESSION_ID, studentEntity.getSessionId());
                    ps.setLong(USER_ID, studentEntity.getUserId());
                    ps.setString(NAME, studentEntity.getName());
                    ps.setString(STATUS, studentEntity.getStatus());
                }

                @Override
                public int getBatchSize() {
                    return entity.getStudentEntities().size();
                }
            });
    }

    @Transactional(readOnly = true)
    @Override
    public Session findById(long id) {
        List<Student> students = selectStudents(id);
        SessionEntity sessionEntity = selectSession(id, students);

        SessionMapper sessionMapper = new SessionMapper();
        return sessionMapper.toDomain(sessionEntity);
    }

    private SessionEntity selectSession(long id, List<Student> students) {
        String sql = "SELECT id, course_id, progress_status, enrollment_status, fee, capacity, start_date, end_date FROM session WHERE id = ?";

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> new SessionEntity(
                rs.getLong(SessionEntity.COL_ID),
                rs.getLong(SessionEntity.COL_COURSE_ID),
                rs.getString(SessionEntity.COL_PROGRESS_STATUS),
                rs.getString(SessionEntity.COL_ENROLLMENT_STATUS),
                rs.getInt(SessionEntity.COL_FEE),
                rs.getInt(SessionEntity.COL_CAPACITY),
                rs.getTimestamp(SessionEntity.COL_START_DATE).toLocalDateTime().toLocalDate(),
                rs.getTimestamp(SessionEntity.COL_END_DATE).toLocalDateTime().toLocalDate(),
                students
            ),
            id
        );
    }

    private List<Student> selectStudents(long id) {
        StudentMapper mapper = new StudentMapper();
        String sql = "SELECT id, user_id, session_id, name, status FROM student WHERE session_id = ?";

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new StudentEntity(
                rs.getLong(StudentEntity.COL_ID),
                rs.getLong(StudentEntity.COL_USER_ID),
                rs.getLong(StudentEntity.COL_SESSION_ID),
                rs.getString(StudentEntity.COL_NAME),
                rs.getString(StudentEntity.COL_STATUS)
            ),
            id
        ).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
