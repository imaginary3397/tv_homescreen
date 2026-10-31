# Startskärm för Sony KD-55A1

En egen startskärm för Android TV. Den har en videobakgrund, en klocka och en rad med favoritappar.

## Det här behövs

- Datorn (Windows) och tv:n på samma nätverk
- Utvecklarläget och USB-felsökning påslaget på tv:n (det har du redan gjort)
- Tv:ns IP-adress
- Android Studio (gratis): https://developer.android.com/studio

## 1. Öppna projektet

1. Packa upp zip-filen, till exempel till `C:\Projekt\Startskarm`.
2. Starta Android Studio och välj **Open**. Peka på mappen `Startskarm`, alltså den som innehåller `settings.gradle`.
3. Vänta medan Android Studio laddar ner det som behövs. Det står "Gradle sync" längst ner, och första gången kan det ta några minuter. Om Android Studio föreslår att uppdatera "Android Gradle Plugin" går det bra att säga ja.

## 2. Anslut till tv:n

Öppna **Terminal** längst ner i Android Studio och skriv följande. Byt ut IP-adressen mot tv:ns.

```
cd $env:LOCALAPPDATA\Android\Sdk\platform-tools
.\adb connect 192.168.1.47
```

Tv:n frågar om felsökning ska tillåtas. Bocka i **Tillåt alltid från den här datorn** och tryck **OK**. Kör sedan `.\adb devices`. Där ska tv:n stå som `device`.

## 3. Installera och starta

Välj tv:n i listan över enheter högst upp i Android Studio. Den heter ungefär "Sony BRAVIA 4K". Tryck sedan på den gröna ▶-knappen (Run). Appen byggs, installeras och startar på tv:n.

Appen syns nu också i tv:ns vanliga applista som **Startskärm**.

## 4. Gör den till startskärm (hemknappen)

Kör det här i samma terminal:

```
.\adb shell cmd package set-home-activity se.hemma.startskarm/.HomeActivity
```

Tryck på hemknappen på fjärrkontrollen. Om din startskärm visas är du klar.

**Om tv:ns vanliga startskärm ändå visas** håller den fast vid hemknappen. Då kan du stänga av den. Ta först reda på vad den heter:

```
.\adb shell pm list packages launcher
```

Den heter troligen `com.google.android.tvlauncher` eller `com.google.android.leanbacklauncher`. Stäng av den:

```
.\adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Inget tas bort. Den är bara avstängd.

### Ångra

Så här får du tillbaka den vanliga startskärmen:

```
.\adb shell pm enable com.google.android.tvlauncher
```

Kör sedan `.\adb uninstall se.hemma.startskarm` om du vill ta bort appen helt.

## Ändra saker

| Vad | Var |
|---|---|
| Vilka appar som visas och i vilken ordning | `app/src/main/java/se/hemma/startskarm/Apps.java`, listan `FAVORITES` |
| Färgen på markeringen | `app/src/main/res/values/colors.xml`, `accent` |
| Storlek och placering | `app/src/main/res/layout/activity_home.xml` |

Tryck på ▶ igen efter en ändring. Då installeras den nya versionen.

Apparna hittas på sitt namn. Om någon favorit inte syns beror det på att namnet på tv:n inte matchar sökorden i `FAVORITES`. Titta på vad appen heter under **Alla appar** och lägg till det namnet som sökord.

## Bakgrundsklippen

Klippen ligger i mappen `klipp/` i repot. De spelas i namnordning, `01_…`, `02_…` och så vidare, och slingan börjar om när den sista har spelats.

**Lägga till ett klipp:** ladda upp filen till mappen `klipp` på GitHub (Add file → Upload files) med ett namn som styr var i ordningen den hamnar. När bygget är klart hämtar du den nya versionen med Downloader. Ljudet tas bort, bilden skalas ner till högst 1920 px och tonningar läggs på automatiskt. Det behöver du inte göra själv.

**Säsongsklipp:** ett klipp med `halloween` i namnet spelas bara 24 oktober–1 november, till exempel `06b_halloween_pumpaasteroid.mp4`.

**Prova utan att bygga om:** filer i `/sdcard/Android/data/se.hemma.startskarm/files/videos/` på tv:n används i stället för de inbyggda, så länge det ligger minst en fil där.

## Bra att veta

- Skärmsläckaren påverkas inte. Appen håller inte skärmen vaken, så Google Photos-albumet visas som vanligt.
- Videon pausas när du öppnar en app och när skärmsläckaren startar.
- Tillbaka-knappen gör ingenting på startskärmen. Det är meningen.
- **Alla appar** och **Inställningar** nås genom att trycka uppåt från appraden.
