package chubu.innovation;

import java.util.ArrayList;
import java.util.List;

public class Test {

	public static void main(String[] args) {

		SolrSerch solr = new SolrSerch();
		try {

			List<String> test = new ArrayList<String>();
			test.add("user01");
			test.add("テスト");
			test.add("user");

			solr.search(test);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
