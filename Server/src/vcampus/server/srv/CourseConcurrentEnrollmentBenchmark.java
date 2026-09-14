/*
 * CourseConcurrentEnrollmentBenchmark
 *
 * Version 1.0
 *
 * 2026-09-13
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Repeatable contention benchmark for the transactional enrollment path. */
public final class CourseConcurrentEnrollmentBenchmark {

    private static final int CLIENTS = 100;
    private static final int CAPACITY = 10;
    private static final String COURSE_ID = "T_BENCH_0913";
    private static final String CLASS_ID = COURSE_ID + "-01";

    private CourseConcurrentEnrollmentBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        CourseServerSrv service = new CourseServerSrv();
        prepareFixture();
        try {
            CountDownLatch ready = new CountDownLatch(CLIENTS);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            List<Throwable> unexpected = new ArrayList<>();
            ExecutorService pool = Executors.newFixedThreadPool(CLIENTS);
            Instant began = Instant.now();
            for (int index = 1; index <= CLIENTS; index++) {
                String studentId = studentId(index);
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (service.selectCourse(studentId, CLASS_ID)) {
                            success.incrementAndGet();
                        }
                    } catch (CourseServiceException expected) {
                        rejected.incrementAndGet();
                    } catch (Throwable error) {
                        synchronized (unexpected) {
                            unexpected.add(error);
                        }
                    }
                });
            }
            require(ready.await(15, TimeUnit.SECONDS), "all clients ready");
            start.countDown();
            pool.shutdown();
            require(pool.awaitTermination(60, TimeUnit.SECONDS), "all clients finished");

            long elapsed = Duration.between(began, Instant.now()).toMillis();
            int records = scalar("SELECT COUNT(*) FROM tblSelectCourse WHERE teachingClassId=?",
                    CLASS_ID);
            int selectedCount = scalar(
                    "SELECT selectedCount FROM tblTeachingClass WHERE teachingClassId=?", CLASS_ID);
            int oversold = Math.max(0, records - CAPACITY);
            int remaining = CAPACITY - selectedCount;

            System.out.println("clients=" + CLIENTS);
            System.out.println("capacity_remaining=" + remaining);
            System.out.println("success=" + success.get());
            System.out.println("rejected=" + rejected.get());
            System.out.println("elapsed_ms=" + elapsed);
            System.out.println("oversold=" + oversold);
            require(unexpected.isEmpty(), "no unexpected client failures: " + unexpected);
            require(success.get() == CAPACITY, "exactly capacity enrollments succeed");
            require(rejected.get() == CLIENTS - CAPACITY,
                    "all excess enrollment attempts are rejected");
            require(records == CAPACITY, "selection rows equal capacity");
            require(selectedCount == CAPACITY, "selectedCount equals selection rows");
            require(oversold == 0, "no oversell");
            System.out.println("COURSE_CONCURRENT_ENROLLMENT_BENCHMARK=PASS");
        } finally {
            cleanupFixture();
            int residue = fixtureResidue();
            System.out.println("COURSE_CONCURRENT_ENROLLMENT_BENCHMARK_RESIDUE=" + residue);
            require(residue == 0, "benchmark fixture cleanup");
        }
    }

    private static void prepareFixture() throws Exception {
        require(fixtureResidue() == 0, "reserved benchmark IDs are unused");
        CourseTestData.prepareCourse(new Course(
                COURSE_ID, "Course concurrent benchmark", "Benchmark teacher", 1, CAPACITY, 0));
        for (int index = 1; index <= CLIENTS; index++) {
            CourseTestData.prepareStudent(userId(index), studentId(index));
        }
    }

    private static void cleanupFixture() throws Exception {
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selections = conn.prepareStatement(
                    "DELETE FROM tblSelectCourse WHERE teachingClassId=?")) {
                selections.setString(1, CLASS_ID);
                selections.executeUpdate();
                conn.commit();
            } catch (Exception error) {
                conn.rollback();
                throw error;
            }
        }
        if (new CourseDAO().findById(COURSE_ID) != null) {
            CourseTestData.cleanupCourse(COURSE_ID);
        }
        for (int index = 1; index <= CLIENTS; index++) {
            CourseTestData.cleanupStudent(userId(index), studentId(index));
        }
    }

    private static int fixtureResidue() throws Exception {
        int count = scalar("SELECT COUNT(*) FROM tblCourse WHERE courseId=?", COURSE_ID);
        count += scalar("SELECT COUNT(*) FROM tblTeachingClass WHERE teachingClassId=?", CLASS_ID);
        count += scalar("SELECT COUNT(*) FROM tblSelectCourse WHERE teachingClassId=?", CLASS_ID);
        for (int index = 1; index <= CLIENTS; index++) {
            count += CourseTestData.countResidue(userId(index), studentId(index));
        }
        return count;
    }

    private static int scalar(String sql, String value) throws Exception {
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                require(result.next(), "scalar query returned a row");
                return result.getInt(1);
            }
        }
    }

    private static String userId(int index) {
        return "CB" + String.format("%06d", index);
    }

    private static String studentId(int index) {
        return "CB" + String.format("%08d", index);
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
    }
}
