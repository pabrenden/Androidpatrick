# Min timeplan (Android)

En enkel norsk Android-app for halvtimeplan, med elev/aktivitet, notater og lokal lagring.

**Viktig:** Varsler i denne prototypen fungerer kun mens appen kjører i forgrunnen. Varsler ved lukket app krever Android AlarmManager/WorkManager og er ikke implementert.

## Bygg APK

Åpne prosjektet i Android Studio og bygg `assembleDebug`, eller last prosjektet opp i et GitHub-repository og kjør GitHub Actions -> Build APK -> Run workflow. Last ned `MinTimeplan-APK` under Artifacts. Pakk ut ZIP-filen og installer `app-debug.apk` på Android-telefonen.

Dette prosjektet er ikke kompilert eller testet i dette miljøet.

Ikke lagre sensitive elevopplysninger i prototypen. Avklar skolens regler for personvern før bruk.
