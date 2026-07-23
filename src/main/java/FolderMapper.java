import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class FolderMapper {

  public static void Run() {
    // 탐색할 루트 디렉토리 경로 (사용자 환경에 맞게 수정)
    String targetDirectoryPath = "C:/4rp2";
    // 저장할 JSON 파일 경로
    String outputJsonPath = "C:/4rp2/dev/developing/assistant/src/main/resources/json/dirs.json";

    Map<String, List<String>> folderMap = new HashMap<>();

    try (Stream<Path> paths = Files.walk(Paths.get(targetDirectoryPath))) {
      paths.filter(Files::isDirectory)
          .forEach(path -> {
            String folderName = path.getFileName().toString();
            String fullPath = path.toAbsolutePath().toString();

            // 폴더 이름을 키로 하고, 경로들을 리스트로 추가
            if (!fullPath.matches(".*(node_modules|git|godot).*")) {
              folderMap.computeIfAbsent(folderName, k -> new ArrayList<>()).add(fullPath);
            }
          });

      // JSON으로 저장
      ObjectMapper mapper = new ObjectMapper();
      // 예쁘게 출력(Pretty Print)하기 위한 설정
      mapper.enable(SerializationFeature.INDENT_OUTPUT);

      mapper.writeValue(new File(outputJsonPath), folderMap);
      System.out.println("JSON 저장 완료: " + outputJsonPath);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
