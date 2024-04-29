package com.example.lonua.review.model.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.MappingException;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchUpdateReviewReq {

    @NotNull(message = "리뷰 IDX는 필수 입력 항목입니다.")
    @Min(value = 1, message = "리뷰 IDX는 최소 1 이상의 숫자여야 합니다.")
    @ApiModelProperty(value = "리뷰 IDX( 1이상의 숫자 )", example = "1", required = true)
    private Integer reviewIdx;

    @Length(min=1, max=200, message = "리뷰 내용은 최소 1글자 이상, 최대 200자 이하여야 합니다.")
    @ApiModelProperty(value = "리뷰내용(200자 이하)", example = "색깔도 예쁘고 너무 맘에 들어요", required = true)
    private String reviewContent;

    @Min(value = 1, message = "평점은 최소 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 최대 5 이하여야 합니다.")
    @ApiModelProperty(value = "평점", example = "1", required = true)
    private Integer evaluation;

}
