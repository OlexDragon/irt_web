package irt.web;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.web.bean.jpa.VariableContent;
import irt.web.bean.jpa.WebContent;
import irt.web.bean.jpa.WebContentRepository;

@RestController
@RequestMapping("rest")
public class IrtWebRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private WebContentRepository	 	webContentRepository;

	@PostMapping("get")
	public Object get(@RequestParam String url) throws ClientProtocolException, IOException{
		logger.error(url);

			HttpGet httpGet = new HttpGet(url.replaceAll(" ","%20"));
			try(	CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse response = httpClient.execute(httpGet);){

				return EntityUtils.toString(response.getEntity(), "UTF-8");
		}
	}

	@PostMapping("post")
	public Object post(@RequestParam String url, @RequestParam String data) throws ClientProtocolException, IOException{
		logger.error("\n\t{}\n\t{}", url, data);

			HttpPost httpGet = new HttpPost(url.replaceAll(" ","%20"));
			try(	CloseableHttpClient httpClient = HttpClients.createDefault();
					CloseableHttpResponse response = httpClient.execute(httpGet);){

				return response;
		}
	}

	@PostMapping("page_valiables")
    List<VariableContent> getPageVariables(@RequestParam String pageName) {
		logger.traceEntry(pageName);

    	return webContentRepository.findByPageName(pageName).stream().map(WebContent::getVariableContent).collect(Collectors.toList());
    }
}
