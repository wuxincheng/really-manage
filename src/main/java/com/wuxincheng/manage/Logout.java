package com.wuxincheng.manage;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.wuxincheng.common.util.Constants;

/**
 * 退出
 * 
 * @author wuxincheng
 *
 */
@Controller
@RequestMapping("/admins")
public class Logout {

	private static Logger logger = LoggerFactory.getLogger(Logout.class);
	
	private static final String MANAGE_NAME = Constants.MANAGE_NAME;
	
	@RequestMapping(value = "/logout")
	public String logout(HttpServletRequest request, Model model) {
		logger.info(MANAGE_NAME+"退出系统");
		
		request.getSession().removeAttribute("admins"); // 这一步可以不需要
		
		request.getSession().invalidate(); // 完全使用整个Session失效
		
		return "redirect:/admins/login";
	}
	
}
