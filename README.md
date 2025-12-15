# Android Bridge (Health Connect writer)
## Cosa fa
- Apre link `glucosebridge://import?batch=<id>`
- Scarica `GET <server>/api/batch.php?batch=<id>`
- Inserisce record `BloodGlucoseRecord` in Health Connect

## Configurazione
- Modifica `serverBaseUrl` in `MainActivity.kt` con l'URL della tua webapp.

## Permessi
- L'app chiede il permesso di scrittura su `BloodGlucoseRecord` tramite Health Connect.

## Build senza Android Studio (opzione)
- Puoi caricare questa cartella su un repo GitHub e usare GitHub Actions per buildare un APK.
