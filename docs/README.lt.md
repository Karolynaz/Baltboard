# Baltboard

Moderni lietuvių ir anglų klaviatūra Android telefonui, sukurta remiantis [AnySoftKeyboard](https://github.com/AnySoftKeyboard/AnySoftKeyboard).

[Atsisiųsti APK](https://github.com/Karolynaz/Baltboard/releases/download/v2.0.35/Baltboard-v2.0.35.apk) · [Leidimai](https://github.com/Karolynaz/Baltboard/releases) · [English](../README.md)

## Galimybės

- Lietuvių ir anglų QWERTY išdėstymai bei integruoti žodynai.
- Atskira lietuviškų raidžių eilutė; anglų išdėstyme – skaičių eilutė.
- Kalbos keitimas braukiant per tarpo klavišą ir paskutinės kalbos įsiminimas.
- Day, Night ir Auto temos. Auto atsižvelgia į Android tamsų režimą.
- Reguliuojami žodžių siūlymai, automatinis taisymas ir rašymas braukiant.
- Iki 20 tekstų iškarpinėje, du stulpeliai ir pasirinktinai trinami įrašai. Pakartotinai nukopijuotas tekstas perkeliamas į priekį.
- Laikinas ką tik nukopijuoto teksto pasiūlymas su uždarymo mygtuku.
- Emoji kategorijos ir paskutiniai naudoti emoji; pasirinkus emoji nebūtina grįžti į raidžių klaviatūrą.
- Pasirenkamas AI teksto taisymas ir prasminis vertimas į anglų kalbą.
- AI rezultato peržiūra prieš kopijuojant arba pakeičiant įvestą tekstą.

Nuostatų kalba priklauso nuo **Android sistemos kalbos**, ne nuo to, ar šiuo metu rašote LT, ar EN išdėstymu.

## Įdiegimas

Reikia **Android 6.0 arba naujesnės versijos**.

1. Atsisiųskite `Baltboard-v2.0.35.apk` ir atidarykite telefone.
2. Jei Android paprašo, leiskite naršyklei arba failų tvarkyklei įdiegti programą. Vėliau šį leidimą galite išjungti.
3. Atidarykite Baltboard ir skiltyje **Informacija → Klaviatūros įjungimas** sekite instrukciją.
4. Pasirinkite Baltboard kaip aktyvią klaviatūrą.
5. Braukite per tarpo klavišą, kad persijungtumėte tarp LT ir EN.

Atskiro „Lithuanian for AnySoftKeyboard“ paketo nereikia.

Tai **testavimo leidimas**, ne Google Play versija. APK pasirašytas vietiniu Android debug sertifikatu, tačiau nėra debuggable. Atnaujinant esamą programą būtinas toks pats pasirašymo sertifikatas.

## AI

AI nėra privalomas. Nuostatose **AI taisymas ir vertimas** galite pasirinkti tiekėją, įrašyti savo API raktą ir modelį bei atskirai įjungti taisymą ir vertimą. Numatytasis tiekėjas – Gemini. Ryšio testas ir rakto pašalinimas yra API lauko **⋯** meniu.

Tikrinamas ar verčiamas tekstas siunčiamas pasirinktam tiekėjui tik paleidus atitinkamą veiksmą. Prieš pakeisdami tekstą peržiūrėkite rezultatą. Paslaugos kainos, modelių prieiga ir limitai priklauso nuo tiekėjo.

## Šaltinis ir klaidų pranešimai

Visas šios versijos kodas pridėtas prie leidimo kaip [Baltboard-v2.0.35-source.zip](https://github.com/Karolynaz/Baltboard/releases/download/v2.0.35/Baltboard-v2.0.35-source.zip). Kol kas pagrindinėje GitHub šakoje pateikiamas pristatymas; viso šaltinio įkėlimui dar reikia sutvarkyti vietinio Git prisijungimo teises. Automatiškai sugeneruoti GitHub „Source code“ archyvai nėra pilnas programos šaltinis.

Klaidas aprašykite [Issues](https://github.com/Karolynaz/Baltboard/issues): telefono modelis, Android ir Baltboard versijos, programėlė bei atkūrimo žingsniai. Neskelbkite API raktų ar asmeninių tekstų.

## Autoriai ir licencija

Projektas paremtas AnySoftKeyboard. Dėkojame originaliems kūrėjams ir žodynų autoriams. Day/Night klaviatūros temų dizainas remiasi Algimanto Yochees temomis.

[Apache License 2.0](../LICENSE). Baltboard nėra oficialus AnySoftKeyboard leidimas.
