package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MAX_BALANCE = 100_000L;
    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint addPoints(long amount) {
        long newAmount = this.point + amount;
        if(newAmount > MAX_BALANCE){
            throw new IllegalStateException("최대 잔액은 " + MAX_BALANCE + "원을 초과할 수 없습니다.");
        }
        return new UserPoint(this.id, newAmount, System.currentTimeMillis());
    }

}
