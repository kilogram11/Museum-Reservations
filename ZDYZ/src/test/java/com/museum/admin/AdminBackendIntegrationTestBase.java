package com.museum.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.museum.entity.Activity;
import com.museum.entity.Admin;
import com.museum.entity.Day;
import com.museum.entity.Identity;
import com.museum.entity.Join;
import com.museum.entity.Museum;
import com.museum.entity.News;
import com.museum.entity.Time;
import com.museum.mapper.ActivityMapper;
import com.museum.mapper.AdminMapper;
import com.museum.mapper.DayMapper;
import com.museum.mapper.IdentityMapper;
import com.museum.mapper.JoinMapper;
import com.museum.mapper.MuseumMapper;
import com.museum.mapper.NewsMapper;
import com.museum.mapper.TimeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "booking.scheduler.enabled=false"
)
public abstract class AdminBackendIntegrationTestBase {

    protected static final String TEST_PREFIX = "test_admin_backend_";
    protected static final String TEST_ADMIN_ID = TEST_PREFIX + "admin";
    protected static final String TEST_USER_ID = TEST_PREFIX + "user";
    protected static final String TEST_IDENTITY_ID = TEST_PREFIX + "identity";
    protected static final String TEST_JOIN_ID = TEST_PREFIX + "join";
    protected static final String TEST_TIME_MARK = TEST_PREFIX + "time_mark";

    @Autowired
    protected AdminMapper adminMapper;

    @Autowired
    protected MuseumMapper museumMapper;

    @Autowired
    protected ActivityMapper activityMapper;

    @Autowired
    protected NewsMapper newsMapper;

    @Autowired
    protected IdentityMapper identityMapper;

    @Autowired
    protected JoinMapper joinMapper;

    @Autowired
    protected DayMapper dayMapper;

    @Autowired
    protected TimeMapper timeMapper;

    @BeforeEach
    void cleanBeforeEach() {
        cleanTestData();
    }

    @AfterEach
    void cleanAfterEach() {
        cleanTestData();
    }

    protected void cleanTestData() {
        List<Museum> museums = museumMapper.selectList(
                new QueryWrapper<Museum>().like("MUSEUM_TITLE", TEST_PREFIX));
        for (Museum museum : museums) {
            dayMapper.delete(new QueryWrapper<Day>().eq("MUSEUM_ID", museum.getMuseumId()));
            timeMapper.delete(new QueryWrapper<Time>().eq("MUSEUM_ID", museum.getMuseumId()));
            museumMapper.deleteById(museum.getId());
        }

        List<Activity> activities = activityMapper.selectList(
                new QueryWrapper<Activity>().like("ACTIVITY_TITLE", TEST_PREFIX));
        for (Activity activity : activities) {
            dayMapper.delete(new QueryWrapper<Day>().eq("ACTIVITY_ID", activity.getActivityId()));
            timeMapper.delete(new QueryWrapper<Time>().eq("ACTIVITY_ID", activity.getActivityId()));
            activityMapper.deleteById(activity.getId());
        }

        newsMapper.delete(new QueryWrapper<News>()
                .like("NEWS_TITLE", TEST_PREFIX));
        joinMapper.delete(new QueryWrapper<Join>()
                .like("JOIN_ID", TEST_PREFIX));
        timeMapper.delete(new QueryWrapper<Time>()
                .like("TIME_MARK", TEST_PREFIX));
        identityMapper.delete(new QueryWrapper<Identity>()
                .like("IDENTITY_ID", TEST_PREFIX));
        adminMapper.delete(new QueryWrapper<Admin>()
                .like("ADMIN_NAME", TEST_PREFIX));
    }
}
