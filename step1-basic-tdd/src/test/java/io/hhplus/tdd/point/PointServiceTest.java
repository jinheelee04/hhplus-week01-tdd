package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointServiceTest {

    private PointService pointService;

    @Test
    @DisplayName("포인트가 없는 유저를 조회하면 0 포인트를 반환한다")
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

}
