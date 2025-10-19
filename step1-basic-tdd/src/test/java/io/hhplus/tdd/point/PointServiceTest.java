package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
         userPointTable = new UserPointTable();
         pointService = new PointServiceImpl(userPointTable);
    }
    @Test
    @DisplayName("존재하지 않는 유저의 포인트를 조회하면 0 포인트를 반환한다")
    void getPoint_whenUserNotExists_returnsZeroPoint() {
        // given
        long userId = 1L;

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(0L, result.point());
    }

    @Test
    @DisplayName("존재하는 유저의 포인트를 조회하면 해당 유저의 포인트를 반환한다")
    void getPoint_whenUserExists_returnsUserPoint(){
        // given
        long userId = 1L;
        long expectedPoint = 1000L;
        userPointTable.insertOrUpdate(userId, expectedPoint);

        // when
        UserPoint result = pointService.getPoint(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(expectedPoint, result.point());
    }

    @Test
    @DisplayName("음수 ID로 포인트를 조회하면 예외가 발생한다")
    void getPoint_withNegativeUserId_throwsException() {
        // given
        long invalidUserId = -1L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.getPoint(invalidUserId);
        });
    }

    @Test
    @DisplayName("0 ID로 포인트를 조회하면 예외가 발생한다")
    void getPoint_withZeroUserId_throwsException() {
        // given
        long invalidUserId = 0L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.getPoint(invalidUserId);
        });
    }
}
