package com.simple.book.domain.user.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simple.book.domain.alarm.service.AlarmUrlService;
import com.simple.book.domain.jwt.dto.AuthTokenDto;
import com.simple.book.domain.jwt.dto.CustomUserDetails;
import com.simple.book.domain.oauth2.CustomOAuth2User;
import com.simple.book.domain.user.dto.request.ModifyPwdRequestDto;
import com.simple.book.domain.user.dto.request.ModifyUserInfoRequestDto;
import com.simple.book.domain.user.dto.request.SignupRequestDto;
import com.simple.book.domain.user.entity.Authentication;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.AuthenticationRepository;
import com.simple.book.domain.user.repository.UserRepository;
import com.simple.book.domain.user.util.InfoSet;
import com.simple.book.domain.user.util.Role;
import com.simple.book.global.advice.ErrorCode;
import com.simple.book.global.advice.ResponseMessage;
import com.simple.book.global.exception.AuthenticationFailureException;
import com.simple.book.global.exception.UnknownException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
	private final UserRepository userRepository;
	private final AuthenticationRepository authenticationRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final AlarmUrlService alarmUrlService;

	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage remove(String userId) {
		try {
			Authentication authentication = authenticationRepository.findByUserId(userId);
			authenticationRepository.delete(authentication);
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().message("success").build();
	}

	// 회원가입
	@Transactional(rollbackFor = { Exception.class })
	public User signup(SignupRequestDto signupRequestDto) {
		boolean isSuccess;
		long id;
//		log.info("signup password : " + signupRequestDto.getPassword());
		Authentication authentication = Authentication.builder().userId(signupRequestDto.getUserId())
				.email(signupRequestDto.getEmail())
				.password(bCryptPasswordEncoder.encode(signupRequestDto.getPassword())).infoSet(InfoSet.DEFAULT)
				.build();
		try {
			authenticationRepository.saveAndFlush(authentication);
			isSuccess = true;
		} catch (DataIntegrityViolationException e) {
			throw new DataIntegrityViolationException("중복된 아이디입니다.", e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		User user = User.builder().username(signupRequestDto.getUsername()).role(Role.USER)
				.nickname(signupRequestDto.getNickname()).build();
		user.setAuthentication(authentication);
		authentication.setUser(user);
		try {
			id = userRepository.saveAndFlush(user).getId();
			isSuccess = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (isSuccess) {
			alarmUrlService.createAlarmUrl(id);
		}

		return user;
	}

	// 로그인
	@Transactional(rollbackFor = { Exception.class })
	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		// DB에서 조회
		Authentication authentication = authenticationRepository.findByUserId(userId);
		if (authentication != null) {
			AuthTokenDto authTokenDto = AuthTokenDto.builder().infoSet(authentication.getInfoSet().toString())
					.name(authentication.getUser().getUsername()).username(authentication.getUserId())
					.password(authentication.getPassword()).role(authentication.getUser().getRole().toString()).build();

			// UserDetails에 담아서 return하면 AutneticationManager가 검증 함
			return new CustomUserDetails(authTokenDto);
		}
		throw new AuthenticationFailureException("아이디가 잘못되었습니다.", ErrorCode.USER_FAILED_AUTHENTICATION);
	}

	/**
	 * 사용자 정보 가져오기
	 * 
	 * @return
	 */
	public ResponseMessage getUserInfo() {
		Map<String, Object> map = new HashMap<>();
		try {
			String id = getCurrentUserId();
			User info = userRepository.findByAuthenticationUserId(id);
			map.put("userId", id);
			map.put("username", info.getUsername());
			map.put("nickname", info.getNickname());
			map.put("position", info.getPosition());
			map.put("introduction", info.getIntroduction());
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().value(map).build();
	}
	
	/**
	 * 정보 변경
	 * @param body
	 * @param userId
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage modifyUserInfo(ModifyUserInfoRequestDto body, String userId) {
		User user = userRepository.findByAuthenticationUserId(userId);
		user = typeToSet(body, user);
		try {
			userRepository.saveAndFlush(user);
		} catch (Exception e) {
			throw new UnknownException(e.getMessage());
		}
		return ResponseMessage.builder().message("수정 되었습니다.").build();
	}

	private User typeToSet(ModifyUserInfoRequestDto body, User user) {
		String type = body.getType();
		String value = body.getValue();
		switch (type) {
		case "N":
			user.setNickname(value);
			break;
		case "P":
			user.setPosition(value);
			break;
		case "I":
			user.setIntroduction(value);
			break;
		default:
			throw new UnknownException(null);
		}
		return user;
	}


	/**
	 * 비밀번호 변경
	 * 
	 * @param body
	 * @param userDetails
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class })
	public ResponseMessage modifyPwd(ModifyPwdRequestDto body, UserDetails userDetails) {
		ResponseMessage result = null;
		if (userDetails != null) {
			String encodedPassword = userDetails.getPassword();
			boolean isCurrenPwdMatch = bCryptPasswordEncoder.matches(body.getCurrentPwd(), encodedPassword);
			if (isCurrenPwdMatch) {
				if (body.getNewPwd().equals(body.getCheckNewPwd())) {
					Authentication auth = authenticationRepository.findByUserId(userDetails.getUsername());
					auth.setPassword(bCryptPasswordEncoder.encode(body.getNewPwd()));
					authenticationRepository.saveAndFlush(auth);
					result = ResponseMessage.builder().message("success").build();
				} else {
					result = ResponseMessage.builder().result(false).message("비밀번호가 일치 하지 않습니다.").build();
				}
			} else {
				result = ResponseMessage.builder().result(false).message("비밀번호를 확인 해 주세요.").build();
			}
		} else {
			result = ResponseMessage.builder().result(false).message("로그인이 만료 되었습니다.").build();
		}
		return result;
	}

	public String getCurrentUserId() {
		org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			if (authentication instanceof OAuth2AuthenticationToken) {
				CustomOAuth2User oauthToken = (CustomOAuth2User) authentication.getPrincipal();
				return oauthToken.getUsername(); // OAuth2로 인증된 경우 사용자 ID 추출
			} else if (authentication instanceof UsernamePasswordAuthenticationToken) {
				CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
				return customUserDetails.getUsername();
			}
		}
		return null; // 사용자가 인증되지 않았거나 인증 정보가 없는 경우
	}

}
