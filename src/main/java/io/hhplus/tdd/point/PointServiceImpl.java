package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint getPoint(Long userId) {
        if(userId <= 0){
            throw new IllegalArgumentException("사용자 ID는 1 이상이어야 합니다.");
        }
        return userPointTable.selectById(userId);
    }

    @Override
    public UserPoint charge(long userId, long chargeAmount) {
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다");
        }

        UserPoint current = getPoint(userId);
        UserPoint updated = current.addPoints(chargeAmount);

        validateDailyChargeLimit(userId, chargeAmount);

        UserPoint result = userPointTable.insertOrUpdate(updated.id(), updated.point());
        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, result.updateMillis());

        return result;
    }

    private void validateDailyChargeLimit(long userId, long chargeAmount) {
        LocalDate today = LocalDate.now();

        // 오늘 충전한 총액 계산
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        long todayChargeTotal = histories.stream()
                .filter(history -> history.type() == TransactionType.CHARGE)
                .filter(history -> {
                    LocalDate historyDate = Instant.ofEpochMilli(history.updateMillis())
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return historyDate.equals(today);
                })
                .mapToLong(PointHistory::amount)
                .sum();

        long totalAfterCharge = todayChargeTotal + chargeAmount;
        if (totalAfterCharge > UserPoint.DAILY_CHARGE_LIMIT) {
            throw new IllegalStateException(
                    "일일 충전 한도(" + UserPoint.DAILY_CHARGE_LIMIT + "원)를 초과할 수 없습니다. " +
                    "(오늘 충전 금액: " + todayChargeTotal + "원, 시도 금액: " + chargeAmount + "원)"
            );
        }
    }

    @Override
    public UserPoint use(long userId, long useAmount) {
        UserPoint current = getPoint(userId);
        UserPoint updated = current.deductPoints(useAmount);
        UserPoint result = userPointTable.insertOrUpdate(updated.id(), updated.point());

        pointHistoryTable.insert(userId, useAmount, TransactionType.USE, result.updateMillis());

        return result;
    }

    @Override
    public List<PointHistory> getHistory(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("사용자 ID는 1 이상이어야 합니다.");
        }
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
