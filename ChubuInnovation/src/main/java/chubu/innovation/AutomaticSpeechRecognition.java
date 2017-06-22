package chubu.innovation;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

class AutomaticSpeechRecognition {

	private AutomaticSpeechRecognition() {

	}

	private static final AutomaticSpeechRecognition recaius = new AutomaticSpeechRecognition();

	public static AutomaticSpeechRecognition getInstance() {
		return recaius;
	}

	// リクエストURL
	private static final String REQUEST_BASE_URL = "https://try-api.recaius.jp/asr/v1/";
	private static final String REQUEST_URL_LOGIN = REQUEST_BASE_URL + "login";
	private static final String UUID_PLACEHOLDER = "${UUID}";
	private static final String REQUEST_URL_VOICE = REQUEST_BASE_URL + UUID_PLACEHOLDER + "/voice"; // UUIDはURLに含めてリクエスト
	private static final String REQUEST_URL_RESULT = REQUEST_BASE_URL + UUID_PLACEHOLDER + "/result"; // UUIDはURLに含めてリクエスト
	private static final String REQUEST_URL_LOGOUT = REQUEST_BASE_URL + UUID_PLACEHOLDER + "/logout"; // UUIDはURLに含めてリクエスト

	// リクエストHEADER
	private static String CONTENT_TYPE_KEY = "Content-Type";
	// リクエストBODY KEY
	private static final String ID_KEY = "id";
	private static final String PASSWORD_KEY = "password";
	private static final String MODEL_KEY = "model";
	private static final String MODEL_ID_KEY = "model_id";
	private static final String RESULT_TYPE_KEY = "resulttype";
	private static final String RESULT_COUNT_KEY = "resultcount";
	private static final String VOICE_ID_KEY = "voiceid";
	private static final String VOICE_KEY = "voice";

	// リクエストBODY VALUE
	private static String serviceIdJAJP = "DV1N0897OQK"; // 取得したキーを設定
	private static String servicePasswordJAJP = "6c794d5da633879d"; // 取得したキーを設定
	private static final int MODEL_ID_JAJP = 1;
	private static final String RESULT_TYPE_NBEST = "nbest";
	private static final int RESULT_COUNT = 1;
	//
	private static int voiceId = 1;
	private static final int SAMPLE_BIT = 16;
	private static final int SMAPLE_RATE = 16000;
	private static final double SEND_RANG_MILLSEC = 512d / 1000d;
	private static final int CHUNK_SIZE = (int) ((SAMPLE_BIT / 8) * SMAPLE_RATE * SEND_RANG_MILLSEC);

	public List<JsonArray> call(String wav) {

		List<JsonArray> jsonArrayList = new ArrayList<JsonArray>();

		HttpClient httpClient = HttpClientBuilder.create().build();
		try {

			// loginをしてUUIDを取得
			String uuid = login(httpClient);

			// ローカルのwavファイルを読み込み、終了するまでデータ送信する
			List<JsonArray> voiceJsonArrayList = voice(wav, uuid, httpClient);
			for (JsonArray jsonArray : voiceJsonArrayList) {
				jsonArrayList.add(jsonArray);
			}
			// 送信結果を取得する
			List<JsonArray> resultJsonArrayList = result(uuid, httpClient);
			for (JsonArray jsonArray : resultJsonArrayList) {
				jsonArrayList.add(jsonArray);
			}

			// logoutを行う
			logout(uuid, httpClient);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonArrayList;
	}

	// RECAIUSのWebAPIへログインを実施します。
	private static String login(HttpClient httpClient) throws Exception {
		// model用のJsonObjectを作成
		JsonObject model = Json.createObjectBuilder().add(RESULT_TYPE_KEY, RESULT_TYPE_NBEST) // nbest
				.add(RESULT_COUNT_KEY, RESULT_COUNT) // 1番精度の良い答えを取得
				.add(MODEL_ID_KEY, MODEL_ID_JAJP).build();
		// リクエストBODYの設定
		JsonObject requestBody = Json.createObjectBuilder().add(ID_KEY, serviceIdJAJP)
				.add(PASSWORD_KEY, servicePasswordJAJP).add(MODEL_KEY, model).build();

		// POSTの準備
		HttpPost httpPost = new HttpPost(REQUEST_URL_LOGIN);
		// HEADERの設定
		httpPost.setHeader(CONTENT_TYPE_KEY, ContentType.APPLICATION_JSON.toString());
		// リクエストBODYの設定およびContentの文字コードをUTF-8に設定
		httpPost.setEntity(new StringEntity(requestBody.toString(), ContentType.create("text/plain", "UTF-8")));

		// POSTを実行
		System.out.println("-------------------------------");
		System.out.println("RECAIUS APIにログインのためのPOSTを実行します。");
		HttpResponse response = httpClient.execute(httpPost);
		System.out.println("-------------------------------");
		// Responseの結果を出力
		System.out.println("RECAIUS APIにリクエストを実行結果を表示します。");
		System.out.println("StatusCode  :" + response.getStatusLine().getStatusCode());
		System.out.println("ContentLength  :" + response.getEntity().getContentLength());
		System.out.println("ContentType  :" + response.getEntity().getContentType());
		String uuid = EntityUtils.toString(response.getEntity());
		System.out.println("取得したUUID  :" + uuid);
		System.out.println("-------------------------------");
		return uuid;
	}

	private static List<JsonArray> voice(String wav, String uuid, HttpClient httpClient) throws Exception {
		List<JsonArray> readArrayList = new ArrayList<JsonArray>();
		// ローカルの音声ファイルを取得
		File wavFile = new File(wav);
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wavFile);
		// 音声ファイルのフォーマットを表示
		System.out.println("読み込み対象の音声ファイルのフォーマットは下記のとおりです。");
		System.out.println("AudioFormat Encording: " + audioInputStream.getFormat().getEncoding()); // 符号付きリニア
																									// PCM
		System.out.println("AudioFormat SampleRate: " + audioInputStream.getFormat().getSampleRate()); // 1600kHz
		System.out.println("AudioFormat SampleBit: " + audioInputStream.getFormat().getSampleSizeInBits()); // 16bits
		System.out.println("AudioFormat channel: " + audioInputStream.getFormat().getChannels()); // 1channel(モノラル)
		System.out.println("AudioFormat FrameRate: " + audioInputStream.getFormat().getFrameRate()); // 1600kHz
		System.out.println("AudioFormat FrameSize: " + audioInputStream.getFormat().getFrameSize()); // 2
		System.out.println("AudioFormat BigEndian or Not: " + audioInputStream.getFormat().isBigEndian()); // false(little-Endian)

		System.out.println("-------------------------------");
		System.out.println("音声ファイルのデータを読み込みます");
		// 音声ファイル分割
		BufferedInputStream bufferedInputStream = new BufferedInputStream(audioInputStream);
		ByteArrayOutputStream convetedBufferedOutputStream = new ByteArrayOutputStream();
		int read;
		// 1MのBuffSizeでファイルをByte配列に変換
		byte[] buff = new byte[1024];
		while ((read = bufferedInputStream.read(buff)) > 0) {
			convetedBufferedOutputStream.write(buff, 0, read);
		}
		convetedBufferedOutputStream.flush();
		byte[] audioBytes = convetedBufferedOutputStream.toByteArray();
		int fileSize = audioBytes.length;
		System.out.println("読み込んだ音声ファイルのデータサイズは" + fileSize + " Byteです。");
		System.out.println("-------------------------------");
		// 音声ファイルが送信されるまでPUTし続ける
		System.out.println("音声ファイルの送信を開始します。");
		System.out.println("-------------------------------");
		int copyStartRange = 0; // 分割の開始時点
		int copyEndRange = CHUNK_SIZE; // 分割の終了時点
		while (true) {
			byte[] voice;
			// 読み込みの開始位置が音声ファイルより大きくなったら処理は抜ける
			if (fileSize < copyStartRange) {
				System.out.println("すべての音声データを送信し終わったので、送信終了の合図のため0バイトのファイルを送信します。");
				voice = new byte[0];
			} else {
				voice = Arrays.copyOfRange(audioBytes, copyStartRange, copyEndRange);
				System.out.println("バイナリ音声を送っています:" + voice.length);
			}
			// リクエストBODYの設定
			HttpEntity httpEntity = MultipartEntityBuilder.create().addTextBody(VOICE_ID_KEY, Integer.toString(voiceId))
					.addBinaryBody(VOICE_KEY, voice).build();
			HttpPut httpPut;
			HttpResponse httpResponse;
			// PUTの準備
			httpPut = new HttpPut(REQUEST_URL_VOICE.replace(UUID_PLACEHOLDER, uuid));
			// HEADERの設定
			httpPut.setHeader(CONTENT_TYPE_KEY, ContentType.MULTIPART_FORM_DATA.toString());
			// リクエストBODYの設定およびContentの文字コードをUTF-8に設定
			httpPut.setEntity(httpEntity);
			httpResponse = httpClient.execute(httpPut);

			// 200の時のみ認識結果あり
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				System.out.println("StatusCode  :" + httpResponse.getStatusLine().getStatusCode());
				System.out.println("ContentLength  :" + httpResponse.getEntity().getContentLength());
				System.out.println("ContentType  :" + httpResponse.getEntity().getContentType());

				JsonReader jsonReader = Json.createReader(httpResponse.getEntity().getContent());
				JsonArray readArray = jsonReader.readArray();
				readArrayList.add(readArray);
				// 取得結果の表示
				System.out.println(readArray.toString());
			} else {
				System.out.println("voice_request:" + httpResponse.getStatusLine().getStatusCode());
			}

			// リクエストごとにEntityを削除する。
			// 削除しないとEntityは空にならず、値がある状態でPUTすると正常にレスポンスが受け取れない
			EntityUtils.consume(httpResponse.getEntity());

			// voiceが0バイトであれば終了
			if (voice.length == 0)
				break;

			// 次の開始
			copyStartRange += CHUNK_SIZE;
			copyEndRange += CHUNK_SIZE;
			if (copyEndRange >= fileSize)
				copyEndRange = fileSize;
			++voiceId;
		}
		System.out.println("音声ファイルの送信を完了しました。");
		voiceId = 1;
		audioInputStream.close();
		bufferedInputStream.close();
		convetedBufferedOutputStream.close();
		return readArrayList;
	}

	private static List<JsonArray> result(String uuid, HttpClient httpClient) throws Exception {
		List<JsonArray> readArrayList = new ArrayList<JsonArray>();
		System.out.println("------------------------");
		System.out.println("音声認識の結果を取得します。");
		// リクエストBODYは必要なし
		// GETのURLの準備
		HttpGet httpGet = new HttpGet(REQUEST_URL_RESULT.replace(UUID_PLACEHOLDER, uuid));
		// int timeout = 0;
		// HEADERの設定は必要なし
		while (true) {
			HttpResponse httpResponse = httpClient.execute(httpGet);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				System.out.println("StatusCode  :" + httpResponse.getStatusLine().getStatusCode());
				System.out.println("ContentLength  :" + httpResponse.getEntity().getContentLength());
				System.out.println("ContentType  :" + httpResponse.getEntity().getContentType());
				JsonReader jsonReader = Json.createReader(httpResponse.getEntity().getContent());

				JsonArray readArray = jsonReader.readArray();
				System.out.println("データ全体は下記のとおりです。");
				System.out.println("readArray:" + readArray.toString());
				readArrayList.add(readArray);
				if (readArray.toString().contains("NO_DATA")) {
					EntityUtils.consume(httpResponse.getEntity());//
					break;
				}
			} else {
				System.out.println(httpResponse.getStatusLine().getStatusCode());
				if (httpResponse.getStatusLine().getStatusCode() == 403) {
					System.out.println("リクエスト頻度が制限を超過している状態");
					EntityUtils.consume(httpResponse.getEntity());//
					break;
				}
				// timeout++;
				// if (timeout > 60) {
				// System.out.println("タイムアウトしました");
				// EntityUtils.consume(httpResponse.getEntity());//
				// break;
				// }
			}
			EntityUtils.consume(httpResponse.getEntity());
			Thread.sleep(100);
		}
		return readArrayList;
	}

	private static void logout(String uuid, HttpClient httpClient) throws Exception {
		System.out.println("-------------------------------");
		System.out.println("ログアウトの処理を開始します。");
		// リクエストBODYは必要なし
		// HEADERの設定は必要なし
		// POSTのURLの準備
		HttpPost httpPost = new HttpPost(REQUEST_URL_LOGOUT.replace(UUID_PLACEHOLDER, uuid));
		// POSTを実行
		System.out.println("-------------------------------");
		HttpResponse response = httpClient.execute(httpPost);

		// Responseの結果を出力
		int statusCode = response.getStatusLine().getStatusCode();

		if (statusCode == 200) {
			System.out.println("正常にログアウトしました。");
		} else {
			System.out.println("ログアウト失敗しました。");
		}
	}
}
