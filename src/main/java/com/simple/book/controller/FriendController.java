package com.simple.book.controller;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.simple.book.service.fried.AddFriendService;
import com.simple.book.service.fried.FriendAcceptService;
import com.simple.book.service.fried.FriendListService;
import com.simple.book.service.fried.FriendRejectService;
import com.simple.book.service.fried.FriendCancelService;
import com.simple.book.service.fried.RequestFriendService;
import com.simple.book.service.fried.ResponseFriendService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/friend")
public class FriendController {

	@Autowired
	private AddFriendService addFriendService;

	@Autowired
	private FriendAcceptService friendAcceptService;

	@Autowired
	private FriendListService friendListService;

	@Autowired
	private RequestFriendService requestFriendService;
	
	@Autowired
	private ResponseFriendService responseFriendService;

	@Autowired
	private FriendCancelService friendCancelService;
	
	@Autowired
	private FriendRejectService friendRejectService;

	@GetMapping("/list")
	@ResponseBody
	public HashMap<String, Object> getList(HttpSession session) {
		HashMap<String, Object> result = friendListService.getList(session);
		return result;
	}

	@GetMapping("/request")
	@ResponseBody
	public HashMap<String, Object> requestFriend(HttpSession session) {
		HashMap<String, Object> result = requestFriendService.requestFriend(session);
		return result;
	}
	
	@GetMapping("/response")
	@ResponseBody
	public HashMap<String, Object> responseFriend(HttpSession session){
		HashMap<String, Object> result = responseFriendService.responseFriend(session);
		return result;
	}

	@GetMapping("/add")
	@ResponseBody
	public HashMap<String, Object> addFriend(HttpSession session, @RequestParam(value = "id") String id) {
		HashMap<String, Object> result = addFriendService.addFriend(session, id);
		return result;
	}

	@GetMapping("/accept")
	@ResponseBody
	public HashMap<String, Object> friendAccept(HttpSession session, @RequestParam(value = "id") String id) {
		HashMap<String, Object> result = friendAcceptService.friendAccept(session, id);
		return result;
	}

	@PostMapping("/cancel")
	@ResponseBody
	public HashMap<String, Object> friendCancel(HttpSession session, @RequestParam(value = "id") String id) {
		HashMap<String, Object> result = friendCancelService.requestCancle(session, id);
		return result;
	}
	
	@GetMapping("/reject")
	@ResponseBody
	public HashMap<String, Object> friendReject(HttpSession session, @RequestParam(value = "id") String id) {
		HashMap<String, Object> result = friendRejectService.responseReject(session, id);
		return result;
	}
}