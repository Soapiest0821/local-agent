public class SearchResult {
  public enum Type {
    FOLDER, APP, EDITOR_FOLDER, GOOGLE, SITE_SEARCH
  }

  private final String displayText;
  private final Type type;
  private final String primaryValue; // 폴더 경로 / 앱 경로 / 에디터 커맨드
  private final String secondaryValue; // editor일 때만 폴더 경로

  public SearchResult(String displayText, Type type, String primaryValue, String secondaryValue) {
    this.displayText = displayText;
    this.type = type;
    this.primaryValue = primaryValue;
    this.secondaryValue = secondaryValue;
  }

  public String getDisplayText() {
    return displayText;
  }

  public Type getType() {
    return type;
  }

  public String getPrimaryValue() {
    return primaryValue;
  }

  public String getSecondaryValue() {
    return secondaryValue;
  }

  @Override
  public String toString() {
    return displayText;
  } // ListView에 이 텍스트로 표시됨
}
