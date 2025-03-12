package org.gujo.poppul.answer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gujo.poppul.answer.entity.Answer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDto {
    private Long id;
    private int no;
    private String content;
    @JsonProperty("is_answer")
    private boolean isAnswer;

    // Entity -> DTO 변환
    public static AnswerDto fromEntity(Answer answer) {
        return AnswerDto.builder()
                .id(answer.getId())
                .no(answer.getNo())
                .content(answer.getContent())
                .isAnswer(answer.is_answer())  // 더 자연스러운 메서드명
                .build();
    }

    // DTO -> Entity 변환
    public Answer toEntity() {
        return Answer.builder()
                .no(this.no)
                .content(this.content)
                .is_answer(this.isAnswer)  // 필드명도 일관성 있게
                .build();
    }
}