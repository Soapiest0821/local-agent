import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class MacroManager {
  private static final String TOP_JSON_PATH = "src/main/resources/json/top.json";

  private Map<String, List<String>> commands; // commands.json (원본 결과 목록)
  private Map<String, Map<String, Integer>> freqData; // top.json (빈도)
  private final ObjectMapper mapper = new ObjectMapper();

  public MacroManager(String jsonPath) {
    try {
      commands = mapper.readValue(new File(jsonPath), Map.class);
    } catch (IOException e) {
      e.printStackTrace();
      commands = new HashMap<>();
    }
    loadFrequency();
  }

  private void loadFrequency() {
    File file = new File(TOP_JSON_PATH);
    if (file.exists()) {
      try {
        freqData = mapper.readValue(file, Map.class);
      } catch (IOException e) {
        e.printStackTrace();
        freqData = new HashMap<>();
      }
    } else {
      freqData = new HashMap<>();
      saveFrequency();
    }
  }

  private void saveFrequency() {
    try {
      File file = new File(TOP_JSON_PATH);
      file.getParentFile().mkdirs();
      mapper.writerWithDefaultPrettyPrinter().writeValue(file, freqData);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 제한 없이 전체 반환: 빈도순 정렬 + 빈도기록 없는 항목은 뒤에 이어붙임
  public List<String> getAllResultsSorted(String key) {
    key = key.trim();

    // 1. 빈도 데이터 있는 것들 (내림차순 정렬)
    Map<String, Integer> freqMap = freqData.getOrDefault(key, new HashMap<>());
    List<String> sortedByFreq = freqMap.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    // 2. commands.json 원본 목록 중, 빈도 데이터에 없는 것들 (중복 제거)
    List<String> rawList = commands.getOrDefault(key, Collections.emptyList());
    List<String> remaining = rawList.stream()
        .distinct()
        .filter(path -> !freqMap.containsKey(path))
        .collect(Collectors.toList());

    // 3. 합쳐서 반환 (빈도순 먼저, 나머지는 원본 순서대로 뒤에)
    List<String> result = new ArrayList<>(sortedByFreq);
    result.addAll(remaining);
    return result;
  }

  public void incrementFrequency(String key, String path) {
    freqData.computeIfAbsent(key.trim(), k -> new HashMap<>())
        .merge(path, 1, Integer::sum);
    saveFrequency();
  }

  public void openFolder(String path) {
    try {
      Runtime.getRuntime().exec(new String[] { "explorer.exe", path });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void searchGoogle(String query) {
    try {
      String url = "https://www.google.com/search?q=" +
          java.net.URLEncoder.encode(query, "UTF-8");
      // "" 는 start의 창 제목 자리 -> 비워둬야 url이 제대로 인자로 들어감
      Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", "", url });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
