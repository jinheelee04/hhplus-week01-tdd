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
            throw new IllegalArgumentException();
        }
        UserPoint current = getPoint(userId);
        UserPoint updated = current.addPoints(chargeAmount);
        return userPointTable.insertOrUpdate(updated.id(), updated.point());
    }
}
