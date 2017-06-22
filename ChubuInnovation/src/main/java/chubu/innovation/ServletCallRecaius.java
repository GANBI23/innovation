package chubu.innovation;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.solr.common.SolrDocument;

//【パス情報：AWSに上げる時には変更する必要あり】
@MultipartConfig(location = "/tmp", maxFileSize = 500000000)
// @MultipartConfig(location = "C:/a", maxFileSize = 500000000)
public class ServletCallRecaius extends HttpServlet {

	// 区切り文字
	String separater = "|";

	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		try {
			// リクエストパラメータの文字エンコーディングを設定
			request.setCharacterEncoding("Windows-31J");

			// InputStreamのblobデータを読み込み
			Part part = request.getPart("waveFile");

			// 【パス情報：AWSに上げる時には変更する必要あり】
			String name = "/tmp/" + System.currentTimeMillis() + ".wav";
			String rname = "/tmp/resampled_" + System.currentTimeMillis() + ".wav";
			// String name = "C:/a/" + System.currentTimeMillis() + ".wav";
			// String rname = "C:/a/resampled_" + System.currentTimeMillis() +
			// ".wav";

			// JavaBeansCallRecaiusをインスタンス化
			CallRecaius callRecaius = new CallRecaius();
			callRecaius.setWav(name);
			callRecaius.setResampledWav(rname);
			part.write(name);

			// RECAIUS呼び出し
			Map<String, Object> resultMap = callRecaius.call();

			// 結果の取得
			@SuppressWarnings("unchecked")
			List<String> wordList = (List<String>) resultMap.get("word");
			@SuppressWarnings("unchecked")
			List<String> meaningList = (List<String>) resultMap.get("meaning");
			@SuppressWarnings("unchecked")
			List<String> sentenseList = (List<String>) resultMap.get("sentense");

			// 画面表示用のリストを作成
			String result = "";
			if (sentenseList.size() > 0) {
				result += "▼文章<br>";
				for (String sentense : sentenseList) {
					result += sentense + "<br>";
				}
				result += "▼単語<br>";
				for (String meaning : meaningList) {
					result += meaning + "<br>";
				}
				if (wordList.size() > 0) {
					result += "▼検索用単語<br>";
					for (String word : wordList) {
						result += word + ",";
					}
					result = result.substring(0, result.length() - 1);

					// レスポンスデータに区切り文字の追加
					result += separater;

					// 画像情報用Solrで検索を実施
					System.out.println("Solr検索を実施します。");

					// JavaBeansSolrSerchをインスタンス化
					SolrSerch solrSerch = new SolrSerch();

					List<SolrDocument> resultList = solrSerch.search(wordList);

					if (resultList.size() == 0) {
						result += "";
					} else {
						String preSearchWord = "";
						for (SolrDocument document : resultList) {
							Object title = document.getFieldValue("title");
							Object article = document.getFieldValue("article");
							Object abstractArticle = document.getFieldValue("abstract");
							Object searchWord = document.getFieldValue("searchWord");
							System.out.println(document.toString());

							if (article != null && article.toString().contains("アダルト")) {
								continue;
							}
							if (abstractArticle != null && abstractArticle.toString().contains("アダルト")) {
								continue;
							}

							if (!preSearchWord.equals(searchWord.toString())) {
								result += "<h5>『" + searchWord.toString() + "』で検索</h5>";
								preSearchWord = searchWord.toString();
							}

							// タイトル
							result += "<p>";
							if (title != null) {
								result += "<h4>" + title.toString().replaceAll("\\[", "").replaceAll("\\]", "")
										.replaceAll("\\|", "") + "</h4>";
							} else {
								result += "<p><h4>タイトル無し</h4>";
							}

							// ボディ部
							result += "<div class = 'block'>";
							int no_content = 1;

							// 記事
							if (article != null) {
								result += article.toString().replaceAll("\\[", "").replaceAll("\\]", "")
										.replaceAll("\\|", "");
								no_content = 0;
							}

							// Wiki記事
							if (abstractArticle != null) {
								result += abstractArticle.toString().replaceAll("\\[", "").replaceAll("\\]", "")
										.replaceAll("\\|", "");
								no_content = 0;
							}

							// 中身無しの場合
							if (no_content == 1) {
								result += "（情報無し）";
							}

							result += "</div></p>";
						}
					}

				} else {
					// レスポンスデータに区切り文字の追加
					result += separater;
				}
			} else {
				// レスポンスデータにNodataの追加
				result += "識別できませんでした" + separater;
			}

			response.setContentType("text/plain; charset=UTF-8");
			PrintWriter out = response.getWriter();
			out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
