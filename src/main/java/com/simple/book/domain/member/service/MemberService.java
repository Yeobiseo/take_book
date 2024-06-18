package com.simple.book.domain.member.service;


import com.simple.book.domain.alarm.service.AlarmService;
import com.simple.book.domain.member.dto.request.MemberMappingToProjectRequestDto;
import com.simple.book.domain.member.dto.request.MemberMappingToTaskRequestDto;
import com.simple.book.domain.member.entity.Member;
import com.simple.book.domain.member.entity.MemberTask;
import com.simple.book.domain.member.entity.MemberTaskId;
import com.simple.book.domain.member.repository.MemberRepository;
import com.simple.book.domain.member.repository.TaskMemberRepository;
import com.simple.book.domain.member.repository.UserTaskRepository;
import com.simple.book.domain.project.entity.Project;
import com.simple.book.domain.project.repository.ProjectRepository;
import com.simple.book.domain.task.entity.Task;
import com.simple.book.domain.task.repository.TaskRepository;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.entity.UserTask;
import com.simple.book.domain.user.entity.UserTaskId;
import com.simple.book.domain.user.repository.UserRepository;
import com.simple.book.global.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final TaskMemberRepository taskMemberRepository;
    private final UserTaskRepository userTaskRepository;
    private final AlarmService alarmService;

    @Transactional(rollbackFor = {Exception.class})
    public Member memberAddToProject(MemberMappingToProjectRequestDto memberMappingToProjectRequestDto){
        Member member;
        try {
            Project project = projectRepository.findById(memberMappingToProjectRequestDto.getProjectId())
                    .orElseThrow(() -> new EntityNotFoundException("Project not found with ID: " + memberMappingToProjectRequestDto.getProjectId()));
            String UserId = memberMappingToProjectRequestDto.getUserId();
            User user = userRepository.findByAuthenticationUserId(UserId);
            member = Member.builder()
                    .project(project)
                    .isManager(memberMappingToProjectRequestDto.getIsManager())
                    .user(user)
                    .build();
        	memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new MemberDuplicateInProjectException(e.getMessage());
         }catch (NullPointerException e) {
            throw new UserNotFoundException(e.getMessage());
        }

        return member;
    }
    @Transactional(rollbackFor = {Exception.class})
    public Member memberAddToTask(MemberMappingToTaskRequestDto memberMappingToTaskRequestDto){

        Optional<Member> optionalMember = memberRepository.findById(memberMappingToTaskRequestDto.getMemberId());
        Member member = optionalMember.orElseThrow(() -> new EntityNotFoundException("Member not found with Member ID: " + memberMappingToTaskRequestDto.getMemberId()));
        Optional<Task> optionalTask = taskRepository.findById(memberMappingToTaskRequestDto.getTaskId());
        Task task = optionalTask.orElseThrow(() -> new EntityNotFoundException("Task not found with Task ID: " + memberMappingToTaskRequestDto.getTaskId()));
        if(task.getProject().equals(member.getProject())){
            User user = userRepository.getReferenceById(member.getUser().getId());
            UserTaskId userTaskId = UserTaskId.builder()
                    .taskId(task.getId())
                    .userId(user.getId())
                    .build();
            UserTask userTask = UserTask.builder()
                    .id(userTaskId)
                    .user(user)
                    .task(task)
                    .build();
            MemberTaskId memberTaskId = MemberTaskId.builder()
                    .mappingTaskId(task.getId())
                    .mappingMemberId(member.getId())
                    .build();
            MemberTask memberTask = MemberTask.builder()
                    .id(memberTaskId)
                    .member(member)
                    .task(task)
                    .build();
	        try {
	            taskMemberRepository.saveAndFlush(memberTask);
                userTaskRepository.saveAndFlush(userTask);
	        } catch (DataIntegrityViolationException e) {
	        	throw new MemberDuplicateInTaskException(e.getMessage());
			}
	        alarmService.sendTaskManager(memberMappingToTaskRequestDto.getMemberId());
        
        }else{
            throw new InvalidValueException("해당 작업과 멤버는 같은 프로젝트 소속이어야 합니다.");
        }

        return member;
    }
}
