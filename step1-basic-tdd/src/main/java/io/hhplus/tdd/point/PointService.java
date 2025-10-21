package io.hhplus.tdd.point;

public interface PointService {
    UserPoint getPoint(Long userId);

    UserPoint charge(long userId, long chargeAmount);

    UserPoint use(long userId, long useAmount);
}
