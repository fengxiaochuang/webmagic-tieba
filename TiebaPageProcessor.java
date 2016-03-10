/**
* @author 冯晓闯 E-mail:lanxinxichen@126.com
* @version 创建时间：2016年3月9日 上午11:58:52
* 类说明:
*/
package cc.leiju.ljyq.crawler;

import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;
import us.codecraft.webmagic.scheduler.component.BloomFilterDuplicateRemover;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.utils.UrlUtils;

public class TiebaPageProcessor implements PageProcessor {
	private String domain;
	private int siteId;
	private int sleepTime;
	
	public TiebaPageProcessor(String domain,int siteId,int sleepTime) {
		this.domain = domain;
		this.siteId = siteId;
		this.sleepTime = sleepTime;
	}

	@Override
	public Site getSite() {
		Site site = Site.me();
		site.setUserAgent(
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.63 Safari/537.36")
				// .enableHttpProxyPool()
				.addHeader("Referer", "http://www.baidu.com").setTimeOut(15000).setRetryTimes(3)
				.setSleepTime(this.sleepTime)
				.addCookie("tieba.baidu.com", "TIEBAUID", "52dc7439f3e72559bb48a912")
				.addCookie("tieba.baidu.com", "TIEBA_USERTYPE", "fcf922b15b80e609daefdb56")
				.addCookie("tieba.baidu.com", "bdshare_firstime", "1413434482309")
				.addCookie("tieba.baidu.com", "dasense_show_10172", "1")
				.addCookie("tieba.baidu.com", "dasense_show_10495", "1")
				.addCookie("tieba.baidu.com", "fuwu_center_bubble", "1").addCookie("tieba.baidu.com", "rpln_guide", "1")
				.addCookie("tieba.baidu.com", "wanleTipCircle", "1415277415883")
				.addCookie("tieba.baidu.com", "zt2meizhi", "");
				site.setDomain(this.domain);
		return site;
	}

	/**
	 * 处理流程
	 */
	@Override
	public void process(Page page) {
		// 判断内容页或者详情页
		String handleString = page.getHtml().replace("<!--", "").replace("-->", "").get();
		Html html = new Html(handleString);
		List<String> tieList = html.$(".j_thread_list:not(.thread_top)").all();
		System.out.println(tieList.size());
		if (tieList.size() > 0) {
			// add url
			boolean addNextPage = false;
			for (String tie : tieList) {
				Html tieHtml = new Html(tie);
				String tieTime = tieHtml.$(".threadlist_reply_date.j_reply_data","text").regex("\\d{2}:\\d{2}").get();
				if (tieTime != null) {
					addNextPage = true;
					page.addTargetRequest(tieHtml.$(".threadlist_text.threadlist_title.j_th_tit").links().get());
					System.out.println(tieTime);
				}else{
					// 如果不满足添加时间的要求,则不添加下一页
					addNextPage = false;
					System.out.println("no has time");
				}
			}
			if (addNextPage) {
				page.addTargetRequest(html.$(".next").links().get());
			}
		}else{
			System.out.println("item");
			System.out.println(page.getUrl());
			/*
			// 主内容  //div[@class='l_post j_l_post l_post_bright noborder']//cc//text()
			// 标题  //h1[@class='core_title_txt']/text()
			// 发帖时间  //div[@class='l_post j_l_post l_post_bright noborder']//ul[@class='p_tail']/li[2]/span/text()
			// 发帖人 //div[@class='l_post j_l_post l_post_bright noborder']//ul[@class='p_author']/li[@class='d_name']/a/text()
			// 总页数 //div[@class='pb_footer']//ul[@class='l_posts_num'][1]/li[@class='l_reply_num']/span[2]/text()
			// 其他也的所有内容 //div[@class='p_postlist']//cc//text()
			String title = html.$(".core_title_txt","text").get();
			String oneContent = html.$(".noborder cc div","text").get();
			String publicTime = html.$(".l_post.j_l_post.l_post_bright.noborder","data-field").regex("date\":\"([\\w\\- :]{16})").get();
			String pageTotal = html.$(".l_posts_num:eq(0) .l_reply_num span.red:eq(1)","text").get();
			String author = html.$(".noborder .p_author .d_name a","text").get();
			int totalFloors = 0;
			
			List<String> floorList = html.$(".d_post_content_main cc div ","text").all();
			String thisUrl = page.getUrl().get();
			totalFloors += floorList.size();
			String allContent = StringUtils.join(floorList, "\n");
			List<String> allRepeatTime = html.$(".l_post_bright","data-field").regex("date\":\"([\\w\\- :]{16})").all();
			String lastRepeatTime;
			int currentPage = 1;
			if (allRepeatTime.size() > 1) {
				lastRepeatTime = allRepeatTime.get(allRepeatTime.size() - 1);
				int pageTotalInt = Integer.valueOf(pageTotal);
				if (pageTotalInt > 1) {
					while (currentPage <= pageTotalInt) {
						String pageUrl = thisUrl + "?pn=" + String.valueOf(currentPage + 1);
						HttpClientDownloader downloader = new HttpClientDownloader();
						Page pagePage = downloader.download(new Request(pageUrl), this.getSite().toTask());
						Html pageHtml = pagePage.getHtml();

						// 分页里面的楼层内容
						List<String> pageFloor = pageHtml.$(".d_post_content_main cc div ","text").all();
						allContent += StringUtils.join(pageFloor, "\n");

						for (String string : pageFloor) {
							System.out.println("page:" + string);
						}
						
						// 分页里面的page
						List<String> pageRepeatTime = pageHtml.$(".l_post_bright","data-field").regex("date\":\"([\\w\\- :]{16})").all();
						lastRepeatTime = pageRepeatTime.get(pageRepeatTime.size() - 1);
						
						for (String string : pageRepeatTime) {
							System.out.println("date:" + string);
						}
						totalFloors += pageFloor.size();
						
						pageTotalInt = Integer.valueOf(html.$(".l_posts_num:eq(0) .l_reply_num span.red:eq(1)","text").get());
						currentPage ++;
					}
				}
			}else{
				lastRepeatTime = publicTime;
			}

			System.out.println(title + "\n");
			System.out.println(oneContent + "\n");
			System.out.println(publicTime + "\n");
			System.out.println(pageTotal + "\n");
			System.out.println(author + "\n");
			System.out.println(allContent + "\n");
			System.out.println(lastRepeatTime + "\n");
			System.out.println(totalFloors + "\n");
			*/
		}
	}

	public static void main(String[] args) {
//		String startUrl = "http://tieba.baidu.com/p/4393036486";
		String startUrl = "http://tieba.baidu.com/f?kw=手绘动漫";
		int siteId = 2;
		int sleepTime = 3000;
		int thread = 1;
		String domain = UrlUtils.getDomain(startUrl);
		TiebaPageProcessor pageProcessor = new TiebaPageProcessor(domain,siteId,sleepTime);
		String log4jConfPath = pageProcessor.getClass().getResource("/").getPath() + "log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);
		Spider spider = Spider.create(pageProcessor).addUrl(startUrl)
				 .setScheduler(new QueueScheduler().setDuplicateRemover(new BloomFilterDuplicateRemover(10000)))
				.thread(thread);
		spider.start();
	}
}
