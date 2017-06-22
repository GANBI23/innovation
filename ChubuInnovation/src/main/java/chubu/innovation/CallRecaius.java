package chubu.innovation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.java.sen.SenFactory;
import net.java.sen.StringTagger;
import net.java.sen.dictionary.Token;

public class CallRecaius {
	public CallRecaius() {
	}

	private String wav;
	private String resampledWav;
	private static AutomaticSpeechRecognition recaius = AutomaticSpeechRecognition.getInstance();

	public Map<String, Object> call() {
		List<String> sentenseList = new ArrayList<String>();
		List<String> meaningList = new ArrayList<String>();
		List<String> wordList = new ArrayList<String>();

		// ローカルの音声データのリサンプリングを実施。
		File wavFile = new File(wav);
		File resampledWavFile = new File(resampledWav);

		AudioInputStream ais;
		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			ais = AudioSystem.getAudioInputStream(wavFile);

			// 変換後のwaveデータ型
			AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0F, 16, 1, 2, 16000.0F, false);
			AudioInputStream pcm = AudioSystem.getAudioInputStream(af, ais);
			AudioSystem.write(pcm, AudioFileFormat.Type.WAVE, resampledWavFile);

			// 字句解析用インスタンスの生成
			StringTagger tagger = SenFactory.getStringTagger(null);
			List<Token> tokens = new ArrayList<Token>();

			// RECAIUS接続用インスタンスの生成
			List<JsonArray> jsonArrayList = recaius.call(resampledWav);
			for (JsonArray jsonArray : jsonArrayList) {
				for (int i = 0; i < jsonArray.size(); i++) {
					JsonObject json = jsonArray.getJsonObject(i);

					// 抽出条件
					// typeがRESULTであるjsonオブジェクト->result->str値
					if (json.getString("type").equals("RESULT")) {
						String sentense = jsonArray.getJsonObject(i).getJsonArray("result").getJsonObject(0)
								.getString("str");
						tagger.analyze(sentense, tokens);
						for (Token token : tokens) {
							if (token.getMorpheme().getPartOfSpeech().contains("名詞")
									&& !token.getMorpheme().getPartOfSpeech().contains("数")
									&& !token.getMorpheme().getPartOfSpeech().contains("非自立")
									&& !token.getMorpheme().getPartOfSpeech().contains("接尾")) {
								// 画面表示用リストに追加」
								meaningList.add("《" + token.getMorpheme().getPartOfSpeech() + "》 " + token.getSurface()
										+ token.getMorpheme().getReadings());
								// 検索用リストに追加
								wordList.add(token.getSurface());
							}
						}
						// 文章リストに追加
						sentenseList.add(sentense);

					}
				}
			}
			ais.close();
			pcm.close();

			System.out.println(wordList);
			System.out.println(meaningList);
			System.out.println(sentenseList);

			resultMap.put("word", wordList);
			resultMap.put("meaning", meaningList);
			resultMap.put("sentense", sentenseList);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultMap;
	}

	public String getWav() {
		return wav;
	}

	public void setWav(String wav) {
		this.wav = wav;
	}

	public String getResampledWav() {
		return resampledWav;
	}

	public void setResampledWav(String resampledWav) {
		this.resampledWav = resampledWav;
	}

}
