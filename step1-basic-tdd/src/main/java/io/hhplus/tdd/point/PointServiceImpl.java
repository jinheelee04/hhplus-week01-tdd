package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final UserPointTable userPointTable;

    @Override
    public UserPoint getPoint(Long userId) {
        if(userId <= 0){
            throw new IllegalArgumentException();
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
        return userPointTable.insertOrUpdate(updated.id(), updated.point());
    }

    @Override
    public UserPoint use(long userId, long useAmount) {
        UserPoint current = getPoint(userId);
        UserPoint updated = current.deductPoints(useAmount);
        return userPointTable.insertOrUpdate(updated.id(), updated.point());
    }
}
