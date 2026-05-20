# FotoCommesse

App Android per fotografare blocchi di marmo (o qualsiasi altro oggetto) raggruppando
gli scatti per numero di commessa. Le foto sono salvate in `Pictures/FotoCommesse/`
con il pattern **`{commessa}_{N}.jpg`** dove `N` è progressivo e parte da 1, riprendendo
correttamente se si torna a fotografare la stessa commessa giorni dopo.

```
/storage/emulated/0/Pictures/FotoCommesse/
├── 12345_1.jpg
├── 12345_2.jpg
├── 12345_3.jpg
├── 12346_1.jpg
└── 12346_2.jpg
```

La cartella è una directory pubblica standard, quindi può essere sincronizzata
con qualsiasi servizio cloud (Box, Dropbox, Google Drive, OneDrive) usando il
loro client ufficiale o un'app come FolderSync.

---

## Schermate dell'app

**Schermata principale**
- Campo per inserire il numero di commessa
- Pulsante "INIZIA SCATTI" che apre la fotocamera
- Lista delle commesse già fotografate con conteggio foto (tap per riprendere)

**Schermata fotocamera**
- Anteprima live a tutto schermo
- In alto: numero commessa e contatore (es. "Scattate: 3 — Prossima: #4")
- In basso: pulsante FINE (torna alla home) e pulsante scatto

---

## Setup iniziale del repository

1. Crea un repository **privato** su GitHub (es. `fotocommesse-android`).
2. Carica tutti i file di questo progetto nella root del repo.
   Da terminale, dopo aver clonato il repo vuoto:
   ```bash
   cp -r * /percorso/clone/repo/
   cp .gitignore /percorso/clone/repo/
   cd /percorso/clone/repo
   git add .
   git commit -m "Initial commit"
   git push
   ```
3. Vai nel tab **Actions** del repo su GitHub. Il workflow "Build APK" partirà
   automaticamente al primo push.
4. Al termine (ci vogliono 4-6 minuti la prima volta, poi 2-3), clicca sull'esecuzione
   del workflow → scorri fino alla sezione **Artifacts** → scarica
   `FotoCommesse-debug-apk`. Dentro c'è l'APK.

## Installazione sul telefono

L'APK è **non firmato per il Play Store** (è un APK "debug" auto-firmato).
Va installato manualmente:

1. Trasferisci l'APK sul telefono (via Box, Dropbox, email, USB, ecc.).
2. Sul telefono, apri il file: Android chiederà di abilitare
   *"Origini sconosciute"* per il file manager che stai usando.
3. Conferma l'installazione.

## Uso dell'app

1. **Prima apertura**: l'app chiede i permessi *Fotocamera* e
   *Lettura immagini* (per il counter resume). Accetta entrambi.
2. **Inserisci il numero di commessa** (es. `12345`) e premi **INIZIA SCATTI**.
3. Scatta tutte le foto necessarie per quel blocco. Il contatore si aggiorna in tempo
   reale e ogni foto è salvata immediatamente come `12345_1.jpg`, `12345_2.jpg`, ecc.
4. Premi **FINE** per tornare alla home, dove puoi cambiare commessa.
5. Se torni a fotografare la commessa `12345` dopo giorni, il contatore riparte
   dal valore corretto (es. `12345_4.jpg`).

Caratteri non alfanumerici (a parte `_` e `-`) vengono sostituiti con `_` automaticamente
e l'app chiede conferma del nome risultante prima di procedere.

## Sincronizzazione con Box / Dropbox / Drive

### Opzione A — Client ufficiale del servizio (più semplice)
- **Box**: nell'app Box → impostazioni → Upload da cartella → seleziona
  `Pictures/FotoCommesse`.
- **Dropbox**: l'opzione di upload automatico è stata limitata da Android 11 in poi;
  conviene usare FolderSync (vedi sotto).
- **Google Drive**: simile a Dropbox, è limitato; usa FolderSync.

### Opzione B — FolderSync Lite (gratis, più flessibile)
1. Installa **FolderSync Lite** dal Play Store.
2. Aggiungi un account (Box, Dropbox, Drive, OneDrive — supporta tutti).
3. Crea una "Folderpair":
   - Cartella locale: `Pictures/FotoCommesse`
   - Cartella remota: a scelta (es. `/Margraf/foto_blocchi/`)
   - Direzione: **Solo upload** (consigliato; bidirezionale rischia cancellazioni)
   - Trigger: **Su modifica file** (sincronizzazione quasi in tempo reale) o
     a tempo (ogni ora, solo Wi-Fi).
4. Le nuove foto vengono caricate automaticamente in cloud non appena scattate.

## Personalizzazione

| Cosa cambiare | File |
|---|---|
| Nome dell'app | `app/src/main/res/values/strings.xml` → `app_name` |
| Nome cartella di salvataggio | `PhotoStorage.kt` → `FOLDER_NAME` |
| Pattern del nome file | `PhotoStorage.buildContentValues()` |
| Colori del tema | `colors.xml` |
| Package / applicationId | `app/build.gradle.kts` (e refactor del package Kotlin) |
| Versione mostrata | `app/build.gradle.kts` → `versionName`, `versionCode` |

Dopo qualsiasi modifica, basta `git push`: GitHub Actions ricompila e produce
un nuovo APK.

## Build release firmato (opzionale, per distribuzione)

L'APK debug funziona perfettamente per uso interno. Se in futuro vorrai un APK
release firmato:

1. Genera una keystore locale:
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias fotocommesse -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Codifica la keystore in base64 e mettila come secret GitHub:
   ```bash
   base64 release.keystore | tr -d '\n' > release.keystore.b64
   ```
   Su GitHub → Settings → Secrets → New repository secret:
   - `KEYSTORE_BASE64` = contenuto di `release.keystore.b64`
   - `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
3. Aggiorna `app/build.gradle.kts` con la signing config e il workflow per usare
   i secret (chiedi a Claude di farlo).

## Specifiche tecniche

- **Linguaggio**: Kotlin 2.0
- **Min SDK**: 29 (Android 10) — necessario per MediaStore RELATIVE_PATH
- **Target SDK**: 35 (Android 15)
- **Fotocamera**: CameraX 1.4.0
- **Build**: Gradle 8.10.2, AGP 8.6.1
- **JDK richiesto in CI**: 17

## Struttura del progetto

```
fotocommesse/
├── .github/workflows/build.yml       # CI: genera l'APK ad ogni push
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/it/apconsulting/fotocommesse/
│       │   ├── MainActivity.kt       # Schermata input commessa + lista
│       │   ├── CameraActivity.kt     # Fotocamera + scatto
│       │   ├── CommesseAdapter.kt    # Adapter RecyclerView
│       │   └── PhotoStorage.kt       # MediaStore: lettura/scrittura
│       └── res/                      # Layout, strings, theme, icona
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── README.md
```

## Note

- L'app crea la cartella `FotoCommesse` automaticamente al primo scatto.
- Le foto includono i metadati EXIF standard (data, orientamento, GPS se attivo).
- Per attivare il GPS sulle foto, ti serve aggiungere la gestione esplicita
  in `CameraActivity.kt` con `ImageCapture.Metadata`; di default CameraX non
  inserisce coordinate.
- Il contatore "prossima foto" parte da 1 se la commessa è nuova, oppure
  da `max(N)+1` se ci sono già foto per quella commessa nella cartella.
