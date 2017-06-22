package chubu.innovation;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class SolrSerch {

	private SolrClient solrClient = null;
	private String url = "http://13.112.12.217:8983/solr/practice_core";

	public SolrSerch() {
		solrClient = new HttpSolrClient(url);
	}

	public List<SolrDocument> search(List<String> args) {

		List<SolrDocument> resultList = new ArrayList<SolrDocument>();

		try {
			// 接続、クエリの準備
			// 廃止final SolrServer solr = new HttpSolrServer(url);
			final SolrQuery solrQuery = new SolrQuery("title:*");

			// 引数の文字数回、ApacheSolrを検索
			for (String arg : args) {

				solrQuery.set("q", "\"" + arg + "\"");
				System.out.println("▼" + arg + "で検索");
				SolrDocumentList results;
				results = solrClient.query(solrQuery).getResults();

				// SolrDocumentの追加
				if (results.size() > 0) {
					for (SolrDocument document : results) {
						int insertFlg = 1;
						for (SolrDocument doc : resultList) {
							if (document.getFieldValue("title") == null) {
								// タイトル無しは登録しない
								insertFlg = 0;
								break;
							}
							if (doc.getFieldValue("title").toString()
									.equals(document.getFieldValue("title").toString())) {
								// 同一項目が既に存在する場合登録しない
								insertFlg = 0;
								break;
							}
						}
						if (insertFlg == 1) {
							document.addField("searchWord", arg);
							resultList.add(document);
							System.out.println(document.toString());
						}
					}
				} else {
					System.out.println("検索結果なし");
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
