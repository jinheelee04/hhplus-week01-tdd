package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        // 테스트마다 독립적인 userId 사용
    }

    @Test
    @DisplayName("동일 사용자에 대한 동시 충전 요청이 모두 정확하게 반영되어야 한다")
    void charge_concurrentRequestsForSameUser_shouldProcessAllChargesCorrectly() throws InterruptedException {
        // given
        long userId = System.currentTimeMillis(); // 각 테스트마다 고유한 userId
        int threadCount = 10;
        long chargeAmount = 1_000L;
        long expectedFinalBalance = threadCount * chargeAmount; // 10,000원

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("Charge failed: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertTrue(finished, "모든 스레드가 제한 시간 내에 완료되어야 합니다");

        UserPoint finalPoint = pointService.getPoint(userId);

        log.info("=== 동시성 테스트 결과 ===");
        log.info("성공한 충전 횟수: {}", successCount.get());
        log.info("실패한 충전 횟수: {}", failCount.get());
        log.info("기대 잔액: {}", expectedFinalBalance);
        log.info("실제 잔액: {}", finalPoint.point());

        // 모든 충전이 성공해야 함
        assertEquals(threadCount, successCount.get(), "모든 충전이 성공해야 합니다");
        assertEquals(0, failCount.get(), "실패한 충전이 없어야 합니다");

        // 최종 잔액이 정확해야 함
        assertEquals(expectedFinalBalance, finalPoint.point(),
                "동시 충전 후 최종 잔액이 정확해야 합니다");
    }

    @Test
    @DisplayName("다른 사용자에 대한 동시 충전 요청은 서로 독립적으로 처리되어야 한다")
    void charge_concurrentRequestsForDifferentUsers_shouldBeIndependent() throws InterruptedException {
        // given
        long baseUserId = System.currentTimeMillis();
        int userCount = 5;
        int chargesPerUser = 3;
        long chargeAmount = 1_000L;
        long expectedBalancePerUser = chargesPerUser * chargeAmount;

        int totalThreadCount = userCount * chargesPerUser;
        ExecutorService executorService = Executors.newFixedThreadPool(totalThreadCount);
        CountDownLatch latch = new CountDownLatch(totalThreadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < userCount; i++) {
            final long userId = baseUserId + i;
            for (int j = 0; j < chargesPerUser; j++) {
                executorService.submit(() -> {
                    try {
                        pointService.charge(userId, chargeAmount);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Charge failed for user {}: {}", userId, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertTrue(finished, "모든 스레드가 제한 시간 내에 완료되어야 합니다");
        assertEquals(totalThreadCount, successCount.get(), "모든 충전이 성공해야 합니다");

        // 각 사용자의 잔액이 정확해야 함
        for (int i = 0; i < userCount; i++) {
            long userId = baseUserId + i;
            UserPoint userPoint = pointService.getPoint(userId);
            assertEquals(expectedBalancePerUser, userPoint.point(),
                    "사용자 " + userId + "의 잔액이 정확해야 합니다");
        }
    }

    @Test
    @DisplayName("동일 사용자에 대한 동시 충전과 사용 요청이 정확하게 처리되어야 한다")
    void chargeAndUse_concurrentRequests_shouldProcessCorrectly() throws InterruptedException {
        // given
        long userId = System.currentTimeMillis();
        long initialCharge = 50_000L;

        // 초기 포인트 충전
        pointService.charge(userId, initialCharge);

        int threadCount = 10;
        long chargeAmount = 1_000L;
        long useAmount = 500L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when: 5번 충전, 5번 사용
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    if (finalI % 2 == 0) {
                        pointService.charge(userId, chargeAmount);
                    } else {
                        pointService.use(userId, useAmount);
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Transaction failed: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertTrue(finished, "모든 스레드가 제한 시간 내에 완료되어야 합니다");

        UserPoint finalPoint = pointService.getPoint(userId);

        // 기대값: 초기 50,000 + (5 * 1,000) - (5 * 500) = 52,500
        long expectedBalance = initialCharge + (5 * chargeAmount) - (5 * useAmount);

        log.info("=== 충전/사용 동시성 테스트 결과 ===");
        log.info("성공한 트랜잭션: {}", successCount.get());
        log.info("기대 잔액: {}", expectedBalance);
        log.info("실제 잔액: {}", finalPoint.point());

        assertEquals(threadCount, successCount.get(), "모든 트랜잭션이 성공해야 합니다");
        assertEquals(expectedBalance, finalPoint.point(),
                "최종 잔액이 정확해야 합니다");
    }

    @Test
    @DisplayName("동시 충전 시 일일 한도 검증이 정확하게 동작해야 한다")
    void charge_concurrentRequestsNearDailyLimit_shouldEnforceLimitCorrectly() throws InterruptedException {
        // given
        long userId = System.currentTimeMillis();
        int threadCount = 10;
        long chargeAmount = 15_000L; // 15,000 * 10 = 150,000 (한도 초과)

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // 일일 한도 초과 또는 최대 잔액 초과 예외
                    if (e.getMessage().contains("일일 충전 한도") ||
                        e.getMessage().contains("최대 잔액")) {
                        failCount.incrementAndGet();
                    } else {
                        log.error("Unexpected IllegalStateException: {}", e.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Unexpected error: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertTrue(finished, "모든 스레드가 제한 시간 내에 완료되어야 합니다");

        UserPoint finalPoint = pointService.getPoint(userId);

        log.info("=== 일일 한도 동시성 테스트 결과 ===");
        log.info("성공한 충전: {}", successCount.get());
        log.info("실패한 충전: {}", failCount.get());
        log.info("최종 잔액: {}", finalPoint.point());

        // 일일 한도(100,000원) 내에서만 충전되어야 함
        assertTrue(finalPoint.point() <= UserPoint.DAILY_CHARGE_LIMIT,
                "최종 잔액이 일일 한도를 초과하지 않아야 합니다");

        // 성공 + 실패 = 전체 시도
        assertEquals(threadCount, successCount.get() + failCount.get(),
                "모든 요청이 성공 또는 실패로 처리되어야 합니다");

        // 최소한 일부는 성공하고 일부는 실패해야 함
        assertTrue(successCount.get() > 0, "최소한 일부 충전은 성공해야 합니다");
        assertTrue(failCount.get() > 0, "한도를 초과하는 충전은 실패해야 합니다");
    }
}
