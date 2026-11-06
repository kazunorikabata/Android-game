# てくてくさんぽ 🐾

歩くと旅が進む、お散歩ゲームの Android アプリです（Kotlin + Jetpack Compose）。

## あそびかた
- スマホの歩数センサーで歩数を数えます（アプリを閉じていても、次に開いたときに反映）。
- **たびの道**：歩くほど「おうち → 公園 → パン屋さん → … → 虹のはし」と新しい場所に到着。
- **あいぼう「テクトリ」**：合計歩数で たまご → ひな → … と成長。
- **きょうの目標**：1日 5,000 歩。達成した日数を記録。
- デバッグ版には「テスト +100歩」ボタンがあり、歩かずに動作確認できます。

## APK の入手
GitHub に push すると GitHub Actions がビルドします。
Actions タブ → 「Build APK」の実行結果 → Artifacts の `tekuteku-sanpo-debug-apk` をダウンロード。

## 端末へのインストール（ワイヤレスデバッグ）
同じネットワーク（Tailscale など）にある PC から:

```sh
adb connect <端末のIP>:<ポート>
adb install -r app-debug.apk
```

## 手元でビルド
Android Studio でこのフォルダを開くか、`./gradlew assembleDebug`。
