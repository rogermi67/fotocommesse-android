# Foto Blocchi

App Android per fotografare blocchi di marmo (o qualsiasi oggetto) raggruppando
gli scatti per numero di commessa. Le foto sono salvate in
`Pictures/<cartella_configurabile>/` con il pattern **`{commessa}_{N}.jpg`**
dove `N` è progressivo e parte da 1, riprendendo correttamente se si torna a
fotografare la stessa commessa giorni dopo.

```
/storage/emulated/0/Pictures/FotoBlocchi/   (default; modificabile da Impostazioni)
├── 12345_1.jpg
├── 12345_2.jpg
├── 12345_3.jpg
├── 12346_1.jpg
└── 12346_2.jpg
```

La cartella è una directory pubblica standard, quindi può essere sincronizzata
con qualsiasi servizio cloud (Box, Dropbox, Google Drive, OneDrive) usando il
loro client ufficiale o FolderSync.

---

## Novità v1.1

- Nome app rinominato in **"Foto Blocchi"**
- **Schermata Impostazioni** (icona ingranaggio in alto a destra): nome della
  cartella di destinazione personalizzabile
- Toolbar Material visibile in tutte le schermate
- Il package interno resta `it.apconsulting.fotocommesse` per consentire
  l'aggiornamento in-place sopra la v1.0 (non serve disinstallare)

---

## Build dell'APK su GitHub Actions

Il repo include un workflow `.github/workflows/build.yml` che compila l'APK
automaticamente a ogni push su `main`.

1. Push delle modifiche → vai nel tab **Actions** del repo
2. Apri l'ultimo run con la spunta verde → scorri in fondo
3. Sezione **Artifacts** → scarica `FotoCommesse-debug-apk` (è uno zip)
4. Estrai lo zip → ottieni `FotoCommesse-debug-<run_number>.apk`

Tempi: primo build 4-8 minuti, successivi 2-4 minuti (cache).

---

## Installazione sul telefono

L'APK è **debug** (auto-firmato), non passa dal Play Store. Va installato manualmente:

1. Trasferisci l'APK sul telefono (Box, Dropbox, email, USB, ...)
2. Sul telefono apri il file: Android chiede di abilitare *"Installa app sconosciute"*
   per il file manager che stai usando — autorizza
3. Conferma l'installazione

**Aggiornamento da una versione precedente**: poiché il package e il certificato
debug sono gli stessi, basta installare il nuovo APK sopra quello vecchio.
Le impostazioni (cartella personalizzata) e le foto già scattate restano intatte.

---

## Uso dell'app

### Schermata principale
1. Inserisci il numero di commessa (es. `12345`)
2. **INIZIA SCATTI** apre la fotocamera
3. La lista in basso mostra le commesse con foto già salvate; tap su una
   commessa per riportarla nel campo di input

### Schermata Impostazioni (icona ingranaggio)
- **Cartella di destinazione**: modifica il nome della sottocartella in `Pictures/`.
  Default `FotoBlocchi`. Caratteri non validi sostituiti con `_` in automatico.
- Anteprima del percorso aggiornata in tempo reale
- **Sincronizzazione cloud**: nota informativa con link al README
- **Informazioni**: versione installata

### Schermata fotocamera
- Anteprima live a tutto schermo
- In alto: numero commessa e contatore (`Scattate: 3 — Prossima: #4`)
- In basso: pulsante FINE (torna alla home) e pulsante scatto

---

## Architettura di sincronizzazione cloud

Pipeline consigliata per arrivare dalle foto sul telefono fino al server aziendale:

```
[Telefono]                  [Cloud]              [Server aziendale]
   │                          │                        │
App Foto Blocchi              │                        │
   ↓ (scrive locale)          │                        │
Pictures/FotoBlocchi/  ──→  Box  ──→  Box Drive (Windows)
   │   FolderSync             │           sincronizza in
   │   (upload only)          │           \\SERVER\share\foto_blocchi\
   ↓                          │                        ↓
                                            cartella locale del server,
                                            accessibile da Mago4 / script Python / SQL
```

**Perché questa pipeline e non integrazione Box diretta nell'app:**
- L'app continua a funzionare offline (in cava/piazzale può non esserci 4G)
- FolderSync è battle-tested e gestisce retry/queue/limitazione banda
- Cambiare provider cloud non richiede ricompilare l'app

### Account cloud gratuiti utili

| Servizio | Spazio gratuito | Note |
|---|---|---|
| **Box Personal** | 10 GB | Max 250 MB/file — perfetto per JPEG. Box Drive client gratuito |
| Google Drive | 15 GB | Condivisi con Gmail; rclone su Linux |
| OneDrive | 5 GB | Integrazione Windows nativa |
| Dropbox | 2 GB | Poco; Autosync Dropbox limitato su Android 11+ |

Consigliato: **Box** (spazio + client desktop solido + buon supporto multipiattaforma).

---

## Setup FolderSync (telefono → Box)

### 1. Crea l'account Box e la cartella di destinazione
- Vai su [box.com](https://www.box.com/pricing/individual) → "Individual Free Plan"
- Crea sul cloud una cartella tipo `/AP_Consulting/foto_blocchi/`

### 2. Installa FolderSync sul telefono
- Play Store → **FolderSync Lite** (gratis con qualche ad) o **FolderSync** (€3, no ads)

### 3. Configura l'account Box in FolderSync
- Apri FolderSync → menu laterale → **Accounts** → **+** (Add account)
- Scegli **Box** dalla lista
- Tap "Authenticate" → si apre browser → login Box → autorizza
- Torna in FolderSync, l'account è registrato

### 4. Crea il folderpair
- Menu → **Folderpairs** → **+**
- **Account**: scegli quello Box appena creato
- **Sync type**: **To remote folder** (upload-only — consigliato; bidirectional
  rischia cancellazioni accidentali)
- **Remote folder**: naviga fino a `/AP_Consulting/foto_blocchi/`
- **Local folder**: naviga fino a `Pictures/FotoBlocchi/` (o il nome che hai
  configurato nelle impostazioni dell'app)
- **Sync interval**: "Instant sync" (consigliato — appena viene scritto un file
  parte l'upload) oppure ogni ora se vuoi battery-friendly
- **Sync subfolders**: ON
- **Only on Wi-Fi**: scelta tua. Se la copertura 4G è buona e i piani dati
  abbondanti, lascia OFF per upload immediato; altrimenti ON
- **Save** → da quel momento le nuove foto vengono caricate automaticamente

### 5. Test
- Apri Foto Blocchi → scatta una foto su una commessa di test
- Apri box.com da browser → la foto deve apparire nella cartella entro 30-60 secondi

---

## Setup ricezione sul server aziendale

Hai diverse opzioni a seconda dell'OS del server e del livello di automazione desiderato.

### Opzione A — Box Drive (Windows, zero codice)

Box Drive è il client desktop ufficiale, gratuito, che sincronizza le cartelle Box
con il filesystem locale come fa Dropbox.

1. Sul server Windows scarica Box Drive da [box.com/resources/downloads](https://www.box.com/resources/downloads)
2. Installa e fai login con lo stesso account Box del telefono
3. In Esplora Risorse appare un drive virtuale `Box (X:)`
4. Naviga a `Box (X:)\AP_Consulting\foto_blocchi\` → click destro →
   "Make available offline" (così i file sono sempre sul disco, non solo on-demand)
5. Eventualmente condividi la cartella locale come share di rete
   (`\\SERVER\foto_blocchi`) per renderla accessibile ad altre macchine o
   integrazioni Mago4

Box Drive gira come servizio in background, gestisce conflitti, e sincronizza in
tempo reale.

### Opzione B — rclone (Linux, anche Windows)

Per server senza GUI o se preferisci un tool da riga di comando:

```bash
# Installa rclone (Linux)
curl https://rclone.org/install.sh | sudo bash

# Configura il remote Box
rclone config
# scegli "n" → name: box → type: "box" → segui wizard OAuth

# Sync periodica (esempio crontab ogni 5 minuti)
*/5 * * * * rclone copy box:AP_Consulting/foto_blocchi /var/foto_blocchi --log-file=/var/log/rclone.log
```

`rclone copy` è incrementale: scarica solo file nuovi/modificati.

### Opzione C — Script Python con Box SDK

Se vuoi controllo programmatico (es. spostare file in cartelle per commessa,
caricare su SQL Server BLOB, inviare notifiche), scrivi un piccolo script Python
con `boxsdk` schedulato come Task. Indicativo:

```python
from boxsdk import OAuth2, Client
oauth = OAuth2(client_id="...", client_secret="...", access_token="...")
client = Client(oauth)
folder = client.folder(folder_id="<id_foto_blocchi>")
for item in folder.get_items():
    if item.type == "file" and not os.path.exists(local_path(item.name)):
        with open(local_path(item.name), "wb") as f:
            item.download_to(f)
```

(Per produzione meglio JWT app authentication invece che user token.)

---

## Personalizzazioni rapide

| Cosa | Come |
|---|---|
| Nome app (label launcher e toolbar) | `app/src/main/res/values/strings.xml` → `app_name` |
| Cartella di salvataggio | App → Impostazioni → "Nome cartella" |
| Cartella di default per nuovi install | `SettingsManager.DEFAULT_FOLDER_NAME` |
| Pattern del nome file | `PhotoStorage.buildContentValues()` |
| Colori del tema | `colors.xml` |
| Versione mostrata | `app/build.gradle.kts` → `versionName`, `versionCode` |

Dopo qualsiasi modifica al codice, `git push` → GitHub Actions ricompila → scarichi
l'APK aggiornato.

---

## Specifiche tecniche

- **Linguaggio**: Kotlin 2.0
- **Min SDK**: 29 (Android 10) — necessario per MediaStore RELATIVE_PATH
- **Target SDK**: 35 (Android 15)
- **Fotocamera**: CameraX 1.4.0
- **Build**: Gradle 8.10.2, AGP 8.6.1
- **JDK richiesto in CI**: 17
- **Package**: `it.apconsulting.fotocommesse` (invariato per upgrade in-place)
- **Versione**: 1.1 (versionCode 2)

---

## Struttura del progetto

```
fotoblocchi/
├── .github/workflows/build.yml       # CI: genera l'APK ad ogni push
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/it/apconsulting/fotocommesse/
│       │   ├── MainActivity.kt       # Home: input commessa + lista recenti
│       │   ├── CameraActivity.kt     # Fotocamera CameraX + scatto
│       │   ├── SettingsActivity.kt   # Schermata impostazioni
│       │   ├── SettingsManager.kt    # SharedPreferences wrapper
│       │   ├── PhotoStorage.kt       # MediaStore lettura/scrittura
│       │   └── CommesseAdapter.kt    # Adapter RecyclerView
│       └── res/                      # Layout, strings, theme, icone, menu
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── README.md
```

---

## Note finali

- L'app crea la cartella di destinazione al primo scatto
- Le foto includono EXIF standard (data, orientamento). GPS NON incluso di default
- Il contatore "prossima foto" è derivato da `max(N)+1` scandendo i file
  presenti per quella commessa nella cartella configurata
- Cambiando cartella in Impostazioni, le commesse della cartella precedente
  non appariranno più nella lista "recenti" (i file restano sul telefono nella
  vecchia posizione)
