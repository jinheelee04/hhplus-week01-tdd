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
}
