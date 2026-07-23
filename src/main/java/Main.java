import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.ArrayList;

public class Main extends Application {

  private MacroManager macro;
  private ListView<String> resultList;

  @Override
  public void start(Stage stage) {
    // FolderMapper.Run();
    macro = new MacroManager("src/main/resources/json/commands.json");

    // 입력창
    TextField inputField = new TextField();
    inputField.setPromptText("여기에 입력하세요");
    inputField.setPrefWidth(350);
    inputField.setPrefHeight(45);
    inputField.setStyle("-fx-font-size: 16px;");

    // 결과 리스트
    resultList = new ListView<>();
    resultList.setPrefHeight(90); // 3개 정도 보이는 높이
    resultList.setMaxWidth(350);
    resultList.setVisible(false);
    resultList.setManaged(false);

    resultList.setCellFactory(lv -> {
      ListCell<String> cell = new ListCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);
          setText(empty ? null : item);
          setStyle(
              "-fx-background-color: transparent;" +
                  "-fx-background-radius: 10;" +
                  "-fx-text-fill: white;" +
                  "-fx-font-size: 14px;" +
                  "-fx-padding: 8 12 8 12;");
        }
      };
      cell.setOnMouseEntered(e -> {
        if (!cell.isEmpty()) {
          resultList.getSelectionModel().select(cell.getIndex());
        }
      });
      return cell;
    });

    // 입력할 때마다 검색 결과 갱신
    inputField.textProperty().addListener((obs, oldText, newText) -> {
      String trimmed = newText.trim();
      List<String> allResults = macro.getAllResultsSorted(trimmed);

      if (trimmed.isEmpty()) {
        resultList.setVisible(false);
        resultList.setManaged(false);
        return;
      }

      List<String> displayList = new ArrayList<>(allResults);
      displayList.add("🔍 " + trimmed + " 검색하기!"); // 맨 밑에 항상 추가

      resultList.getItems().setAll(displayList);
      resultList.getSelectionModel().select(0); // 기본 선택 = 1위 (결과 있으면 결과, 없으면 검색하기)
      resultList.setVisible(true);
      resultList.setManaged(true);
    });

    // 위/아래/엔터 처리
    inputField.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ESCAPE) {
        stage.close();
        return;
      }

      if (!resultList.isVisible())
        return;

      int currentIndex = resultList.getSelectionModel().getSelectedIndex();
      int size = resultList.getItems().size();

      if (e.getCode() == KeyCode.DOWN) {
        resultList.getSelectionModel().select(Math.min(currentIndex + 1, size - 1));
        e.consume();
      } else if (e.getCode() == KeyCode.UP) {
        resultList.getSelectionModel().select(Math.max(currentIndex - 1, 0));
        e.consume();
      } else if (e.getCode() == KeyCode.ENTER) {
        String selected = resultList.getSelectionModel().getSelectedItem();
        String query = inputField.getText().trim();

        if (selected != null) {
          if (selected.startsWith("🔍")) {
            // "검색하기!" 항목 선택된 경우
            macro.searchGoogle(query);
          } else {
            // 실제 폴더 경로 선택된 경우
            macro.incrementFrequency(query, selected);
            macro.openFolder(selected);
          }
        }

        inputField.clear();
        resultList.setVisible(false);
        resultList.setManaged(false);
        e.consume();
      }
    });

    // 레이아웃 배치 (왼쪽 위쪽에 위치)
    VBox topBox = new VBox(10, inputField, resultList);
    topBox.setAlignment(Pos.CENTER);
    topBox.setPadding(new Insets(100, 0, 0, 0));

    BorderPane root = new BorderPane();
    root.setTop(topBox);
    root.setStyle(
        "-fx-background-color: rgba(255, 181, 217, 0.5);" +
            "-fx-background-radius: 20;" +
            "-fx-border-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.2);" +
            "-fx-border-width: 1;");

    // Scene & Stage 설정
    Scene scene = new Scene(root, 600, 400);
    scene.getStylesheets().add(getClass().getResource("/style/style.css").toExternalForm());
    scene.setFill(Color.TRANSPARENT);

    stage.initStyle(StageStyle.TRANSPARENT); // 프레임 제거 + 투명
    stage.setScene(scene);
    stage.setX(800); // 왼쪽 위 위치
    stage.setY(120);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
