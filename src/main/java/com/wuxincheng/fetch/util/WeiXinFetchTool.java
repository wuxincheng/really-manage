package com.wuxincheng.fetch.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wuxincheng.common.util.CharacterUtil;
import com.wuxincheng.common.util.FontTag;
import com.wuxincheng.common.util.HttpClientHelper;
import com.wuxincheng.common.util.NumberUtil;
import com.wuxincheng.manage.model.News;
import com.wuxincheng.manage.model.WeChat;

/**
 * 抓取微信内容
 * 
 * @author wuxincheng
 *
 */
public class WeiXinFetchTool {
	private static Logger logger = LoggerFactory.getLogger(WeiXinFetchTool.class);
	
	public static void main(String[] args) {
		String openid = "";
		String encryData = "";
		String encryLink = "http://weixin.sogou.com/gzhjs?cb=sogou.weixin.gzhcb&openid=oIWsFt9Jt9iSM0fpb9H8cgy4bq5U&eqs=9SszowQgmN2TolhfwliAyuiu1t05naoi9g6nbSArBMcXcswYYjTXpp7Ag9MHA%2FzfnBy97&ekv=4&page=1&t=1433726183174";
		int page = 1;
		List<News> Articles = fectArticle(openid, encryData, encryLink, page);
		logger.info("Articles size={}", Articles.size());
	}
	
	/**
	 * 根据openid抓取从搜狗搜索出来的微信文章
	 * 
	 * @param openid 搜索上的OpenId
	 * @param page 当前页
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<News> fectArticle(String openid, String encryData, String encryLink, int page) {
		List<News> fectWeiXinContents = new ArrayList<News>();
    	HttpClientHelper hp = new HttpClientHelper();
    	
    	String sogouUrl = null;
    	if (StringUtils.isEmpty(openid) || StringUtils.isEmpty(encryData)) {
    		sogouUrl = encryLink;
    	} else {
    		sogouUrl = "http://weixin.sogou.com/gzhjs?cb=sogou.weixin.gzhcb&openid="+openid+"&eqs="+encryData+"&ekv=4&page="+page;
    	}
    	
    	logger.info("抓取数据的地址: " + sogouUrl);
    	
    	String sogouWeiXinResult = null;
		try {
			sogouWeiXinResult = hp.getData(sogouUrl);
			logger.info("接收到搜狗搜索微信返回的原始数据");
			sogouWeiXinResult = sogouWeiXinResult.replace("sogou.weixin.gzhcb(", "");
			sogouWeiXinResult = sogouWeiXinResult.substring(0, sogouWeiXinResult.lastIndexOf("//"));
		} catch (Exception e) {
			logger.error("接收到搜狗搜索微信返回原始数据出现异常: ", e);
		} 
		
		if (null == sogouWeiXinResult || sogouWeiXinResult.length() < 1) {
			logger.warn("解析后的数据为空");
			return fectWeiXinContents;
		}
		
		// logger.info("sogouWeiXinResult= " + sogouWeiXinResult);
		
		if (sogouWeiXinResult.indexOf("}") < 1 || sogouWeiXinResult.indexOf("{") < 1 
				|| sogouWeiXinResult.indexOf("page") < 1 || sogouWeiXinResult.indexOf("items") < 1) {
			return null;
		}
		
		JSONObject sogouWeiXinObject = new JSONObject(sogouWeiXinResult);
		logger.debug("数据已经转换成JSONObject对象");
		
		// logger.info("sogouWeiXinObject= " + sogouWeiXinObject);
		
		JSONArray items = (JSONArray)sogouWeiXinObject.get("items");
		logger.debug("数据已经转换成JSONArray数组");
		
		// logger.info("items= " + items);
		
		String xmlSource = items.toString();
		xmlSource = xmlSource.replace("\\", "").replace("[\"", "").replace("\"]", "");
		String[] xmls = xmlSource.split("\",\"");
		logger.debug("JSONArray数组中反斜杠已经处理");
		
		logger.debug("循环解析JSONArray数组的数据");
    	try {
			for (int i = 0; i < xmls.length; i++) {
				News fectWeiXin = new News();
				Document doc = DocumentHelper.parseText(xmls[i]); // 将字符串转为XML
				Element rootElt = doc.getRootElement(); // 获取根节点
				
				Iterator<Element> iter = rootElt.elementIterator("item"); // 获取根节点下的子节点
				while (iter.hasNext()) {
					Element recordEle = (Element) iter.next();
					Iterator<Element> iters = recordEle.elementIterator("display"); 
					while (iters.hasNext()) {
						Element itemEle = (Element) iters.next();
						// String headimage = itemEle.elementTextTrim("headimage"); // 公众号图标
						
						fectWeiXin.setUrl(itemEle.elementTextTrim("url")); // 文章地址
						fectWeiXin.setDomain(""+itemEle.elementTextTrim("sourcename")); // 公众号名称
						fectWeiXin.setImgLink(itemEle.elementTextTrim("imglink")); // 图片链接
						fectWeiXin.setTitle(CharacterUtil.replaceQuoteCode(itemEle.elementTextTrim("title"))); // 文章标题
						fectWeiXin.setSubTitle(CharacterUtil.replaceQuoteCode(itemEle.elementTextTrim("content168"))); // 文章摘要/即为文章子标题
						fectWeiXin.setCreateTime(itemEle.elementTextTrim("date")); // 发布日期
						fectWeiXin.setSogouDocid(itemEle.elementTextTrim("docid")); // 唯一标识
						fectWeiXin.setVirtualActive(NumberUtil.getRandom()+""); // 产生一个随机数
						fectWeiXin.setState("1"); // 1-不显示, 0-显示
						fectWeiXin.setCreator("2"); // 为默认用户
						
						fectWeiXinContents.add(fectWeiXin);
					}
				}
			}
			logger.info("数据解析完成");
		} catch (Exception e) {
			logger.error("抓取微信公众号文章解析数据出现异常: ", e);
		}
    	
    	return fectWeiXinContents;
	}

	@SuppressWarnings("unchecked")
	public static List<News> parseData(String resource) {
		logger.info("数据解析");
		
		List<News> fectWeiXinContents = new ArrayList<News>();
    	
    	String sogouWeiXinResult = null;
		try {
			sogouWeiXinResult = resource;
			sogouWeiXinResult = sogouWeiXinResult.replace("sogou.weixin.gzhcb(", "");
			sogouWeiXinResult = sogouWeiXinResult.substring(0, sogouWeiXinResult.lastIndexOf("//"));
		} catch (Exception e) {
			logger.error("接收到搜狗搜索微信返回原始数据出现异常: ", e);
		} 
		
		if (null == sogouWeiXinResult || sogouWeiXinResult.length() < 1) {
			logger.warn("解析后的数据为空");
			return fectWeiXinContents;
		}
		
		// logger.info("sogouWeiXinResult= " + sogouWeiXinResult);
		
		if (sogouWeiXinResult.indexOf("}") < 1 || sogouWeiXinResult.indexOf("{") < 1 
				|| sogouWeiXinResult.indexOf("page") < 1 || sogouWeiXinResult.indexOf("items") < 1) {
			return null;
		}
		
		JSONObject sogouWeiXinObject = new JSONObject(sogouWeiXinResult);
		logger.debug("数据已经转换成JSONObject对象");
		
		// logger.info("sogouWeiXinObject= " + sogouWeiXinObject);
		
		JSONArray items = (JSONArray)sogouWeiXinObject.get("items");
		logger.debug("数据已经转换成JSONArray数组");
		
		// logger.info("items= " + items);
		
		String xmlSource = items.toString();
		xmlSource = xmlSource.replace("\\", "").replace("[\"", "").replace("\"]", "");
		String[] xmls = xmlSource.split("\",\"");
		logger.debug("JSONArray数组中反斜杠已经处理");
		
		logger.debug("循环解析JSONArray数组的数据");
    	try {
			for (int i = 0; i < xmls.length; i++) {
				News fectWeiXin = new News();
				Document doc = DocumentHelper.parseText(xmls[i]); // 将字符串转为XML
				Element rootElt = doc.getRootElement(); // 获取根节点
				
				Iterator<Element> iter = rootElt.elementIterator("item"); // 获取根节点下的子节点
				while (iter.hasNext()) {
					Element recordEle = (Element) iter.next();
					Iterator<Element> iters = recordEle.elementIterator("display"); 
					while (iters.hasNext()) {
						Element itemEle = (Element) iters.next();
						// String headimage = itemEle.elementTextTrim("headimage"); // 公众号图标
						
						fectWeiXin.setUrl(itemEle.elementTextTrim("url")); // 文章地址
						fectWeiXin.setDomain(""+itemEle.elementTextTrim("sourcename")); // 公众号名称
						fectWeiXin.setImgLink(itemEle.elementTextTrim("imglink")); // 图片链接
						fectWeiXin.setTitle(CharacterUtil.replaceQuoteCode(itemEle.elementTextTrim("title"))); // 文章标题
						fectWeiXin.setSubTitle(CharacterUtil.replaceQuoteCode(itemEle.elementTextTrim("content168"))); // 文章摘要/即为文章子标题
						fectWeiXin.setCreateTime(itemEle.elementTextTrim("date")); // 发布日期
						fectWeiXin.setSogouDocid(itemEle.elementTextTrim("docid")); // 唯一标识
						fectWeiXin.setVirtualActive(NumberUtil.getRandom()+""); // 产生一个随机数
						fectWeiXin.setState("1"); // 1-不显示, 0-显示
						fectWeiXin.setCreator("2"); // 为默认用户
						
						fectWeiXinContents.add(fectWeiXin);
					}
				}
			}
			logger.info("数据解析完成");
		} catch (Exception e) {
			logger.error("抓取微信公众号文章解析数据出现异常: ", e.getMessage());
		}
    	
    	return fectWeiXinContents;
	}
	
	/**
	 * 根据搜狗openId抓取微信公众号信息
	 * 
	 * @param openId
	 */
	public static WeChat fetchWechatPublicNoInfoBySogouOpenid (String openId) {
		logger.info("要获取微信公众号的搜狗openId: {}", openId);
		
		String url = "http://weixin.sogou.com/gzh?openid=" + openId;
		
		logger.info("要获取微信公众号信息的url: {}", url);
		
		WeChat wechat = null;
		
		// == 微信公众号名称
		NodeList newTitles = getNodeList(url, "h3", "id", "weixinname");
		SimpleNodeIterator nodeIterator = newTitles.elements();
		if (!nodeIterator.hasMoreNodes()) {
			return null;
		}
		wechat = new WeChat();
		while (nodeIterator.hasMoreNodes()) {
			Node node = nodeIterator.nextNode();
			HeadingTag tag = (HeadingTag) node;
			wechat.setPublicName(tag.getStringText().trim());
		}
		
		// == 微信公众号
		NodeList newPublicNos = getNodeList(url, "div", "class", "txt-box");
		SimpleNodeIterator publicIterator = newPublicNos.elements();
		if (publicIterator.hasMoreNodes()) {
			Node node = publicIterator.nextNode();
			Div tag = (Div) node;
			String divtag = tag.getStringText().trim();
			String subDivTag = divtag.substring(divtag.indexOf("<span>"), divtag.indexOf("</span>"));
			String publicNoValue = subDivTag.substring(subDivTag.indexOf("：")+1);
			wechat.setPublicNO(publicNoValue);
		}
		
		// == 微信公众号功能介绍及微信认证
		NodeList newNos = getNodeList(url, "span", "class", "sp-txt");
		SimpleNodeIterator noIterator = newNos.elements();
		int spanFlag = 1;
		while (noIterator.hasMoreNodes()) {
			Node node = noIterator.nextNode();
			Span tag = (Span) node;
			if (spanFlag == 1) {
				wechat.setMemo(tag.getStringText().trim());
			}
			if (spanFlag == 2) {
				wechat.setWeChatCret(tag.getStringText().trim());
			}
			spanFlag++;
		}
		
		// == 微信公众号Logo和二维码地址
		NodeList imgs = getNodeList(url, "img");
		for (int i = 0; i < imgs.size(); i++) {
			ImageTag imgNode = (ImageTag) imgs.elementAt(i);
			if (imgNode.getImageURL().contains(openId)) {
				wechat.setLogoLink(imgNode.getImageURL());
				continue;
			}
			
			if (!(imgNode.getImageURL().contains(openId)) && 
					imgNode.getImageURL().contains("http://img03.sogoucdn.com/app/a")) {
				wechat.setCdnLink(imgNode.getImageURL());
				continue;
			}
		}
		
		wechat.setOpenId(openId);
		
		return wechat;
	}
	
	/**
	 * 根据给定的节点名字、标签属性、标签值提取出符合条件的所有tag节点
	 * 
	 * @param url
	 * @param tagName
	 * @param attributeName
	 * @param attributeValue
	 * @return 符合条件的List
	 */
	private static NodeList getNodeList(String url, String tagName,
			String attributeName, String attributeValue) {
		ConnectionManager manager;
		manager = org.htmlparser.lexer.Page.getConnectionManager();
		Parser parser;
		try {
			parser = new Parser(manager.openConnection(url));
			parser.setEncoding("UTF-8");

			// 下面的节点注册一定要放在最前面，才能把指定节点的所有孩子节点都按我们的要求解析（有些自定义标签必须能够解析）
			// 注册新的结点解析器，其实我觉得在htmlparser的源码里面可以直接编写新的节点类，然后重新编译
			PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
			factory.registerTag(new FontTag());
			parser.setNodeFactory(factory);

			NodeFilter filterAttribute = new HasAttributeFilter(attributeName,
					attributeValue);
			NodeFilter filterTag = new TagNameFilter(tagName);
			NodeFilter andFilter = new AndFilter(filterAttribute, filterTag);

			return parser.parse(andFilter);// 如果没有对应的节点，则会返回size=0的NodeList
		} catch (ParserException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 根据给定的节点名字提取出符合条件的所有tag节点
	 * 
	 * @param url
	 * @return 符合条件的List
	 */
	private static NodeList getNodeList(String url, String tagName) {
		ConnectionManager manager;
		manager = org.htmlparser.lexer.Page.getConnectionManager();
		Parser parser;
		try {
			parser = new Parser(manager.openConnection(url));
			parser.setEncoding("UTF-8");

			// 下面的节点注册一定要放在最前面，才能把指定节点的所有孩子节点都按我们的要求解析（有些自定义标签必须能够解析）
			// 注册新的结点解析器，其实我觉得在htmlparser的源码里面可以直接编写新的节点类，然后重新编译
			PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
			factory.registerTag(new FontTag());
			parser.setNodeFactory(factory);

			NodeFilter filterTag = new TagNameFilter(tagName);
			return parser.parse(filterTag);// 如果没有对应的节点，则会返回size=0的NodeList
		} catch (ParserException e) {
			e.printStackTrace();
			return null;
		}
	}
}
