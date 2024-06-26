package com.example.memic.statistics.ui;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memic.common.database.NoTransactionExtension;
import com.example.memic.member.dto.MemberSignInRequest;
import com.example.memic.member.dto.MemberSignInResponse;
import com.example.memic.statistics.dto.StatisticResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("NonAsciiCharacters")
@AutoConfigureMockMvc
@SpringBootTest
class StatisticsIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    @ExtendWith(NoTransactionExtension.class)
    void 사용자의_서비스_사용_기록을_반환한다() throws Exception {

        try (final var connection = dataSource.getConnection()) {
            connection.createStatement().execute("SET FOREIGN_KEY_CHECKS=0");
            connection.createStatement().execute("INSERT INTO member (id, email, password) VALUES (1, 'yunho@naver.com', '1234')");
            connection.createStatement().execute("INSERT INTO recognized_sentence (id, member_id, spoken_at) VALUES (1, 1, '2024-5-30T00:00:00')");
            connection.createStatement().execute("INSERT INTO recognized_sentence (id, member_id, spoken_at) VALUES (2, 1, '2024-5-29T00:00:00')");
            connection.createStatement().execute("INSERT INTO recognized_sentence (id, member_id, spoken_at) VALUES (3, 1, '2024-5-28T00:00:00')");
            connection.createStatement().execute("INSERT INTO transcription (id, url, transcribed_at) VALUES (1, 'https://test01.com', '2024-5-27T00:00:00')");
            connection.createStatement().execute("INSERT INTO transcription (id, url, transcribed_at) VALUES (2, 'https://test02.com', '2024-5-26T00:00:00')");
            connection.createStatement().execute("INSERT INTO transcription_member(id, transcription_id, member_id) VALUES (1,1,1)");
            connection.createStatement().execute("INSERT INTO transcription_member(id, transcription_id, member_id) VALUES (2,2,1)");
        }

        final var signInRequest = new MemberSignInRequest("yunho@naver.com", "1234");

        final var mvcSignInResponse = mockMvc.perform(post("/v1/members/sign-in")
                                                     .contentType(MediaType.APPLICATION_JSON)
                                                     .content(objectMapper.writeValueAsString(signInRequest)))
                                             .andDo(print())
                                             .andExpect(status().isOk())
                                             .andReturn();

        final var signInResponse = objectMapper.readValue(
                mvcSignInResponse.getResponse().getContentAsString(),
                MemberSignInResponse.class
        );

        final var mvcStatisticResponse = mockMvc.perform(get("/v1/statistics")
                                                        .param("year", "2024")
                                                        .param("month", "5")
                                                        .header("Authorization", signInResponse.accessToken()))
                                                .andDo(print())
                                                .andExpect(status().isOk())
                                                .andReturn();

        final var statisticResponse = objectMapper.readValue(
                mvcStatisticResponse.getResponse().getContentAsString(),
                StatisticResponse.class
        );

        SoftAssertions.assertSoftly(softly ->{
                    softly.assertThat(statisticResponse.visitedDays().size()).isEqualTo(5);
                    softly.assertThat(statisticResponse.records()).isEqualTo(3);
                    softly.assertThat(statisticResponse.convert()).isEqualTo(2);
        });
    }
}
