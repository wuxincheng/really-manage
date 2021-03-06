package com.wuxincheng.manage.controller;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import com.wuxincheng.common.util.ConfigHelper;
import com.wuxincheng.common.util.Constants;
import com.wuxincheng.common.util.DateUtil;
import com.wuxincheng.common.util.HttpClientHelper;
import com.wuxincheng.common.util.JsonHelper;
import com.wuxincheng.common.util.Validation;
import com.wuxincheng.manage.Pager;
import com.wuxincheng.manage.model.Comment;
import com.wuxincheng.manage.model.News;
import com.wuxincheng.manage.model.WeChat;
import com.wuxincheng.manage.service.NewsService;
import com.wuxincheng.manage.service.WeChatService;

/**
 * 微信文章管理/出版管理
 * 
 * @author wuxincheng
 *
 */
@Controller
@RequestMapping("/manage/news/send")
public class NewsSendController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(NewsSendController.class);
	
	/** 每页显示条数 */
	private final Integer pageSize = Pager.PAGER_SIZE;
	
	private String currentPage;
	
	@Autowired private NewsService newsService;
	@Autowired private WeChatService weChatService;
	
	/** 查询日期 */
	private String queryStartDate;
	private String queryEndDate;
	
	private String sogouOpenid;
	
	@RequestMapping(value = "/list")
	public String list(HttpServletRequest request, String currentPage, String queryStartDate, 
			String queryEndDate, String query, String sogouOpenid, Model model) {
		logger.info("显示文章列表页面");
		setMenuFlag(request, "news");
		
		// ---------------------------------
		// 判断当前页码的合法性
		if (Validation.isBlank(currentPage) || !Validation.isInt(currentPage, "0+")) {
			currentPage = "1";
		}
		
		// ---------------------------------
		// 验证是否为查询
		if (!StringUtils.isEmpty(query)) { // 查询
			this.queryStartDate = queryStartDate;
			this.queryEndDate = queryEndDate;
			this.sogouOpenid = sogouOpenid;
		} else {
			this.queryStartDate = DateUtil.getCurrentDate(new Date(), "yyyyMMdd");
			this.queryEndDate = DateUtil.getSpecifiedDayAfter(this.queryStartDate, "yyyyMMdd");
		}
		// 验证查询日期
		String queryEndDateAfter = null;
		if (StringUtils.isEmpty(this.queryStartDate) || StringUtils.isEmpty(this.queryEndDate)) {
			this.queryStartDate = null;
			this.queryEndDate = null;
		} else {
			queryEndDateAfter = DateUtil.getSpecifiedDayAfter(this.queryEndDate, "yyyyMMdd");
		}
		// 验证查询的公众号
		if (StringUtils.isEmpty(this.sogouOpenid)){
			this.sogouOpenid = null;
		}
		
		// ---------------------------------
		// 根据当前页处理查询条件
		this.currentPage = currentPage;
		Integer current = Integer.parseInt(currentPage);
		Integer start = null;
		Integer end = null;
		if (current > 1) {
			start = (current - 1) * pageSize;
			end = pageSize;
		} else {
			start = 0;
			end = pageSize;
		}
		
		// 封装查询条件
		Map<String, Object> queryParam = new HashMap<String, Object>();
		queryParam.put("start", start);
		queryParam.put("end", end);
		queryParam.put("presend", "presend"); // 
		queryParam.put("queryStartDate", this.queryStartDate);
		queryParam.put("queryEndDate", queryEndDateAfter);
		queryParam.put("sogouOpenid", this.sogouOpenid);
		
		Map<String, Object> pager = newsService.queryPager(queryParam);
		
		try {
			if (pager != null && pager.size() > 0) {
				Integer totalCount = (Integer)pager.get("totalCount");
				Integer lastPage = (totalCount/pageSize);
				Integer flag = (totalCount%pageSize)>0?1:0;
				pager.put("lastPage", lastPage + flag);
				
				// 如果当前页数大于总页数, 减1处理
				if (current > (lastPage + flag)) {
					current--;
					this.currentPage = current+"";
				}
				
				pager.put("currentPage", current);
				pager.put("pageSize", pageSize);
				
				model.addAttribute("pager", pager);
			} else {
				model.addAttribute("news", Collections.EMPTY_LIST);
				logger.info("没有查询到文章信息");
			}
		} catch (Exception e) {
			logger.error("在查询文章明细时出现异常", e);
		}
		
		model.addAttribute("queryStartDate", this.queryStartDate);
		model.addAttribute("queryEndDate", this.queryEndDate);
		model.addAttribute("sogouOpenid", this.sogouOpenid);
		
		model.addAttribute("pageSize", pageSize);
		
		@SuppressWarnings("unchecked")
		List<WeChat> weChats = (List<WeChat>)request.getSession().getAttribute("weChats");
		if (weChats == null || weChats.size() < 1) {
			weChats = weChatService.queryAll();
			request.getSession().setAttribute("weChats", weChats);
		}
		
		return "send/list";
	}
	
	@RequestMapping(value = "/praeUrl")
	public String praeUrl() {
		logger.info("显示解析URL页面");
		return "send/prae_url";
	}
	
	@RequestMapping(value = "/praeShow")
	public String praeShow(String url, Model model) {
		logger.info("解析URL, 并显示解析后的内容");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("news_url", url);
		
		String serverBase = ConfigHelper.getInstance().getConfig("serverBase", "config.properties");
		String serverUrl = serverBase + "/news/parseLinkData";
		String responseStr = HttpClientHelper.doPost(serverUrl, data);
		@SuppressWarnings("unchecked")
		Map<String, Object> responseData = JsonHelper.parse(responseStr, Map.class);
		
		if(responseData == null) {
			model.addAttribute("message", "没有找到文章内容");
			return "news/prae_url";
		}
		
		News news = new News();
		news.setTitle(responseData.get("news_title").toString());
		news.setImgLink(responseData.get("news_img_link").toString());
		news.setDomain(responseData.get("news_domain").toString());
		news.setUrl(url);
		
		model.addAttribute("news", news);
		
		return "send/edit";
	}
	
	@RequestMapping(value = "/edit")
	public String edit(String newsId, Model model) {
		if (StringUtils.isEmpty(newsId)) { // 
			logger.info("显示文章新增页面");
		} else {
			logger.info("显示文章修改页面");
			
			News news = null;
			logger.info("修改文章的编号: " + newsId);
			
			news = newsService.queryNewsById(newsId);
			
			// logger.info("查询到文章信息 news: " + news.toString());
			
			model.addAttribute("news", news);
		}
		
		return "send/edit";
	}
	
	@RequestMapping(value = "/doEdit")
	public String doEdit(HttpServletRequest request, News news, Model model) {
		logger.info("处理编辑文章数据");
		
		try {
			newsService.edit(news);
			
			model.addAttribute(Constants.MSG_TYPE_SUCCESS, "文章编辑成功");
		} catch (Exception e) {
			logger.error("在编辑文章时出现异常: ", e);
			model.addAttribute(Constants.MSG_TYPE_DANGER, "文章编辑时出现异常，请联系管理员");
		}
		
		return list(request, this.currentPage, null, null, null, null, model);
	}
	
	@RequestMapping(value = "/send")
	public String send(HttpServletRequest request, String newsId, Model model) {
		logger.info("发布文章");
		
		if (!StringUtils.isEmpty(newsId)) {
			newsService.sendNews4App(newsId);
		}
		
		return list(request, this.currentPage, null, null, null, null, model);
	}
	
	@RequestMapping(value = "/sendBatch")
	public String sendBatch(HttpServletRequest request, Long[] newsIds, Model model) {
		logger.info("批量发布文章");
		
		if (newsIds.length < 1) {
			model.addAttribute(Constants.MSG_TYPE_WARNING, "批量发布文章失败");
			return list(request, "1", null, null, null, null, model);
		}
		
		try {
			newsService.sendBatch(newsIds);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return list(request, this.currentPage, null, null, null, null, model);
	}
	
	@RequestMapping(value = "/showBatch")
	public String showBatch(HttpServletRequest request, Long[] newsIds, Model model) {
		logger.info("批量发布文章");
		
		if (newsIds.length < 1) {
			return null;
		}
		
		List<News> news = newsService.getNewsByIds(newsIds);
		model.addAttribute("news", news);
		
		return "send/show";
	}
	
	@RequestMapping(value = "/comment")
	public String comment(String newsId, String commentId, Model model) {
		logger.info("显示评论页面, newsId=" + newsId + ", commentId=" +commentId);
		
		model.addAttribute("newsId", newsId);
		model.addAttribute("commentId", commentId);
		
		return "send/comment";
	}
	
	@RequestMapping(value = "/doComment")
	public String doComment(HttpServletRequest request, Comment comment, Model model) {
		logger.info("处理文章评论的数据");
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("commentId", comment.getId());
		data.put("userId", "2");
		data.put("truthDegree", comment.getTruthDegree());
		data.put("relationship", comment.getRelationship());
		data.put("content", comment.getContent());
		
		String serverBase = ConfigHelper.getInstance().getConfig("serverBase", "config.properties");
		String url = serverBase + "/opinion/add";
		HttpClientHelper.doPost(url, data);
		
		model.addAttribute(Constants.MSG_TYPE_SUCCESS, "评论成功");
		
		return list(request, this.currentPage, null, null, null, null, model);
	}
	
	@RequestMapping(value = "/rollback")
	public String rollback(HttpServletRequest request, Long newsId, Model model) {
		logger.info("退回文章信息");
		
		if (StringUtils.isEmpty(newsId)) {
			model.addAttribute(Constants.MSG_TYPE_DANGER, "退回失败: 帖子newsId为空");
			return list(request, "1", null, null, null, null, model);
		}
		
		newsService.rollback(newsId);
		
		model.addAttribute(Constants.MSG_TYPE_SUCCESS, "退回成功");
		
		return list(request, this.currentPage, null, null, null, null, model);
	}

	public String getQueryStartDate() {
		return queryStartDate;
	}

	public void setQueryStartDate(String queryStartDate) {
		this.queryStartDate = queryStartDate;
	}

	public String getQueryEndDate() {
		return queryEndDate;
	}

	public void setQueryEndDate(String queryEndDate) {
		this.queryEndDate = queryEndDate;
	}
	
}
