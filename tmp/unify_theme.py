from pathlib import Path
import re

base = Path('Client/src/vcampus/client/view')
def edit(path, transform):
    raw = path.read_bytes()
    text = raw.decode('utf-8')
    ending = '\r\n' if b'\r\n' in raw else '\n'
    text = text.replace('\r\n', '\n')
    result = transform(text)
    if result != text:
        path.write_bytes(result.replace('\n', ending).encode('utf-8'))

palette = {
 '#1a73e8':'#3f8f46', '#1a237e':'#203629', '#4caf50':'#3f8f46',
 '#607d8b':'#477a61', '#ff9800':'#c88a18', '#f44336':'#c43c3c',
 '#f5f7fa':'#f4f8f1', '#94b55a':'#3f8f46', '#77b55a':'#3f8f46',
 '#7f9f4d':'#32783b', '#6b8a40':'#2f7540', '#cce0ed':'#d7e4d3',
 '#f4f8fb':'#f4f8f1', '#e8f0f7':'#edf4e9', '#e8f2fc':'#f3f8f0',
 '#c5e0fa':'#e4f0df', '#446078':'#526457', '#2980b9':'#3f8f46',
 '#2196f3':'#477a61', '#27ae60':'#3f8f46', '#2d6a9f':'#2f7540',
 '#eef3f8':'#f4f8f1', '#d97a4a':'#3f8f46', '#fbeee6':'#e8f2e3',
 '#f4f6f8':'#f4f8f1', '#9a8f88':'#647469', '#3a2e28':'#203629', '#7a6f68':'#647469',
}
def recolor(t, colors):
    return re.sub(r'#[0-9a-fA-F]{6}', lambda m: colors.get(m[0].lower(),m[0]), t)
for path in [* (base/'Library').glob('*.java'),base/'HospitalFrame.java',base/'LoginFrame.java',base/'ai/AIChatPanel.java']:
    edit(path, lambda t: recolor(t,palette))

# All independent windows and modal dialogs receive the same baseline.
for path in base.rglob('*.java'):
    if 'Test' in path.name or path.name == 'CampusTheme.java': continue
    def wire(t):
        t = re.sub(r'(\bScene scene = new Scene\([^;]+;)',r'\1\n        vcampus.client.view.CampusTheme.apply(scene);',t)
        # Dialog styles are installed immediately before display, including late-created dialogs.
        t = re.sub(r'(?m)^(\s*)(?=(?:Optional<[^\n]+? = )?(\w+)\.showAndWait\(\))',
            lambda m: m[1]+'vcampus.client.view.CampusTheme.apply('+m[2]+');\n'+m[1] if m[2] not in ('stage','dialogStage') else m[0],t)
        return t
    edit(path,wire)

edit(base/'HospitalFrame.java',lambda t: re.sub(r'        root.setStyle\(""".*?"""\);',
    '        CampusTheme.apply(this);',t,count=1,flags=re.S)
    .replace('banner.setStyle("-fx-background-color:#ffffff;-fx-border-color:#d7e4d3;-fx-border-width:0 0 1 0;");','banner.getStyleClass().add("campus-header");')
    .replace('systemTitle.setTextFill(Color.web(themePrimaryColor));','systemTitle.setTextFill(Color.WHITE);')
    .replace('userTopTip.setTextFill(Color.web("#526457"));','userTopTip.setTextFill(Color.WHITE);')
    .replace('new HBox();\n        banner.setAlignment','new HBox(12);\n        banner.setAlignment')
    .replace('ImageView logoView = new ImageView();','ImageView logoView = new ImageView();\n        logoView.setFitWidth(42);\n        logoView.setFitHeight(42);\n        logoView.setPreserveRatio(true);'))

edit(base/'Library/LibraryPanel.java',lambda t: t
    .replace('        initUI();','        vcampus.client.view.CampusTheme.apply(this);\n        initUI();',1)
    .replace('titleLabel.setStyle("-fx-text-fill: #203629;");','titleLabel.setStyle("-fx-text-fill: white;");')
    .replace('subtitleLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");','subtitleLabel.getStyleClass().add("campus-subtitle");\n        subtitleLabel.setWrapText(true);')
    .replace('headerBox.setStyle("-fx-background-color: #f4f8f1;");','headerBox.getStyleClass().add("campus-header");')
    .replace('new HBox(0, libraryTab, borrowTab)','new HBox(6, libraryTab, borrowTab)')
    .replace('tabBox.setPadding(new Insets(0, 30, 0, 30));','tabBox.setPadding(new Insets(10, 22, 10, 22));')
    .replace('tabBox.setStyle("-fx-background-color: white;");','tabBox.setStyle("-fx-background-color: #f4f8f1;");')
    .replace('return "-fx-background-color: transparent; " +','return "-fx-background-color: " + (active ? "#3f8f46" : "transparent") + "; -fx-background-radius: 9; " +')
    .replace('(active ? "#3f8f46" : "#666666")','(active ? "white" : "#526457")')
    .replace('"-fx-border-width: 0 0 3 0; "','"-fx-border-width: 0; "'))

edit(base/'MainFrame.java',lambda t:t.replace('wrapper.setMaxWidth(900);','wrapper.setMaxWidth(1120);\n        wrapper.setMaxHeight(Double.MAX_VALUE);\n        VBox.setVgrow(libraryPanel, javafx.scene.layout.Priority.ALWAYS);'))
edit(base/'StudentManagementFrame.java',lambda t:t.replace('_root = new BorderPane();','_root = new BorderPane();\n        CampusTheme.apply(_root);'))
edit(base/'StoreFrame.java',lambda t:t.replace('private void applyStylesheet(Parent root) {','private void applyStylesheet(Parent root) {\n        CampusTheme.apply(root);'))
edit(base/'ai/AIChatPanel.java',lambda t:t.replace('this._currentUser = currentUser;','this._currentUser = currentUser;\n        vcampus.client.view.CampusTheme.apply(this);')
    .replace('Region background = buildBackground();','Region background = new Region();\n        background.setStyle("-fx-background-color: #f4f8f1;");')
    .replace('tipBar.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 12;");','tipBar.getStyleClass().add("campus-header");\n        tip.setStyle("-fx-text-fill: white;");'))

store = {'#0f7a72':'#2f7d4b','#2fb3a0':'#78a943','#2fa89a':'#3f8f46','#0d6b64':'#32783b',
 '#12564e':'#2f7540','#18786c':'#2f7540','#1f7f72':'#32783b','#1f8f80':'#3f8f46',
 '#f2f6fb':'#f4f8f1','#17233a':'#203629','#27384e':'#304137','#33475f':'#38543e',
 '#4c5e75':'#526457','#5b6b80':'#647469','#cad7e6':'#cbdcc7','#cbd8e6':'#cbdcc7',
 '#dbe5f0':'#d7e4d3','#e2eaf3':'#e9eee7','#e2eaf4':'#e9eee7','#dff1ee':'#e8f2e3',
 '#e0f1ed':'#e8f2e3','#d8ece8':'#e4f0df','#e9f5f3':'#f3f8f0','#d3f2ec':'#edf5e7',
 '#a9e3d8':'#dceccf','#f7fafd':'#f6faf3','#eef3f9':'#edf4e9','#2f72d6':'#3f8f46'}
edit(base/'store.css',lambda t:recolor(t,store).replace('-fx-background-radius: 0 0 22 22;','-fx-background-radius: 18 18 0 0;'))
student={'#285d4c':'#3f8f46','#214e40':'#32783b','#1a3f34':'#2f7540','#1e4f40':'#2f7d4b',
 '#3f8a68':'#78a943','#f4f7f5':'#f4f8f1','#f6f8f7':'#f6faf3','#edf1ef':'#edf4e9',
 '#d5ddd9':'#d7e4d3','#202a26':'#203629','#55635d':'#526457','#e5eee9':'#e8f2e3','#3b6c54':'#6aa04a'}
edit(base/'student-management.css',lambda t:recolor(t,student))
edit(Path('build.bat'),lambda t:t.replace('echo Build succeeded.', 'copy /y "Client\\src\\vcampus\\client\\view\\campus-theme.css" "bin\\vcampus\\client\\view\\campus-theme.css" >nul\nif errorlevel 1 exit /b 1\n\necho Build succeeded.'))
