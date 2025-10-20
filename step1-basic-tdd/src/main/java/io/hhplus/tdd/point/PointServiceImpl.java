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
        UserPoint current = getPoint(userId);
        long updatedAmount = current.point() + chargeAmount;
        return userPointTable.insertOrUpdate(userId, updatedAmount);
    }
}
