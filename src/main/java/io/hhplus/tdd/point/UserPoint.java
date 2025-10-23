package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    public static final long MAX_BALANCE = 100_000L;
    public static final long MIN_CHARGE_AMOUNT = 100L;
    public static final long MAX_CHARGE_AMOUNT = 50_000L;
    public static final long MIN_USE_AMOUNT = 100L;
    public static final long MAX_USE_AMOUNT = 50_000L;
    public static final long DAILY_CHARGE_LIMIT = 100_000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint addPoints(long amount) {
        if (amount < MIN_CHARGE_AMOUNT) {
            throw new IllegalArgumentException(
                    "충전 금액은 최소 " + MIN_CHARGE_AMOUNT + "원 이상이어야 합니다."
            );
        }

        if (amount > MAX_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("1회 최대 충전 금액은 " + MAX_CHARGE_AMOUNT + "원입니다");
        }

        long newAmount = this.point + amount;
        if(newAmount > MAX_BALANCE){
            throw new IllegalStateException("최대 잔액은 " + MAX_BALANCE + "원을 초과할 수 없습니다.");
        }
        return new UserPoint(this.id, newAmount, System.currentTimeMillis());
    }

    public UserPoint deductPoints(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 1원 이상이어야 합니다.");
        }

        if (amount < MIN_USE_AMOUNT) {
            throw new IllegalArgumentException(
                    "사용 금액은 최소 " + MIN_USE_AMOUNT + "원 이상이어야 합니다."
            );
        }

        if (amount > MAX_USE_AMOUNT) {
            throw new IllegalArgumentException("1회 최대 사용 금액은 " + MAX_USE_AMOUNT + "원입니다");
        }

        if (this.point < amount) {
            throw new IllegalStateException("포인트 잔액이 부족합니다. (현재 잔액: " + this.point + "원, 사용 시도: " + amount + "원)");
        }

        long newAmount = this.point - amount;
        return new UserPoint(this.id, newAmount, System.currentTimeMillis());
    }

}
