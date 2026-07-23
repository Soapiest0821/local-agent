import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;

public class MacroManager {
  public static class AliasEntry {
    public String target;
    public String type;
  }

  private static final String ALIASES_JSON_PATH = "src/main/resources/json/aliases.json";
  private static final String TOP_JSON_PATH = "src/main/resources/json/top.json";
  private static final String APPS_JSON_PATH = "src/main/resources/json/apps.json";
  private static final String EDITORS_JSON_PATH = "src/main/resources/json/editors.json";
  private static final String SITES_JSON_PATH = "src/main/resources/json/sites.json"; // 경로 추가

  private Map<String, AliasEntry> aliases; // 필드 추가
  private Map<String, List<String>> commands; // 폴더 별칭
  private Map<String, Map<String, Integer>> freqData; // 빈도
  private Map<String, String> apps; // 앱 실행 경로
  private Map<String, String> editors; // 에디터 커맨드
  private Map<String, String> sites;
  private final ObjectMapper mapper = new ObjectMapper();

  public MacroManager(String commandsJsonPath) {
    aliases = loadMap(ALIASES_JSON_PATH, new HashMap<>());
    commands = loadMap(commandsJsonPath, new HashMap<>());
    apps = loadMap(APPS_JSON_PATH, new HashMap<>());
    editors = loadMap(EDITORS_JSON_PATH, new HashMap<>());
    sites = loadMap(SITES_JSON_PATH, new HashMap<>()); // 추가
    loadFrequency();
  }

  private <T> Map<String, T> loadMap(String path, Map<String, T> fallback) {
    try {
      File file = new File(path);
      if (!file.exists())
        return fallback;
      return mapper.readValue(file, Map.class);
    } catch (IOException e) {
      e.printStackTrace();
      return fallback;
    }
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

  // 핵심: 입력값 분석해서 결과 리스트 만들기
  public List<SearchResult> resolve(String input) {
    private String resolveAlias(String word) {
        AliasEntry entry = aliases.get(word);
        return (entry != null) ? entry.target : word; // 별칭이면 원본으로, 아니면 그대로
    }
    String trimmed = input.trim();
    if (trimmed.isEmpty())
      return Collections.emptyList();

    List<SearchResult> results = new ArrayList<>();
    String[] parts = trimmed.split("\\s+", 2);

    if (parts.length == 1) {
      String word = resolveAlias(parts[0]);

      if (apps.containsKey(word)) {
        results.add(new SearchResult("🚀 " + word + " 실행하기", SearchResult.Type.APP, apps.get(word), null));
      }

      if (commands.containsKey(word)) {
        for (String path : getAllResultsSorted(word)) {
          results.add(new SearchResult("📁 " + path, SearchResult.Type.FOLDER, path, null));
        }
      }
    } else {
      String firstKey = parts[0];
      String secondArg = parts[1];

      // zed dev 같은 에디터 조합
      if (editors.containsKey(firstKey) && commands.containsKey(secondArg)) {
        for (String path : getAllResultsSorted(secondArg)) {
          results.add(new SearchResult(
              "🛠 " + firstKey + "(으)로 " + secondArg + " 열기 [" + path + "]",
              SearchResult.Type.EDITOR_FOLDER,
              editors.get(firstKey),
              path));
        }
      }

      // yt 고양이, naver 날씨 같은 사이트 검색 조합
      if (sites.containsKey(firstKey)) {
        results.add(new SearchResult(
            "🔎 " + firstKey + "에서 \"" + secondArg + "\" 검색",
            SearchResult.Type.SITE_SEARCH,
            sites.get(firstKey) + java.net.URLEncoder.encode(secondArg, java.nio.charset.StandardCharsets.UTF_8),
            null));
      }
    }

    results.add(new SearchResult("🔍 \"" + trimmed + "\" 검색하기!", SearchResult.Type.GOOGLE, trimmed, null));
    return results;
  }

  // 빈도순 정렬 + 나머지 이어붙이기 (기존 로직 그대로)
  private List<String> getAllResultsSorted(String key) {
    Map<String, Integer> freqMap = freqData.getOrDefault(key, new HashMap<>());
    List<String> sortedByFreq = freqMap.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    List<String> rawList = commands.getOrDefault(key, Collections.emptyList());
    List<String> remaining = rawList.stream()
        .distinct()
        .filter(path -> !freqMap.containsKey(path))
        .collect(Collectors.toList());

    List<String> result = new ArrayList<>(sortedByFreq);
    result.addAll(remaining);
    return result;
  }

  public void incrementFrequency(String key, String path) {
    freqData.computeIfAbsent(key.trim(), k -> new HashMap<>())
        .merge(path, 1, Integer::sum);
    saveFrequency();
  }

  // 결과 실행 (타입별 분기)
  public void execute(SearchResult result) {
    switch (result.getType()) {
      case FOLDER -> openFolder(result.getPrimaryValue());
      case APP -> runCommand(result.getPrimaryValue());
      case EDITOR_FOLDER -> runEditorWithFolder(result.getPrimaryValue(), result.getSecondaryValue());
      case GOOGLE -> searchGoogle(result.getPrimaryValue());
      case SITE_SEARCH -> runCommand(result.getPrimaryValue()); // 이미 완성된 URL이라 바로 열기

    }
  }

  private void openFolder(String path) {
    try {
      Runtime.getRuntime().exec(new String[] { "explorer.exe", path });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void runCommand(String command) {
    try {
      Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", "", command });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void runEditorWithFolder(String editorCmd, String folderPath) {
    try {
      Runtime.getRuntime().exec(new String[] { editorCmd, folderPath });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void searchGoogle(String query) {
    try {
      String url = "https://www.google.com/search?q=" +
          java.net.URLEncoder.encode(query, "UTF-8");
      Runtime.getRuntime().exec(new String[] { "cmd", "/c", "start", "", url });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
