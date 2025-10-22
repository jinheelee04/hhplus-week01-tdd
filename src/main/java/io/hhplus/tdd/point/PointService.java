package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {
    UserPoint getPoint(Long userId);

    UserPoint charge(long userId, long chargeAmount);

    UserPoint use(long userId, long useAmount);

    List<PointHistory> getHistory(long userId);
}
