package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("포인트 API 통합 테스트")
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("[GET /point/{id}] 존재하지 않는 사용자 포인트 조회 시 0 포인트를 반환한다")
    void getPoint_whenUserNotExists_returnsZeroPoint() throws Exception {
        // given
        long userId = System.currentTimeMillis();

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(0));
    }

    @Test
    @DisplayName("[GET /point/{id}] 충전 후 포인트를 조회하면 충전된 포인트가 반환된다")
    void getPoint_afterCharge_returnsChargedPoint() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long chargeAmount = 5_000L;

        // 먼저 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("[GET /point/{id}] 여러 번 충전 후 포인트를 조회하면 누적된 포인트가 반환된다")
    void getPoint_afterMultipleCharges_returnsAccumulatedPoint() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long firstCharge = 3_000L;
        long secondCharge = 2_000L;
        long expectedTotal = firstCharge + secondCharge;

        // 두 번 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedTotal));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 포인트 충전이 성공하면 충전된 포인트를 반환한다")
    void charge_withValidAmount_returnsChargedPoint() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long chargeAmount = 10_000L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(chargeAmount));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 여러 번 충전하면 포인트가 누적된다")
    void charge_multiple_accumulatesPoints() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long firstCharge = 5_000L;
        long secondCharge = 3_000L;
        long expectedTotal = firstCharge + secondCharge;

        // when
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.point").value(firstCharge));

        // then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(expectedTotal));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 최소 충전 금액(100원) 미만으로 충전 시 실패한다")
    void charge_belowMinimum_fails() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long belowMinimumAmount = 99L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(belowMinimumAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 최소 충전 금액(100원)으로 충전이 성공한다")
    void charge_withMinimumAmount_succeeds() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long minimumAmount = 100L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(minimumAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(minimumAmount));
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 1회 최대 충전 금액(50,000원)을 초과하면 실패한다")
    void charge_exceedingMaxAmount_fails() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long exceedingAmount = 50_001L;

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(exceedingAmount)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 최대 잔액(100,000원)을 초과하면 실패한다")
    void charge_exceedingMaxBalance_fails() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long firstCharge = 50_000L;  // 1회 최대 충전 금액
        long secondCharge = 45_000L; // 50,000 + 45,000 = 95,000
        long thirdCharge = 10_000L;  // 95,000 + 10,000 = 105,000 (최대 잔액 초과)

        // 50,000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk());

        // 45,000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk());

        // when & then - 10,000원 추가 충전 시 최대 잔액 초과로 실패
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(thirdCharge)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("[PATCH /point/{id}/charge] 일일 충전 한도(100,000원)를 초과하면 실패한다")
    void charge_exceedingDailyLimit_fails() throws Exception {
        // given
        long userId = System.currentTimeMillis();
        long firstCharge = 50_000L;   // 1회 최대 금액
        long secondCharge = 40_000L;  // 50,000 + 40,000 = 90,000
        long thirdCharge = 15_000L;   // 90,000 + 15,000 = 105,000 (일일 한도 100,000 초과)

        // 50,000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(firstCharge)))
                .andExpect(status().isOk());

        // 40,000원 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(secondCharge)))
                .andExpect(status().isOk());

        // when & then - 15,000원 추가 충전 시 일일 한도 초과로 실패
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(thirdCharge)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
