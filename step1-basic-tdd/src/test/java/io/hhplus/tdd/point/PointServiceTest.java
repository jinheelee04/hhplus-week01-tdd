package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private PointService pointService;
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
         userPointTable = new UserPointTable();
         pointService = new PointServiceImpl(userPointTable, pointHistoryTable);
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

    @Test
    @DisplayName("유효한 사용자 ID와 충전 금액으로 포인트를 충전하면 충전된 포인트가 반환된다")
    void charge_withValidUserIdAndAmount_returnsChargedPoint() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(chargeAmount, result.point());
    }

    @Test
    @DisplayName("이미 포인트가 있는 유저가 추가 충전하면 기존 포인트에 누적된다")
    void charge_whenExistingUser_accumulatesPoint() {
        // given
        long userId = 2L;
        long initialAmount = 1000L;
        long chargeAmount = 500L;

        // 초기 포인트 설정
        userPointTable.insertOrUpdate(userId, initialAmount);

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(initialAmount + chargeAmount, result.point());
    }

    @Test
    @DisplayName("0 이하의 금액으로 충전하면 예외가 발생한다")
    void charge_withZeroOrNegativeAmount_throwsException() {
        // given
        long userId = 1L;
        long invalidAmount = 0L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });
    }

    @Test
    @DisplayName("음수 금액으로 충전하면 예외가 발생한다")
    void charge_withNegativeAmount_throwsException() {
        // given
        long userId = 1L;
        long invalidAmount = -1L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });
    }

    @Test
    @DisplayName("충전 후 최대 잔액(100,000)을 초과하면 예외가 발생한다")
    void charge_exceedingMaxBalance_throwsException() {
        // given
        long userId = 1L;
        long currentBalance = 95_000L;
        long chargeAmount = 10_000L;

        userPointTable.insertOrUpdate(userId, currentBalance);

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            pointService.charge(userId, chargeAmount);
        });

    }

    @Test
    @DisplayName("최소 충전 금액(100원) 미만으로 충전하면 예외가 발생한다")
    void charge_belowMinimumAmount_throwsException() {
        // given
        long userId = 1L;
        long belowMinimumAmount = 99L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, belowMinimumAmount);
        });
    }

    @Test
    @DisplayName("최소 충전 금액(100원)으로 충전하면 성공한다")
    void charge_withMinimumAmount_success() {
        // given
        long userId = 1L;
        long minimumAmount = 100L;

        // when
        UserPoint result = pointService.charge(userId, minimumAmount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(minimumAmount, result.point());
    }

    @Test
    @DisplayName("1회 최대 충전 금액(50,000원)을 초과하면 예외가 발생한다")
    void charge_exceedingMaxChargeAmount_throwsException() {
        // given
        long userId = 1L;
        long exceedingAmount = 50_001L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.charge(userId, exceedingAmount);
        });
    }

    @Test
    @DisplayName("보유한 포인트 내에서 포인트를 사용하면 차감된 포인트가 반환된다")
    void use_withSufficientBalance_returnsDeductedPoint() {
        // given
        long userId = 1L;
        long initialPoint = 10_000L;
        long useAmount = 3_000L;

        userPointTable.insertOrUpdate(userId, initialPoint);

        // when
        UserPoint result = pointService.use(userId, useAmount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(initialPoint - useAmount, result.point());
    }

    @Test
    @DisplayName("보유 포인트보다 많은 금액을 사용하면 예외가 발생한다")
    void use_withInsufficientBalance_throwsException() {
        // given
        long userId = 1L;
        long currentBalance = 5_000L;
        long useAmount = 10_000L;

        userPointTable.insertOrUpdate(userId, currentBalance);

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            pointService.use(userId, useAmount);
        });
    }

    @Test
    @DisplayName("0원을 사용하면 예외가 발생한다")
    void use_withZeroAmount_throwsException() {
        // given
        long userId = 1L;
        long currentBalance = 10_000L;
        long useAmount = 0L;

        userPointTable.insertOrUpdate(userId, currentBalance);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(userId, useAmount);
        });
    }

    @Test
    @DisplayName("음수 금액을 사용하면 예외가 발생한다")
    void use_withNegativeAmount_throwsException() {
        // given
        long userId = 1L;
        long currentBalance = 10_000L;
        long useAmount = -1_000L;

        userPointTable.insertOrUpdate(userId, currentBalance);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.use(userId, useAmount);
        });
    }

    @Test
    @DisplayName("보유 포인트 전액을 사용하면 잔액이 0이 된다")
    void use_withExactBalance_returnsZeroPoint() {
        // given
        long userId = 1L;
        long currentBalance = 10_000L;
        long useAmount = 10_000L;

        userPointTable.insertOrUpdate(userId, currentBalance);

        // when
        UserPoint result = pointService.use(userId, useAmount);

        // then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(0L, result.point());
    }

    @Test
    @DisplayName("잔액이 0인 상태에서 포인트를 사용하면 예외가 발생한다")
    void use_withZeroBalance_throwsException() {
        // given
        long userId = 1L;
        long useAmount = 1_000L;

        // 잔액 0 (초기 상태)

        // when & then
        assertThrows(IllegalStateException.class, () -> {
            pointService.use(userId, useAmount);
        });
    }

    @Test
    @DisplayName("내역이 없는 유저의 포인트 내역을 조회하면 빈 리스트를 반환한다")
    void getHistory_whenNoHistory_returnsEmptyList() {
        // given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(List.of());

        // when
        List<PointHistory> result = pointService.getHistory(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 충전 시 CHARGE 타입의 내역이 생성된다")
    void charge_createsChargeHistory() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;

        PointHistory expectedHistory = new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
        when(pointHistoryTable.insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(expectedHistory);

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertNotNull(result);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 시 USE 타입의 내역이 생성된다")
    void use_createsUseHistory() {
        // given
        long userId = 1L;
        long initialAmount = 5000L;
        long useAmount = 2000L;

        userPointTable.insertOrUpdate(userId, initialAmount);

        PointHistory expectedHistory = new PointHistory(1L, userId, useAmount, TransactionType.USE, System.currentTimeMillis());
        when(pointHistoryTable.insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong()))
                .thenReturn(expectedHistory);

        // when
        pointService.use(userId, useAmount);

        // then
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("여러 번의 충전과 사용 내역이 모두 조회된다")
    void getHistory_withMultipleTransactions_returnsAllHistories() {
        // given
        long userId = 1L;
        long firstCharge = 1000L;
        long secondCharge = 2000L;
        long firstUse = 500L;

        List<PointHistory> mockHistories = List.of(
                new PointHistory(1L, userId, firstCharge, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, secondCharge, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(3L, userId, firstUse, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(mockHistories);

        // when
        List<PointHistory> histories = pointService.getHistory(userId);

        // then
        assertNotNull(histories);
        assertEquals(3, histories.size());

        // 첫 번째 거래: 충전 1000
        PointHistory firstHistory = histories.get(0);
        assertEquals(userId, firstHistory.userId());
        assertEquals(firstCharge, firstHistory.amount());
        assertEquals(TransactionType.CHARGE, firstHistory.type());

        // 두 번째 거래: 충전 2000
        PointHistory secondHistory = histories.get(1);
        assertEquals(userId, secondHistory.userId());
        assertEquals(secondCharge, secondHistory.amount());
        assertEquals(TransactionType.CHARGE, secondHistory.type());

        // 세 번째 거래: 사용 500
        PointHistory thirdHistory = histories.get(2);
        assertEquals(userId, thirdHistory.userId());
        assertEquals(firstUse, thirdHistory.amount());
        assertEquals(TransactionType.USE, thirdHistory.type());

        verify(pointHistoryTable, times(1)).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("특정 유저의 내역 조회 시 다른 유저의 내역은 조회되지 않는다")
    void getHistory_onlyReturnsOwnHistory() {
        // given
        long user1 = 1L;
        long user2 = 2L;

        List<PointHistory> user1Histories = List.of(
                new PointHistory(1L, user1, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, user1, 500L, TransactionType.CHARGE, System.currentTimeMillis())
        );

        List<PointHistory> user2Histories = List.of(
                new PointHistory(3L, user2, 2000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(4L, user2, 500L, TransactionType.USE, System.currentTimeMillis())
        );

        when(pointHistoryTable.selectAllByUserId(user1))
                .thenReturn(user1Histories);
        when(pointHistoryTable.selectAllByUserId(user2))
                .thenReturn(user2Histories);

        // when
        List<PointHistory> result1 = pointService.getHistory(user1);
        List<PointHistory> result2 = pointService.getHistory(user2);

        // then
        // user1의 내역은 2개만 조회
        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertTrue(result1.stream().allMatch(h -> h.userId() == user1));

        // user2의 내역은 2개만 조회
        assertNotNull(result2);
        assertEquals(2, result2.size());
        assertTrue(result2.stream().allMatch(h -> h.userId() == user2));

        verify(pointHistoryTable, times(1)).selectAllByUserId(user1);
        verify(pointHistoryTable, times(1)).selectAllByUserId(user2);
    }

    @Test
    @DisplayName("0 이하의 사용자 ID로 내역 조회 시 예외가 발생한다")
    void getHistory_withInvalidUserId_throwsException() {
        // given
        long invalidUserId = 0L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.getHistory(invalidUserId);
        });
    }

    @Test
    @DisplayName("음수 사용자 ID로 내역 조회 시 예외가 발생한다")
    void getHistory_withNegativeUserId_throwsException() {
        // given
        long invalidUserId = -1L;

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            pointService.getHistory(invalidUserId);
        });
    }
}
