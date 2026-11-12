package com.kabata.eigo

// 書式: レベル|英文（空欄は ___）|答え|日本語訳
internal const val QUESTIONS = """
1|I ___ a student.|am|私は学生です。
1|She ___ a cat.|has|彼女はネコを飼っています。
1|This is my ___.|pen|これは私のペンです。
1|I like ___ very much.|apples|私はリンゴが大好きです。
1|He ___ soccer after school.|plays|彼は放課後サッカーをします。
1|What ___ is it now?|time|今何時ですか？
1|My mother is in the ___.|kitchen|母は台所にいます。
1|Please ___ the door.|open|ドアを開けてください。
1|I go to school by ___.|bus|私はバスで学校に行きます。
1|It is ___ today.|sunny|今日は晴れています。
1|We have ___ classes on Monday.|six|月曜日は授業が6つあります。
1|Do you ___ English?|speak|あなたは英語を話しますか？
1|The sky is ___.|blue|空は青い。
1|I ___ up at six every morning.|get|私は毎朝6時に起きます。
1|Let's ___ lunch together.|eat|いっしょに昼ごはんを食べよう。
1|My ___ is Ken.|name|私の名前はケンです。
1|How ___ are you?|old|あなたは何歳ですか？
1|I have two ___.|brothers|私には兄弟が2人います。
1|She can ___ the piano.|play|彼女はピアノをひくことができます。
1|Where do you ___?|live|あなたはどこに住んでいますか？
1|I am ___. Can I have some water?|thirsty|のどがかわきました。水をもらえますか？
1|The dog is under the ___.|table|イヌはテーブルの下にいます。
1|I ___ TV every night.|watch|私は毎晩テレビを見ます。
1|Thank you very ___.|much|どうもありがとう。
1|He is my best ___.|friend|彼は私の親友です。
1|There are seven days in a ___.|week|1週間は7日あります。
1|I usually ___ to bed at ten.|go|私はふだん10時に寝ます。
1|It's cold. Please close the ___.|window|寒いです。窓を閉めてください。
1|My father ___ a car to work.|drives|父は車を運転して仕事に行きます。
1|We ___ in the pool in summer.|swim|私たちは夏にプールで泳ぎます。
1|Turn ___ at the next corner.|left|次の角を左に曲がってください。
1|I want to ___ a doctor.|be|私は医者になりたい。
1|Happy ___ to you!|birthday|お誕生日おめでとう！
1|She is ___ a letter now.|writing|彼女は今、手紙を書いています。
1|I ___ my homework yesterday.|did|私はきのう宿題をしました。
1|Look at that tall ___.|tree|あの高い木を見て。
1|How ___ is this bag?|much|このかばんはいくらですか？
1|I'm ___. I want to eat something.|hungry|おなかがすいた。何か食べたい。
1|Winter is my favorite ___.|season|冬は私の好きな季節です。
1|Can you ___ me?|help|手伝ってくれますか？
1|He ___ to the park last Sunday.|went|彼はこの前の日曜日に公園へ行きました。
1|Please ___ down.|sit|座ってください。
1|This flower is very ___.|beautiful|この花はとても美しい。
1|I ___ a new bike last week.|bought|先週、新しい自転車を買いました。
1|Tom is ___ than Ken.|taller|トムはケンより背が高い。
1|The library is ___ the station.|near|図書館は駅の近くにあります。
1|Wash your ___ before dinner.|hands|夕食の前に手を洗いなさい。
1|It's ___ o'clock.|three|3時です。
1|I can't find my ___. Where is it?|key|かぎが見つからない。どこだろう？
1|We are going to ___ Kyoto next month.|visit|私たちは来月、京都を訪れる予定です。
1|Don't ___ in the classroom.|run|教室で走ってはいけません。
1|Is this your ___? — Yes, it is.|umbrella|これはあなたのかさですか？ーはい、そうです。
1|My sister ___ coffee every morning.|drinks|姉は毎朝コーヒーを飲みます。
1|I listen to ___ every day.|music|私は毎日音楽を聞きます。
1|Which ___ do you like, red or blue?|color|赤と青、どちらの色が好きですか？
1|The train ___ at nine.|leaves|電車は9時に出発します。
1|Good ___. See you tomorrow.|night|おやすみ。また明日。
1|My grandmother is ___ years old.|eighty|祖母は80歳です。
1|I'm sorry I'm ___.|late|遅れてごめんなさい。
1|Let's ___ a picture here.|take|ここで写真をとろう。
2|I have ___ been to Hokkaido.|never|私は北海道に一度も行ったことがありません。
2|This book was ___ by a famous writer.|written|この本は有名な作家によって書かれました。
2|I'm looking ___ to seeing you.|forward|あなたに会えるのを楽しみにしています。
2|She is good ___ cooking.|at|彼女は料理が得意です。
2|It is important ___ study every day.|to|毎日勉強することは大切です。
2|I have lived here ___ ten years.|for|私はここに10年間住んでいます。
2|He has been sick ___ last week.|since|彼は先週からずっと病気です。
2|Do you know the man ___ is standing there?|who|あそこに立っている男の人を知っていますか？
2|If it ___ tomorrow, I will stay home.|rains|もし明日雨なら、家にいます。
2|English is ___ all over the world.|spoken|英語は世界中で話されています。
2|I was very ___ at the news.|surprised|その知らせにとても驚きました。
2|Could you tell me the ___ to the station?|way|駅への道を教えていただけますか？
2|This is the ___ movie I have ever seen.|best|これは今まで見た中で最高の映画です。
2|He runs ___ than anyone in his class.|faster|彼はクラスのだれよりも速く走ります。
2|I don't know ___ to do next.|what|次に何をすべきかわかりません。
2|She ___ me to help her.|asked|彼女は私に手伝ってほしいと頼みました。
2|We ___ to wear uniforms at school.|have|学校では制服を着なければなりません。
2|You ___ not touch this button.|must|このボタンにさわってはいけません。
2|I'm ___ of spiders.|afraid|私はクモがこわい。
2|The game was put ___ because of the rain.|off|試合は雨のため延期されました。
2|Please take ___ your shoes here.|off|ここでくつをぬいでください。
2|I'll ___ you up at the station.|pick|駅まで車で迎えに行きます。
2|He ___ his umbrella on the train.|left|彼は電車にかさを置き忘れました。
2|My hobby is ___ stamps.|collecting|私の趣味は切手を集めることです。
2|Thank you for ___ me.|inviting|招待してくれてありがとう。
2|I want something cold to ___.|drink|何か冷たい飲み物がほしい。
2|The girl ___ by the window is my sister.|sitting|窓のそばに座っている女の子は私の妹です。
2|This is the house ___ my uncle lives.|where|これはおじが住んでいる家です。
2|I have ___ finished my homework.|already|私はもう宿題を終えました。
2|Have you finished it ___?|yet|もうそれを終えましたか？
2|The festival is ___ every summer.|held|その祭りは毎年夏に開かれます。
2|Let's go ___ if it's sunny.|hiking|晴れたらハイキングに行こう。
2|I ___ my keys. I can't open the door.|lost|かぎをなくした。ドアが開けられない。
2|Mt. Fuji is the ___ mountain in Japan.|highest|富士山は日本で一番高い山です。
2|She speaks English as ___ as her teacher.|well|彼女は先生と同じくらい上手に英語を話します。
2|I'm ___ tired to walk.|too|疲れすぎて歩けません。
2|He studied hard ___ that he could pass the exam.|so|彼は試験に合格できるように一生懸命勉強しました。
2|Wait here ___ I come back.|until|私が戻るまでここで待っていて。
2|I lived in Osaka ___ I was a child.|when|子どものころ大阪に住んでいました。
2|My brother is interested ___ science.|in|兄は科学に興味があります。
2|How ___ does it take to get there?|long|そこまでどれくらい時間がかかりますか？
2|How ___ times have you been to Tokyo?|many|東京に何回行ったことがありますか？
2|I'd like to ___ a table for two.|book|2人用のテーブルを予約したいのですが。
2|Could you ___ me the salt?|pass|塩を取っていただけますか？
2|The movie made me ___.|cry|その映画を見て泣きました。
2|My dream came ___.|true|夢がかないました。
2|She ___ out of the room without saying anything.|ran|彼女は何も言わずに部屋から走って出ていきました。
2|Don't be ___ for school.|late|学校に遅れないように。
2|I'll call you ___ I arrive.|after|着いたあとで電話します。
2|Both Ken ___ Mike are from Canada.|and|ケンもマイクもカナダ出身です。
2|You can choose ___ tea or coffee.|either|紅茶かコーヒーのどちらかを選べます。
2|It's getting darker and ___.|darker|だんだん暗くなってきた。
2|I have a lot of things to do ___.|today|今日はやることがたくさんある。
2|The shop is closed ___ Sundays.|on|その店は日曜日は閉まっています。
2|We arrived ___ the airport at noon.|at|私たちは正午に空港に着きました。
2|It's dark. Please turn ___ the light.|on|暗いので電気をつけてください。
2|My computer is out ___ order.|of|私のパソコンは故障しています。
2|Take ___ of yourself.|care|お大事に。（体に気をつけて）
2|I make it a rule to ___ every morning.|jog|私は毎朝ジョギングすることにしています。
2|Practice makes ___.|perfect|習うより慣れよ。（練習が完璧をつくる）
3|I wish I ___ fly like a bird.|could|鳥のように飛べたらいいのに。
3|If I ___ you, I would accept the offer.|were|もし私があなたなら、その申し出を受けるでしょう。
3|She has a ___ for languages.|talent|彼女には語学の才能があります。
3|The ___ of the meeting is to decide the budget.|purpose|会議の目的は予算を決めることです。
3|He apologized ___ being late.|for|彼は遅刻したことを謝った。
3|Smoking is not ___ in this building.|allowed|この建物では喫煙は許されていません。
3|I can't ___ the noise any longer.|stand|これ以上その騒音にがまんできない。
3|She ___ a lot of effort into the project.|put|彼女はその計画に多くの努力を注いだ。
3|We should ___ the environment for future generations.|protect|未来の世代のために環境を守るべきだ。
3|The number of tourists has ___ greatly.|increased|観光客の数は大幅に増えた。
3|It is ___ that he will come.|likely|彼はたぶん来るだろう。
3|I couldn't ___ laughing.|help|笑わずにはいられなかった。
3|This medicine will ___ your headache.|relieve|この薬は頭痛を和らげるでしょう。
3|The train was ___ due to heavy snow.|delayed|大雪のため電車が遅れた。
3|Please ___ in the form and send it back.|fill|用紙に記入して返送してください。
3|I'm not ___ with the result.|satisfied|その結果に満足していない。
3|He gave ___ smoking last year.|up|彼は去年たばこをやめた。
3|She takes ___ her mother.|after|彼女は母親に似ている。
3|We ran ___ of milk.|out|牛乳を切らしてしまった。
3|The concert was a great ___.|success|コンサートは大成功だった。
3|It ___ me two hours to finish the report.|took|レポートを終えるのに2時間かかった。
3|He is ___ of passing the exam.|capable|彼は試験に合格する能力がある。
3|Nothing is more ___ than health.|important|健康ほど大切なものはない。
3|She is ___ about her future.|anxious|彼女は自分の将来を心配している。
3|You had ___ go to the doctor.|better|医者に行ったほうがいい。
3|The more you read, the ___ you learn.|more|読めば読むほど多くを学ぶ。
3|He ___ to have been rich.|seems|彼は昔お金持ちだったようだ。
3|This is the town ___ I was born in.|which|ここは私が生まれた町です。
3|I'm used to ___ up early.|getting|早起きには慣れている。
3|He insisted ___ paying the bill.|on|彼は勘定を払うと言い張った。
3|Her speech made a deep ___ on us.|impression|彼女のスピーチは私たちに深い印象を与えた。
3|I'll ___ you know when I'm ready.|let|準備ができたら知らせます。
3|The price ___ tax is 1,100 yen.|including|税込みの価格は1,100円です。
3|Please ___ your seat belt.|fasten|シートベルトを締めてください。
3|This plan has both ___ and disadvantages.|advantages|この計画には長所と短所の両方がある。
3|He ___ his success to hard work.|owes|彼の成功は努力のおかげだ。
3|We need to ___ a solution to this problem.|find|この問題の解決策を見つける必要がある。
3|She was ___ to tears by the movie.|moved|彼女はその映画に感動して泣いた。
3|Air ___ is getting worse every year.|pollution|大気汚染は年々ひどくなっている。
3|Not only Tom ___ also his sister came.|but|トムだけでなく彼の妹も来た。
3|He spoke slowly ___ that everyone could understand.|so|みんなが理解できるように彼はゆっくり話した。
3|She has been ___ for a job for months.|looking|彼女は何か月も仕事を探している。
3|I should ___ told you the truth.|have|本当のことを言うべきだった。
3|The company ___ 500 people.|employs|その会社は500人を雇っている。
3|Global ___ is a serious problem.|warming|地球温暖化は深刻な問題だ。
3|He was ___ of stealing the money.|accused|彼はお金を盗んだと非難された。
3|You can ___ on me.|count|私を頼りにしていいよ。
3|She ___ the test with a perfect score.|passed|彼女は満点でテストに合格した。
3|I have no ___ but to agree.|choice|同意するしかない。
3|It's no ___ crying over spilt milk.|use|覆水盆に返らず。（こぼれたミルクを嘆いてもむだだ）
3|They have a lot in ___.|common|彼らには共通点が多い。
3|He ___ out the candles on the cake.|blew|彼はケーキのろうそくを吹き消した。
3|I will ___ the meeting at 3 p.m.|attend|午後3時の会議に出席します。
3|The sooner, the ___.|better|早ければ早いほどよい。
3|She ___ in finding a new job.|succeeded|彼女は新しい仕事を見つけることに成功した。
3|This road ___ to the station.|leads|この道は駅に通じている。
3|Don't ___ the chance.|miss|そのチャンスを逃すな。
3|He is ___ known as a great scientist.|widely|彼は偉大な科学者として広く知られている。
3|Time ___ when you are having fun.|flies|楽しいと時間はあっという間に過ぎる。
3|Actions speak louder than ___.|words|言葉より行動が大事。
4|The meeting has been ___ until next Friday.|postponed|会議は来週の金曜日まで延期された。
4|Please ___ the attached document carefully.|review|添付書類をよく確認してください。
4|All employees must ___ with the safety rules.|comply|全従業員は安全規則に従わなければならない。
4|The new policy will take ___ next month.|effect|新しい方針は来月から施行される。
4|We are ___ to announce our new product.|pleased|新製品を発表できることをうれしく思います。
4|The event was called ___ due to the storm.|off|嵐のためイベントは中止になった。
4|She was ___ to manager last year.|promoted|彼女は去年、部長に昇進した。
4|Customers can ___ a refund within 30 days.|request|お客様は30日以内に返金を請求できます。
4|Her presentation was very ___.|persuasive|彼女のプレゼンはとても説得力があった。
4|The museum is ___ accessible by train.|easily|その美術館へは電車で簡単に行ける。
4|We need to ___ costs by 10 percent.|reduce|コストを10%削減する必要がある。
4|Please ___ from using your phone during the show.|refrain|公演中は携帯電話の使用をお控えください。
4|The project was completed ahead of ___.|schedule|プロジェクトは予定より早く完了した。
4|The contract will ___ at the end of the year.|expire|契約は年末に満了する。
4|He is ___ for the sales department.|responsible|彼は営業部の責任者だ。
4|The company's profits rose ___ last year.|sharply|会社の利益は昨年急増した。
4|Applicants must have at least three years of ___.|experience|応募者は最低3年の経験が必要です。
4|The hotel offers a free shuttle ___.|service|そのホテルは無料のシャトルサービスを提供している。
4|Could you ___ the documents by Friday?|submit|金曜日までに書類を提出していただけますか？
4|The store is ___ located near the station.|conveniently|その店は駅の近くの便利な場所にある。
4|We ___ your patience.|appreciate|ご辛抱いただき感謝いたします。
4|The ___ will be held in the main hall.|conference|会議は大ホールで開かれます。
4|Tickets are ___ online.|available|チケットはオンラインで入手できます。
4|Please ___ your receipt.|keep|レシートは保管しておいてください。
4|She has a ___ knowledge of history.|profound|彼女は歴史について深い知識を持っている。
4|The two companies agreed to ___.|merge|その2社は合併することに合意した。
4|The weather ___ says it will rain tomorrow.|forecast|天気予報によると明日は雨だ。
4|His opinion is ___ from mine.|different|彼の意見は私のとは異なる。
4|This product is ___ to that one in quality.|superior|この製品は品質の点であれより優れている。
4|The government ___ new measures against the virus.|introduced|政府はウイルスに対する新たな対策を導入した。
4|The ___ of this city is about one million.|population|この都市の人口は約100万人だ。
4|She made a ___ to study abroad.|decision|彼女は留学する決心をした。
4|Regular exercise ___ to good health.|contributes|定期的な運動は健康に役立つ。
4|The ___ of living in Tokyo is high.|cost|東京の生活費は高い。
4|The ancient temple ___ the earthquake.|survived|その古い寺は地震を生き延びた。
4|We ___ the problem from every angle.|examined|私たちはあらゆる角度からその問題を検討した。
4|He ___ from his position as president.|resigned|彼は社長の職を辞任した。
4|The scientist made an important ___.|discovery|その科学者は重要な発見をした。
4|Our sales ___ last year's by 20 percent.|exceeded|売上は前年を20%上回った。
4|The project requires careful ___.|planning|そのプロジェクトには慎重な計画が必要だ。
4|Please ___ me if you have any questions.|contact|ご質問があればご連絡ください。
4|Our ___ hours are from 9 a.m. to 5 p.m.|business|営業時間は午前9時から午後5時です。
4|He tends to ___ the importance of sleep.|underestimate|彼は睡眠の大切さを過小評価しがちだ。
4|The city is famous for its ___ heritage.|cultural|その都市は文化遺産で有名だ。
4|The team worked hard to ___ the deadline.|meet|チームは締め切りに間に合わせるため懸命に働いた。
4|We offer a wide ___ of products.|range|当社は幅広い製品を提供しています。
4|Please make sure the door is ___ before leaving.|locked|出かける前にドアにかぎがかかっているか確認してください。
4|This software is ___ with most computers.|compatible|このソフトはほとんどのコンピューターと互換性がある。
4|The ___ of the survey will be published next week.|results|調査の結果は来週公表される。
4|He gave a ___ answer to the question.|vague|彼はその質問にあいまいな答えをした。
4|The festival ___ thousands of visitors every year.|attracts|その祭りは毎年何千人もの観光客を引きつける。
4|Prices are ___ to change without notice.|subject|価格は予告なく変更されることがあります。
4|She is ___ in three languages.|fluent|彼女は3か国語を流ちょうに話す。
4|We must ___ the risk of accidents.|minimize|事故の危険を最小限にしなければならない。
4|The building was ___ in 1920.|constructed|その建物は1920年に建てられた。
4|The manager ___ the staff for their hard work.|praised|部長はスタッフの努力をほめた。
4|Recent ___ in technology have changed our lives.|advances|最近の技術の進歩は私たちの生活を変えた。
4|The ___ was signed by both parties.|agreement|その合意書は双方によって署名された。
4|He is an ___ in artificial intelligence.|expert|彼は人工知能の専門家だ。
4|Where there's a will, there's a ___.|way|意志あるところに道は開ける。
"""
