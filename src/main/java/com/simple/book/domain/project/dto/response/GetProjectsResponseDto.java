package com.simple.book.domain.project.dto.response;

import com.simple.book.domain.member.entity.Member;
import com.simple.book.domain.task.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Getter
@Schema(description = "해당 유저가 속한 프로젝트를 가져오는 RESPONSE DTO")
public class GetProjectsResponseDto {
    private Long projectId;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    @Schema(description = "user entity의 id를 반환")
    private List<Long> usersId = new ArrayList<>();
    public GetProjectsResponseDto(Long projectId, String title, String description, Date startDate, Date endDate, List<Long> usersId) {
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usersId = usersId;
    }
}
